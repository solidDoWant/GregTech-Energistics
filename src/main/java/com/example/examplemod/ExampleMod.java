package com.example.examplemod;

import gregtech.api.util.BaseCreativeTab;
import org.apache.logging.log4j.Logger;

import appeng.api.util.AEColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION, dependencies = "required-after:gregtech;required-after:appliedenergistics2")
public class ExampleMod {
	public static final String MODID = "examplemod";
	public static final String NAME = "Example Modtest";
	public static final String VERSION = "1.0";

	static Logger logger;

	@SidedProxy(modId = ExampleMod.MODID, clientSide = "com.example.examplemod.ClientProxy", serverSide = "com.example.examplemod.CommonProxy")
	public static CommonProxy proxy;
	
	@Mod.Instance
    public static ExampleMod instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		proxy.preInit(event);
	}

	@Mod.EventHandler
	public void onInit(FMLInitializationEvent event) {
		CoverBehaviors.init();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		// some example code
		logger.info("DIRT BLOCK test >> {}", Blocks.DIRT.getRegistryName());
		
		proxy.init(event);
	}
	
	@SideOnly(Side.CLIENT)
    public static class TerminalItemColor implements IItemColor {
        @Override
        public int colorMultiplier(ItemStack stack, int tintIndex) {
            return AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex);
        }
    }
}
