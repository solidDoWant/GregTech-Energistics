package com.soliddowant.gregtechenergistics.networking;

import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {
	public static final String CHANNEL_BASE_NAME = GregTechEnergisticsMod.MODID;
	public static SimpleNetworkWrapper ClientHandlerChannel;
	public static SimpleNetworkWrapper ServerHandlerChannel;
	
	public NetworkHandler () {}
	
	public static void preInit(FMLPreInitializationEvent e) {
		ClientHandlerChannel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_BASE_NAME + "_client_handler");
		ClientHandlerChannel.registerMessage(PacketTerminal.TerminalHandler.class, PacketTerminal.class, 0,
				Side.CLIENT);

		ServerHandlerChannel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_BASE_NAME + "_server_handler");
		ServerHandlerChannel.registerMessage(JEIPacket.JEIHandler.class, JEIPacket.class, 0, Side.SERVER);
	}
}
