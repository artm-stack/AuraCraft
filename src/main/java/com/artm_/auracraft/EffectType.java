package com.artm_.auracraft;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum EffectType {
    SPEED("speed", 0, 1, Items.SUGAR, MobEffects.SPEED, "effect.minecraft.speed", "effect.auracraft.speed.desc"),
    HASTE("haste", 1, 1, Items.GOLDEN_PICKAXE, MobEffects.HASTE, "effect.minecraft.haste", "effect.auracraft.haste.desc"),
    STRENGTH("strength", 2, 1, Items.IRON_SWORD, MobEffects.STRENGTH, "effect.minecraft.strength", "effect.auracraft.strength.desc"),
    JUMP_BOOST("jump_boost", 3, 1, Items.RABBIT_FOOT, MobEffects.JUMP_BOOST, "effect.minecraft.jump_boost", "effect.auracraft.jump_boost.desc"),
    RESISTANCE("resistance", 4, 1, Items.SHIELD, MobEffects.RESISTANCE, "effect.minecraft.resistance", "effect.auracraft.resistance.desc"),
    FIRE_RESISTANCE("fire_resistance", 5, 0, Items.MAGMA_CREAM, MobEffects.FIRE_RESISTANCE, "effect.minecraft.fire_resistance", "effect.auracraft.fire_resistance.desc"),
    INVISIBILITY("invisibility", 6, 0, Items.GLASS, MobEffects.INVISIBILITY, "effect.minecraft.invisibility", "effect.auracraft.invisibility.desc"),
    SATURATION("saturation", 7, 0, Items.COOKED_BEEF, MobEffects.SATURATION, "effect.minecraft.saturation", "effect.auracraft.saturation.desc"),
    CONDUIT_POWER("conduit_power", 8, 0, Items.NAUTILUS_SHELL, MobEffects.CONDUIT_POWER, "effect.minecraft.conduit_power", "effect.auracraft.conduit_power.desc");

    private final String id;
    private final int slot;
    private final int amplifier;
    private final Item iconItem;
    private final Holder<MobEffect> effect;
    private final String nameKey;
    private final String descKey;

    EffectType(String id, int slot, int amplifier, Item iconItem, Holder<MobEffect> effect, String nameKey, String descKey) {
        this.id = id;
        this.slot = slot;
        this.amplifier = amplifier;
        this.iconItem = iconItem;
        this.effect = effect;
        this.nameKey = nameKey;
        this.descKey = descKey;
    }

    public String id() {
        return this.id;
    }

    public int slot() {
        return this.slot;
    }

    public Item iconItem() {
        return this.iconItem;
    }

    public String nameKey() {
        return this.nameKey;
    }

    public String descKey() {
        return this.descKey;
    }

    public Holder<MobEffect> effect() {
        return this.effect;
    }

    public int defaultAmplifier() {
        return this.amplifier;
    }

    public void apply(ServerPlayer player) {
        apply(player, 0);
    }

    public void apply(ServerPlayer player, int additionalAmplifier) {
        int amplifier = this.amplifier + Math.max(0, additionalAmplifier);
        int durationTicks = 30 * 20;
        MobEffectInstance current = player.getEffect(this.effect);
        if (current == null || current.getDuration() < 20 * 10 || current.getAmplifier() < amplifier) {
            player.addEffect(new MobEffectInstance(this.effect, durationTicks, amplifier, true, false, true));
        }
    }

    public static EffectType byId(String id) {
        if (id == null) {
            return null;
        }
        for (EffectType effectType : values()) {
            if (effectType.id.equalsIgnoreCase(id)) {
                return effectType;
            }
        }
        return null;
    }

    public static EffectType bySlot(int slot) {
        for (EffectType effectType : values()) {
            if (effectType.slot == slot) {
                return effectType;
            }
        }
        return null;
    }
}
