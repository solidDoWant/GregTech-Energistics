package com.soliddowant.gregtechenergistics.integration.jei;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.soliddowant.gregtechenergistics.gui.ExtendedPatternContainer;
import com.soliddowant.gregtechenergistics.items.MetaItems;
import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;
import com.soliddowant.gregtechenergistics.networking.JEIPacket;
import com.soliddowant.gregtechenergistics.networking.NetworkHandler;

import appeng.helpers.IContainerCraftingPacket;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

public class ExtendedRecipeTransferHandler implements IRecipeTransferHandler<ExtendedPatternContainer> {
    @Nonnull
    @Override
    public Class<ExtendedPatternContainer> getContainerClass() {
        return ExtendedPatternContainer.class;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(@Nonnull ExtendedPatternContainer container,
            @Nonnull IRecipeLayout recipeLayout, @Nonnull EntityPlayer player,
            boolean maxTransfer, boolean doTransfer) {
        if (doTransfer)
            performTransfer(container, recipeLayout, player, maxTransfer);

        return null;
    }

    protected void performTransfer(ExtendedPatternContainer container, IRecipeLayout recipeLayout, EntityPlayer player,
            boolean maxTransfer) {
        // Extended pattern terminal only supports processing mode
        boolean isCraftingRecipe = false;

        ItemStack[] inputItems = new ItemStack[9];   // 3x3 grid
        ItemStack[] outputItems = new ItemStack[12]; // 12 output slots
        FluidStack[] inputFluids = new FluidStack[9];
        FluidStack[] outputFluids = new FluidStack[12];

        // Fill sequentially for processing mode
        performTransferWithSlots(recipeLayout.getItemStacks(), inputItems, outputItems, isCraftingRecipe,
                this::getFirstItemStack);
        performTransferWithSlots(recipeLayout.getFluidStacks(), inputFluids, outputFluids, isCraftingRecipe,
                this::getFirstFluidStack);

        NetworkHandler.ServerHandlerChannel.sendToServer(
                new JEIPacket(
                        inputItems,
                        outputItems,
                        inputFluids,
                        outputFluids,
                        isCraftingRecipe));
    }

    protected <T> void performTransferWithSlots(IGuiIngredientGroup<T> ingredientGroup,
            T[] inputs, T[] outputs, boolean preserveSlots, Function<Iterable<T>, T> getFirstStack) {
        // Processing mode only: fill sequentially
        int inputIndex = 0;
        int outputIndex = 0;

        for (final var entry : ingredientGroup.getGuiIngredients().entrySet()) {
            IGuiIngredient<T> ingredientEntry = entry.getValue();

            if (ingredientEntry == null)
                continue;

            T currentStack = getFirstStack.apply(ingredientEntry.getAllIngredients());

            if (currentStack == null)
                continue;

            if (ingredientEntry.isInput()) {
                if (inputIndex < inputs.length)
                    inputs[inputIndex++] = currentStack;
            } else {
                if (outputIndex < outputs.length)
                    outputs[outputIndex++] = currentStack;
            }
        }
    }

    @Nullable
    protected ItemStack getFirstItemStack(@Nullable Iterable<ItemStack> stackList) {
        if (stackList == null)
            return null;

        // First try to get prioritized ItemStacks (i.e. pure AE2 crystals)
        ItemStack firstValidItem = null;
        for (ItemStack stack : stackList) {
            if (stack == null || stack.isEmpty())
                continue;

            if (Platform.isRecipePrioritized(stack))
                return stack;

            if (firstValidItem == null)
                firstValidItem = stack;
        }

        // If that fails, get the first valid item. This may be null.
        return firstValidItem;
    }

    @Nullable
    protected FluidStack getFirstFluidStack(@Nullable Iterable<FluidStack> stackList) {
        if (stackList == null)
            return null;

        for (FluidStack stack : stackList)
            if (stack != null && stack.amount > 0)
                return stack;

        return null;
    }

    protected static ItemStack createFluidEncoder(FluidStack fluid) {
        ItemStack fluidEncoder = MetaItems.FLUID_ENCODER.getStackForm();
        FluidEncoderBehaviour.setItemStackFluid(fluidEncoder, fluid);
        FluidEncoderBehaviour.setItemStackFluidAmount(fluidEncoder, fluid.amount);
        return fluidEncoder;
    }

    public static void transferToExtendedTerminal(JEIPacket message, Container con) {
        // Get information about the crafting terminal, and do some checks
        if (!(con instanceof IContainerCraftingPacket))
            return;
        IContainerCraftingPacket cct = (IContainerCraftingPacket) con;
        if (cct.getNetworkNode() == null && !cct.getActionSource().machine().isPresent())
            return;

        IItemHandler craftMatrix = cct.getInventoryByName("crafting");
        int inputAreaSize = craftMatrix.getSlots();  // Should be 9

        IItemHandler outputInv = cct.getInventoryByName("output");
        int outputAreaSize = outputInv.getSlots();  // Should be 12

        if (!(con instanceof ExtendedPatternContainer))
            return;

        ItemStack[] inputStacks = mergeStacks(message.inputItems, message.inputFluids, inputAreaSize,
                message.isCraftingRecipe);

        for (int i = 0; i < inputStacks.length && i < inputAreaSize; i++) {
            ItemStack stack = inputStacks[i];
            ItemHandlerUtil.setStackInSlot(craftMatrix, i, stack != null ? stack : ItemStack.EMPTY);
        }

        // Extended pattern terminal is always processing mode
        ItemStack[] outputStacks = mergeStacks(message.outputItems, message.outputFluids, outputAreaSize,
                message.isCraftingRecipe);

        for (int i = 0; i < outputStacks.length && i < outputAreaSize; i++) {
            ItemStack stack = outputStacks[i];
            ItemHandlerUtil.setStackInSlot(outputInv, i, stack != null ? stack : ItemStack.EMPTY);
        }
    }

    protected static ItemStack[] mergeStacks(ItemStack[] items, FluidStack[] fluids, int maxCount,
            boolean preserveSlots) {
        ItemStack[] result = new ItemStack[maxCount];

        // Processing mode only: append fluids after items sequentially
        int currentIndex = 0;

        // First pass: add items sequentially
        if (items != null) {
            for (int i = 0; i < items.length && currentIndex < maxCount; i++) {
                ItemStack item = items[i];
                if (item != null && !item.isEmpty()) {
                    result[currentIndex++] = item;
                }
            }
        }

        // Second pass: add fluids as fluid encoders sequentially after items
        if (fluids != null) {
            for (int i = 0; i < fluids.length && currentIndex < maxCount; i++) {
                FluidStack fluid = fluids[i];
                if (fluid != null && fluid.amount > 0) {
                    result[currentIndex++] = createFluidEncoder(fluid);
                }
            }
        }

        return result;
    }
}
