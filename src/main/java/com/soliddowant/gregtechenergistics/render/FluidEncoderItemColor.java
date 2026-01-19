package com.soliddowant.gregtechenergistics.render;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import appeng.api.util.AEColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Custom item color handler for MetaItem1.
 * For fluid encoder items:
 * - Layer 0: Fluid layer - no tint if texture found, fluid color tint if
 * texture missing, gray if empty
 * - Layer 1: Base frame - always white (no tint)
 * For other items:
 * - Uses AE2's transparent color with proper tint index (for terminal
 * bright/medium/dark layers)
 */
public class FluidEncoderItemColor implements IItemColor {

    private static final int NO_TINT = 0xFFFFFFFF;
    private static final int EMPTY_GRAY = 0xFF989898;

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Check if this is a fluid encoder
        if (!FluidEncoderBehaviour.hasStackBehavior(stack)) {
            // Not a fluid encoder, use default AE2 color with proper tint index
            // This is critical for terminal items that use tintindex 1,2,3,4 for their
            // layers
            return AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex);
        }

        // Layer 0 (fluid/underlay layer)
        if (tintIndex != 0) {
            // Layer 1 (base frame) - no tint
            return NO_TINT;
        }

        FluidStack fluidStack = FluidEncoderBehaviour.getFluidStack(stack);
        if (fluidStack == null || fluidStack.getFluid() == null) {
            // Empty - tint gray
            return EMPTY_GRAY;
        }

        // Check if we have a valid fluid texture
        Fluid fluid = fluidStack.getFluid();
        if (hasFluidTexture(fluid, fluidStack)) {
            // Fluid texture found - no tint so actual texture colors show
            return NO_TINT;
        }

        // Fluid texture not found - tint with fluid color as fallback
        return fluid.getColor(fluidStack);
    }

    /**
     * Check if a fluid's texture is available in the texture atlas
     */
    private boolean hasFluidTexture(Fluid fluid, FluidStack fluidStack) {
        ResourceLocation fluidStill = fluid.getStill(fluidStack);
        if (fluidStill == null) {
            return false;
        }

        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite fluidSprite = textureMap.getAtlasSprite(fluidStill.toString());
        TextureAtlasSprite missingSprite = textureMap.getMissingSprite();

        return fluidSprite != null && fluidSprite != missingSprite;
    }
}
