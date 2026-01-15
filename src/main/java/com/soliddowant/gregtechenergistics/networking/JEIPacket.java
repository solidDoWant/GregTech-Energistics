package com.soliddowant.gregtechenergistics.networking;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.soliddowant.gregtechenergistics.integration.jei.RecipeTransferHandler;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class JEIPacket extends PacketCompressedNBT {
    public ItemStack[] inputItems;
    public ItemStack[] outputItems;
    public FluidStack[] inputFluids;
    public FluidStack[] outputFluids;
    public boolean isCraftingRecipe;

    public JEIPacket() {
    }

    public JEIPacket(ItemStack[] inputItems, ItemStack[] outputItems,
            FluidStack[] inputFluids, FluidStack[] outputFluids, boolean isCraftingRecipe) {
        this.inputItems = inputItems;
        this.outputItems = outputItems;
        this.inputFluids = inputFluids;
        this.outputFluids = outputFluids;
        this.isCraftingRecipe = isCraftingRecipe;
    }

    @Override
    public NBTTagCompound serialize() {
        NBTTagCompound recipeTag = super.serialize();

        NBTTagList tags;
        if ((tags = serializeArray(inputItems, ItemStack::serializeNBT)) != null)
            recipeTag.setTag("InputItems", tags);
        if ((tags = serializeArray(outputItems, ItemStack::serializeNBT)) != null)
            recipeTag.setTag("OutputItems", tags);
        if ((tags = serializeArray(inputFluids, stack -> stack.writeToNBT(new NBTTagCompound()))) != null)
            recipeTag.setTag("InputFluids", tags);
        if ((tags = serializeArray(outputFluids, stack -> stack.writeToNBT(new NBTTagCompound()))) != null)
            recipeTag.setTag("OutputFluids", tags);

        recipeTag.setBoolean("IsCraftingRecipe", isCraftingRecipe);

        return recipeTag;
    }

    @Nullable
    protected <T> NBTTagList serializeArray(@Nullable T[] serializableItems,
            @Nonnull Function<T, NBTTagCompound> serializer) {
        if (serializableItems == null || serializableItems.length == 0)
            return null;

        NBTTagList tags = new NBTTagList();
        for (T stack : serializableItems) {
            if (stack == null) {
                // Add empty tag to preserve slot index
                tags.appendTag(new NBTTagCompound());
            } else {
                tags.appendTag(serializer.apply(stack));
            }
        }

        if (tags.isEmpty())
            return null;

        return tags;
    }

    @Override
    public void deserialize(NBTTagCompound tag) {
        super.deserialize(tag);

        if (tag.hasKey("InputItems"))
            this.inputItems = deserializeItemStackArray(tag.getTagList("InputItems", 10));
        if (tag.hasKey("InputFluids"))
            this.inputFluids = deserializeFluidStackArray(tag.getTagList("InputFluids", 10));
        if (tag.hasKey("OutputItems"))
            this.outputItems = deserializeItemStackArray(tag.getTagList("OutputItems", 10));
        if (tag.hasKey("OutputFluids"))
            this.outputFluids = deserializeFluidStackArray(tag.getTagList("OutputFluids", 10));
        if (tag.hasKey("IsCraftingRecipe"))
            this.isCraftingRecipe = tag.getBoolean("IsCraftingRecipe");
    }

    @Nullable
    protected ItemStack[] deserializeItemStackArray(@Nullable NBTTagList tags) {
        if (tags == null || tags.isEmpty())
            return null;

        ItemStack[] extractedItems = new ItemStack[tags.tagCount()];
        for (int i = 0; i < tags.tagCount(); i++) {
            NBTBase tag = tags.get(i);
            if (!(tag instanceof NBTTagCompound))
                continue;

            NBTTagCompound compound = (NBTTagCompound) tag;
            // Empty compound means null/empty slot
            if (compound.isEmpty()) {
                extractedItems[i] = null;
            } else {
                extractedItems[i] = new ItemStack(compound);
            }
        }

        return extractedItems;
    }

    @Nullable
    protected FluidStack[] deserializeFluidStackArray(@Nullable NBTTagList tags) {
        if (tags == null || tags.isEmpty())
            return null;

        FluidStack[] extractedItems = new FluidStack[tags.tagCount()];
        for (int i = 0; i < tags.tagCount(); i++) {
            NBTBase tag = tags.get(i);
            if (!(tag instanceof NBTTagCompound))
                continue;

            NBTTagCompound compound = (NBTTagCompound) tag;
            // Empty compound means null/empty slot
            if (compound.isEmpty()) {
                extractedItems[i] = null;
            } else {
                extractedItems[i] = FluidStack.loadFluidStackFromNBT(compound);
            }
        }

        return extractedItems;
    }

    public static class JEIHandler extends PacketCompressedNBT.Handler<JEIPacket> {
        @Override
        protected void handle(JEIPacket message, MessageContext context) {
            RecipeTransferHandler.transferToTerminal(message, context.getServerHandler().player.openContainer);
        }
    }
}