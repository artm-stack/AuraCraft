package artm.auracraft;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public class Items {

    public static final Item AURA_PLUS = register(
            "aura_plus",
            props -> new Item(props) {
                @Override
                @SuppressWarnings("NullableProblems")
                public InteractionResult use(Level level, Player player, InteractionHand hand) {
                    if (level.isClientSide()) return InteractionResult.SUCCESS;
                    if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
                    MinecraftServer server = serverPlayer.level().getServer();
                    if (AuraCraft.getTotalLoad(serverPlayer) >= AuraCraft.getMaxCapacity(server)) {
                        serverPlayer.sendSystemMessage(Component.translatable(
                                "message.auracraft.max_capacity")
                                .withStyle(ChatFormatting.RED));
                        return InteractionResult.FAIL;
                    }
                    AuraCraft.grantAuraPoint(serverPlayer);
                    if (!serverPlayer.getAbilities().instabuild) {
                        player.getItemInHand(hand).shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            },
            new Item.Properties().stacksTo(1));

    public static final Item AURA_RESET = register(
            "aura_reset",
            props -> new Item(props) {
                @Override
                @SuppressWarnings("NullableProblems")
                public InteractionResult use(Level level, Player player, InteractionHand hand) {
                    if (level.isClientSide()) return InteractionResult.SUCCESS;
                    if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

                    if (AuraCraft.getAuraList(serverPlayer).isEmpty()) {
                        serverPlayer.sendSystemMessage(Component.translatable(
                                "message.auracraft.reset_no_history")
                                .withStyle(ChatFormatting.YELLOW));
                        return InteractionResult.FAIL;
                    }

                    int newPoints = AuraCraft.resetPlayer(serverPlayer);
                    if (newPoints == 0) return InteractionResult.FAIL;

                    if (!serverPlayer.getAbilities().instabuild) {
                        serverPlayer.getItemInHand(hand).shrink(1);
                    }

                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                            SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.6F, 1.0F);
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "message.auracraft.reset_used", newPoints)
                            .withStyle(ChatFormatting.GREEN));

                    return InteractionResult.SUCCESS;
                }
            },
            new Item.Properties().stacksTo(8));

    public static final ResourceKey<CreativeModeTab> AURA_TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            Identifier.fromNamespaceAndPath(AuraCraft.MOD_ID, "aura_items")
    );

    public static final CreativeModeTab AURA_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(AURA_PLUS))
            .title(Component.translatable("itemGroup.auracraft.aura_items"))
            .displayItems((_, output) -> {
                output.accept(AURA_PLUS);
                output.accept(AURA_RESET);
            })
            .build();

    public static <T extends Item> T register(String name,
                                              Function<Item.Properties, T> itemFactory,
                                              Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(AuraCraft.MOD_ID, name));
        T item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, AURA_TAB_KEY, AURA_TAB);
    }
}
