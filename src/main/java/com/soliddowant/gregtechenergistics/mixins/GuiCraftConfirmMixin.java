package com.soliddowant.gregtechenergistics.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.core.sync.GuiBridge;
import net.minecraft.entity.player.InventoryPlayer;

/**
 * Mixin to add support for ExtendedPatternTerminalPart in GuiCraftConfirm.
 * Without this, the craft confirmation GUI will have a null OriginalGui field,
 * causing a NullPointerException when rendering buttons.
 */
@Mixin(value = GuiCraftConfirm.class, remap = false)
public abstract class GuiCraftConfirmMixin {

    @Shadow
    private GuiBridge OriginalGui;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(InventoryPlayer inventoryPlayer, ITerminalHost te, CallbackInfo ci) {
        // Check if this is the extended pattern terminal
        if (te instanceof ExtendedPatternTerminalPart && this.OriginalGui == null) {
            // Set a dummy OriginalGui value to avoid NPEs
            // This allows the cancel button to be created properly
            this.OriginalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        }
    }
}
