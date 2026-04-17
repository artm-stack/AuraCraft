package com.artm_.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChooseEffectPayload(String effectId) implements CustomPacketPayload {
    public static final Type<ChooseEffectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("auracraft", "choose_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseEffectPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        ChooseEffectPayload::effectId,
        ChooseEffectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
