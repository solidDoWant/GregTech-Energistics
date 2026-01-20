package com.soliddowant.gregtechenergistics.integration.jei;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.soliddowant.gregtechenergistics.items.MetaItems;
import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;
import com.soliddowant.gregtechenergistics.networking.JEIPacket;
import com.soliddowant.gregtechenergistics.networking.NetworkHandler;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.WrapperInvItemHandler;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

public class RecipeTransferHandler implements IRecipeTransferHandler<ContainerPatternTerm> {
    @Nonnull
    @Override
    public Class<ContainerPatternTerm> getContainerClass() {
        return ContainerPatternTerm.class;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(@Nonnull ContainerPatternTerm container,
            @Nonnull IRecipeLayout recipeLayout, @Nonnull EntityPlayer player,
            boolean maxTransfer, boolean doTransfer) {
        if (doTransfer)
            performTransfer(container, recipeLayout, player, maxTransfer);

        return null;
    }

    protected void performTransfer(ContainerPatternTerm container, IRecipeLayout recipeLayout, EntityPlayer player,
            boolean maxTransfer) {
        boolean isCraftingRecipe = recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING);

        ItemStack[] inputItems = new ItemStack[9];
        ItemStack[] outputItems = new ItemStack[3];
        FluidStack[] inputFluids = new FluidStack[9];
        FluidStack[] outputFluids = new FluidStack[3];

        // Crafting recipes: preserve slot indices (with JEI's 1-based offset)
        // Processing recipes: fill sequentially (ignore slot indices)
        performTransferWithSlots(recipeLayout.getItemStacks(), inputItems, outputItems, isCraftingRecipe,
                this::getFirstItemStack);
        // For fluids, always use processing mode (sequential fill) to avoid slot
        // conflicts
        performTransferWithSlots(recipeLayout.getFluidStacks(), inputFluids, outputFluids, false,
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
        if (ingredientGroup == null || ingredientGroup.getGuiIngredients() == null)
            return;

        if (preserveSlots) {
            // Crafting mode: preserve exact slot positions (with JEI 1-based offset
            // correction)
            for (final var entry : ingredientGroup.getGuiIngredients().entrySet()) {
                Integer jeiSlotIndex = entry.getKey();
                IGuiIngredient<T> ingredientEntry = entry.getValue();

                if (ingredientEntry == null)
                    continue;

                T currentStack = getFirstStack.apply(ingredientEntry.getAllIngredients());

                if (currentStack == null)
                    continue;

                // JEI uses 1-based indexing, convert to 0-based for array indexing
                int slotIndex = jeiSlotIndex - 1;

                // Range check the slot index
                if (slotIndex < 0)
                    continue;

                if (ingredientEntry.isInput()) {
                    if (slotIndex < inputs.length)
                        inputs[slotIndex] = currentStack;
                } else {
                    if (slotIndex < outputs.length)
                        outputs[slotIndex] = currentStack;
                }
            }

            return;
        }

        // Processing mode: fill sequentially, ignore JEI's slot indices
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

    protected static boolean hasAnyFluids(@Nullable FluidStack[] fluids) {
        if (fluids == null)
            return false;

        for (FluidStack fluid : fluids) {
            if (fluid != null && fluid.amount > 0)
                return true;
        }

        return false;
    }

    public static void transferToTerminal(JEIPacket message, Container con) {
        // Get information about the crafting terminal, and do some checks
        if (!(con instanceof IContainerCraftingPacket))
            return;
        IContainerCraftingPacket cct = (IContainerCraftingPacket) con;
        if (cct.getNetworkNode() == null && !cct.getActionSource().machine().isPresent())
            return;

        if (cct instanceof ContainerPatternTerm)
            ((ContainerPatternTerm) cct).getPart().setCraftingRecipe(message.isCraftingRecipe);

        IItemHandler craftMatrix = cct.getInventoryByName("crafting");
        // Should always be 9, but craftMatrix.getSlots() is the real limiting factor
        // regardless of what it returns
        int inputAreaSize = craftMatrix.getSlots();

        IItemHandler outputInv = cct.getInventoryByName("output");
        int outputAreaSize = outputInv.getSlots();

        if (!(con instanceof ContainerPatternTerm))
            return;

        // If there are fluids, always use processing mode to avoid slot conflicts in
        // crafting mode
        boolean hasInputFluids = hasAnyFluids(message.inputFluids);
        boolean preserveSlots = message.isCraftingRecipe && !hasInputFluids;

        ItemStack[] inputStacks = mergeStacks(message.inputItems, message.inputFluids, inputAreaSize,
                preserveSlots);

        for (int i = 0; i < inputStacks.length && i < inputAreaSize; i++) {
            ItemStack stack = inputStacks[i];
            ItemHandlerUtil.setStackInSlot(craftMatrix, i, stack != null ? stack : ItemStack.EMPTY);
        }

        if (message.isCraftingRecipe)
            con.onCraftMatrixChanged(new WrapperInvItemHandler(craftMatrix));
        else {
            ItemStack[] outputStacks = mergeStacks(message.outputItems, message.outputFluids, outputAreaSize, false);

            for (int i = 0; i < outputStacks.length && i < outputAreaSize; i++) {
                ItemStack stack = outputStacks[i];
                ItemHandlerUtil.setStackInSlot(outputInv, i, stack != null ? stack : ItemStack.EMPTY);
            }
        }
    }

    protected static ItemStack[] mergeStacks(ItemStack[] items, FluidStack[] fluids, int maxCount,
            boolean preserveSlots) {
        ItemStack[] result = new ItemStack[maxCount];

        if (preserveSlots) {
            // Crafting mode: preserve slot indices, fluids only fill empty slots
            // First pass: add items preserving their slot indices
            if (items != null) {
                for (int i = 0; i < items.length && i < maxCount; i++) {
                    ItemStack item = items[i];
                    if (item != null && !item.isEmpty()) {
                        result[i] = item;
                    }
                }
            }

            // Second pass: add fluids as fluid encoders preserving their slot indices
            if (fluids != null) {
                for (int i = 0; i < fluids.length && i < maxCount; i++) {
                    FluidStack fluid = fluids[i];
                    // Only add fluid encoder if slot is empty and fluid is valid
                    if (fluid != null && fluid.amount > 0 && result[i] == null) {
                        result[i] = createFluidEncoder(fluid);
                    }
                }
            }
        } else {
            // Processing mode: append fluids after items sequentially
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
        }

        return result;
    }
}
