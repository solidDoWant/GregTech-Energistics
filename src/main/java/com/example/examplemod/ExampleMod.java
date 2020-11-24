package com.example.examplemod;

import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.Logger;

@Mod(
		modid = ExampleMod.MODID, 
		name = ExampleMod.NAME, 
		version = ExampleMod.VERSION,
		dependencies = "after:gregtech"
	)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    public static final String NAME = "Example Modtest";
    public static final String VERSION = "1.0";

    static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MetaItems.init();
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
    	CoverBehaviors.init();
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {    	
        // some example code
        logger.info("DIRT BLOCK test >> {}", Blocks.DIRT.getRegistryName());
    }
}
