package com.soliddowant.gregtechenergistics.mixins;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.items.MetaItems;
import com.soliddowant.gregtechenergistics.networking.NetworkHandler;
import com.soliddowant.gregtechenergistics.networking.PacketOpenExtendedPatternTerminal;
import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.parts.IPartHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.util.AEPartLocation;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.core.sync.GuiBridge;
import net.minecraft.client.gui.GuiButton;
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
public abstract class GuiCraftingStatusMixin extends AEBaseGui {

    public GuiCraftingStatusMixin(AEBaseContainer container) {
        super(container);
    }

    @Shadow
    private ItemStack myIcon;

    @Shadow
    private GuiBridge originalGui;

    @Shadow
    private GuiTabButton originalGuiBtn;

    @Shadow
    private ContainerCraftingStatus status;

    @Unique
    private boolean gtce_isExtendedPatternTerminal = false;
    @Unique
    private ExtendedPatternTerminalPart gtce_extendedPart = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(InventoryPlayer inventoryPlayer, ITerminalHost te, CallbackInfo ci) {
        final Object target = this.status.getTarget();

        if (target instanceof ExtendedPatternTerminalPart) {
            if (this.myIcon == null || this.myIcon.isEmpty()) {
                this.myIcon = MetaItems.EXTENDED_PATTERN_TERMINAL.getStackForm();
                this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL; // Placeholder, won't be used
                this.gtce_isExtendedPatternTerminal = true;
                this.gtce_extendedPart = (ExtendedPatternTerminalPart) target;
            }
        }
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = true)
    private void onInitGui(CallbackInfo ci) {
        // If originalGuiBtn wasn't created but we have a valid myIcon, create the
        // button manually
        if (this.originalGuiBtn == null && this.gtce_isExtendedPatternTerminal && !this.myIcon.isEmpty()) {
            this.originalGuiBtn = new GuiTabButton(this.guiLeft + 213, this.guiTop - 4, this.myIcon,
                    this.myIcon.getDisplayName(), this.itemRender);
            this.originalGuiBtn.setHideEdge(13);
            this.buttonList.add(this.originalGuiBtn);
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true, remap = true)
    private void onActionPerformed(GuiButton btn, CallbackInfo ci) throws IOException {
        if (btn == this.originalGuiBtn && this.gtce_isExtendedPatternTerminal && this.gtce_extendedPart != null) {
            final IPartHost host = this.gtce_extendedPart.getHost();
            if (host == null)
                return;

            final TileEntity tile = host.getTile();
            if (tile == null)
                return;

            final BlockPos pos = tile.getPos();
            final AEPartLocation side = this.gtce_extendedPart.getSide();

            // Send packet to server to open the GUI properly
            NetworkHandler.ServerHandlerChannel.sendToServer(
                    new PacketOpenExtendedPatternTerminal(pos, side));

            ci.cancel();
        }
    }
}
