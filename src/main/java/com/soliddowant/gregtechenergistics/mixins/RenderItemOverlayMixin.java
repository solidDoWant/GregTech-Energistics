package com.soliddowant.gregtechenergistics.mixins;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;

/**
 * Mixin to show fluid quantity (in buckets) instead of "1" for fluid encoder items
 * in AE2 pattern terminal displays only.
 */
@Mixin(RenderItem.class)
public class RenderItemOverlayMixin {

    @Inject(method = "renderItemOverlayIntoGUI", at = @At("HEAD"), cancellable = true)
    private void onRenderItemOverlay(FontRenderer fr, ItemStack stack, int xPosition, int yPosition,
            @Nullable String text, CallbackInfo ci) {
        // Only process if no custom text is already provided
        if (text != null) {
            return;
        }

        // Only apply in AE2 pattern terminal context
        if (!isInPatternTerminalContext()) {
            return;
        }

        // Check if this is a fluid encoder with fluid
        if (!FluidEncoderBehaviour.hasStackBehavior(stack)) {
            return;
        }

        int amountMb = FluidEncoderBehaviour.getFluidAmount(stack);

        // Only show if there's a fluid amount set
        if (amountMb <= 0) {
            return;
        }

        // Convert to buckets and format
        double buckets = amountMb / 1000.0;
        String formatted = formatBuckets(buckets);

        // Render custom quantity text (replicating vanilla's rendering)
        // Use a smaller scale to match the expected text size in pattern terminal
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();

        // Calculate position and render with scaling to reduce text size
        float scale = 0.5f;
        float x = xPosition + 19 - 2 - fr.getStringWidth(formatted) * scale;
        float y = yPosition + 6 + 3;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        fr.drawStringWithShadow(formatted, 0, 0, 16777215);
        GlStateManager.popMatrix();

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();

        // Cancel the original method to prevent it from rendering "1"
        ci.cancel();
    }

    /**
     * Check if we're currently rendering in an AE2 pattern terminal context.
     */
    private boolean isInPatternTerminalContext() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc.currentScreen;
        if (screen == null) {
            return false;
        }

        // Check if it's an AE2 pattern terminal or related GUI
        String className = screen.getClass().getName();
        return className.contains("GuiPatternTerm") ||
               className.contains("GuiExpandedProcessingPatternTerm") ||
               className.contains("GuiCraftingStatus") ||
               className.contains("GuiMEMonitorable");
    }

    /**
     * Format bucket amount according to AE2 style:
     * - 11mb -> 0.011
     * - 10mb -> 0.01
     * - 20,000B -> 20K
     * - 640B -> 640.0
     * - 6B -> 6.00
     * - 5.21B -> 5.21
     * - 24B -> 24.0
     */
    private String formatBuckets(double buckets) {
        if (buckets >= 10000) {
            // Very large amounts: use K/M suffixes
            if (buckets >= 1000000) {
                return String.format("%.0fM", buckets / 1000000);
            } else {
                return String.format("%.0fK", buckets / 1000);
            }
        } else if (buckets >= 1000) {
            // 1000-9999: show as X.XK
            double k = buckets / 1000;
            if (k == Math.floor(k)) {
                return String.format("%.0fK", k);
            } else {
                return String.format("%.1fK", k);
            }
        } else if (buckets >= 100) {
            // 100-999: show one decimal
            return String.format("%.1f", buckets);
        } else if (buckets >= 10) {
            // 10-99: show one decimal
            return String.format("%.1f", buckets);
        } else if (buckets >= 1) {
            // 1-9.99: show two decimals
            return String.format("%.2f", buckets);
        } else {
            // Sub-bucket amounts: show enough precision
            // 0.011, 0.01, 0.1, 0.5, etc.
            String result = String.format("%.3f", buckets);
            // Remove trailing zeros but keep at least one decimal place
            result = result.replaceAll("0+$", "");
            if (result.endsWith(".")) {
                result = result + "0";
            }
            return result;
        }
    }
}
