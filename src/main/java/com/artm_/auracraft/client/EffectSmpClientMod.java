package com.artm_.auracraft.client;

import com.artm_.auracraft.EffectSmpConfig;
import com.artm_.auracraft.payload.ChooseEffectPayload;
import com.artm_.auracraft.payload.ClientHelloPayload;
import com.artm_.auracraft.payload.PromptEffectPayload;
import com.artm_.auracraft.payload.SyncEffectPayload;
import com.artm_.auracraft.payload.UiStatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EffectSmpClientMod implements ClientModInitializer {
    private static KeyMapping PICK_EFFECT_KEY;
    private static final Set<String> selectedEffectIds = new HashSet<>();
    private static final Set<String> cappedEffectIds = new HashSet<>();
    private static final Set<String> enabledEffectIds = new HashSet<>();
    private static int availableTokens;
    private static int maxDuplicateAmplifierBonus = 1;
    private static boolean uiDisabled;

    @Override
    public void onInitializeClient() {
        PICK_EFFECT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.auracraft.pick_effect",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            KeyMapping.Category.MISC
        ));

        ClientPlayNetworking.registerGlobalReceiver(PromptEffectPayload.TYPE, (payload, context) ->
            context.client().execute(() -> showPrompt(context.client()))
        );

        ClientPlayNetworking.registerGlobalReceiver(SyncEffectPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                selectedEffectIds.clear();
                selectedEffectIds.addAll(payload.selectedEffectIds());
                cappedEffectIds.clear();
                cappedEffectIds.addAll(payload.cappedEffectIds());
                enabledEffectIds.clear();
                enabledEffectIds.addAll(payload.enabledEffectIds());
                availableTokens = payload.availableTokens();
                maxDuplicateAmplifierBonus = payload.maxDuplicateAmplifierBonus();
            })
        );

        ClientPlayNetworking.registerGlobalReceiver(UiStatePayload.TYPE, (payload, context) ->
            context.client().execute(() -> uiDisabled = payload.uiDisabled())
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            ClientPlayNetworking.send(new ClientHelloPayload(ClientHelloPayload.PROTOCOL_VERSION))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PICK_EFFECT_KEY.consumeClick()) {
                if (uiDisabled) {
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.translatable("message.auracraft.ui_disabled"));
                    }
                    continue;
                }

                if (availableTokens <= 0) {
                    if (client.player != null) {
                        String chosen = selectedEffectIds.stream().findFirst().orElse(null);
                        if (chosen != null) {
                            client.player.sendSystemMessage(
                                Component.translatable(
                                    "message.auracraft.already_chosen_client",
                                    effectName(chosen)
                                ).copy().withStyle(ChatFormatting.RED)
                            );
                        } else {
                            client.player.sendSystemMessage(
                                Component.translatable("message.auracraft.already_chosen").copy().withStyle(ChatFormatting.RED)
                            );
                        }
                    }
                } else if (!(client.screen instanceof EffectSelectionScreen)) {
                    client.setScreen(new EffectSelectionScreen());
                }
            }
        });
    }

    public static void sendChooseEffect(String effectId) {
        ClientPlayNetworking.send(new ChooseEffectPayload(effectId));
    }

    private static void showPrompt(Minecraft client) {
        if (client.player == null || uiDisabled) {
            return;
        }

        Component prompt = Component.literal("AuraCraft").withStyle(ChatFormatting.GREEN)
            .append(Component.literal(": Press ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal("[").withStyle(ChatFormatting.WHITE))
            .append(PICK_EFFECT_KEY.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA))
            .append(Component.literal("]").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" to pick an effect.").withStyle(ChatFormatting.WHITE));

        client.player.sendSystemMessage(prompt);
    }

    public static boolean hasSelectedEffect(String effectId) {
        return selectedEffectIds.contains(effectId);
    }

    public static boolean isEffectCapped(String effectId) {
        return cappedEffectIds.contains(effectId);
    }

    public static int getMaxDuplicateAmplifierBonus() {
        return maxDuplicateAmplifierBonus;
    }

    public static List<String> getEnabledEffectsForPicker() {
        List<String> list = new ArrayList<>();
        if (enabledEffectIds.isEmpty()) {
            return list;
        } else {
            for (String effectId : enabledEffectIds) {
                try {
                    Identifier.parse(effectId);
                    list.add(effectId);
                } catch (Exception ignored) {
                }
            }
        }
        list.sort(Comparator.naturalOrder());
        return list;
    }

    public static Component effectName(String effectId) {
        try {
            Identifier id = Identifier.parse(effectId);
            return Component.translatable("effect." + id.getNamespace() + "." + id.getPath());
        } catch (Exception ignored) {
            return Component.literal(effectId);
        }
    }

    public static void refreshEnabledEffectsFromConfig() {
        enabledEffectIds.clear();
        for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            String effectId = id.toString();
            if (EffectSmpConfig.get().isEffectEnabled(effectId)) {
                enabledEffectIds.add(effectId);
            }
        }
    }
}
