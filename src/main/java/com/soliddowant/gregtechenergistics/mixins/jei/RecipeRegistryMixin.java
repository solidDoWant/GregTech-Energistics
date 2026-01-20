package com.soliddowant.gregtechenergistics.mixins.jei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soliddowant.gregtechenergistics.integration.jei.RecipeTransferHandler;

import appeng.container.implementations.ContainerPatternTerm;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.recipes.RecipeRegistry;
import net.minecraft.inventory.Container;

/**
 * Mixin to override JEI's recipe transfer handler lookup for ContainerPatternTerm.
 *
 * This is necessary because RecipeRegistry creates an immutable snapshot of handlers
 * during construction, so runtime modifications via reflection don't work. AE2's native
 * JEI handler doesn't support fluid encoding, so we need to replace it with our own.
 */
@Mixin(value = RecipeRegistry.class, remap = false)
public class RecipeRegistryMixin {

    private static RecipeTransferHandler GT_ENERGISTICS_HANDLER = null;

    @Inject(
        method = "getRecipeTransferHandler",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void injectPatternTermHandler(
            Container container,
            IRecipeCategory recipeCategory,
            CallbackInfoReturnable<IRecipeTransferHandler> cir) {

        // Only intercept for ContainerPatternTerm
        if (!(container instanceof ContainerPatternTerm)) {
            return;
        }

        IRecipeTransferHandler originalHandler = cir.getReturnValue();

        // If there's no handler or it's already ours, do nothing
        if (originalHandler == null) {
            return;
        }

        if (originalHandler instanceof RecipeTransferHandler) {
            return;
        }

        // Replace AE2's handler with ours (lazy initialization)
        if (GT_ENERGISTICS_HANDLER == null) {
            GT_ENERGISTICS_HANDLER = new RecipeTransferHandler();
        }

        cir.setReturnValue(GT_ENERGISTICS_HANDLER);
    }
}
