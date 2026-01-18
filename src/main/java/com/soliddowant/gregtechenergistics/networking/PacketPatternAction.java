package com.soliddowant.gregtechenergistics.networking;

import com.soliddowant.gregtechenergistics.gui.ExtendedPatternContainer;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPatternAction implements IMessage {
    public enum Action {
        ENCODE,
        CLEAR
    }

    private Action action;

    public PacketPatternAction() {
    }

    public PacketPatternAction(Action action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.action.ordinal());
    }

    public static class Handler implements IMessageHandler<PacketPatternAction, IMessage> {
        @Override
        public IMessage onMessage(PacketPatternAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Container container = player.openContainer;
                if (container instanceof ExtendedPatternContainer) {
                    ExtendedPatternContainer patternContainer = (ExtendedPatternContainer) container;
                    switch (message.action) {
                        case ENCODE:
                            patternContainer.encodeAndMoveToInventory();
                            break;
                        case CLEAR:
                            patternContainer.clear();
                            break;
                    }
                }
            });
            return null;
        }
    }
}
