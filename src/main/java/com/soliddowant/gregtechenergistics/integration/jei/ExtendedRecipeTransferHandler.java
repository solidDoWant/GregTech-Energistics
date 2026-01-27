package com.soliddowant.gregtechenergistics.integration.jei;

import java.util.ArrayList;
import java.util.List;
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

        // Collect all inputs and outputs (unlimited size for outputs to allow
        // consolidation)
        List<ItemStack> inputItemsList = new ArrayList<>();
        List<ItemStack> outputItemsList = new ArrayList<>();
        List<FluidStack> inputFluidsList = new ArrayList<>();
        List<FluidStack> outputFluidsList = new ArrayList<>();

        // Fill sequentially for processing mode
        collectIngredientsFromJEI(recipeLayout.getItemStacks(), inputItemsList, outputItemsList,
                this::getFirstItemStack);
        collectIngredientsFromJEI(recipeLayout.getFluidStacks(), inputFluidsList, outputFluidsList,
                this::getFirstFluidStack);

        // Convert to arrays with size limits, consolidating outputs first
        ItemStack[] inputItems = itemListToArray(inputItemsList, 20, false);
        ItemStack[] outputItems = itemListToArray(outputItemsList, 12, true); // Consolidate outputs
        FluidStack[] inputFluids = fluidListToArray(inputFluidsList, 20, false);
        FluidStack[] outputFluids = fluidListToArray(outputFluidsList, 12, false);

        NetworkHandler.ServerHandlerChannel.sendToServer(
                new JEIPacket(
                        inputItems,
                        outputItems,
                        inputFluids,
                        outputFluids,
                        isCraftingRecipe));
    }

    protected <T> void collectIngredientsFromJEI(IGuiIngredientGroup<T> ingredientGroup,
            List<T> inputs, List<T> outputs, Function<Iterable<T>, T> getFirstStack) {
        // Processing mode only: fill sequentially, collecting ALL outputs without size
        // limit
        for (final var entry : ingredientGroup.getGuiIngredients().entrySet()) {
            IGuiIngredient<T> ingredientEntry = entry.getValue();

            if (ingredientEntry == null)
                continue;

            T currentStack = getFirstStack.apply(ingredientEntry.getAllIngredients());

            if (currentStack == null)
                continue;

            List<T> targetList = ingredientEntry.isInput() ? inputs : outputs;
            targetList.add(currentStack);
        }
    }

    protected ItemStack[] itemListToArray(List<ItemStack> stacks, int maxSize, boolean consolidate) {
        if (!consolidate) {
            // Just convert to array with size limit
            ItemStack[] result = new ItemStack[maxSize];
            for (int i = 0; i < maxSize && i < stacks.size(); i++)
                result[i] = stacks.get(i);

            return result;
        }

        // First consolidate identical stacks
        ItemStack[] allStacks = stacks.toArray(new ItemStack[0]);
        // consolidateStacks already limits the output array to maxSize
        return consolidateStacks(allStacks, maxSize);
    }

    protected FluidStack[] fluidListToArray(List<FluidStack> stacks, int maxSize, boolean consolidate) {
        FluidStack[] result = new FluidStack[maxSize];
        for (int i = 0; i < maxSize && i < stacks.size(); i++)
            result[i] = stacks.get(i);

        return result;
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

    public static void transferToExtendedTerminal(@Nonnull JEIPacket message, @Nonnull Container con) {
        // Get information about the crafting terminal, and do some checks
        if (!(con instanceof IContainerCraftingPacket))
            return;
        IContainerCraftingPacket cct = (IContainerCraftingPacket) con;
        if (cct.getNetworkNode() == null && !cct.getActionSource().machine().isPresent())
            return;

        IItemHandler craftMatrix = cct.getInventoryByName("crafting");
        int inputAreaSize = craftMatrix.getSlots(); // Should be 20

        IItemHandler outputInv = cct.getInventoryByName("output");
        int outputAreaSize = outputInv.getSlots(); // Should be 12

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

        // Consolidate identical output items
        outputStacks = consolidateStacks(outputStacks, outputAreaSize);

        for (int i = 0; i < outputStacks.length && i < outputAreaSize; i++) {
            ItemStack stack = outputStacks[i];
            ItemHandlerUtil.setStackInSlot(outputInv, i, stack != null ? stack : ItemStack.EMPTY);
        }
    }

    protected static ItemStack[] consolidateStacks(ItemStack[] stacks, int maxCount) {
        if (stacks == null || stacks.length == 0) {
            return stacks;
        }

        List<ItemStack> consolidated = new ArrayList<>();

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            // Try to find an existing stack to merge with
            boolean merged = false;
            for (ItemStack existing : consolidated) {
                if (ItemStack.areItemsEqual(stack, existing) &&
                        ItemStack.areItemStackTagsEqual(stack, existing)) {
                    // Calculate new count, checking for overflow
                    long newCount = (long) existing.getCount() + stack.getCount();
                    if (newCount <= Integer.MAX_VALUE) {
                        existing.setCount((int) newCount);
                        merged = true;
                        break;
                    }
                }
            }

            if (!merged) {
                consolidated.add(stack.copy());
            }
        }

        // Convert back to array
        ItemStack[] result = new ItemStack[maxCount];
        for (int i = 0; i < consolidated.size() && i < maxCount; i++) {
            result[i] = consolidated.get(i);
        }

        return result;
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
