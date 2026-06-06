package artm.auracraft.mixin;

import artm.auracraft.PlayerAuraData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerMixin implements PlayerAuraData {
    // NBT Keys
    @Unique
    private static final String KEY_CHOSEN_AURAS     = "auracraft_chosen_auras";
    @Unique
    private static final String KEY_AURA_POINTS      = "auracraft_aura_points";
    @Unique
    private static final String KEY_RESTORATION_QUEUE = "auracraft_restoration_queue";

    // Fields
    @Unique
    private String auracraft$chosenAuras;
    @Unique
    private String auracraft$auraPoints;
    @Unique
    private String auracraft$restorationQueue;

    @Override
    public String auracraft$getChosenAuras() {
        return this.auracraft$chosenAuras;
    }
    @Override
    public void auracraft$setChosenAuras(String auras) {
        this.auracraft$chosenAuras = auras;
    }
    @Override
    public String auracraft$getAuraPoints() {
        return this.auracraft$auraPoints;
    }
    @Override
    public void auracraft$setAuraPoints(String points) {
        this.auracraft$auraPoints = points;
    }
    @Override
    public String auracraft$getRestorationQueue() {
        return this.auracraft$restorationQueue;
    }
    @Override
    public void auracraft$setRestorationQueue(String queue) {
        this.auracraft$restorationQueue = queue;
    }
    // Save
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void auracraft$save(ValueOutput output, CallbackInfo ci) {
        if (this.auracraft$chosenAuras != null)
            output.putString(KEY_CHOSEN_AURAS, this.auracraft$chosenAuras);
        if (this.auracraft$auraPoints != null)
            output.putString(KEY_AURA_POINTS, this.auracraft$auraPoints);
        if (this.auracraft$restorationQueue != null)
            output.putString(KEY_RESTORATION_QUEUE, this.auracraft$restorationQueue);
    }
    // Load
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void auracraft$load(ValueInput input, CallbackInfo ci) {
        this.auracraft$chosenAuras     = input.getString(KEY_CHOSEN_AURAS).orElse(null);
        this.auracraft$auraPoints      = input.getString(KEY_AURA_POINTS).orElse(null);
        this.auracraft$restorationQueue = input.getString(KEY_RESTORATION_QUEUE).orElse(null);
    }
    //Restore
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void auracraft$restoreFrom(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        PlayerAuraData old = (PlayerAuraData) oldPlayer;
        this.auracraft$chosenAuras      = old.auracraft$getChosenAuras();
        this.auracraft$auraPoints       = old.auracraft$getAuraPoints();
        this.auracraft$restorationQueue = old.auracraft$getRestorationQueue();
    }
}
