package artm.auracraft;

public interface PlayerAuraData {

    //A <Player> aura list
    String auracraft$getChosenAuras();
    void auracraft$setChosenAuras(String auras);

    // <Player> Aura Points Amount
    String auracraft$getAuraPoints();
    void auracraft$setAuraPoints(String points);

    // Auras queued for restoration (lost via PvP)
    String auracraft$getRestorationQueue();
    void auracraft$setRestorationQueue(String queue);

}
