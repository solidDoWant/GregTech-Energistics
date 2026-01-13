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
 *   - Layer 0: Underlay (droplet shape) - tinted with fluid color, or gray when empty
 *   - Layer 1: Base frame with transparent droplet area - no tint
 * For other items:
 *   - Uses AE2's transparent color (default behavior)
 */
public class FluidEncoderItemColor implements IItemColor {

    private static final int DEFAULT_COLOR = AEColor.TRANSPARENT.getVariantByTintIndex(0);
    // Gray color matching the original droplet (adjust if needed)
    private static final int EMPTY_DROPLET_COLOR = 0xFF808080;

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Check if this is a fluid encoder
        if (!FluidEncoderBehaviour.hasStackBehavior(stack)) {
            // Not a fluid encoder, use default AE2 color
            return DEFAULT_COLOR;
        }

        // Layer 0 (underlay/droplet) - tint with fluid color or gray when empty
        if (tintIndex == 0) {
            FluidStack fluidStack = FluidEncoderBehaviour.getFluidStack(stack);
            if (fluidStack == null) {
                // No fluid set - show gray droplet
                return EMPTY_DROPLET_COLOR;
            }

            Fluid fluid = fluidStack.getFluid();
            if (fluid == null) {
                return EMPTY_DROPLET_COLOR;
            }

            // Return the fluid's color
            return fluid.getColor(fluidStack);
        }

        // Layer 1 (base frame) - no tint
        if (tintIndex == 1) {
            return -1; // White/no tint
        }

        return -1;
    }
}
