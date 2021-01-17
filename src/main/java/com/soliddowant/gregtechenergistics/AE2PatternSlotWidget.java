package com.soliddowant.gregtechenergistics;

import javax.annotation.Nonnull;

import appeng.core.AppEng;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.Platform;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.SlotWidget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class AE2PatternSlotWidget extends BackgroundSlotWidget {
	public AE2PatternSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition, int yPosition) {
		super(itemHandler, slotIndex, xPosition, yPosition, true, true);
		this.slotReference = new AE2WidgetSlotDelegate(itemHandler, slotIndex, xPosition, yPosition);
		setBackgroundTexture(GuiTextures.SLOT, new TextureArea(new ResourceLocation(AppEng.MOD_ID,
				"textures/guis/states.png"), (15.0/16.0), (7.0/16.0), (1.0/16.0), (1.0/16.0)));
	}

	protected class AE2WidgetSlotDelegate extends SlotWidget.WidgetSlotDelegate {
		protected boolean isDisplay = false;

		public AE2WidgetSlotDelegate(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
			super(itemHandler, index, xPosition, yPosition);
		}

		@Override
		public boolean isItemValid(@Nonnull ItemStack stack) {
			return AE2PatternSlotWidget.this.canPutStack(stack) && super.isItemValid(stack);
		}

		@Override
		public boolean canTakeStack(EntityPlayer playerIn) {
			return AE2PatternSlotWidget.this.canTakeStack(playerIn) && super.canTakeStack(playerIn);
		}

		@Override
		public void putStack(@Nonnull ItemStack stack) {
			super.putStack(stack);
			if (changeListener != null)
				changeListener.run();
		}

		@Override
		public void onSlotChanged() {
			AE2PatternSlotWidget.this.onSlotChanged();
		}

		@Override
		public boolean isEnabled() {
			return AE2PatternSlotWidget.this.isEnabled();
		}

		@Override
		@Nonnull
		public ItemStack getStack() {
			if (this.getItemHandler().getSlots() <= this.getSlotIndex())
				return ItemStack.EMPTY;

			if (this.isDisplay()) {
				this.setDisplay(false);
				return this.getDisplayStack();
			}

			return this.getItemHandler().getStackInSlot(0);
		}

		public ItemStack getDisplayStack() {
			if (Platform.isClient()) {
				final ItemStack is = super.getStack();
				if (!is.isEmpty() && is.getItem() instanceof ItemEncodedPattern) {
					final ItemEncodedPattern iep = (ItemEncodedPattern) is.getItem();
					final ItemStack out = iep.getOutput(is);
					if (!out.isEmpty())
						return out;
				}
			}
			return super.getStack();
		}

		private boolean isDisplay() {
			return this.isDisplay;
		}

		public void setDisplay(final boolean isDisplay) {
			this.isDisplay = isDisplay;
		}
	}
}
