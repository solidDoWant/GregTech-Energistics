package com.soliddowant.gregtechenergistics.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import appeng.items.misc.ItemEncodedPattern;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Mixin to track when AE2 is rendering pattern tooltips.
 * This allows fluid encoders to display shortened names in pattern tooltips.
 *
 * Also fixes rendering issues with patterns that have >9 inputs (extended
 * patterns).
 */
@Mixin(value = ItemEncodedPattern.class, remap = false)
public class ItemEncodedPatternMixin {

    @Inject(method = "addCheckedInformation", at = @At("HEAD"))
    private void onAddCheckedInformationStart(ItemStack stack, World world, List<String> lines,
            ITooltipFlag advancedTooltips, CallbackInfo ci) {
        FluidEncoderBehaviour.enterPatternContext();
    }

    @Inject(method = "addCheckedInformation", at = @At("RETURN"))
    private void onAddCheckedInformationEnd(ItemStack stack, World world, List<String> lines,
            ITooltipFlag advancedTooltips, CallbackInfo ci) {
        FluidEncoderBehaviour.exitPatternContext();
    }

    /**
     * Intercept the method that AE2 uses to get the "output" item for rendering.
     * This method is called to determine what texture/model to show for the
     * pattern.
     *
     * For extended patterns, we need to ensure it returns a valid output item
     * even if the pattern has >9 inputs.
     */
    @SideOnly(Side.CLIENT)
    @Inject(method = "getOutput", at = @At("HEAD"), cancellable = true)
    private void onGetOutput(ItemStack is, CallbackInfoReturnable<ItemStack> cir) {
        if (!isExtendedPattern(is)) {
            return;
        }

        // Parse the pattern ourselves to get the first output
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null || !tag.hasKey("out")) {
            return;
        }

        NBTTagList outputs = tag.getTagList("out", 10);
        if (outputs.tagCount() == 0) {
            return;
        }

        // Get the first output item
        NBTTagCompound firstOutput = outputs.getCompoundTagAt(0);
        ItemStack outputStack = new ItemStack(firstOutput);

        if (!outputStack.isEmpty()) {
            cir.setReturnValue(outputStack);
        }
    }

    /**
     * Check if a pattern has more than 9 inputs (extended pattern from our
     * terminal).
     */
    private static boolean isExtendedPattern(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return false;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("in")) {
            return false;
        }

        NBTTagList inputs = tag.getTagList("in", 10); // 10 = compound tag
        return inputs.tagCount() > 9;
    }
}
