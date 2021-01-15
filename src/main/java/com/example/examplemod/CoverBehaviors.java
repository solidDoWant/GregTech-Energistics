package com.example.examplemod;

import java.util.function.BiFunction;

import gregtech.api.GTValues;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverDefinition;
import gregtech.api.cover.ICoverable;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.common.items.behaviors.CoverPlaceBehavior;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class CoverBehaviors {
	public static final int startingCoverId = 40;
	public static void init() {
		ExampleMod.logger.info("Registering cover behaviors...");
		registerBehavior(startingCoverId, new ResourceLocation(ExampleMod.MODID, "ae2.interface.lv"), MetaItems.AE2_STOCKER_LV,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.LV, 1280));
		registerBehavior(startingCoverId + 1, new ResourceLocation(ExampleMod.MODID, "ae2.interface.mv"), MetaItems.AE2_STOCKER_MV,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.MV, 5120));
		registerBehavior(startingCoverId + 2, new ResourceLocation(ExampleMod.MODID, "ae2.interface.hv"), MetaItems.AE2_STOCKER_HV,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.HV, 20480));
		registerBehavior(startingCoverId + 3, new ResourceLocation(ExampleMod.MODID, "ae2.interface.ev"), MetaItems.AE2_STOCKER_EV,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.EV, 81920));
		registerBehavior(startingCoverId + 4, new ResourceLocation(ExampleMod.MODID, "ae2.interface.iv"), MetaItems.AE2_STOCKER_IV,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.IV, 327680));
		registerBehavior(startingCoverId + 5, new ResourceLocation(ExampleMod.MODID, "ae2.interface.luv"), MetaItems.AE2_STOCKER_LUV,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.LuV, 1310720));
		registerBehavior(startingCoverId + 6, new ResourceLocation(ExampleMod.MODID, "ae2.interface.zpm"), MetaItems.AE2_STOCKER_ZPM,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.ZPM, 5242880));
		registerBehavior(startingCoverId + 7, new ResourceLocation(ExampleMod.MODID, "ae2.interface.uv"), MetaItems.AE2_STOCKER_UV,
				(tile, side) -> new CoverAE2Stocker(tile, side, GTValues.UV, 20971520));

		registerBehavior(startingCoverId + 8, new ResourceLocation(ExampleMod.MODID, "machine.status"), MetaItems.MACHINE_STATUS,
				CoverMachineStatus::new);
	}

	public static void registerBehavior(int coverNetworkId, ResourceLocation coverId,
			MetaItem<?>.MetaValueItem placerItem, BiFunction<ICoverable, EnumFacing, CoverBehavior> behaviorCreator) {
		CoverDefinition coverDefinition = new CoverDefinition(coverId, behaviorCreator, placerItem.getStackForm());
		CoverDefinition.registerCover(coverNetworkId, coverDefinition);
		placerItem.addStats(new CoverPlaceBehavior(coverDefinition));
	}
}
