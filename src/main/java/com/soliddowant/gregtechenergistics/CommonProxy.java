package com.soliddowant.gregtechenergistics;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = GregTechEnergisticsMod.MODID)
public class CommonProxy {
	public void preInit(FMLPreInitializationEvent e) {
		NetworkHandler.preInit(e);
		MetaItems.preInit();
	}

	public void init(FMLInitializationEvent e) {
		NetworkRegistry.INSTANCE.registerGuiHandler(GregTechEnergisticsMod.instance, new GuiProxy());
		CoverBehaviors.init();
	}

	@SubscribeEvent
	public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
		MetaItems.registerRecipes();
	}
}
