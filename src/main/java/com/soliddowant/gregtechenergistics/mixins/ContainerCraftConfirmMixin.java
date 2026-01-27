package com.soliddowant.gregtechenergistics.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import com.soliddowant.gregtechenergistics.gui.GuiProxy;
import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.helpers.IContainerCraftingPacket;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

/**
 * Mixin to handle GUI opening in ContainerCraftConfirm when dealing with
 * Extended Pattern Terminal.
 * This ensures the GUI returns to the EPT after starting a craft.
 */
@Mixin(value = ContainerCraftConfirm.class, remap = false)
public abstract class ContainerCraftConfirmMixin implements IContainerCraftingPacket {

    /**
     * Inject at the very end of startJob to handle EPT GUI opening.
     * At this point, the craft has been submitted and we just need to open the GUI.
     */
    @Inject(method = "startJob", at = @At("RETURN"))
    private void onStartJobReturn(CallbackInfo ci) {
        // Cast to parent class to access inherited methods
        AEBaseContainer container = (AEBaseContainer) (Object) this;

        // Check if the action host is an Extended Pattern Terminal
        Object target = container.getTarget();
        if (!(target instanceof ExtendedPatternTerminalPart))
            return;

        ExtendedPatternTerminalPart part = (ExtendedPatternTerminalPart) target;

        // Open our custom GUI
        ContainerOpenContext openContext = container.getOpenContext();
        if (openContext == null)
            return;

        final TileEntity te = openContext.getTile();
        if (te == null)
            return;
        InventoryPlayer invPlayer = container.getInventoryPlayer();

        // Open the Extended Pattern Terminal GUI
        invPlayer.player.openGui(
                GregTechEnergisticsMod.instance,
                GuiProxy.getOrdinalFromGuiId(GuiProxy.GUI_EXTENDED_PATTERN_TERMINAL) | part.getSide().ordinal(),
                te.getWorld(),
                te.getPos().getX(),
                te.getPos().getY(),
                te.getPos().getZ());
    }
}
