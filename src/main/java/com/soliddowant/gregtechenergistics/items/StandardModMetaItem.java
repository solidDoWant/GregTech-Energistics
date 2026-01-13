package com.soliddowant.gregtechenergistics.items;

import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class StandardModMetaItem extends ModMetaItem<ModMetaItem<?>.ModMetaValueItem> {
    public StandardModMetaItem(short metaItemOffset) {
        super(metaItemOffset);
    }

    @Override
    protected ModMetaValueItem constructMetaValueItem(short metaValue, String unlocalizedName) {
        return new ModMetaValueItem(metaValue, unlocalizedName);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String baseName = super.getItemStackDisplayName(stack);

        if (!FluidEncoderBehaviour.hasStackBehavior(stack))
            return baseName;

        FluidStack fluidStack = FluidEncoderBehaviour.getFluidStack(stack);
        if (fluidStack == null)
            return baseName;

        int amount = FluidEncoderBehaviour.getFluidAmount(stack);
        return baseName + ": " + fluidStack.getLocalizedName() + " (" + amount + " mb)";
    }
}
