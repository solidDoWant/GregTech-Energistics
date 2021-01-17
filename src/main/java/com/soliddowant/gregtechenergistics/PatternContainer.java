package com.soliddowant.gregtechenergistics;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AppEng;
import appeng.items.misc.ItemEncodedPattern;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.LabelWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

public class PatternContainer implements INBTSerializable<NBTTagCompound> {
	private final ItemStackHandler patternInventory;
	private final TextureArea patternSlotOverlay;
	private final ICoverable coverHolder;
	private AE2PatternSlotWidget patternSlot;
	private boolean isPatternAvailable;
	private ICraftingPatternDetails patternInformation;
	private IAEItemStack[] inputItems;
	private IAEItemStack[] outputItems;
	protected Consumer<Boolean> patternChangeCallback;
	
	public PatternContainer(ICoverable coverHolder) {
		this(coverHolder, null);
	}
	
	public PatternContainer(ICoverable coverHolder, Consumer<Boolean> patternChangeCallback) {
		this.patternInventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {            	
            	return stack.getItem() instanceof ItemEncodedPattern;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
            	super.onContentsChanged(slot);
            	
            	ItemStack patternStack = patternInventory.getStackInSlot(0);
            	//Pattern slot will be null before UI is opened for the first time after world load
            	if(patternStack != null) {
            		if(patternStack.isEmpty()) {
            			onPatternRemoved();
            		} else {
            			onPatternInserted();
            		}
            	}
            	
            	coverHolder.markDirty();
            }
        };
        
        
        this.patternSlotOverlay = new TextureArea(new ResourceLocation(AppEng.MOD_ID, "textures/guis/states.png"), (15.0/16.0), (7.0/16.0), (1.0/16.0), (1.0/16.0));
        this.coverHolder = coverHolder;
        this.patternChangeCallback = patternChangeCallback;
	}
	
	protected void onPatternInserted() {
		setPatternAvailable(true);		
		patternInformation = getPattern().getPatternForItem(getPatternStack(), coverHolder.getWorld());
		
		if(patternSlot != null)
			patternSlot.setBackgroundTexture(GuiTextures.SLOT);
		
		if(patternChangeCallback != null)
			patternChangeCallback.accept(true);
	}
	
	
	protected void onPatternRemoved() {
		setPatternAvailable(false);
		
		if(patternSlot != null)
			patternSlot.setBackgroundTexture(GuiTextures.SLOT, patternSlotOverlay);
		
		if(patternChangeCallback != null)
			patternChangeCallback.accept(true);
	}
	
	public boolean isPatternAvailable() {
		return isPatternAvailable;
	}
	
	protected void setPatternAvailable(boolean available) {
		isPatternAvailable = available;
		
		if(!available) {
			patternInformation = null;
			inputItems = null;
			outputItems = null;
		}
	}
	
	public ItemStackHandler getPatternInventory() {
        return patternInventory;
    }
	
	public ItemStack getPatternStack() {
		return getPatternInventory().getStackInSlot(0);
	}
	
	public ItemEncodedPattern getPattern() {		
		if(!isPatternAvailable) {
			return null;
		}
		
		ItemStack patternStack = getPatternStack();		
		return (ItemEncodedPattern) patternStack.getItem();
	}
	
	public ICraftingPatternDetails getPatternDetails() {
		if(!isPatternAvailable) {
			return null;
		}
		
		if(patternInformation == null) {
			ItemEncodedPattern pattern = getPattern();
			if(pattern == null)
				return null;
			
			patternInformation = pattern.getPatternForItem(getPatternStack(), coverHolder.getWorld());
		}
		
		return patternInformation;
	}
	
	public IAEItemStack[] getInputItems() {
		if(!isPatternAvailable) {
			return null;
		}
		
		if(inputItems == null) {
			inputItems = getPatternDetails().getCondensedInputs();
		}
		
		return inputItems;
	}
	
	public IAEItemStack[] getOutputItems() {
		if(!isPatternAvailable) {
			return null;
		}
		
		if(outputItems == null) {
			ICraftingPatternDetails patternDetails = getPatternDetails();
			if(patternDetails == null)
				return new IAEItemStack[0];
			
			outputItems = patternDetails.getOutputs();
		}
		
		return outputItems;
	}
	
	public void initUI(int y, Consumer<Widget> widgetGroup) {
        widgetGroup.accept(new LabelWidget(32, y + 5, "cover.stocker.pattern.title"));
        this.patternSlot = new AE2PatternSlotWidget(patternInventory, 0, 11, y);
//        patternSlot.setDisplay(true);
        widgetGroup.accept(patternSlot.setBackgroundTexture(GuiTextures.SLOT, patternSlotOverlay));
    }
	
	@Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setTag("PatternInventory", patternInventory.serializeNBT());
        tagCompound.setBoolean("PatternAvailable", isPatternAvailable);
        return tagCompound;
    }
	
	 @Override
    public void deserializeNBT(NBTTagCompound tagCompound) {
		if(tagCompound.hasKey("PatternInventory")) {
		 	this.patternInventory.deserializeNBT(tagCompound.getCompoundTag("PatternInventory"));
		}
		
		if(tagCompound.hasKey("PatternAvailable")) {
			this.setPatternAvailable(tagCompound.getBoolean("PatternAvailable"));
		}
    }
}
