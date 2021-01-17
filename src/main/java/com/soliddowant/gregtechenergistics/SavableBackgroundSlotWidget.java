package com.soliddowant.gregtechenergistics;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;

public class SavableBackgroundSlotWidget<T extends IItemHandler & INBTSerializable<NBTTagCompound>>
        extends BackgroundSlotWidget implements INBTSerializable<NBTTagCompound> {
    protected T slotHandler;
    public SavableBackgroundSlotWidget(T itemHandler, int slotIndex, int xPosition, int yPosition, boolean canTakeItems,
                                       boolean canPutItems) {
        super(itemHandler, slotIndex, xPosition, yPosition, canTakeItems, canPutItems);
        this.slotHandler = itemHandler;
    }

    @SuppressWarnings("unused")
    public SavableBackgroundSlotWidget(T itemHandler, int slotIndex, int xPosition, int yPosition) {
        this(itemHandler, slotIndex, xPosition, yPosition, true, true);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setTag("StackHandler", slotHandler.serializeNBT());
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tagCompound) {
        if(tagCompound.hasKey("StackHandler"))
            this.slotHandler.deserializeNBT(tagCompound.getCompoundTag("StackHandler"));
    }
}
