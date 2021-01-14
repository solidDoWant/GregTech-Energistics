package com.example.examplemod;

import appeng.api.config.Upgrades;
import appeng.items.materials.ItemMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.function.BiFunction;

public class SavableBackgroundSlotWidget<T extends IItemHandler & INBTSerializable<NBTTagCompound>>
        extends BackgroundSlotWidget implements INBTSerializable<NBTTagCompound> {
    protected T slotHandler;
    protected int slotIndex;
    public SavableBackgroundSlotWidget(T itemHandler, int slotIndex, int xPosition, int yPosition, boolean canTakeItems, boolean canPutItems) {
        super(itemHandler, slotIndex, xPosition, yPosition, canTakeItems, canPutItems);
        this.slotHandler = itemHandler;
        this.slotIndex = slotIndex;
    }

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
        if(tagCompound.hasKey("StackHandler")) {
            this.slotHandler.deserializeNBT(tagCompound.getCompoundTag("StackHandler"));
        }
    }

    protected void setSlotHandler(T slotHandler) {
        this.slotHandler = slotHandler;
        this.slotReference = createSlot(this.slotHandler, this.slotIndex);
    }
}
