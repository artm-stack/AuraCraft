package com.artm_.auracraft.mixin;

import com.artm_.auracraft.EffectSmpMod;
import com.artm_.auracraft.PlayerEffectChoice;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin implements PlayerEffectChoice {
    private String auracraft$chosenEffect;
    private String auracraft$chosenEffects;
    private String auracraft$effectAmplifierBonuses;
    private String auracraft$selectionTokens;
    private String auracraft$firstEffect;

    @Override
    public String auracraft$getChosenEffect() {
        return this.auracraft$chosenEffect;
    }

    @Override
    public String auracraft$getChosenEffects() {
        return this.auracraft$chosenEffects;
    }

    @Override
    public String auracraft$getSelectionTokens() {
        return this.auracraft$selectionTokens;
    }

    @Override
    public String auracraft$getEffectAmplifierBonuses() {
        return this.auracraft$effectAmplifierBonuses;
    }

    @Override
    public String auracraft$getFirstEffect() {
        return this.auracraft$firstEffect;
    }

    @Override
    public void auracraft$setChosenEffect(String effectId) {
        this.auracraft$chosenEffect = effectId;
    }

    @Override
    public void auracraft$setChosenEffects(String effectIds) {
        this.auracraft$chosenEffects = effectIds;
    }

    @Override
    public void auracraft$setEffectAmplifierBonuses(String bonuses) {
        this.auracraft$effectAmplifierBonuses = bonuses;
    }

    @Override
    public void auracraft$setSelectionTokens(String tokens) {
        this.auracraft$selectionTokens = tokens;
    }

    @Override
    public void auracraft$setFirstEffect(String effectId) {
        this.auracraft$firstEffect = effectId;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void auracraft$addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        if (this.auracraft$chosenEffect != null) {
            output.putString(EffectSmpMod.getChosenEffectKey(), this.auracraft$chosenEffect);
        }
        if (this.auracraft$chosenEffects != null) {
            output.putString(EffectSmpMod.getChosenEffectsKey(), this.auracraft$chosenEffects);
        }
        if (this.auracraft$effectAmplifierBonuses != null) {
            output.putString(EffectSmpMod.getEffectAmplifierBonusesKey(), this.auracraft$effectAmplifierBonuses);
        }
        if (this.auracraft$selectionTokens != null) {
            output.putString(EffectSmpMod.getSelectionTokensKey(), this.auracraft$selectionTokens);
        }
        if (this.auracraft$firstEffect != null) {
            output.putString(EffectSmpMod.getFirstEffectKey(), this.auracraft$firstEffect);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void auracraft$readAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        this.auracraft$chosenEffect = input.getString(EffectSmpMod.getChosenEffectKey()).orElse(null);
        this.auracraft$chosenEffects = input.getString(EffectSmpMod.getChosenEffectsKey()).orElse(null);
        this.auracraft$effectAmplifierBonuses = input.getString(EffectSmpMod.getEffectAmplifierBonusesKey()).orElse(null);
        this.auracraft$selectionTokens = input.getString(EffectSmpMod.getSelectionTokensKey()).orElse(null);
        this.auracraft$firstEffect = input.getString(EffectSmpMod.getFirstEffectKey()).orElse(null);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void auracraft$restoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerEffectChoice oldData = (PlayerEffectChoice) oldPlayer;
        this.auracraft$chosenEffect = oldData.auracraft$getChosenEffect();
        this.auracraft$chosenEffects = oldData.auracraft$getChosenEffects();
        this.auracraft$effectAmplifierBonuses = oldData.auracraft$getEffectAmplifierBonuses();
        this.auracraft$selectionTokens = oldData.auracraft$getSelectionTokens();
        this.auracraft$firstEffect = oldData.auracraft$getFirstEffect();
    }
}
