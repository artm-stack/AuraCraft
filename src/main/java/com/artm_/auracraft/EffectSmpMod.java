package com.artm_.auracraft;

import com.artm_.auracraft.payload.ChooseEffectPayload;
import com.artm_.auracraft.payload.ClientHelloPayload;
import com.artm_.auracraft.payload.PromptEffectPayload;
import com.artm_.auracraft.payload.SyncEffectPayload;
import com.artm_.auracraft.payload.UiStatePayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class EffectSmpMod implements ModInitializer {
    public static final String MOD_ID = "auracraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String CHOSEN_EFFECT_KEY = "auracraft_chosen_effect";
    private static final String CHOSEN_EFFECTS_KEY = "auracraft_chosen_effects";
    private static final String EFFECT_AMPLIFIER_BONUSES_KEY = "auracraft_effect_amplifier_bonuses";
    private static final String SELECTION_TOKENS_KEY = "auracraft_selection_tokens";
    private static final String FIRST_EFFECT_KEY = "auracraft_first_effect";
    private static final int MAX_SELECTION_TOKENS = 3;
    private static final int HANDSHAKE_TIMEOUT_TICKS = 100;
    private static final Map<UUID, Integer> PENDING_HANDSHAKE_DEADLINES = new HashMap<>();
    private static final Set<String> DEFAULT_PLUS_ONE_EFFECTS = Set.of(
        "minecraft:speed",
        "minecraft:haste",
        "minecraft:strength",
        "minecraft:jump_boost",
        "minecraft:resistance"
    );

    public static final Item REPICK_ITEM = new Item(itemProperties("aura_reset").stacksTo(16).rarity(Rarity.RARE)) {
        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer) {
                int selectedCount = getSelectedEffectIds(serverPlayer).size();
                if (selectedCount <= 0) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.auracraft.repick_no_effects").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.FAIL;
                }
                clearAllSelectedEffects(serverPlayer);
                setAvailableSelectionTokens(serverPlayer, selectedCount);
                syncChosenEffect(serverPlayer);
                if (!isUiDisabled(serverPlayer.level().getServer())) {
                    ServerPlayNetworking.send(serverPlayer, PromptEffectPayload.INSTANCE);
                }
                serverPlayer.sendSystemMessage(
                    Component.literal("All effects removed. ").withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("message.auracraft.repick_item_used_suffix", selectedCount).withStyle(ChatFormatting.WHITE))
                );
                if (!serverPlayer.getAbilities().instabuild) {
                    ItemStack stack = player.getItemInHand(hand);
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    };

    public static final Item EXTRA_EFFECT_TOKEN_ITEM = new Item(itemProperties("aura_plus").stacksTo(16).rarity(Rarity.EPIC)) {
        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer) {
                int maxEffects = getEffectPlusCapacity(serverPlayer);
                if (getSelectionLoad(serverPlayer) >= maxEffects) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.auracraft.max_effects_reached", maxEffects).withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
                if (getAvailableSelectionTokens(serverPlayer) >= MAX_SELECTION_TOKENS) {
                    serverPlayer.sendSystemMessage(Component.literal("You already have the max token count (" + MAX_SELECTION_TOKENS + ").").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
                if (getSelectedEffectIds(serverPlayer).size() >= maxEffects && !hasUpgradableSelectedEffect(serverPlayer)) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.auracraft.max_effects_reached", maxEffects).withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }

                boolean restoredFirst = tryRestoreFirstEffect(serverPlayer);
                if (!restoredFirst) {
                    addSelectionTokens(serverPlayer, 1);
                }
                syncChosenEffect(serverPlayer);
                if (!isUiDisabled(serverPlayer.level().getServer())) {
                    ServerPlayNetworking.send(serverPlayer, PromptEffectPayload.INSTANCE);
                }
                if (restoredFirst) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.auracraft.extra_token_restored_first").withStyle(ChatFormatting.WHITE));
                } else {
                    serverPlayer.sendSystemMessage(
                        Component.literal("You gained ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal("+1").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(" effect token.").withStyle(ChatFormatting.WHITE))
                    );
                }
                playTokenGainSound(serverPlayer);

                if (!serverPlayer.getAbilities().instabuild) {
                    ItemStack stack = player.getItemInHand(hand);
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    };

    @Override
    public void onInitialize() {
        EffectSmpConfig.load();
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "aura_reset"), REPICK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "aura_plus"), EXTRA_EFFECT_TOKEN_ITEM);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
            output.accept(new ItemStack(REPICK_ITEM), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            output.accept(new ItemStack(EXTRA_EFFECT_TOKEN_ITEM), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });

        PayloadTypeRegistry.serverboundPlay().register(ChooseEffectPayload.TYPE, ChooseEffectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClientHelloPayload.TYPE, ClientHelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PromptEffectPayload.TYPE, PromptEffectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncEffectPayload.TYPE, SyncEffectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UiStatePayload.TYPE, UiStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ChooseEffectPayload.TYPE, (payload, context) ->
            context.server().execute(() -> {
                if (isUiDisabled(context.server())) {
                    context.player().sendSystemMessage(Component.translatable("message.auracraft.ui_disabled"));
                    return;
                }
                String effectId = normalizeEffectId(payload.effectId());
                if (effectId == null || !isKnownEffectId(effectId)) {
                    context.player().sendSystemMessage(Component.translatable("message.auracraft.invalid_effect").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!EffectSmpConfig.get().isEffectEnabled(effectId)) {
                    context.player().sendSystemMessage(Component.translatable("message.auracraft.effect_disabled_in_config").withStyle(ChatFormatting.RED));
                    return;
                }
                handleEffectSelection(context.player(), effectId);
            })
        );

        ServerPlayNetworking.registerGlobalReceiver(ClientHelloPayload.TYPE, (payload, context) ->
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (payload.protocolVersion() != ClientHelloPayload.PROTOCOL_VERSION) {
                    PENDING_HANDSHAKE_DEADLINES.remove(player.getUUID());
                    player.connection.disconnect(
                        Component.literal("AuraCraft client is outdated. Requires: " + ClientHelloPayload.PROTOCOL_VERSION + ", found: " + payload.protocolVersion())
                    );
                    return;
                }
                PENDING_HANDSHAKE_DEADLINES.remove(player.getUUID());
            })
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            PENDING_HANDSHAKE_DEADLINES.put(player.getUUID(), server.getTickCount() + HANDSHAKE_TIMEOUT_TICKS);
            syncUiState(player);
            syncChosenEffect(player);
            if (!isUiDisabled(server) && getAvailableSelectionTokens(player) > 0) {
                ServerPlayNetworking.send(player, PromptEffectPayload.INSTANCE);
            } else {
                reapplyChosenEffect(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            PENDING_HANDSHAKE_DEADLINES.remove(handler.getPlayer().getUUID())
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(createRootCommand("aura"));
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {
            if (entity instanceof ServerPlayer killer && killedEntity instanceof ServerPlayer) {
                ServerPlayer victim = (ServerPlayer) killedEntity;
                int selectedBefore = getSelectedEffectIds(victim).size();
                int tokensBefore = getAvailableSelectionTokens(victim);
                int effectsLostOnDeath = EffectSmpConfig.get().pvpEffectsLostOnDeath;
                String lost = null;
                boolean lostTokenInstead = false;
                if (effectsLostOnDeath > 0) {
                    if (selectedBefore == 1 && tokensBefore > 0) {
                        setAvailableSelectionTokens(victim, tokensBefore - 1);
                        lostTokenInstead = true;
                    } else {
                        lost = removeLastSelectedEffects(victim, effectsLostOnDeath);
                    }
                }
                boolean droppedPlusItem = false;
                if (EffectSmpConfig.get().dropPlusItemOnPvpKill && (selectedBefore == 0 || lost != null || lostTokenInstead)) {
                    ItemStack drop = new ItemStack(EXTRA_EFFECT_TOKEN_ITEM);
                    killedEntity.spawnAtLocation(world, drop);
                    droppedPlusItem = true;
                }

                syncChosenEffect(victim);
                syncChosenEffect(killer);

                if (lost != null) {
                    victim.sendSystemMessage(
                        Component.translatable("message.auracraft.pvp_lost_effect", effectNameComponent(lost)).withStyle(ChatFormatting.WHITE)
                    );
                } else if (lostTokenInstead) {
                    victim.sendSystemMessage(Component.literal("You were killed lost 1 Aura Plus token.").withStyle(ChatFormatting.WHITE));
                }
                if (droppedPlusItem) {
                    killer.sendSystemMessage(
                        Component.literal("You killed a player. They dropped a ").withStyle(ChatFormatting.WHITE)
                            .append(Component.literal("Aura Plus.").withStyle(ChatFormatting.GREEN))
                    );
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!PENDING_HANDSHAKE_DEADLINES.isEmpty()) {
                List<UUID> toKick = new ArrayList<>();
                int tick = server.getTickCount();
                for (Map.Entry<UUID, Integer> entry : PENDING_HANDSHAKE_DEADLINES.entrySet()) {
                    if (tick >= entry.getValue()) {
                        toKick.add(entry.getKey());
                    }
                }
                for (UUID id : toKick) {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player != null) {
                        player.connection.disconnect(Component.literal("AuraCraft mod is required on client"));
                    }
                    PENDING_HANDSHAKE_DEADLINES.remove(id);
                }
            }

            if (server.getTickCount() % 100 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                reapplyChosenEffect(player);
            }
        });

        LOGGER.info("AuraCraft initialized");
    }

    public static void handleEffectSelection(ServerPlayer player, String selectedId) {
        Set<String> selectedIds = getSelectedEffectIds(player);
        int tokens = getAvailableSelectionTokens(player);
        if (tokens <= 0) {
            player.sendSystemMessage(Component.translatable("message.auracraft.no_tokens").withStyle(ChatFormatting.YELLOW));
            syncChosenEffect(player);
            return;
        }

        if (selectedIds.contains(selectedId)) {
            int maxDuplicateBonus = getMaxDuplicateAmplifierBonus();
            int currentBonus = getAdditionalAmplifierBonus(player, selectedId);
            if (currentBonus >= maxDuplicateBonus) {
                player.sendSystemMessage(Component.translatable("message.auracraft.effect_upgrade_cap", effectNameComponent(selectedId)));
                syncChosenEffect(player);
                return;
            }

            int newBonus = currentBonus + 1;
            setAdditionalAmplifierBonus(player, selectedId, newBonus);
            setAvailableSelectionTokens(player, tokens - 1);
            applyEffectById(player, selectedId, newBonus);
            syncChosenEffect(player);
            playEffectChooseSound(player);
            player.sendSystemMessage(
                Component.literal("Upgraded ").withStyle(ChatFormatting.WHITE)
                    .append(effectNameComponent(selectedId).copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(". Bonus amplifier now ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("+" + (newBonus + 1)).withStyle(ChatFormatting.AQUA))
            );
            return;
        }

        int maxEffects = EffectSmpConfig.get().maxEffects;
        if (selectedIds.size() >= maxEffects) {
            player.sendSystemMessage(Component.translatable("message.auracraft.max_effects_reached", maxEffects).withStyle(ChatFormatting.RED));
            syncChosenEffect(player);
            return;
        }

        selectedIds.add(selectedId);
        if (selectedIds.size() == 1) {
            setFirstEffectId(player, selectedId);
        }
        setAdditionalAmplifierBonus(player, selectedId, 0);
        setSelectedEffectIds(player, selectedIds);
        setAvailableSelectionTokens(player, tokens - 1);
        applyEffectById(player, selectedId, 0);
        syncChosenEffect(player);
        playEffectChooseSound(player);
        player.sendSystemMessage(Component.translatable("message.auracraft.chosen", effectNameComponent(selectedId)).withStyle(ChatFormatting.WHITE));
    }

    public static void reapplyChosenEffect(ServerPlayer player) {
        for (String effectId : getSelectedEffectIds(player)) {
            applyEffectById(player, effectId, getAdditionalAmplifierBonus(player, effectId));
        }
    }

    public static boolean hasChosenEffect(ServerPlayer player) {
        return !getSelectedEffectIds(player).isEmpty();
    }

    public static String getChosenEffect(ServerPlayer player) {
        return getSelectedEffectIds(player).stream().findFirst().orElse(null);
    }

    public static void setChosenEffect(ServerPlayer player, String effectId) {
        if (effectId == null || effectId.isBlank()) {
            setSelectedEffectIds(player, Set.of());
            return;
        }
        setSelectedEffectIds(player, Set.of(effectId));
    }

    public static void syncChosenEffect(ServerPlayer player) {
        ServerPlayNetworking.send(
            player,
            new SyncEffectPayload(
                new ArrayList<>(getSelectedEffectIds(player)),
                new ArrayList<>(getCappedEffectIds(player)),
                new ArrayList<>(getEnabledPickerEffectIds()),
                getAvailableSelectionTokens(player),
                getMaxDuplicateAmplifierBonus()
            )
        );
    }

    public static void resetForRepick(ServerPlayer player) {
        clearAllSelectedEffects(player);
        setAvailableSelectionTokens(player, 1);
        setFirstEffectId(player, null);
        syncChosenEffect(player);
        if (!isUiDisabled(player.level().getServer())) {
            ServerPlayNetworking.send(player, PromptEffectPayload.INSTANCE);
        }
        player.sendSystemMessage(Component.literal("Your effect was reset. Press [Y] to pick again."));
    }

    public static boolean isUiDisabled(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EffectSmpWorldData.TYPE).uiDisabled();
    }

    public static void setUiDisabled(MinecraftServer server, boolean disabled) {
        EffectSmpWorldData data = server.overworld().getDataStorage().computeIfAbsent(EffectSmpWorldData.TYPE);
        data.setUiDisabled(disabled);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncUiState(player);
            if (disabled) {
                player.sendSystemMessage(Component.translatable("message.auracraft.ui_disabled"));
            } else if (getAvailableSelectionTokens(player) > 0) {
                ServerPlayNetworking.send(player, PromptEffectPayload.INSTANCE);
            }
        }
    }

    public static void syncUiState(ServerPlayer player) {
        boolean disabled = isUiDisabled(player.level().getServer());
        ServerPlayNetworking.send(player, new UiStatePayload(disabled));
    }

    public static String getChosenEffectKey() {
        return CHOSEN_EFFECT_KEY;
    }

    public static String getChosenEffectsKey() {
        return CHOSEN_EFFECTS_KEY;
    }

    public static String getEffectAmplifierBonusesKey() {
        return EFFECT_AMPLIFIER_BONUSES_KEY;
    }

    public static String getSelectionTokensKey() {
        return SELECTION_TOKENS_KEY;
    }

    public static String getFirstEffectKey() {
        return FIRST_EFFECT_KEY;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRootCommand(String rootName) {
        return literal(rootName)
            .executes(context -> {
                if (!context.getSource().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    context.getSource().sendFailure(Component.literal("You don't have permission to toggle AuraCraft UI."));
                    return 0;
                }
                MinecraftServer server = context.getSource().getServer();
                boolean newDisabled = !isUiDisabled(server);
                setUiDisabled(server, newDisabled);
                context.getSource().sendSuccess(
                    () -> Component.literal("AuraCraft UI is now " + (newDisabled ? "disabled" : "enabled") + " for this world."),
                    true
                );
                return 1;
            })
            .then(literal("status")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> {
                    ServerPlayer self = context.getSource().getPlayerOrException();
                    sendStatus(context.getSource(), self);
                    return 1;
                })
                .then(argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        sendStatus(context.getSource(), target);
                        return 1;
                    })
                )
            )
            .then(literal("reset")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> {
                    ServerPlayer self = context.getSource().getPlayerOrException();
                    resetForRepick(self);
                    context.getSource().sendSuccess(
                        () -> Component.literal("Reset effect for " + self.getName().getString() + ". They can now pick again."),
                        true
                    );
                    return 1;
                })
                .then(argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        resetForRepick(target);
                        context.getSource().sendSuccess(
                            () -> Component.literal("Reset effect for " + target.getName().getString() + ". They can now pick again."),
                            true
                        );
                        return 1;
                    })
                )
            )
            .then(literal("withdraw")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> withdrawTokenCommand(context.getSource(), context.getSource().getPlayerOrException()))
            )
            .then(literal("plus")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> addTokensCommand(context.getSource(), context.getSource().getPlayerOrException(), 1))
                .then(argument("amount", IntegerArgumentType.integer(1, MAX_SELECTION_TOKENS))
                    .executes(context -> addTokensCommand(
                        context.getSource(),
                        context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "amount")
                    ))
                )
                .then(argument("player", EntityArgument.player())
                    .executes(context -> addTokensCommand(
                        context.getSource(),
                        EntityArgument.getPlayer(context, "player"),
                        1
                    ))
                    .then(argument("amount", IntegerArgumentType.integer(1, MAX_SELECTION_TOKENS))
                        .executes(context -> addTokensCommand(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "player"),
                            IntegerArgumentType.getInteger(context, "amount")
                        ))
                    )
                )
            )
            .then(literal("remove")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> removeTokensCommand(context.getSource(), context.getSource().getPlayerOrException(), 1))
                .then(argument("amount", IntegerArgumentType.integer(1, MAX_SELECTION_TOKENS))
                    .executes(context -> removeTokensCommand(
                        context.getSource(),
                        context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "amount")
                    ))
                )
                .then(argument("player", EntityArgument.player())
                    .executes(context -> removeTokensCommand(
                        context.getSource(),
                        EntityArgument.getPlayer(context, "player"),
                        1
                    ))
                    .then(argument("amount", IntegerArgumentType.integer(1, MAX_SELECTION_TOKENS))
                        .executes(context -> removeTokensCommand(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "player"),
                            IntegerArgumentType.getInteger(context, "amount")
                        ))
                    )
                )
            );
    }

    private static int withdrawTokenCommand(CommandSourceStack source, ServerPlayer player) {
        int current = getAvailableSelectionTokens(player);
        if (current <= 0) {
            source.sendFailure(Component.literal("You have no Aura Plus tokens to withdraw."));
            return 0;
        }
        setAvailableSelectionTokens(player, current - 1);
        syncChosenEffect(player);
        source.sendSuccess(
            () -> Component.literal("Withdrew 1 Aura Plus token. Remaining tokens: " + getAvailableSelectionTokens(player) + "."),
            false
        );
        return 1;
    }

    private static int addTokensCommand(CommandSourceStack source, ServerPlayer target, int requestedAmount) {
        int current = getAvailableSelectionTokens(target);
        int tokenCapacity = MAX_SELECTION_TOKENS - current;
        int loadCapacity = getEffectPlusCapacity(target) - getSelectionLoad(target);
        int capacity = Math.min(tokenCapacity, loadCapacity);
        if (capacity <= 0) {
            if (tokenCapacity <= 0) {
                source.sendFailure(Component.literal(target.getName().getString() + " already has the max token count (" + MAX_SELECTION_TOKENS + ")."));
            } else {
                source.sendFailure(Component.literal(target.getName().getString() + " already has the maximum of " + getEffectPlusCapacity(target) + " effects."));
            }
            return 0;
        }

        int added = Math.min(Math.max(1, requestedAmount), capacity);
        addSelectionTokens(target, added);
        syncChosenEffect(target);
        playTokenGainSound(target);
        if (!isUiDisabled(target.level().getServer())) {
            ServerPlayNetworking.send(target, PromptEffectPayload.INSTANCE);
        }
        source.sendSuccess(
            () -> Component.literal("Added +" + added + " Effect token(s) to " + target.getName().getString() + "."),
            true
        );
        return 1;
    }

    private static int removeTokensCommand(CommandSourceStack source, ServerPlayer target, int requestedAmount) {
        int current = getAvailableSelectionTokens(target);
        if (current <= 0) {
            source.sendFailure(Component.literal(target.getName().getString() + " already has 0 tokens."));
            return 0;
        }

        int removed = Math.min(Math.max(1, requestedAmount), current);
        setAvailableSelectionTokens(target, current - removed);
        syncChosenEffect(target);
        source.sendSuccess(
            () -> Component.literal("Removed " + removed + " token(s) from " + target.getName().getString() + "."),
            true
        );
        return 1;
    }

    private static void sendStatus(CommandSourceStack source, ServerPlayer target) {
        Set<String> ids = getSelectedEffectIds(target);
        String first = getFirstEffectId(target);
        int tokens = getAvailableSelectionTokens(target);
        source.sendSuccess(() -> Component.literal(
            "Status for " + target.getName().getString()
                + ": tokens=" + tokens
                + ", first=" + (first == null ? "none" : first)
                + ", selected=" + (ids.isEmpty() ? "none" : String.join(", ", ids))
                + ", uiDisabled=" + isUiDisabled(target.level().getServer())
        ), false);
    }

    private static Item.Properties itemProperties(String path) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, path));
        return new Item.Properties().setId(key);
    }

    private static Set<String> getSelectedEffectIds(ServerPlayer player) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        String raw = data.auracraft$getChosenEffects();

        if ((raw == null || raw.isBlank()) && data.auracraft$getChosenEffect() != null) {
            return new LinkedHashSet<>(List.of(data.auracraft$getChosenEffect()));
        }
        if (raw == null || raw.isBlank()) {
            return new LinkedHashSet<>();
        }

        Set<String> ids = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = normalizeEffectId(part.trim());
            if (id != null && isKnownEffectId(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static void setSelectedEffectIds(ServerPlayer player, Set<String> ids) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        if (ids == null || ids.isEmpty()) {
            data.auracraft$setChosenEffects(null);
            data.auracraft$setChosenEffect(null);
            data.auracraft$setEffectAmplifierBonuses(null);
            return;
        }

        String csv = String.join(",", ids);
        data.auracraft$setChosenEffects(csv);
        data.auracraft$setChosenEffect(ids.iterator().next());

        Map<String, Integer> bonuses = new HashMap<>(getEffectAmplifierBonuses(player));
        bonuses.keySet().removeIf(key -> !ids.contains(key));
        setEffectAmplifierBonuses(player, bonuses);
    }

    private static int getAvailableSelectionTokens(ServerPlayer player) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        String raw = data.auracraft$getSelectionTokens();
        if (raw == null || raw.isBlank()) {
            return hasChosenEffect(player) ? 0 : 1;
        }
        try {
            return Math.max(0, Math.min(MAX_SELECTION_TOKENS, Integer.parseInt(raw)));
        } catch (NumberFormatException ignored) {
            return hasChosenEffect(player) ? 0 : 1;
        }
    }

    private static void setAvailableSelectionTokens(ServerPlayer player, int tokens) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        int clamped = Math.max(0, Math.min(MAX_SELECTION_TOKENS, tokens));
        data.auracraft$setSelectionTokens(Integer.toString(clamped));
    }

    private static void addSelectionTokens(ServerPlayer player, int amount) {
        setAvailableSelectionTokens(player, getAvailableSelectionTokens(player) + amount);
    }

    private static int getSelectionLoad(ServerPlayer player) {
        return getSelectedEffectIds(player).size() + getAvailableSelectionTokens(player);
    }

    private static int getEffectPlusCapacity(ServerPlayer player) {
        return Math.max(0, EffectSmpConfig.get().maxEffects);
    }

    private static void clearAllSelectedEffects(ServerPlayer player) {
        for (String effectId : getSelectedEffectIds(player)) {
            removeEffectById(player, effectId);
        }
        setSelectedEffectIds(player, Set.of());
        setEffectAmplifierBonuses(player, Map.of());
    }

    private static String removeLastSelectedEffects(ServerPlayer player, int count) {
        int steps = Math.max(0, count);
        Set<String> selectedIds = getSelectedEffectIds(player);
        if (selectedIds.isEmpty() || steps == 0) {
            return null;
        }
        String removedId = null;
        for (int i = 0; i < steps && !selectedIds.isEmpty(); i++) {
            String lastId = null;
            for (String id : selectedIds) {
                lastId = id;
            }
            if (lastId == null) {
                break;
            }
            removedId = lastId;
            int bonus = getAdditionalAmplifierBonus(player, lastId);
            if (bonus > 0) {
                setAdditionalAmplifierBonus(player, lastId, bonus - 1);
                applyEffectById(player, lastId, bonus - 1);
            } else {
                selectedIds.remove(lastId);
                setAdditionalAmplifierBonus(player, lastId, 0);
                removeEffectById(player, lastId);
            }
        }
        setSelectedEffectIds(player, selectedIds);
        return removedId;
    }

    private static String getFirstEffectId(ServerPlayer player) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        String first = data.auracraft$getFirstEffect();
        String normalized = normalizeEffectId(first);
        if (normalized != null && isKnownEffectId(normalized)) {
            return normalized;
        }

        Set<String> selected = getSelectedEffectIds(player);
        if (!selected.isEmpty()) {
            return selected.iterator().next();
        }
        return null;
    }

    private static void setFirstEffectId(ServerPlayer player, String effectId) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        if (effectId == null || effectId.isBlank()) {
            data.auracraft$setFirstEffect(null);
            return;
        }
        data.auracraft$setFirstEffect(effectId);
    }

    private static boolean tryRestoreFirstEffect(ServerPlayer player) {
        if (getAvailableSelectionTokens(player) != 0) {
            return false;
        }

        String firstId = getFirstEffectId(player);
        if (firstId == null) {
            return false;
        }

        Set<String> selected = getSelectedEffectIds(player);
        if (selected.contains(firstId)) {
            return false;
        }

        if (!isKnownEffectId(firstId)) {
            return false;
        }

        selected.add(firstId);
        setSelectedEffectIds(player, selected);
        setAdditionalAmplifierBonus(player, firstId, 0);
        applyEffectById(player, firstId, 0);
        return true;
    }

    private static int getMaxDuplicateAmplifierBonus() {
        return Math.max(0, EffectSmpConfig.get().maxDuplicateAmplifierBonus);
    }

    private static boolean hasUpgradableSelectedEffect(ServerPlayer player) {
        int maxDuplicateBonus = getMaxDuplicateAmplifierBonus();
        for (String effectId : getSelectedEffectIds(player)) {
            if (getAdditionalAmplifierBonus(player, effectId) < maxDuplicateBonus) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> getCappedEffectIds(ServerPlayer player) {
        int maxDuplicateBonus = getMaxDuplicateAmplifierBonus();
        return getSelectedEffectIds(player).stream()
            .filter(effectId -> getAdditionalAmplifierBonus(player, effectId) >= maxDuplicateBonus)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> getEnabledPickerEffectIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            String effectId = id.toString();
            if (EffectSmpConfig.get().isEffectEnabled(effectId)) {
                ids.add(effectId);
            }
        }
        return ids;
    }

    private static int getAdditionalAmplifierBonus(ServerPlayer player, String effectId) {
        return getEffectAmplifierBonuses(player).getOrDefault(effectId, 0);
    }

    private static void setAdditionalAmplifierBonus(ServerPlayer player, String effectId, int value) {
        Map<String, Integer> bonuses = new HashMap<>(getEffectAmplifierBonuses(player));
        int safe = Math.max(0, value);
        if (safe == 0) {
            bonuses.remove(effectId);
        } else {
            bonuses.put(effectId, safe);
        }
        setEffectAmplifierBonuses(player, bonuses);
    }

    private static Map<String, Integer> getEffectAmplifierBonuses(ServerPlayer player) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        String raw = data.auracraft$getEffectAmplifierBonuses();
        Map<String, Integer> result = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String piece : raw.split(",")) {
            String token = piece.trim();
            if (token.isEmpty()) {
                continue;
            }
            String[] parts = token.split("=");
            if (parts.length != 2) {
                continue;
            }
            String id = normalizeEffectId(parts[0].trim());
            if (id == null || !isKnownEffectId(id)) {
                continue;
            }
            try {
                int value = Integer.parseInt(parts[1].trim());
                if (value > 0) {
                    result.put(id, value);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static void setEffectAmplifierBonuses(ServerPlayer player, Map<String, Integer> bonuses) {
        PlayerEffectChoice data = (PlayerEffectChoice) player;
        if (bonuses == null || bonuses.isEmpty()) {
            data.auracraft$setEffectAmplifierBonuses(null);
            return;
        }
        List<String> pieces = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : bonuses.entrySet()) {
            int value = Math.max(0, entry.getValue());
            String id = normalizeEffectId(entry.getKey());
            if (value > 0 && id != null && isKnownEffectId(id)) {
                pieces.add(id + "=" + value);
            }
        }
        if (pieces.isEmpty()) {
            data.auracraft$setEffectAmplifierBonuses(null);
            return;
        }
        data.auracraft$setEffectAmplifierBonuses(String.join(",", pieces));
    }

    private static String normalizeEffectId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase();
        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }
        try {
            return Identifier.parse(trimmed).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isKnownEffectId(String effectId) {
        String normalized = normalizeEffectId(effectId);
        if (normalized == null) {
            return false;
        }
        return BuiltInRegistries.MOB_EFFECT.containsKey(Identifier.parse(normalized));
    }

    private static Component effectNameComponent(String effectId) {
        String normalized = normalizeEffectId(effectId);
        if (normalized == null) {
            return Component.literal(String.valueOf(effectId));
        }
        Identifier id = Identifier.parse(normalized);
        return Component.translatable("effect." + id.getNamespace() + "." + id.getPath());
    }

    private static void applyEffectById(ServerPlayer player, String effectId, int additionalBonus) {
        String normalized = normalizeEffectId(effectId);
        if (normalized == null) {
            return;
        }
        Identifier id = Identifier.parse(normalized);
        if (!BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
            return;
        }
        int amplifier = baseAmplifierFor(normalized) + Math.max(0, additionalBonus);
        int durationTicks = 30 * 20;
        Holder.Reference<?> genericRef = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (!(genericRef instanceof Holder.Reference<?> refObj)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Holder.Reference<net.minecraft.world.effect.MobEffect> effectRef =
            (Holder.Reference<net.minecraft.world.effect.MobEffect>) refObj;
        MobEffectInstance current = player.getEffect(effectRef);
        if (current == null || current.getDuration() < 20 * 10 || current.getAmplifier() < amplifier) {
            player.addEffect(new MobEffectInstance(effectRef, durationTicks, amplifier, true, false, true));
        }
    }

    private static void removeEffectById(ServerPlayer player, String effectId) {
        String normalized = normalizeEffectId(effectId);
        if (normalized == null) {
            return;
        }
        Identifier id = Identifier.parse(normalized);
        if (!BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
            return;
        }
        Holder.Reference<?> genericRef = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (genericRef instanceof Holder.Reference<?> refObj) {
            @SuppressWarnings("unchecked")
            Holder.Reference<net.minecraft.world.effect.MobEffect> effectRef =
                (Holder.Reference<net.minecraft.world.effect.MobEffect>) refObj;
            player.removeEffect(effectRef);
        }
    }

    private static int baseAmplifierFor(String effectId) {
        return DEFAULT_PLUS_ONE_EFFECTS.contains(effectId) ? 1 : 0;
    }

    private static void playTokenGainSound(ServerPlayer player) {
        // Null "except" means nobody is excluded; recipient will hear it.
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.45F, 1.1F);
    }

    private static void playEffectChooseSound(ServerPlayer player) {
        // Null "except" means nobody is excluded; recipient will hear it.
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.55F, 0.95F);
    }
}
