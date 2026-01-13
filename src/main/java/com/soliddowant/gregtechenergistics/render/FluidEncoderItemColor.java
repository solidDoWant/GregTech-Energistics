package com.soliddowant.gregtechenergistics.render;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import appeng.api.util.AEColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Custom item color handler for MetaItem1.
 * For fluid encoder items:
 *   - Layer 0: Base texture (no tint)
 *   - Layer 1: Fluid overlay (tinted with fluid color)
 * For other items:
 *   - Uses AE2's transparent color (default behavior)
 */
public class FluidEncoderItemColor implements IItemColor {

    private static final int DEFAULT_COLOR = AEColor.TRANSPARENT.getVariantByTintIndex(0);

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Check if this is a fluid encoder
        if (!FluidEncoderBehaviour.hasStackBehavior(stack)) {
            // Not a fluid encoder, use default AE2 color
            return DEFAULT_COLOR;
        }

        // Layer 0 (base texture) - no tint
        if (tintIndex == 0) {
            return -1; // White/no tint
        }

        // Layer 1 (fluid overlay) - tint with fluid color
        if (tintIndex == 1) {
            FluidStack fluidStack = FluidEncoderBehaviour.getFluidStack(stack);
            if (fluidStack == null) {
                // No fluid set - make overlay invisible (transparent)
                return 0;
            }

            Fluid fluid = fluidStack.getFluid();
            if (fluid == null) {
                return 0;
            }

            // Return the fluid's color
            return fluid.getColor(fluidStack);
        }

        return -1;
    }
}
