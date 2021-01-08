package com.example.examplemod;

import gregtech.api.GTValues;
import gregtech.api.GregTechAPI;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class MetaTileEntities {
	public static MetaTileEntityStockerHatch STOCKER_HATCH = new MetaTileEntityStockerHatch(
			new ResourceLocation(GTValues.MODID, "steam_macerator_bronze"), GTValues.LV);

	public static void init() {
		GregTechAPI.registerMetaTileEntity(2000, STOCKER_HATCH);
//		ForgeRegistries.RECIPES
//				.register(new ShapedOreRecipe(null, STOCKER_HATCH.getStackForm().copy(), "D ", "D", Blocks.DIRT));
	}
}
