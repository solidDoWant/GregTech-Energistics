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
	public static void init() {
        ExampleMod.logger.info("Registering cover behaviors...");
        registerBehavior(40, new ResourceLocation(ExampleMod.MODID, "ae2.interface.lv"), MetaItems.AE2_INTERFACE_LV, (tile, side) -> new CoverAE2Stocker(tile, side, GTValues.LV, 8));
    }

    public static void registerBehavior(int coverNetworkId, ResourceLocation coverId, MetaItem<?>.MetaValueItem placerItem, BiFunction<ICoverable, EnumFacing, CoverBehavior> behaviorCreator) {
        CoverDefinition coverDefinition = new CoverDefinition(coverId, behaviorCreator, placerItem.getStackForm());
        CoverDefinition.registerCover(coverNetworkId, coverDefinition);
        placerItem.addStats(new CoverPlaceBehavior(coverDefinition));
    }
}
