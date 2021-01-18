package com.soliddowant.gregtechenergistics.capability.impl;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Efficiently delegates calls into multiple item handlers
 */
public class ItemHandlerListFixed implements IItemHandlerModifiable {
    protected TIntObjectMap<IItemHandler> handlerBySlotIndex = new TIntObjectHashMap<>();
    protected Map<IItemHandler, Integer> baseIndexOffset = new IdentityHashMap<>();

    public ItemHandlerListFixed(@Nonnull List<? extends IItemHandler> itemHandlerList) {
        int currentSlotIndex = 0;
        for (IItemHandler itemHandler : itemHandlerList) {
            if (baseIndexOffset.containsKey(itemHandler))
                throw new IllegalArgumentException("Attempted to add item handler " + itemHandler + " twice");

            baseIndexOffset.put(itemHandler, currentSlotIndex);

            int slotsCount = itemHandler.getSlots();
            for (int slotIndex = 0; slotIndex < slotsCount; slotIndex++)
                handlerBySlotIndex.put(currentSlotIndex + slotIndex, itemHandler);

            currentSlotIndex += slotsCount;
        }
    }

    @Override
    public int getSlots() {
        return handlerBySlotIndex.size();
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        IItemHandler itemHandler = handlerBySlotIndex.get(slot);

        if (!(itemHandler instanceof IItemHandlerModifiable))
            throw new UnsupportedOperationException("Handler " + itemHandler + " does not support this method");

        ((IItemHandlerModifiable) itemHandler).setStackInSlot(slot - baseIndexOffset.get(itemHandler), stack);
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return performRealHandlerFunction(slot, IItemHandler::getStackInSlot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return performRealHandlerFunction(slot, IItemHandler::getSlotLimit);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return performRealHandlerFunction(slot, (handler, realSlot) -> handler.insertItem(realSlot, stack, simulate));
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return performRealHandlerFunction(slot, (handler, realSlot) -> handler.extractItem(realSlot, amount, simulate));
    }

    public int getRealSlot(@Nonnull IItemHandler itemHandler, int slot) {
        return slot - baseIndexOffset.get(itemHandler);
    }

    public <T> T performRealHandlerFunction(int slot, @Nonnull BiFunction<IItemHandler, Integer, T> functionToRun) {
        IItemHandler itemHandler = handlerBySlotIndex.get(slot);
        return functionToRun.apply(itemHandler, getRealSlot(itemHandler, slot));
    }
}
