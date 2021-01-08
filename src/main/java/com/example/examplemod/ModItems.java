package com.example.examplemod;

import java.util.ArrayList;

import net.minecraft.item.Item;

public class ModItems {
	protected static ModItems instance;
	public ArrayList<Item> ITEMS;

	public final StockerTerminalItem STOCKER_TERMINAL;
	public final FluidEncoderItem FLUID_ENCODER;

	public static ModItems getInstance() {
		if (instance == null)
			instance = new ModItems();
		return instance;
	}

	public ModItems() {
		ITEMS = new ArrayList<Item>();

		STOCKER_TERMINAL = addItem(new StockerTerminalItem());
		FLUID_ENCODER = addItem(new FluidEncoderItem());
	}

	protected <T extends Item> T addItem(T item) {
		assert item != null : "Attempted to add a null item";

		ITEMS.add(item);

		return item;
	}

	public void registerModels() {
		ITEMS.stream().filter(ICustomModel.class::isInstance).map(ICustomModel.class::cast)
				.forEach(ICustomModel::registerModel);
	}
	
	public Item[] getItemsArray() {
		return ITEMS.toArray(new Item[0]);
	}
}
