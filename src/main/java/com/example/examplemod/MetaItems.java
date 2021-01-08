package com.example.examplemod;

import gregtech.api.items.metaitem.MetaItem;

public final class MetaItems {
	private MetaItems() {
	}

	public static MetaItem<?>.MetaValueItem AE2_STOCKER_LV;
	public static MetaItem<?>.MetaValueItem MACHINE_STATUS;
	public static MetaItem<?>.MetaValueItem STOCKER_TERMINAL;

	public static void init() {
		GTCEMetaItem1 gtce = new GTCEMetaItem1();
		gtce.setRegistryName("gtce");
	}
}
