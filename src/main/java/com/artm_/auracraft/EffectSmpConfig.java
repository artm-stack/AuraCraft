package com.artm_.auracraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class EffectSmpConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("auracraft.json");
    private static EffectSmpConfig INSTANCE;

    public int maxEffects = 3;
    public int pvpEffectsLostOnDeath = 1;
    public int maxDuplicateAmplifierBonus = 1;
    public boolean dropPlusItemOnPvpKill = true;
    public boolean enableResetRecipe = true;
    public boolean enablePlusRecipe = true;
    public Map<String, Boolean> enabledEffects = new HashMap<>();

    public static EffectSmpConfig load() {
        EffectSmpConfig cfg;
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                cfg = GSON.fromJson(reader, EffectSmpConfig.class);
            } catch (Exception e) {
                EffectSmpMod.LOGGER.warn("Failed to parse auracraft.json, using defaults", e);
                cfg = defaults();
            }
        } else {
            cfg = defaults();
        }

        if (cfg == null) {
            cfg = defaults();
        }

        cfg.applyDefaults();
        cfg.maxEffects = Math.max(1, cfg.maxEffects);
        cfg.pvpEffectsLostOnDeath = Math.max(0, cfg.pvpEffectsLostOnDeath);
        cfg.maxDuplicateAmplifierBonus = Math.max(0, cfg.maxDuplicateAmplifierBonus);
        cfg.save();
        INSTANCE = cfg;
        return cfg;
    }

    public static EffectSmpConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            EffectSmpMod.LOGGER.error("Failed to write auracraft.json", e);
        }
    }

    public boolean isEffectEnabled(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return false;
        }
        Boolean enabled = this.enabledEffects.get(effectId);
        return enabled == null ? true : enabled;
    }

    public void setEffectEnabled(String effectId, boolean enabled) {
        if (effectId == null || effectId.isBlank()) {
            return;
        }
        this.enabledEffects.put(effectId, enabled);
    }

    private void applyDefaults() {
        if (this.enabledEffects == null) {
            this.enabledEffects = new HashMap<>();
        }
        Set<String> defaultEnabled = Set.of(
            "minecraft:speed",
            "minecraft:haste",
            "minecraft:strength",
            "minecraft:jump_boost",
            "minecraft:resistance",
            "minecraft:fire_resistance",
            "minecraft:invisibility",
            "minecraft:saturation",
            "minecraft:conduit_power"
        );
        for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            String key = id.toString();
            this.enabledEffects.putIfAbsent(key, defaultEnabled.contains(key));
        }
    }

    private static EffectSmpConfig defaults() {
        EffectSmpConfig cfg = new EffectSmpConfig();
        cfg.applyDefaults();
        return cfg;
    }
}
