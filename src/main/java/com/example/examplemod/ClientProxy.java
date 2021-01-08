package com.example.examplemod;

import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		ModItems.getInstance().registerModels();
	}
	
	public void init(FMLInitializationEvent e) {
		super.init(e);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new StaticItemColor(AEColor.TRANSPARENT), ModItems.getInstance().getItemsArray());
  }
}
