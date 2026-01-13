package com.soliddowant.gregtechenergistics.render;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import appeng.api.util.AEColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Custom item color handler for MetaItem1.
 * For fluid encoder items:
 * - Layer 0: Fluid layer - white when fluid present (to show actual texture), gray when empty
 * - Layer 1: Base frame - always white (no tint)
 * For other items:
 * - Uses AE2's transparent color (default behavior)
 */
public class FluidEncoderItemColor implements IItemColor {

    private static final int DEFAULT_COLOR = AEColor.TRANSPARENT.getVariantByTintIndex(0);
    private static final int NO_TINT = 0xFFFFFFFF;
    private static final int EMPTY_GRAY = 0xFF989898;

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Check if this is a fluid encoder
        if (!FluidEncoderBehaviour.hasStackBehavior(stack)) {
            // Not a fluid encoder, use default AE2 color
            return DEFAULT_COLOR;
        }

        // Layer 0 (fluid/underlay layer)
        if (tintIndex == 0) {
            FluidStack fluidStack = FluidEncoderBehaviour.getFluidStack(stack);
            if (fluidStack != null && fluidStack.getFluid() != null) {
                // Fluid set - no tint so actual fluid texture colors show
                return NO_TINT;
            } else {
                // Empty - tint gray
                return EMPTY_GRAY;
            }
        }

        // Layer 1 (base frame) - no tint
        return NO_TINT;
    }
}
