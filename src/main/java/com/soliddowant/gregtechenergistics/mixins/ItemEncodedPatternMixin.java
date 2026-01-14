package com.soliddowant.gregtechenergistics.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import appeng.items.misc.ItemEncodedPattern;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Mixin to track when AE2 is rendering pattern tooltips.
 * This allows fluid encoders to display shortened names in pattern tooltips.
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
}
