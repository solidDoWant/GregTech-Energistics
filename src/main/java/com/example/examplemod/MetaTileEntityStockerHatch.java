package com.example.examplemod;

import java.util.List;

import gregtech.api.capability.IEnergyContainer;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class MetaTileEntityStockerHatch extends MetaTileEntityMultiblockPart
		implements IMultiblockAbilityPart<IEnergyContainer> {
	public MetaTileEntityStockerHatch(ResourceLocation metaTileEntityId, int tier) {
		super(metaTileEntityId, tier);
	}

	@Override
	public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
		return new MetaTileEntityStockerHatch(metaTileEntityId, getTier());
	}

	@Override
	protected ModularUI createUI(EntityPlayer entityPlayer) {
		return null;
	}

	@Override
	public MultiblockAbility<IEnergyContainer> getAbility() {
		return MultiblockAbility.INPUT_ENERGY;
	}

	@Override
	public void update() {
		super.update();

		MultiblockControllerBase controller = this.getController();

		if (controller == null)
			return;

		controller.getAbilities(MultiblockAbility.IMPORT_ITEMS).stream().forEach(itemHandler -> {
			for (int i = 0; i < itemHandler.getSlots(); i++) {
				itemHandler.insertItem(i, new ItemStack(Blocks.DIRT, 10), false);
			}
		});
	}

	@Override
	public void registerAbilities(List<IEnergyContainer> abilityList) {
	}

}
