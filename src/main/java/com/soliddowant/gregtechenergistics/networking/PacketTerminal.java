package com.soliddowant.gregtechenergistics.networking;

import com.soliddowant.gregtechenergistics.gui.StockerTerminalGuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTerminal extends PacketCompressedNBT {
    protected NBTTagCompound tag;
    public PacketTerminal() {}

    public PacketTerminal(NBTTagCompound updatePacket) {
        tag = updatePacket;
    }

    @Override
    protected NBTTagCompound serialize() {
        return tag;
    }

    @Override
    protected void deserialize(NBTTagCompound tag) {
        this.tag = tag;
    }

    public static class TerminalHandler extends PacketCompressedNBT.Handler<PacketTerminal> {
        protected void handle(PacketTerminal message, MessageContext context) {
            final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
            if (gs instanceof StockerTerminalGuiContainer)
                ((StockerTerminalGuiContainer) gs).postUpdate(message.tag);
        }
    }
}