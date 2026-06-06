package artm.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChooseAuraPayload(String auraId) implements CustomPacketPayload {

    public static final Type<ChooseAuraPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("auracraft", "choose_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseAuraPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ChooseAuraPayload::auraId,
                    ChooseAuraPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}