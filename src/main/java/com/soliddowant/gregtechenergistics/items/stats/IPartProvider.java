package com.soliddowant.gregtechenergistics.items.stats;

import appeng.api.parts.IPart;
import gregtech.api.items.metaitem.stats.IItemComponent;
import net.minecraft.item.ItemStack;

public interface IPartProvider extends IItemComponent {
    IPart getPart(ItemStack stack);
}