package artm.auracraft.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncAuraPayload(
        List<String>         chosenAuras,    // full aura list, duplicates = upgrades
        List<String>         cappedAuras,    // auras that are at max upgrade level
        int                  auraPoints,     // available points to spend
        Map<String, Integer> upgradeLevels   // per-aura max upgrade level
) implements CustomPacketPayload {

    public static final Type<SyncAuraPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("auracraft", "sync_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAuraPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    SyncAuraPayload::chosenAuras,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    SyncAuraPayload::cappedAuras,
                    ByteBufCodecs.VAR_INT,
                    SyncAuraPayload::auraPoints,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
                    SyncAuraPayload::upgradeLevels,
                    SyncAuraPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
