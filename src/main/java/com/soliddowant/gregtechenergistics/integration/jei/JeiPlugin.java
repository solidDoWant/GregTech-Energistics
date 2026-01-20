package com.soliddowant.gregtechenergistics.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.config.Constants;

/**
 * JEI Plugin for GregTech Energistics.
 *
 * Registers the Extended Pattern Terminal handler directly.
 *
 * Note: The native AE2 Pattern Terminal (ContainerPatternTerm) handler is
 * replaced at runtime via Mixin (see RecipeRegistryMixin) to support fluid
 * encoding.
 */
@SuppressWarnings({ "unused" })
@JEIPlugin
public class JeiPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        // Register our Extended Pattern Terminal handler
        // This uses the public API and works fine since there's no conflict
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                new ExtendedRecipeTransferHandler(),
                Constants.UNIVERSAL_RECIPE_TRANSFER_UID);
    }
}
