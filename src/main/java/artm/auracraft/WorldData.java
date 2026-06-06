package artm.auracraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldData extends SavedData {
    private static final Codec<WorldData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.fieldOf("ui_disabled")
                            .forGetter(WorldData::isUIDisabled)
            ).apply(instance, WorldData::new));

    public static final SavedDataType<WorldData> TYPE = new  SavedDataType<>(
            Identifier.fromNamespaceAndPath(AuraCraft.MOD_ID, "world_data"),
            WorldData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private boolean uiDisabled;

    public WorldData() {
        this(false);
    }
    public WorldData(boolean uiDisabled) {
        this.uiDisabled = uiDisabled;
    }

    public boolean isUIDisabled() {
        return this.uiDisabled;
    }

    public void setUiDisabled(boolean uiDisabled) {
        if (this.uiDisabled != uiDisabled) {
            this.uiDisabled = uiDisabled;
            this.setDirty();
        }
    }
}
