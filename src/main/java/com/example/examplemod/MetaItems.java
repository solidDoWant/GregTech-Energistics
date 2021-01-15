package com.example.examplemod;

import gregtech.api.items.metaitem.MetaItem;

public final class MetaItems {
	private MetaItems() {
	}

	public static MetaItem<?>.MetaValueItem AE2_STOCKER_LV;
	public static MetaItem<?>.MetaValueItem AE2_STOCKER_MV;
	public static MetaItem<?>.MetaValueItem AE2_STOCKER_HV;
	public static MetaItem<?>.MetaValueItem AE2_STOCKER_EV;
	public static MetaItem<?>.MetaValueItem AE2_STOCKER_IV;
	public static MetaItem<?>.MetaValueItem AE2_STOCKER_LUV;
	public static MetaItem<?>.MetaValueItem AE2_STOCKER_ZPM;
	public static MetaItem<?>.MetaValueItem AE2_STOCKER_UV;
	public static MetaItem<?>.MetaValueItem MACHINE_STATUS;

	public static GTCEMetaItem1 gtceMetaItem;

	public static void init() {
		gtceMetaItem = new GTCEMetaItem1();
		gtceMetaItem.setRegistryName("gtce");
	}

	public static void registerRecipes() {
		gtceMetaItem.registerRecipes();
	}
}
