package com.example.examplemod;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketCompressedNBT implements IMessage {
	protected NBTTagCompound tag;

	public PacketCompressedNBT(NBTTagCompound tag) {
		this.tag = tag;
	}
	
	public PacketCompressedNBT() {}
	
	public NBTTagCompound getTag() {
		return tag;
	}

	@Override
	public void fromBytes(ByteBuf buf) {			
		InputStream byteStream = new InputStream() {
			@Override
			public int read() throws IOException {
				if( buf.readableBytes() <= 0 )
				{
					return -1;
				}

				return buf.readByte() & 0xff;
			}				
		};
		
		try(
			GZIPInputStream gzReader = new GZIPInputStream(byteStream);
			DataInputStream inStream = new DataInputStream(gzReader);
		) {
			this.tag = CompressedStreamTools.read( inStream );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		OutputStream byteStream = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				buf.writeByte(b);
			}				
		};
		
		try (
			GZIPOutputStream gzWriter = new GZIPOutputStream(byteStream);
			DataOutputStream outStream = new DataOutputStream(gzWriter);
		){
			CompressedStreamTools.write(this.tag, outStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static abstract class Handler implements IMessageHandler<PacketCompressedNBT, IMessage> {
		@Override
		public IMessage onMessage(PacketCompressedNBT message, MessageContext context) {
			FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() -> handle(message, context));
			return null;
		}
		
		protected abstract void handle(PacketCompressedNBT message, MessageContext context);
	}
	
	public static class TerminalHandler extends Handler {
		protected void handle(PacketCompressedNBT message, MessageContext context) {
			final GuiScreen gs = Minecraft.getMinecraft().currentScreen;

			if( gs instanceof StockerTerminalGuiContainer )
			{
				( (StockerTerminalGuiContainer) gs ).postUpdate(message.getTag());
			}
		}
	}
}