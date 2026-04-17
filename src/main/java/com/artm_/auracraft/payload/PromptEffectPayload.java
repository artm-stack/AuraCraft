package com.artm_.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PromptEffectPayload() implements CustomPacketPayload {
    public static final PromptEffectPayload INSTANCE = new PromptEffectPayload();
    public static final Type<PromptEffectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("auracraft", "prompt_pick_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PromptEffectPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
