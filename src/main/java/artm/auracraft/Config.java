package artm.auracraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("auracraft.json");
    private static Config INSTANCE;

    // Config Fields — gameplay numbers are gamerules; only aura toggles/levels live here
    public Map<String, Boolean>  enabledAuras      = new HashMap<>();
    public Map<String, Integer>  auraUpgradeLevels = new HashMap<>();

    // Load / Save Config
    public static Config load() {
        Config cfg;

        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                cfg = GSON.fromJson(reader, Config.class);
            } catch (Exception e) {
                AuraCraft.LOGGER.warn("Failed to parse auracraft.json, using defaults", e);
                cfg = defaults();
            }
        } else {
            cfg = defaults();
        }

        if (cfg == null) cfg = defaults();
        cfg.applyDefaults();

        cfg.auraUpgradeLevels.replaceAll((k, v) -> Math.max(0, v));

        cfg.save();
        INSTANCE = cfg;
        return cfg;
    }

    public static Config get() {
        if (INSTANCE == null) return load();
        return INSTANCE;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            AuraCraft.LOGGER.error("Failed to write auracraft.json", e);
        }
    }

    // Aura Toggle Helpers
    public boolean isAuraEnabled(String auraId) {
        if (auraId == null || auraId.isBlank()) return false;
        Boolean enabled = this.enabledAuras.get(auraId);
        return enabled != null && enabled;
    }

    public void setAuraEnabled(String auraId, boolean enabled) {
        if (auraId == null || auraId.isBlank()) return;
        this.enabledAuras.put(auraId, enabled);
    }

    // Upgrade Level Helpers
    public int getMaxUpgradeLevel(String auraId) {
        if (auraId == null || auraId.isBlank()) return 1;
        return auraUpgradeLevels.getOrDefault(auraId, 1);
    }

    public void setMaxUpgradeLevel(String auraId, int level) {
        if (auraId == null || auraId.isBlank()) return;
        auraUpgradeLevels.put(auraId, Math.max(0, level));
    }

    // Defaults
    public void applyDefaults() {
        if (this.enabledAuras      == null) this.enabledAuras      = new HashMap<>();
        if (this.auraUpgradeLevels == null) this.auraUpgradeLevels = new HashMap<>();

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
            this.enabledAuras.putIfAbsent(key, defaultEnabled.contains(key));
        }

        for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            String key = id.toString();
            this.auraUpgradeLevels.putIfAbsent(key, 1);
        }
    }

    private static Config defaults() {
        Config cfg = new Config();
        cfg.applyDefaults();
        return cfg;
    }
}
