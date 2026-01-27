package com.soliddowant.gregtechenergistics.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import com.soliddowant.gregtechenergistics.gui.GuiProxy;
import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketSwitchGuis;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

/**
 * Mixin to redirect GUI switches from Extended Pattern Terminal back to the
 * correct custom GUI.
 * Without this, the cancel button and return-after-craft would try to open
 * AE2's regular pattern terminal instead.
 */
@Mixin(value = PacketSwitchGuis.class, remap = false)
public abstract class PacketSwitchGuisMixin {

    @Shadow
    private GuiBridge newGui;

    @Inject(method = "serverPacketData", at = @At("HEAD"), cancellable = true)
    private void onServerPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player, CallbackInfo ci) {
        final Container c = player.openContainer;
        if (!(c instanceof AEBaseContainer))
            return;

        final AEBaseContainer bc = (AEBaseContainer) c;
        final ContainerOpenContext context = bc.getOpenContext();
        if (context == null)
            return;

        final Object target = bc.getTarget();

        // Check if this is from an Extended Pattern Terminal
        if (!(target instanceof ExtendedPatternTerminalPart))
            return;

        // Redirect to our custom GUI instead of AE2's pattern terminal
        ExtendedPatternTerminalPart part = (ExtendedPatternTerminalPart) target;
        final TileEntity te = context.getTile();

        if (te == null)
            return;

        player.openGui(
                GregTechEnergisticsMod.instance,
                GuiProxy.getOrdinalFromGuiId(GuiProxy.GUI_EXTENDED_PATTERN_TERMINAL) | part.getSide().ordinal(),
                te.getWorld(),
                te.getPos().getX(),
                te.getPos().getY(),
                te.getPos().getZ());

        // Cancel the original packet handling since we've handled it
        ci.cancel();
    }
}
