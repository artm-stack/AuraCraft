package com.artm_.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClientHelloPayload(int protocolVersion) implements CustomPacketPayload {
    public static final int PROTOCOL_VERSION = 1;
    public static final Type<ClientHelloPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("auracraft", "client_hello"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientHelloPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ClientHelloPayload::protocolVersion,
        ClientHelloPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
