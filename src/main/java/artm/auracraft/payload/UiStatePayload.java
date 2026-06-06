package artm.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record UiStatePayload(
        boolean uiDisabled,          // is the picker disabled for this world?
        List<String> enabledAuras    // which auras are enabled in config?
) implements CustomPacketPayload {

    public static final Type<UiStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("auracraft", "ui_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UiStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    UiStatePayload::uiDisabled,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    UiStatePayload::enabledAuras,
                    UiStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}