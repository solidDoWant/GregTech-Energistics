package com.soliddowant.gregtechenergistics;

import gregtech.api.items.metaitem.MetaItem;
import net.minecraft.item.ItemStack;

public final class MetaItems {
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_LV;
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_MV;
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_HV;
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_EV;
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_IV;
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_LUV;
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_ZPM;
    public static MetaItem<?>.MetaValueItem AE2_STOCKER_UV;
    public static MetaItem<?>.MetaValueItem MACHINE_STATUS;
    public static MetaItem<?>.MetaValueItem FLUID_ENCODER;
    public static MetaItem<?>.MetaValueItem STOCKER_TERMINAL;
    public static MetaItem1 metaItem1;

    private MetaItems() {
    }

    public static void preInit() {
        metaItem1 = new MetaItem1();
        metaItem1.setRegistryName("metaItem1");
    }

    public static void registerRecipes() {
        metaItem1.registerRecipes();
    }

    public static MetaItem<?>.MetaValueItem getMetaValueItemFromStack(ItemStack stack) {
        return ((MetaItem<?>) stack.getItem()).getItem(stack);
    }
}
