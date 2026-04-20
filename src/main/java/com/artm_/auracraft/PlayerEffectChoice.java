package com.artm_.auracraft;

public interface PlayerEffectChoice {
    String auracraft$getChosenEffect();
    String auracraft$getChosenEffects();
    String auracraft$getEffectAmplifierBonuses();
    String auracraft$getSelectionTokens();
    String auracraft$getFirstEffect();
    String auracraft$getWithdrawnEffects();

    void auracraft$setChosenEffect(String effectId);
    void auracraft$setChosenEffects(String effectIds);
    void auracraft$setEffectAmplifierBonuses(String bonuses);
    void auracraft$setSelectionTokens(String tokens);
    void auracraft$setFirstEffect(String effectId);
    void auracraft$setWithdrawnEffects(String effectIds);
}
