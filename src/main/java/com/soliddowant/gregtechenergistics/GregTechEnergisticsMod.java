package com.soliddowant.gregtechenergistics;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = GregTechEnergisticsMod.MODID, name = GregTechEnergisticsMod.NAME, version = GregTechEnergisticsMod.VERSION,
		dependencies = "required-after:gregtech;required-after:appliedenergistics2")
public class GregTechEnergisticsMod {
	public static final String MODID = "gregtechenergistics";
	public static final String NAME = "GregTech Energistics";
	public static final String VERSION = "1.0";

	static Logger logger;

	@SidedProxy(modId = GregTechEnergisticsMod.MODID, clientSide = "com.soliddowant.gregtechenergistics.ClientProxy",
			serverSide = "com.soliddowant.gregtechenergistics.CommonProxy")
	public static CommonProxy proxy;
	
	@Mod.Instance
    public static GregTechEnergisticsMod instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
	}
}
