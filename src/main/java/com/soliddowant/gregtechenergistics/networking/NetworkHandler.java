package com.soliddowant.gregtechenergistics.networking;

import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {
	public static final String CHANNEL_NAME = GregTechEnergisticsMod.MODID;
	public static SimpleNetworkWrapper snw;
	
	public NetworkHandler () {}
	
	public static void preInit(FMLPreInitializationEvent e) {
		snw = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
		snw.registerMessage(PacketCompressedNBT.TerminalHandler.class, PacketCompressedNBT.class, 0, Side.CLIENT);
	}
}
