package com.example.examplemod;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import appeng.core.AppEng;
import appeng.items.misc.ItemEncodedPattern;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ServerWidgetGroup;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.util.IDirtyNotifiable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

public class PatternContainer implements INBTSerializable<NBTTagCompound>{
	private final ItemStackHandler patternInventory;
	private final TextureArea patternSlotOverlay;
	
	public PatternContainer(IDirtyNotifiable dirtyNotifiable) {
		this.patternInventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            	if(slot != 0) {
            		return false;
            	}
            	
            	return stack.getItem() instanceof ItemEncodedPattern;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onLoad() {
            	
            }

            @Override
            protected void onContentsChanged(int slot) {
            	super.onContentsChanged(slot);
            	dirtyNotifiable.markAsDirty();
            }
        };
        
        patternSlotOverlay = new TextureArea(new ResourceLocation(AppEng.MOD_ID, "textures/guis/states.png"), (15.0/16.0), (7.0/16.0), (1.0/16.0), (1.0/16.0));
	}
	
//	protected void onPatternSlotChange(boolean notify) {
//        ItemStack filterStack = patternInventory.getStackInSlot(0);
//        ItemFilter newItemFilter = FilterTypeRegistry.getItemFilterForStack(filterStack);
//        ItemFilter currentItemFilter = filterWrapper.getItemFilter();
//        if(newItemFilter == null) {
//            if(currentItemFilter != null) {
//                filterWrapper.setItemFilter(null);
//                filterWrapper.setBlacklistFilter(false);
//                if (notify) filterWrapper.onFilterInstanceChange();
//            }
//        } else if (currentItemFilter == null ||
//            newItemFilter.getClass() != currentItemFilter.getClass()) {
//            filterWrapper.setItemFilter(newItemFilter);
//            if (notify) filterWrapper.onFilterInstanceChange();
//        }
//    }
	
	public void initUI(int y, Consumer<Widget> widgetGroup) {
        widgetGroup.accept(new LabelWidget(10, y, "cover.stocker.pattern.title"));
        widgetGroup.accept(new SlotWidget(patternInventory, 0, 10, y + 15)
            .setBackgroundTexture(GuiTextures.SLOT, patternSlotOverlay));

//        ServerWidgetGroup stackSizeGroup = new ServerWidgetGroup(this::showGlobalTransferLimitSlider);
//        stackSizeGroup.addWidget(new ClickButtonWidget(91, 70, 20, 20, "-1", data -> adjustTransferStackSize(data.isShiftClick ? -10 : -1)));
//        stackSizeGroup.addWidget(new ClickButtonWidget(146, 70, 20, 20, "+1", data -> adjustTransferStackSize(data.isShiftClick ? +10 : +1)));
//        stackSizeGroup.addWidget(new ImageWidget(111, 70, 35, 20, GuiTextures.DISPLAY));
//        stackSizeGroup.addWidget(new SimpleTextWidget(128, 80, "", 0xFFFFFF, () -> Integer.toString(transferStackSize)));
//        widgetGroup.accept(stackSizeGroup);

//        this.filterWrapper.initUI(y + 38, widgetGroup);
    }
	
	@Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setTag("PatternInventory", patternInventory.serializeNBT());
        
        return tagCompound;
    }
	
	 @Override
    public void deserializeNBT(NBTTagCompound tagCompound) {
        this.patternInventory.deserializeNBT(tagCompound.getCompoundTag("PatternInventory"));
    }
}
