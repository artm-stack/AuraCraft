package com.artm_.auracraft.payload;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncEffectPayload(
    List<String> selectedEffectIds,
    List<String> cappedEffectIds,
    List<String> enabledEffectIds,
    int availableTokens,
    int maxDuplicateAmplifierBonus
) implements CustomPacketPayload {
    public static final Type<SyncEffectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("auracraft", "sync_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEffectPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.STRING_UTF8),
        SyncEffectPayload::selectedEffectIds,
        ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.STRING_UTF8),
        SyncEffectPayload::cappedEffectIds,
        ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.STRING_UTF8),
        SyncEffectPayload::enabledEffectIds,
        ByteBufCodecs.VAR_INT,
        SyncEffectPayload::availableTokens,
        ByteBufCodecs.VAR_INT,
        SyncEffectPayload::maxDuplicateAmplifierBonus,
        SyncEffectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
