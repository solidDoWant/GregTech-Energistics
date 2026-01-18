package com.soliddowant.gregtechenergistics.gui.widgets;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import com.soliddowant.gregtechenergistics.render.Textures;
import gregtech.api.gui.widgets.SlotWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
		craftingDetails = pattern.getPatternForItem(slotStack, world);
		if (craftingDetails == null) {
			// AE2 couldn't parse the pattern - log a warning for debugging
			appeng.core.AELog.warn("AE2PatternSlotWidget: Failed to parse pattern. Pattern may have too many inputs for AE2 to handle.");
			clearData();
			return;
		}
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
		if(!hasStack())
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
}