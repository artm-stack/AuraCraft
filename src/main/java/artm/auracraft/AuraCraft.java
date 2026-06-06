package artm.auracraft;

import artm.auracraft.payload.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class AuraCraft implements ModInitializer {

    public static final String MOD_ID = "auracraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final GameRuleCategory AURACRAFT_CATEGORY =
            GameRuleCategory.register(Identifier.fromNamespaceAndPath(MOD_ID, "auracraft"));

    public static final GameRule<Integer> MAX_AURAS =
            GameRuleBuilder.forInteger(3)
                    .category(AURACRAFT_CATEGORY)
                    .buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "max_auras"));
    public static final GameRule<Boolean> DROP_PLUS_ON_KILL =
            GameRuleBuilder.forBoolean(true)
                    .category(AURACRAFT_CATEGORY)
                    .buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "drop_plus_on_kill"));
    public static final GameRule<Boolean> ENABLE_HEART_RECIPE =
            GameRuleBuilder.forBoolean(true)
                    .category(AURACRAFT_CATEGORY)
                    .buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "enable_heart_recipe"));

    // Recipe key gated by ENABLE_HEART_RECIPE (checked in RecipeMapMixin)
    public static final ResourceKey<Recipe<?>> HEART_RECIPE_KEY =
            ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(MOD_ID, "heart_of_the_sea"));

    @Override
    public void onInitialize() {
        Config.load();
        Items.init();
        registerPayloads();
        registerPayloadReceivers();
        registerEvents();
        registerCommands();

        LOGGER.info("AuraCraft initialized!");
    }

    // Payload Registration
    private void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay()
                .register(ChooseAuraPayload.TYPE, ChooseAuraPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(ClientHelloPayload.TYPE, ClientHelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay()
                .register(PromptAuraPayload.TYPE, PromptAuraPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay()
                .register(SyncAuraPayload.TYPE, SyncAuraPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay()
                .register(UiStatePayload.TYPE, UiStatePayload.CODEC);
    }

    // Payload Receivers
    private void registerPayloadReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ChooseAuraPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (isUiDisabled(context.server())) {
                        context.player().sendSystemMessage(
                                Component.translatable("message.auracraft.ui_disabled"));
                        return;
                    }
                    String auraId = normalizeAuraId(payload.auraId());
                    if (auraId == null || !isKnownAuraId(auraId)) {
                        context.player().sendSystemMessage(
                                Component.translatable("message.auracraft.invalid_aura")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (!Config.get().isAuraEnabled(auraId)) {
                        context.player().sendSystemMessage(
                                Component.translatable("message.auracraft.aura_disabled")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    handleAuraSelection(context.player(), auraId);
                }));

        ServerPlayNetworking.registerGlobalReceiver(ClientHelloPayload.TYPE,
                (payload, context) -> {
                    if (payload.protocolVersion() != ClientHelloPayload.PROTOCOL_VERSION) {
                        LOGGER.warn("Player {} connected with AuraCraft protocol v{} (expected v{}), disconnecting",
                                context.player().getName().getString(),
                                payload.protocolVersion(),
                                ClientHelloPayload.PROTOCOL_VERSION);
                        context.server().execute(() ->
                                context.player().connection.disconnect(
                                        Component.translatable("message.auracraft.version_mismatch",
                                                ClientHelloPayload.PROTOCOL_VERSION,
                                                payload.protocolVersion())));
                    }
                });
    }

    // Events
    private void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            syncUiState(player);
            syncAuras(player);
            if (!isUiDisabled(server) && getAuraPoints(player) > 0) {
                sendIfSupported(player, PromptAuraPayload.INSTANCE);
            }
            reapplyAuras(player);
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
                (world, entity, killedEntity, damageSource) -> {
                    if (!(entity instanceof ServerPlayer killer)) return;
                    if (!(killedEntity instanceof ServerPlayer victim)) return;

                    List<String> list = getAuraList(victim);
                    int points = getAuraPoints(victim);
                    String lostId = null;
                    boolean lostUpgrade = false;
                    boolean lostPoint = false;

                    if (list.isEmpty() && points > 0) {
                        setAuraPoints(victim, points - 1);
                        lostPoint = true;
                    } else if (!list.isEmpty()) {
                        lostId = removeLastAuraEntry(victim, 1);
                        if (lostId != null) {
                            lostUpgrade = getAuraList(victim).contains(lostId);
                            pushToRestorationQueue(victim, lostId);
                        }
                    }

                    boolean droppedPlus = false;
                    if (world.getGameRules().get(AuraCraft.DROP_PLUS_ON_KILL)
                            && (lostId != null || lostPoint)) {
                        killedEntity.spawnAtLocation(world,
                                new ItemStack(Items.AURA_PLUS));
                        droppedPlus = true;
                    }

                    syncAuras(victim);
                    syncAuras(killer);

                    if (lostId != null) {
                        victim.sendSystemMessage(lostUpgrade
                                ? Component.translatable("message.auracraft.pvp_lost_upgrade",
                                auraNameComponent(lostId)).withStyle(ChatFormatting.WHITE)
                                : Component.translatable("message.auracraft.pvp_lost_aura",
                                auraNameComponent(lostId)).withStyle(ChatFormatting.WHITE));
                    } else if (lostPoint) {
                        victim.sendSystemMessage(Component.translatable(
                                        "message.auracraft.pvp_lost_point")
                                .withStyle(ChatFormatting.WHITE));
                    }
                    if (droppedPlus) {
                        killer.sendSystemMessage(Component.translatable(
                                        "message.auracraft.kill_dropped_plus")
                                .withStyle(ChatFormatting.WHITE));
                    }
                });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                reapplyAuras(player);
            }
        });

        // Re-populate config defaults after all mods have registered their effects
        // (onInitialize runs too early to catch effects from late-loading mods)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Config.get().applyDefaults();
            Config.get().save();
        });
    }

    // Commands
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(buildRootCommand()));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildRootCommand() {
        return literal("aura")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                // /aura — toggle UI
                .executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    boolean nowDisabled = !isUiDisabled(server);
                    setUiDisabled(server, nowDisabled);
                    ctx.getSource().sendSuccess(() -> Component.translatable(
                            nowDisabled ? "command.auracraft.ui_disabled" : "command.auracraft.ui_enabled"), true);
                    return 1;
                })
                // /aura status [player]
                .then(literal("status")
                        .executes(ctx -> {
                            try {
                                sendStatus(ctx.getSource(),
                                        ctx.getSource().getPlayerOrException());
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(
                                        Component.translatable("command.auracraft.not_a_player"));
                            }
                            return 1;
                        })
                        .then(argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    sendStatus(ctx.getSource(),
                                            EntityArgument.getPlayer(ctx, "player"));
                                    return 1;
                                })))
                // /aura reset [player]  – refunds all points
                .then(literal("reset")
                        .executes(ctx -> {
                            try {
                                ServerPlayer self = ctx.getSource().getPlayerOrException();
                                int result = resetPlayer(self);
                                if (result > 0) {
                                    final String name = self.getName().getString();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("command.auracraft.reset.success", name), true);
                                }
                                return result;
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.translatable("command.auracraft.not_a_player"));
                                return 0;
                            }
                        })
                        .then(argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int result = resetPlayer(target);
                                    if (result > 0) {
                                        ctx.getSource().sendSuccess(() -> Component.translatable(
                                                "command.auracraft.reset.success", target.getName().getString()), true);
                                    }
                                    return result;
                                })))
                // /aura restart [player]  – hard reset to 1 point (starting state)
                .then(literal("restart")
                        .executes(ctx -> {
                            try {
                                ServerPlayer self = ctx.getSource().getPlayerOrException();
                                restartPlayer(self);
                                ctx.getSource().sendSuccess(() -> Component.translatable(
                                        "command.auracraft.restart.success", self.getName().getString()), true);
                                return 1;
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.translatable("command.auracraft.not_a_player"));
                                return 0;
                            }
                        })
                        .then(argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    restartPlayer(target);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "command.auracraft.restart.success", target.getName().getString()), true);
                                    return 1;
                                })))
                // /aura plus [player] [amount]
                .then(literal("plus")
                        .executes(ctx -> {
                            try {
                                return addPointsCommand(ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(), 1);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(
                                        Component.translatable("command.auracraft.not_a_player"));
                                return 0;
                            }
                        })
                        .then(argument("amount",
                                IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    try {
                                        return addPointsCommand(ctx.getSource(),
                                                ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "amount"));
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(
                                                Component.translatable("command.auracraft.not_a_player"));
                                        return 0;
                                    }
                                }))
                        .then(argument("player", EntityArgument.player())
                                .executes(ctx -> addPointsCommand(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"), 1))
                                .then(argument("amount",
                                        IntegerArgumentType.integer(1))
                                        .executes(ctx -> addPointsCommand(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(
                                                        ctx, "amount"))))))
                // /aura remove [player] [amount]
                .then(literal("remove")
                        .executes(ctx -> {
                            try {
                                return removePointsCommand(ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(), 1);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(
                                        Component.translatable("command.auracraft.not_a_player"));
                                return 0;
                            }
                        })
                        .then(argument("amount",
                                IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    try {
                                        return removePointsCommand(ctx.getSource(),
                                                ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "amount"));
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(
                                                Component.translatable("command.auracraft.not_a_player"));
                                        return 0;
                                    }
                                }))
                        .then(argument("player", EntityArgument.player())
                                .executes(ctx -> removePointsCommand(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"), 1))
                                .then(argument("amount",
                                        IntegerArgumentType.integer(1))
                                        .executes(ctx -> removePointsCommand(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(
                                                        ctx, "amount"))))))
                // /aura withdraw [amount]  – converts points/auras into AURA_PLUS items
                .then(literal("withdraw")
                        .executes(ctx -> {
                            try {
                                return withdrawPlayer(ctx.getSource().getPlayerOrException(), 1);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.translatable("command.auracraft.not_a_player"));
                                return 0;
                            }
                        })
                        .then(argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    try {
                                        return withdrawPlayer(ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "amount"));
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(Component.translatable("command.auracraft.not_a_player"));
                                        return 0;
                                    }
                                })));
    }


    // API
    public static int getTotalLoad(ServerPlayer player) {
        return getAuraList(player).size() + getAuraPoints(player);
    }

    public static int getMaxCapacity(MinecraftServer server) {
        return server.overworld().getGameRules().get(AuraCraft.MAX_AURAS);
    }

    public static void grantAuraPoint(ServerPlayer player) {
        if (getTotalLoad(player) >= getMaxCapacity(player.level().getServer())) return;
        setAuraPoints(player, getAuraPoints(player) + 1);
        syncAuras(player);
        playPointGainSound(player);

        // Auto-restore the last PvP-lost aura if one is queued
        String restore = popFromRestorationQueue(player);
        if (restore != null && isKnownAuraId(restore) && Config.get().isAuraEnabled(restore)) {
            restoreAura(player, restore);
            return;
        }

        if (!isUiDisabled(player.level().getServer())) {
            sendIfSupported(player, PromptAuraPayload.INSTANCE);
        }
    }

    private static void restoreAura(ServerPlayer player, String auraId) {
        List<String> list = getAuraList(player);
        list.add(auraId);
        setAuraList(player, list);
        setAuraPoints(player, getAuraPoints(player) - 1);
        applyAura(player, auraId, Collections.frequency(list, auraId) - 1);
        syncAuras(player);
        playChooseSound(player);
        player.sendSystemMessage(Component.translatable(
                "message.auracraft.aura_restored",
                auraNameComponent(auraId)).withStyle(ChatFormatting.GREEN));
    }

    public static void handleAuraSelection(ServerPlayer player, String auraId) {
        List<String> list = getAuraList(player);
        int points = getAuraPoints(player);
        if (points <= 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.auracraft.no_points").withStyle(ChatFormatting.YELLOW));
            syncAuras(player);
            return;
        }
        int currentCount = Collections.frequency(list, auraId);
        int maxUpg = Config.get().getMaxUpgradeLevel(auraId);

        if (currentCount > 0) {
            if (currentCount > maxUpg) {
                player.sendSystemMessage(Component.translatable(
                        "message.auracraft.upgrade_cap",
                        auraNameComponent(auraId)));
                syncAuras(player);
                return;
            }
            list.add(auraId);
            setAuraList(player, list);
            setAuraPoints(player, points - 1);
            applyAura(player, auraId, currentCount);
            syncAuras(player);
            playChooseSound(player);
            player.sendSystemMessage(Component.translatable(
                    "message.auracraft.aura_upgraded",
                    auraNameComponent(auraId), currentCount + 1));
            return;
        }

        Set<String> unique = new LinkedHashSet<>(list);
        int maxAuras = getMaxCapacity(player.level().getServer());
        if (unique.size() >= maxAuras) {
            player.sendSystemMessage(Component.translatable(
                    "message.auracraft.max_auras",
                    maxAuras).withStyle(ChatFormatting.RED));
            syncAuras(player);
            return;
        }
        list.add(auraId);
        setAuraList(player, list);
        setAuraPoints(player, points - 1);
        applyAura(player, auraId, 0);
        syncAuras(player);
        playChooseSound(player);
        player.sendSystemMessage(Component.translatable(
                "message.auracraft.aura_chosen",
                auraNameComponent(auraId)).withStyle(ChatFormatting.WHITE));
    }

    public static void reapplyAuras(ServerPlayer player) {
        List<String> list = getAuraList(player);
        Set<String> unique = new LinkedHashSet<>(list);
        for (String auraId : unique) {
            int amplifier = Collections.frequency(list, auraId) - 1;
            applyAura(player, auraId, amplifier);
        }
    }

    public static void syncAuras(ServerPlayer player) {
        Map<String, Integer> upgradeLevels = new HashMap<>();
        for (String id : getEnabledAuraIds()) {
            upgradeLevels.put(id, Config.get().getMaxUpgradeLevel(id));
        }
        sendIfSupported(player, new SyncAuraPayload(
                new ArrayList<>(getAuraList(player)),
                new ArrayList<>(getCappedAuras(player)),
                getAuraPoints(player),
                upgradeLevels
        ));
    }

    public static void syncUiState(ServerPlayer player) {
        sendIfSupported(player, new UiStatePayload(
                isUiDisabled(player.level().getServer()),
                new ArrayList<>(getEnabledAuraIds())
        ));
    }

    // Returns newPoints on success, 0 if the player had nothing to reset.
    public static int resetPlayer(ServerPlayer player) {
        List<String> list  = getAuraList(player);
        int points         = getAuraPoints(player);
        if (list.isEmpty() && points == 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.auracraft.reset_no_auras").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        int refunded = list.size() + points;
        clearAllAurasAndEffects(player);
        MinecraftServer server = player.level().getServer();
        int newPoints = Math.min(refunded, getMaxCapacity(server));
        setAuraPoints(player, newPoints);
        syncAuras(player);
        if (!isUiDisabled(server)) {
            sendIfSupported(player, PromptAuraPayload.INSTANCE);
        }
        return newPoints;
    }

    public static int restartPlayer(ServerPlayer player) {
        clearAllAurasAndEffects(player);
        setAuraPoints(player, 1);
        syncAuras(player);
        if (!isUiDisabled(player.level().getServer())) {
            sendIfSupported(player, PromptAuraPayload.INSTANCE);
        }
        return 1;
    }

    private static int withdrawPlayer(ServerPlayer player, int amount) {
        int points    = getAuraPoints(player);
        int auraCount = getAuraList(player).size();
        int total     = points + auraCount;

        if (total <= 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.auracraft.withdraw_no_auras").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        int toWithdraw     = Math.min(amount, total);
        int withdrawPoints = Math.min(toWithdraw, points);
        int withdrawAuras  = toWithdraw - withdrawPoints;

        if (withdrawPoints > 0) setAuraPoints(player, points - withdrawPoints);
        for (int i = 0; i < withdrawAuras; i++) {
            String removed = removeLastAuraEntry(player, 1);
            if (removed != null) pushToRestorationQueue(player, removed);
        }

        // Give one AURA_PLUS item per withdrawn point (stack size is 1)
        for (int i = 0; i < toWithdraw; i++) {
            player.addItem(new ItemStack(Items.AURA_PLUS));
        }

        syncAuras(player);
        player.sendSystemMessage(Component.translatable(
                "message.auracraft.withdrawn", toWithdraw).withStyle(ChatFormatting.WHITE));
        return 1;
    }

    public static boolean isUiDisabled(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(WorldData.TYPE).isUIDisabled();
    }

    public static void setUiDisabled(MinecraftServer server, boolean disabled) {
        WorldData data = server.overworld().getDataStorage()
                .computeIfAbsent(WorldData.TYPE);
        data.setUiDisabled(disabled);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncUiState(player);
            if (disabled) {
                player.sendSystemMessage(
                        Component.translatable("message.auracraft.aura.ui_disabled"));
            } else if (getAuraPoints(player) > 0) {
                sendIfSupported(player, PromptAuraPayload.INSTANCE);
            }
        }
    }

    // Aura List Helpers
    public static List<String> getAuraList(ServerPlayer player) {
        PlayerAuraData data = (PlayerAuraData) player;
        String raw = data.auracraft$getChosenAuras();
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String id = normalizeAuraId(part.trim());
            if (id != null && isKnownAuraId(id)) list.add(id);
        }
        return list;
    }

    public static void setAuraList(ServerPlayer player, List<String> list) {
        PlayerAuraData data = (PlayerAuraData) player;
        if (list == null || list.isEmpty()) {
            data.auracraft$setChosenAuras(null);
            return;
        }
        data.auracraft$setChosenAuras(String.join(",", list));
    }

    public static void clearAllAurasAndEffects(ServerPlayer player) {
        List<String> list = getAuraList(player);
        new LinkedHashSet<>(list).forEach(id -> removeAura(player, id));
        setAuraList(player, new ArrayList<>());
        setRestorationQueue(player, new ArrayList<>());
    }

    private static String removeLastAuraEntry(ServerPlayer player, int count) {
        List<String> list = getAuraList(player);
        if (list.isEmpty()) return null;
        String removeId = null;
        for (int i = 0; i < count && !list.isEmpty(); i++) {
            removeId = list.removeLast();
            int remaining = Collections.frequency(list, removeId);
            if (remaining == 0) {
                removeAura(player, removeId);
            } else {
                applyAura(player, removeId, remaining - 1);
            }
        }
        setAuraList(player, list);
        return removeId;
    }

    private static Set<String> getCappedAuras(ServerPlayer player) {
        List<String> list = getAuraList(player);
        return new LinkedHashSet<>(list).stream()
                .filter(id -> Collections.frequency(list, id) > Config.get().getMaxUpgradeLevel(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Restoration Queue Helpers
    private static List<String> getRestorationQueue(ServerPlayer player) {
        PlayerAuraData data = (PlayerAuraData) player;
        String raw = data.auracraft$getRestorationQueue();
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        List<String> queue = new ArrayList<>();
        for (String part : raw.split(",")) {
            String id = normalizeAuraId(part.trim());
            if (id != null && isKnownAuraId(id)) queue.add(id);
        }
        return queue;
    }

    private static void setRestorationQueue(ServerPlayer player, List<String> queue) {
        PlayerAuraData data = (PlayerAuraData) player;
        if (queue == null || queue.isEmpty()) {
            data.auracraft$setRestorationQueue(null);
            return;
        }
        data.auracraft$setRestorationQueue(String.join(",", queue));
    }

    private static void pushToRestorationQueue(ServerPlayer player, String auraId) {
        List<String> queue = getRestorationQueue(player);
        queue.add(0, auraId);
        setRestorationQueue(player, queue);
    }

    private static String popFromRestorationQueue(ServerPlayer player) {
        List<String> queue = getRestorationQueue(player);
        if (queue.isEmpty()) return null;
        String auraId = queue.remove(0);
        setRestorationQueue(player, queue);
        return auraId;
    }

    // Points Helpers
    public static int getAuraPoints(ServerPlayer player) {
        PlayerAuraData data = (PlayerAuraData) player;
        String raw = data.auracraft$getAuraPoints();
        if (raw == null || raw.isBlank()) {
            return getAuraList(player).isEmpty() ? 1 : 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void setAuraPoints(ServerPlayer player, int points) {
        PlayerAuraData data = (PlayerAuraData) player;
        data.auracraft$setAuraPoints(Integer.toString(Math.max(0, points)));
    }

    // Aura Application
    private static void applyAura(ServerPlayer player, String auraId, int amplifier) {
        String normalized = normalizeAuraId(auraId);
        if (normalized == null) return;
        Identifier id = Identifier.parse(normalized);
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(id)) return;
        Holder.Reference<?> ref = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (ref == null) return;
        @SuppressWarnings("unchecked")
        Holder.Reference<MobEffect> effectRef = (Holder.Reference<MobEffect>) ref;
        int amp = Math.max(0, amplifier);
        MobEffectInstance current = player.getEffect(effectRef);
        if (current == null || current.getDuration() < 200 || current.getAmplifier() != amp) {
            player.addEffect(new MobEffectInstance(effectRef, 600, amp, true, false, true));
        }
    }

    private static void removeAura(ServerPlayer player, String auraId) {
        String normalized = normalizeAuraId(auraId);
        if (normalized == null) return;
        Identifier id = Identifier.parse(normalized);
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(id)) return;
        Holder.Reference<?> ref = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (ref == null) return;
        @SuppressWarnings("unchecked")
        Holder.Reference<MobEffect> effectRef = (Holder.Reference<MobEffect>) ref;
        player.removeEffect(effectRef);
    }

    // Identifier Helpers
    public static String normalizeAuraId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim().toLowerCase();
        if (!trimmed.contains(":")) trimmed = "minecraft:" + trimmed;
        Identifier id = Identifier.tryParse(trimmed);
        return id == null ? null : id.toString();
    }

    public static boolean isKnownAuraId(String auraId) {
        String normalized = normalizeAuraId(auraId);
        if (normalized == null) return false;
        return BuiltInRegistries.MOB_EFFECT.containsKey(Identifier.parse(normalized));
    }

    public static Component auraNameComponent(String auraId) {
        String normalized = normalizeAuraId(auraId);
        if (normalized == null) return Component.literal(auraId);
        Identifier id = Identifier.parse(normalized);
        return Component.translatable(
                "effect." + id.getNamespace() + "." + id.getPath());
    }

    private static Set<String> getEnabledAuraIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            String auraId = id.toString();
            if (Config.get().isAuraEnabled(auraId)) ids.add(auraId);
        }
        return ids;
    }

    // Command Helpers
    private static int addPointsCommand(CommandSourceStack source, ServerPlayer target, int amount) {
        int current  = getAuraPoints(target);
        int load     = getTotalLoad(target);
        int capacity = getMaxCapacity(source.getServer());
        if (load >= capacity) {
            source.sendFailure(Component.translatable("command.auracraft.plus.full",
                    target.getName().getString()));
            return 0;
        }
        int added = Math.min(amount, capacity - load);
        setAuraPoints(target, current + added);
        syncAuras(target);
        playPointGainSound(target);
        if (!isUiDisabled(target.level().getServer())) {
            sendIfSupported(target, PromptAuraPayload.INSTANCE);
        }
        source.sendSuccess(() -> Component.translatable("command.auracraft.plus.success",
                added, target.getName().getString()), true);
        target.sendSystemMessage(Component.translatable(
                "message.auracraft.points_received", added).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int removePointsCommand(CommandSourceStack source, ServerPlayer target, int amount) {
        int points = getAuraPoints(target);
        int auraCount = getAuraList(target).size();
        int total = points + auraCount;

        if (total <= 0) {
            source.sendFailure(Component.translatable("command.auracraft.remove.nothing",
                    target.getName().getString()));
            return 0;
        }

        int toRemove = Math.min(amount, total);
        int removedPoints = Math.min(toRemove, points);
        int removedAuras  = toRemove - removedPoints;

        if (removedPoints > 0) setAuraPoints(target, points - removedPoints);
        if (removedAuras  > 0) removeLastAuraEntry(target, removedAuras);

        syncAuras(target);
        final int removed = toRemove;
        source.sendSuccess(() -> Component.translatable("command.auracraft.remove.success",
                removed, target.getName().getString()), true);
        return 1;
    }

    private static void sendStatus(CommandSourceStack source, ServerPlayer target) {
        List<String> list = getAuraList(target);
        Set<String> unique = new LinkedHashSet<>(list);
        int points = getAuraPoints(target);

        final Component auraList;
        if (unique.isEmpty()) {
            auraList = Component.translatable("command.auracraft.status.none");
        } else {
            MutableComponent joined = Component.empty();
            boolean first = true;
            for (String id : unique) {
                if (!first) joined = joined.append(Component.literal(", "));
                joined = joined.append(auraNameComponent(id));
                first = false;
            }
            auraList = joined;
        }

        source.sendSuccess(() -> Component.translatable("command.auracraft.status.header"), false);
        source.sendSuccess(() -> Component.translatable("command.auracraft.status.player", target.getName().getString()), false);
        source.sendSuccess(() -> Component.translatable("command.auracraft.status.points", points), false);
        source.sendSuccess(() -> Component.translatable("command.auracraft.status.auras", auraList), false);
    }


    // Network & Sound Helpers
    public static void sendIfSupported(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void playPointGainSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.45F, 1.1F);
    }

    private static void playChooseSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.55F, 0.95F);
    }
}
