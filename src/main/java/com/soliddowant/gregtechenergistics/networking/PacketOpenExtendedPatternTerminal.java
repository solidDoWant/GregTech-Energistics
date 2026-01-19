package com.soliddowant.gregtechenergistics.networking;

import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import com.soliddowant.gregtechenergistics.gui.GuiProxy;
import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet sent from client to server to request opening the Extended Pattern
 * Terminal GUI.
 * This is used when returning from the Crafting Status GUI.
 */
public class PacketOpenExtendedPatternTerminal implements IMessage {
    private BlockPos pos;
    private int side;

    public PacketOpenExtendedPatternTerminal() {
    }

    public PacketOpenExtendedPatternTerminal(BlockPos pos, AEPartLocation side) {
        this.pos = pos;
        this.side = side.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        this.pos = new BlockPos(x, y, z);
        this.side = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(side);
    }

    public static class Handler implements IMessageHandler<PacketOpenExtendedPatternTerminal, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenExtendedPatternTerminal message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // Verify the player can access this terminal
                TileEntity te = player.getServerWorld().getTileEntity(message.pos);
                if (te instanceof IPartHost) {
                    AEPartLocation side = AEPartLocation.fromOrdinal(message.side);
                    IPart part = ((IPartHost) te).getPart(side);
                    if (part instanceof ExtendedPatternTerminalPart) {
                        // Open the GUI on the server side
                        player.openGui(
                                GregTechEnergisticsMod.instance,
                                GuiProxy.getOrdinalFromGuiId(GuiProxy.GUI_EXTENDED_PATTERN_TERMINAL) | side.ordinal(),
                                player.getServerWorld(),
                                message.pos.getX(),
                                message.pos.getY(),
                                message.pos.getZ());
                    }
                }
            });
            return null;
        }
    }
}
