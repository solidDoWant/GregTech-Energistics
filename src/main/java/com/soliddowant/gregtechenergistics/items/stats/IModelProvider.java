package com.soliddowant.gregtechenergistics.items.stats;

import gregtech.api.items.metaitem.stats.IItemComponent;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

public interface IModelProvider extends IItemComponent {
    ModelResourceLocation getModel();
}