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
import java.util.Collection;
import java.util.LinkedList;
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
        // Collect inputs
        LinkedList<ItemStack> inputItemTags = new LinkedList<>();
        LinkedList<ItemStack> outputItemTags = new LinkedList<>();
        performItemTransfer(
                recipeLayout.getItemStacks(),
                stack -> {if(inputItemTags.size() < 9 && stack != null) inputItemTags.add(stack);},
                stack -> {if(outputItemTags.size() < 3 && stack != null) outputItemTags.add(stack);}
                );

        // Collect outputs
        LinkedList<FluidStack> inputFluidTags = new LinkedList<>();
        LinkedList<FluidStack> outputFluidTags = new LinkedList<>();
        performFluidTransfer(
                recipeLayout.getFluidStacks(),
                stack -> {if(inputItemTags.size() + inputFluidTags.size() < 9 && stack != null) inputFluidTags.add(stack);},
                stack -> {if(outputItemTags.size() + outputFluidTags.size() <= 3 && stack != null) outputFluidTags.add(stack);}
                );

        boolean isCraftingRecipe = recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING);

        NetworkHandler.ServerHandlerChannel.sendToServer(
                new JEIPacket(
                        inputItemTags,
                        outputItemTags,
                        inputFluidTags,
                        outputFluidTags,
                        isCraftingRecipe
                )
        );
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
            ((ContainerPatternTerm) cct).getPatternTerminal().setCraftingRecipe(message.isCraftingRecipe);

        IItemHandler craftMatrix = cct.getInventoryByName("crafting");
        // Should always be 9, but craftMatrix.getSlots() is the real limiting factor regardless of what it returns
        int inputAreaSize = craftMatrix.getSlots();

        IItemHandler outputInv = cct.getInventoryByName("output");
        int outputAreaSize = outputInv.getSlots();

        if (!(con instanceof ContainerPatternTerm))
            return;

        LinkedList<ItemStack> inputStacks = collectStacks(message.inputItems, message.inputFluids, inputAreaSize);

        int inputIndex = 0;
        for(ItemStack inputStack : inputStacks)
            ItemHandlerUtil.setStackInSlot(craftMatrix, inputIndex++, inputStack);

        if(message.isCraftingRecipe)
            con.onCraftMatrixChanged(new WrapperInvItemHandler(craftMatrix));
        else {
            LinkedList<ItemStack> outputStacks = collectStacks(message.outputItems, message.outputFluids, outputAreaSize);

            int outputIndex = 0;
            for (ItemStack outputStack : outputStacks)
                ItemHandlerUtil.setStackInSlot(outputInv, outputIndex++, outputStack);
        }
    }

    protected static LinkedList<ItemStack> collectStacks(Collection<ItemStack> items, Collection<FluidStack> fluids, int maxCount) {
        int addedStackCount = 0;
        LinkedList<ItemStack> itemStacks = new LinkedList<>();
        if(items != null)
            for (ItemStack item : items) {
                if(item == null)
                    continue;

                if (addedStackCount >= maxCount)
                    break;

                itemStacks.add(item);
                addedStackCount++;
            }

        if(fluids != null)
            for (FluidStack fluid : fluids) {
                if(fluid == null || fluid.amount == 0)
                    continue;

                if (addedStackCount >= maxCount)
                    break;

                ItemStack fluidEncoder = MetaItems.FLUID_ENCODER.getStackForm();
                FluidEncoderBehaviour.setItemStackFluid(fluidEncoder, fluid);
                FluidEncoderBehaviour.setItemStackFluidAmount(fluidEncoder, fluid.amount);
                itemStacks.add(fluidEncoder);
                addedStackCount++;
            }

        // Blank out the table
        for(; addedStackCount < maxCount; addedStackCount++) {
            itemStacks.add(ItemStack.EMPTY);
        }

        return itemStacks;
    }
}
