package com.soliddowant.gregtechenergistics.integration.jei;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.WrapperInvItemHandler;
import com.soliddowant.gregtechenergistics.items.MetaItems;
import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;
import com.soliddowant.gregtechenergistics.networking.JEIPacket;
import com.soliddowant.gregtechenergistics.networking.NetworkHandler;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

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

    @SuppressWarnings("unused")
    protected void performTransfer(ContainerPatternTerm container, IRecipeLayout recipeLayout, EntityPlayer player,
                                   boolean maxTransfer) {
        boolean isCraftingRecipe = recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING);

        ItemStack[] inputItems = new ItemStack[9];
        ItemStack[] outputItems = new ItemStack[3];
        FluidStack[] inputFluids = new FluidStack[9];
        FluidStack[] outputFluids = new FluidStack[3];

        if (isCraftingRecipe) {
            // Crafting recipes: preserve slot indices (with JEI's 1-based offset)
            performItemTransferWithSlots(recipeLayout.getItemStacks(), inputItems, outputItems, true);
            performFluidTransferWithSlots(recipeLayout.getFluidStacks(), inputFluids, outputFluids, true);
        } else {
            // Processing recipes: fill sequentially (ignore slot indices)
            performItemTransferWithSlots(recipeLayout.getItemStacks(), inputItems, outputItems, false);
            performFluidTransferWithSlots(recipeLayout.getFluidStacks(), inputFluids, outputFluids, false);
        }

        NetworkHandler.ServerHandlerChannel.sendToServer(
                new JEIPacket(
                        inputItems,
                        outputItems,
                        inputFluids,
                        outputFluids,
                        isCraftingRecipe
                )
        );
    }

    protected void performItemTransferWithSlots(IGuiIngredientGroup<ItemStack> itemStacks,
                                                ItemStack[] inputs, ItemStack[] outputs, boolean preserveSlots) {
        if (preserveSlots) {
            // Crafting mode: preserve exact slot positions (with JEI 1-based offset correction)
            for (final var entry : itemStacks.getGuiIngredients().entrySet()) {
                Integer jeiSlotIndex = entry.getKey();
                IGuiIngredient<ItemStack> ingredientEntry = entry.getValue();

                if (ingredientEntry == null)
                    continue;

                ItemStack currentStack = getFirstItemStack(ingredientEntry.getAllIngredients());

                if (currentStack != null) {
                    // JEI uses 1-based indexing, convert to 0-based for array indexing
                    int slotIndex = jeiSlotIndex - 1;

                    if (ingredientEntry.isInput() && slotIndex >= 0 && slotIndex < inputs.length) {
                        inputs[slotIndex] = currentStack;
                    } else if (!ingredientEntry.isInput() && slotIndex >= 0 && slotIndex < outputs.length) {
                        outputs[slotIndex] = currentStack;
                    }
                }
            }
        } else {
            // Processing mode: fill sequentially, ignore JEI's slot indices
            int inputIndex = 0;
            int outputIndex = 0;

            for (final var entry : itemStacks.getGuiIngredients().entrySet()) {
                IGuiIngredient<ItemStack> ingredientEntry = entry.getValue();

                if (ingredientEntry == null)
                    continue;

                ItemStack currentStack = getFirstItemStack(ingredientEntry.getAllIngredients());

                if (currentStack != null) {
                    if (ingredientEntry.isInput() && inputIndex < inputs.length) {
                        inputs[inputIndex++] = currentStack;
                    } else if (!ingredientEntry.isInput() && outputIndex < outputs.length) {
                        outputs[outputIndex++] = currentStack;
                    }
                }
            }
        }
    }

    protected void performItemTransfer(IGuiIngredientGroup<ItemStack> itemStacks, Consumer<ItemStack> addInput,
                                       Consumer<ItemStack> addOutput) {
        performInternalTransfer(itemStacks, addInput, addOutput, this::getFirstItemStack);
    }

    @Nullable
    protected ItemStack getFirstItemStack(@Nullable Iterable<ItemStack> stackList) {
        if(stackList == null)
            return null;

        // First try to get prioritized ItemStacks (i.e. pure AE2 crystals)
        ItemStack firstValidItem = null;
        for(ItemStack stack : stackList) {
            if (stack == null || stack.isEmpty())
                continue;

            if (Platform.isRecipePrioritized(stack))
                return stack;

            if(firstValidItem == null)
                firstValidItem = stack;
        }

        // If that fails, get the first valid item. This may be null.
        return firstValidItem;
    }

    protected void performFluidTransferWithSlots(IGuiIngredientGroup<FluidStack> fluidStacks,
                                                 FluidStack[] inputs, FluidStack[] outputs, boolean preserveSlots) {
        if (preserveSlots) {
            // Crafting mode: preserve exact slot positions (with JEI 1-based offset correction)
            for (final var entry : fluidStacks.getGuiIngredients().entrySet()) {
                Integer jeiSlotIndex = entry.getKey();
                IGuiIngredient<FluidStack> ingredientEntry = entry.getValue();

                if (ingredientEntry == null)
                    continue;

                FluidStack currentStack = getFirstFluidStack(ingredientEntry.getAllIngredients());

                if (currentStack != null) {
                    // JEI uses 1-based indexing, convert to 0-based for array indexing
                    int slotIndex = jeiSlotIndex - 1;

                    if (ingredientEntry.isInput() && slotIndex >= 0 && slotIndex < inputs.length) {
                        inputs[slotIndex] = currentStack;
                    } else if (!ingredientEntry.isInput() && slotIndex >= 0 && slotIndex < outputs.length) {
                        outputs[slotIndex] = currentStack;
                    }
                }
            }
        } else {
            // Processing mode: fill sequentially, ignore JEI's slot indices
            int inputIndex = 0;
            int outputIndex = 0;

            for (final var entry : fluidStacks.getGuiIngredients().entrySet()) {
                IGuiIngredient<FluidStack> ingredientEntry = entry.getValue();

                if (ingredientEntry == null)
                    continue;

                FluidStack currentStack = getFirstFluidStack(ingredientEntry.getAllIngredients());

                if (currentStack != null) {
                    if (ingredientEntry.isInput() && inputIndex < inputs.length) {
                        inputs[inputIndex++] = currentStack;
                    } else if (!ingredientEntry.isInput() && outputIndex < outputs.length) {
                        outputs[outputIndex++] = currentStack;
                    }
                }
            }
        }
    }

    protected void performFluidTransfer(IGuiIngredientGroup<FluidStack> fluidStacks, Consumer<FluidStack> addInput,
                                        Consumer<FluidStack> addOutput) {
        performInternalTransfer(fluidStacks, addInput, addOutput, this::getFirstFluidStack);
    }

    @Nullable
    protected FluidStack getFirstFluidStack(@Nullable Iterable<FluidStack> stackList) {
        if(stackList == null)
            return null;

        for(FluidStack stack : stackList)
            if(stack != null && stack.amount > 0)
                return stack;

        return null;
    }

    protected <T> void performInternalTransfer(@Nonnull IGuiIngredientGroup<T> stacks, Consumer<T> addInput,
                                               Consumer<T> addOutput, Function<Iterable<T>, T> getStack) {
        for (final IGuiIngredient<T> ingredientEntry : stacks.getGuiIngredients().values()) {
            if (ingredientEntry == null)
                continue;

            T currentStack = getStack.apply(ingredientEntry.getAllIngredients());

            if(currentStack != null)
                (ingredientEntry.isInput() ? addInput : addOutput).accept(currentStack);
        }
    }

    public static void transferToTerminal(JEIPacket message, Container con) {
        // Get information about the crafting terminal, and do some checks
        if(!(con instanceof IContainerCraftingPacket))
            return;
        IContainerCraftingPacket cct = (IContainerCraftingPacket) con;
        if (cct.getNetworkNode() == null && !cct.getActionSource().machine().isPresent())
            return;

        if(cct instanceof ContainerPatternTerm)
            ((ContainerPatternTerm) cct).getPart().setCraftingRecipe(message.isCraftingRecipe);

        IItemHandler craftMatrix = cct.getInventoryByName("crafting");
        // Should always be 9, but craftMatrix.getSlots() is the real limiting factor regardless of what it returns
        int inputAreaSize = craftMatrix.getSlots();

        IItemHandler outputInv = cct.getInventoryByName("output");
        int outputAreaSize = outputInv.getSlots();

        if (!(con instanceof ContainerPatternTerm))
            return;

        ItemStack[] inputStacks = mergeStacks(message.inputItems, message.inputFluids, inputAreaSize, message.isCraftingRecipe);

        for(int i = 0; i < inputStacks.length && i < inputAreaSize; i++) {
            ItemStack stack = inputStacks[i];
            ItemHandlerUtil.setStackInSlot(craftMatrix, i, stack != null ? stack : ItemStack.EMPTY);
        }

        if(message.isCraftingRecipe)
            con.onCraftMatrixChanged(new WrapperInvItemHandler(craftMatrix));
        else {
            ItemStack[] outputStacks = mergeStacks(message.outputItems, message.outputFluids, outputAreaSize, message.isCraftingRecipe);

            for(int i = 0; i < outputStacks.length && i < outputAreaSize; i++) {
                ItemStack stack = outputStacks[i];
                ItemHandlerUtil.setStackInSlot(outputInv, i, stack != null ? stack : ItemStack.EMPTY);
            }
        }
    }

    protected static ItemStack[] mergeStacks(ItemStack[] items, FluidStack[] fluids, int maxCount, boolean preserveSlots) {
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
                        ItemStack fluidEncoder = MetaItems.FLUID_ENCODER.getStackForm();
                        FluidEncoderBehaviour.setItemStackFluid(fluidEncoder, fluid);
                        FluidEncoderBehaviour.setItemStackFluidAmount(fluidEncoder, fluid.amount);
                        result[i] = fluidEncoder;
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
                        ItemStack fluidEncoder = MetaItems.FLUID_ENCODER.getStackForm();
                        FluidEncoderBehaviour.setItemStackFluid(fluidEncoder, fluid);
                        FluidEncoderBehaviour.setItemStackFluidAmount(fluidEncoder, fluid.amount);
                        result[currentIndex++] = fluidEncoder;
                    }
                }
            }
        }

        return result;
    }
}
