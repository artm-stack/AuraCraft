package artm.auracraft.mixin;

import artm.auracraft.AuraCraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(RecipeMap.class)
public abstract class RecipeMapMixin {

    /**
     * Filters the heart_of_the_sea recipe out of every crafting lookup when
     * the ENABLE_HEART_RECIPE gamerule is false.  Runs only server-side
     * (ClientLevel won't match instanceof ServerLevel).
     */
    @Inject(method = "getRecipesFor", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("unchecked")
    private <I extends RecipeInput, T extends Recipe<I>> void auracraft$filterHeartRecipe(
            RecipeType<T> type, I input, Level level,
            CallbackInfoReturnable<Stream<RecipeHolder<T>>> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getGameRules().get(AuraCraft.ENABLE_HEART_RECIPE)) return;
        cir.setReturnValue(
                cir.getReturnValue()
                   .filter(h -> !h.id().equals(AuraCraft.HEART_RECIPE_KEY))
        );
    }
}
