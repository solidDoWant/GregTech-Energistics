package com.soliddowant.gregtechenergistics.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PacketCompressedNBT implements IMessage {
    // If an empty constructor isn't here then Forge will lose it's shit
    public PacketCompressedNBT() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        InputStream byteStream = new InputStream() {
            @Override
            public int read() {
                if (buf.readableBytes() <= 0)
                    return -1;

                return buf.readByte() & 0xff;
            }
        };

        NBTTagCompound tag = null;
        try (
                GZIPInputStream gzReader = new GZIPInputStream(byteStream);
                DataInputStream inStream = new DataInputStream(gzReader)
        ) {

            tag = CompressedStreamTools.read(inStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(tag != null)
            deserialize(tag);
    }

    protected void deserialize(NBTTagCompound tag) {}

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound tag = serialize();

        if(tag == null)
            return;

        OutputStream byteStream = new OutputStream() {
            @Override
            public void write(int b) {
                buf.writeByte(b);
            }
        };

        try (
                GZIPOutputStream gzWriter = new GZIPOutputStream(byteStream);
                DataOutputStream outStream = new DataOutputStream(gzWriter)
        ) {
            CompressedStreamTools.write(tag, outStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected NBTTagCompound serialize() {
        return new NBTTagCompound();
    }

    public static abstract class Handler<T extends PacketCompressedNBT> implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler)
                    .addScheduledTask(() -> handle(message, context));
            return null;
        }

        protected abstract void handle(T message, MessageContext context);
    }
}