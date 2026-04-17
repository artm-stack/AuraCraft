package com.artm_.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UiStatePayload(boolean uiDisabled) implements CustomPacketPayload {
    public static final Type<UiStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("auracraft", "ui_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UiStatePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        UiStatePayload::uiDisabled,
        UiStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
