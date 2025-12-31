package com.soliddowant.gregtechenergistics;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION, dependencies = "required-after:gregtech;required-after:appliedenergistics2;after:jei@[4.15.0,);after:justenoughenergistics")
public class GregTechEnergisticsMod {
	@SidedProxy(modId = Tags.MODID, clientSide = "com.soliddowant.gregtechenergistics.ClientProxy", serverSide = "com.soliddowant.gregtechenergistics.CommonProxy")
	public static CommonProxy proxy;

	@Mod.Instance
	public static GregTechEnergisticsMod instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
	}
}
