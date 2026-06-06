package artm.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// This payload carries no data — it's just a signal to the client
// to open the Aura picker screen
public record PromptAuraPayload() implements CustomPacketPayload {

    public static final PromptAuraPayload INSTANCE = new PromptAuraPayload();

    public static final Type<PromptAuraPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("auracraft", "prompt_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PromptAuraPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}