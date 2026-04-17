package com.artm_.auracraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class EffectSmpWorldData extends SavedData {
    private static final Codec<EffectSmpWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("ui_disabled").forGetter(EffectSmpWorldData::uiDisabled)
    ).apply(instance, EffectSmpWorldData::new));

    public static final SavedDataType<EffectSmpWorldData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(EffectSmpMod.MOD_ID, "world_data"),
        EffectSmpWorldData::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    private boolean uiDisabled;

    public EffectSmpWorldData() {
        this(false);
    }

    public EffectSmpWorldData(boolean uiDisabled) {
        this.uiDisabled = uiDisabled;
    }

    public boolean uiDisabled() {
        return this.uiDisabled;
    }

    public void setUiDisabled(boolean uiDisabled) {
        if (this.uiDisabled != uiDisabled) {
            this.uiDisabled = uiDisabled;
            this.setDirty();
        }
    }
}
