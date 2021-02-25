package com.soliddowant.gregtechenergistics.networking;

import com.soliddowant.gregtechenergistics.integration.jei.RecipeTransferHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

public class JEIPacket extends PacketCompressedNBT {
    public Collection<ItemStack> inputItems;
    public Collection<ItemStack> outputItems;
    public Collection<FluidStack> inputFluids;
    public Collection<FluidStack> outputFluids;
    public boolean isCraftingRecipe;

    public JEIPacket() {}

    public JEIPacket(Collection<ItemStack> inputItems, Collection<ItemStack> outputItems, Collection<FluidStack> inputFluids, Collection<FluidStack> outputFluids, boolean isCraftingRecipe) {
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
        if((tags = serializeArray(inputItems, ItemStack::serializeNBT)) != null)
            recipeTag.setTag("InputItems", tags);
        if((tags = serializeArray(outputItems, ItemStack::serializeNBT)) != null)
            recipeTag.setTag("OutputItems", tags);
        if((tags = serializeArray(inputFluids, stack -> stack.writeToNBT(new NBTTagCompound()))) != null)
            recipeTag.setTag("InputFluids", tags);
        if((tags = serializeArray(outputFluids, stack -> stack.writeToNBT(new NBTTagCompound()))) != null)
            recipeTag.setTag("OutputFluids", tags);

        recipeTag.setBoolean("IsCraftingRecipe", isCraftingRecipe);

        return recipeTag;
    }

    @Nullable
    protected <T> NBTTagList serializeArray(@Nullable Collection<T> serializableItems, @Nonnull Function<T, NBTTagCompound> serializer) {
        if(serializableItems == null || serializableItems.isEmpty())
            return null;

        NBTTagList tags = new NBTTagList();
        for(T stack : serializableItems) {
            if(stack == null)
                continue;
            tags.appendTag(serializer.apply(stack));
        }

        if(tags.hasNoTags())
            return null;

        return tags;
    }

    @Override
    public void deserialize(NBTTagCompound tag) {
        super.deserialize(tag);

        if(tag.hasKey("InputItems"))
            this.inputItems = deserializeArray(tag.getTagList("InputItems", 10), ItemStack::new);
        if(tag.hasKey("InputFluids"))
            this.inputFluids = deserializeArray(tag.getTagList("InputFluids", 10), FluidStack::loadFluidStackFromNBT);
        if(tag.hasKey("OutputItems"))
            this.outputItems = deserializeArray(tag.getTagList("OutputItems", 10), ItemStack::new);
        if(tag.hasKey("OutputFluids"))
            this.outputFluids = deserializeArray(tag.getTagList("OutputFluids", 10), FluidStack::loadFluidStackFromNBT);
        if(tag.hasKey("IsCraftingRecipe"))
            this.isCraftingRecipe = tag.getBoolean("IsCraftingRecipe");
    }

    @Nullable
    protected <T> Collection<T> deserializeArray(@Nullable NBTTagList tags, @Nonnull Function<NBTTagCompound, T> deserializer) {
        if(tags == null || tags.hasNoTags())
            return null;

        LinkedList<T> extractedItems = new LinkedList<>();
        for(NBTBase tag : tags) {
            if(!(tag instanceof NBTTagCompound))
                continue;

            extractedItems.add(deserializer.apply((NBTTagCompound) tag));
        }

        if(extractedItems.isEmpty())
            return null;

        return extractedItems;
    }

    public static class JEIHandler extends PacketCompressedNBT.Handler<JEIPacket> {
        @Override
        protected void handle(JEIPacket message, MessageContext context) {
            RecipeTransferHandler.transferToTerminal(message, context.getServerHandler().player.openContainer);
        }
    }
}