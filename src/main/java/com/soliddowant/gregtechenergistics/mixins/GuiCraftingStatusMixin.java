package com.soliddowant.gregtechenergistics.mixins;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import com.soliddowant.gregtechenergistics.items.MetaItems;
import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.parts.IPartHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.util.AEPartLocation;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.core.sync.GuiBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Mixin to add support for ExtendedPatternTerminalPart in GuiCraftingStatus.
 * Without this, the crafting status GUI won't show a "Terminal" button
 * when opened from the Extended Pattern Terminal.
 */
@Mixin(value = GuiCraftingStatus.class, remap = false)
public class GuiCraftingStatusMixin {

    @Shadow
    private ItemStack myIcon;

    @Shadow
    private GuiBridge originalGui;

    @Shadow
    private GuiTabButton originalGuiBtn;

    @Shadow
    private ContainerCraftingStatus status;

    // Store whether this is from our ExtendedPatternTerminal
    private boolean isExtendedPatternTerminal = false;
    private ExtendedPatternTerminalPart extendedPart = null;

    /**
     * Inject after the constructor to check if the target is our
     * ExtendedPatternTerminalPart.
     * We inject at RETURN so all the normal AE2 instanceof checks have already run.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(InventoryPlayer inventoryPlayer, ITerminalHost te, CallbackInfo ci) {
        final Object target = this.status.getTarget();

        // Check if it's our ExtendedPatternTerminalPart
        if (target instanceof ExtendedPatternTerminalPart) {
            // Only set if not already set by AE2
            if (this.myIcon == null || this.myIcon.isEmpty()) {
                this.myIcon = MetaItems.EXTENDED_PATTERN_TERMINAL.getStackForm();
                // We'll handle the GUI switching in actionPerformed
                this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL; // Placeholder, won't be used
                this.isExtendedPatternTerminal = true;
                this.extendedPart = (ExtendedPatternTerminalPart) target;
            }
        }
    }

    /**
     * Intercept button clicks to handle our custom terminal.
     */
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton btn, CallbackInfo ci) throws IOException {
        if (btn == this.originalGuiBtn && this.isExtendedPatternTerminal && this.extendedPart != null) {
            // Open our custom GUI instead of using AE2's packet
            final EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null)
                return;

            final IPartHost host = this.extendedPart.getHost();
            if (host == null)
                return;

            final TileEntity tile = host.getTile();
            if (tile == null || tile.getWorld() == null)
                return;

            final BlockPos pos = tile.getPos();
            final AEPartLocation side = this.extendedPart.getSide();

            // Use our mod's GUI system
            if (GregTechEnergisticsMod.instance != null) {
                player.openGui(
                        GregTechEnergisticsMod.instance,
                        (1 << 3) | side.ordinal(), // guiId 1 for ExtendedPattern
                        tile.getWorld(),
                        pos.getX(),
                        pos.getY(),
                        pos.getZ());
            }

            // Cancel the original action (prevents AE2 from sending PacketSwitchGuis)
            ci.cancel();
        }
    }
}
