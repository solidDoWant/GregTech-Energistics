package com.example.examplemod;

import gregtech.api.items.metaitem.MetaItem;

public final class MetaItems {
	private MetaItems() {
	}

	public static MetaItem<?>.MetaValueItem AE2_INTERFACE_LV;
	
	public static void init() {
        MetaItem1 first = new MetaItem1();
        first.setRegistryName("meta_item_1");
    }
}
