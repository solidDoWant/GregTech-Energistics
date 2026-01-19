package com.soliddowant.gregtechenergistics.gui.widgets;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.soliddowant.gregtechenergistics.render.Textures;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

public class AE2PatternSlotWidget extends CallbackSlotWidget {
	protected ICraftingPatternDetails craftingDetails;
	protected IAEItemStack[] inputItems;
	protected IAEItemStack[] outputItems;
	protected World world;

	public AE2PatternSlotWidget(@Nonnull World world) {
		super();
		this.world = world;
	}

	@Override
	public boolean validateItemStack(@Nonnull ItemStack stack) {
		return stack.getItem() instanceof ItemEncodedPattern;
	}

	protected void setData(@Nonnull ItemStack slotStack) {
		ItemEncodedPattern pattern = getPattern();
		if (pattern == null) {
			clearData();
			return;
		}

		// Try AE2's parser first
		craftingDetails = pattern.getPatternForItem(slotStack, world);

		if (craftingDetails == null) {
			// AE2 couldn't parse - try reading NBT directly for extended patterns
			NBTTagCompound tag = slotStack.getTagCompound();
			if (tag != null && tag.hasKey("in") && tag.hasKey("out")) {
				inputItems = parseItemsFromNBT(tag.getTagList("in", 10));
				outputItems = parseItemsFromNBT(tag.getTagList("out", 10));

				// Only accept if we successfully parsed both inputs and outputs
				if (inputItems != null && outputItems != null) {
					return;
				}
			}
			clearData();
			return;
		}

		// AE2 parsed successfully - use its data
		inputItems = craftingDetails.getCondensedInputs();
		outputItems = craftingDetails.getOutputs();
	}

	protected void clearData() {
		craftingDetails = null;
		inputItems = null;
		outputItems = null;
	}

	@Override
	@Nonnull
	protected BackgroundSlotWidget createSlotWidget(int x, int y) {
		return (BackgroundSlotWidget) super.createSlotWidget(x, y).setBackgroundTexture(Textures.getAE2Sprite(15, 7));
	}

	@Nullable
	public ItemEncodedPattern getPattern() {
		if (!hasStack())
			return null;

		ItemStack patternStack = getSlotStack();
		return (ItemEncodedPattern) patternStack.getItem();
	}

	@Nullable
	public ICraftingPatternDetails getPatternDetails() {
		return craftingDetails;
	}

	@Nullable
	public IAEItemStack[] getInputItems() {
		return inputItems;
	}

	@Nullable
	public IAEItemStack[] getOutputItems() {
		return outputItems;
	}

	/**
	 * Parse IAEItemStack array from NBT tag list.
	 * Used for extended patterns that AE2 cannot parse natively.
	 */
	@Nullable
	protected IAEItemStack[] parseItemsFromNBT(@Nonnull NBTTagList tagList) {
		if (tagList.tagCount() == 0) {
			return null;
		}

		ArrayList<IAEItemStack> items = new ArrayList<>();
		IItemStorageChannel itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);

		for (int i = 0; i < tagList.tagCount(); i++) {
			NBTTagCompound itemTag = tagList.getCompoundTagAt(i);
			ItemStack stack = new ItemStack(itemTag);

			if (stack.isEmpty())
				continue;

			IAEItemStack aeStack = itemChannel.createStack(stack);
			if (aeStack != null)
				items.add(aeStack);
		}

		return items.isEmpty() ? null : items.toArray(new IAEItemStack[0]);
	}
}
