package com.soliddowant.gregtechenergistics;

import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.SlotWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class CallbackSlotWidget implements INBTSerializable<NBTTagCompound> {
    protected ItemStackHandler slotHandler;
    protected boolean hasStack;
    protected Consumer<Boolean> contentsChangedCallback;

    public CallbackSlotWidget() {
        this.slotHandler = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return validateItemStack(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                onSlotChanged();
            }
        };
    }

    @Override
    @Nonnull
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SlotHandler", slotHandler.serializeNBT());

        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull NBTTagCompound nbt) {
        if(nbt.hasKey("SlotHandler")) {
            slotHandler.deserializeNBT(nbt.getCompoundTag("SlotHandler"));
            if(!getSlotStack().isEmpty())
                hasStack = true;

            onSlotDeserialization();
        }
    }

    public void initUI(int x, int y, @Nonnull Consumer<Widget> widgetGroup) {
        widgetGroup.accept(createSlotWidget(x, y));
    }

    @Nonnull
    protected SlotWidget createSlotWidget(int x, int y) {
        return new SavableBackgroundSlotWidget<>(slotHandler, 0, x, y, true,true);
    }

    @SuppressWarnings("unused")
    public boolean validateItemStack(@Nonnull ItemStack stack) {
        return true;
    }

    protected void onSlotChanged() {
        ItemStack slotStack = getSlotStack();

        hasStack = !slotStack.isEmpty();
        if(hasStack)
            onSlotInserted(slotStack);
        else
            onSlotRemoved();
        maybeCallChangedCallback();
}

    @Nonnull
    public ItemStack getSlotStack() {
        return this.slotHandler.getStackInSlot(0);
    }

    @SuppressWarnings("unused")
    protected void onSlotInserted(@Nonnull ItemStack slotStack) {
        updateData(slotStack);
    }

    protected void onSlotRemoved() {
        updateData(null);
    }

    public boolean hasStack() {
        return hasStack;
    }

    public void setContentsChangedCallback(@Nullable Consumer<Boolean> callback) {
        this.contentsChangedCallback = callback;
    }

    protected void maybeCallChangedCallback() {
        if(contentsChangedCallback != null)
            contentsChangedCallback.accept(hasStack);
    }

    @Nonnull
    public ItemStackHandler getSlotHandler() {
        return slotHandler;
    }

    protected void onSlotDeserialization() {
        updateData(getSlotStack());
    }

    protected void updateData(@Nullable ItemStack slotStack) {
        if(slotStack == null || slotStack.isEmpty())
            clearData();
        else
            setData(slotStack);
    }

    protected void clearData() {
    }

    protected void setData(@Nonnull ItemStack slotStack) {
    }
}
