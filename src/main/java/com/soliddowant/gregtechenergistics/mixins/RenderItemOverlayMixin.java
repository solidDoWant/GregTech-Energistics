package com.soliddowant.gregtechenergistics.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.render.StackSizeRenderer;
import appeng.core.AEConfig;
import appeng.util.ReadableNumberConverter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

/**
 * Mixin to show fluid quantity (in buckets) instead of "1" for fluid encoder
 * items
 * in AE2 pattern terminal displays.
 *
 * This hooks into AE2's StackSizeRenderer which is used to render quantity
 * overlays
 * in ME terminals and pattern terminals.
 */
@Mixin(value = StackSizeRenderer.class, remap = false)
public class RenderItemOverlayMixin {

    @Inject(method = "renderStackSize", at = @At("HEAD"), cancellable = true)
    private void onRenderStackSize(FontRenderer fontRenderer, IAEItemStack aeStack, int xPos, int yPos, CallbackInfo ci) {
        if (aeStack == null) {
            return;
        }

        // Get the underlying ItemStack
        ItemStack itemStack = aeStack.createItemStack();

        // Check if this is a fluid encoder
        if (!FluidEncoderBehaviour.hasStackBehavior(itemStack)) {
            return;
        }

        // Check if fluid is set - if not, let AE2 render the default "1"
        if (!FluidEncoderBehaviour.hasFluidStack(itemStack)) {
            return;
        }

        int amountMb = FluidEncoderBehaviour.getFluidAmount(itemStack);

        // Convert to buckets and format using AE2's style
        // Show "0" if amount is 0 but fluid is set
        String formatted = amountMb <= 0 ? "0" : formatFluidAmount(amountMb);

        // Render using AE2's style (matching StackSizeRenderer behavior)
        final float scaleFactor = AEConfig.instance().useTerminalUseLargeFont() ? 0.85f : 0.5f;
        final float inverseScaleFactor = 1.0f / scaleFactor;
        final int offset = AEConfig.instance().useTerminalUseLargeFont() ? 0 : -1;

        final boolean unicodeFlag = fontRenderer.getUnicodeFlag();
        fontRenderer.setUnicodeFlag(false);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFactor, scaleFactor, scaleFactor);

        final int X = (int) (((float) xPos + offset + 16.0f - fontRenderer.getStringWidth(formatted) * scaleFactor) * inverseScaleFactor);
        final int Y = (int) (((float) yPos + offset + 16.0f - 7.0f * scaleFactor) * inverseScaleFactor);

        fontRenderer.drawStringWithShadow(formatted, X, Y, 16777215);

        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();

        fontRenderer.setUnicodeFlag(unicodeFlag);

        // Cancel the original method
        ci.cancel();
    }

    /**
     * Format fluid amount (in millibuckets) to a readable bucket format.
     * Uses AE2's ReadableNumberConverter for large numbers (1000+).
     *
     * Examples:
     * - 11mb -> "0.011"
     * - 1000mb -> "1.00"
     * - 20000000mb -> "20K" (20,000 buckets)
     */
    private String formatFluidAmount(int amountMb) {
        double buckets = amountMb / 1000.0;

        // Use AE2's converter for 1000+ (handles K, M, G, etc.)
        if (buckets >= 1000) {
            return ReadableNumberConverter.INSTANCE.toWideReadableForm((long) buckets);
        }

        // Below 1000: show appropriate decimal precision
        if (buckets >= 10) {
            return String.format("%.1f", buckets); // 10-999: "24.0"
        }

        if (buckets >= 1) {
            return String.format("%.2f", buckets); // 1-9.99: "6.00", "5.21"
        }

        // Sub-bucket: "0.011", "0.01", "0.5"
        String result = String.format("%.3f", buckets);
        result = result.replaceAll("0+$", ""); // Remove trailing zeros
        return result.endsWith(".") ? result + "0" : result; // Keep at least one decimal
    }
}
