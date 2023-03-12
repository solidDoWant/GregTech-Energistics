package com.soliddowant.gregtechenergistics.items;

import appeng.core.Api;
import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;
import com.soliddowant.gregtechenergistics.items.behaviors.StockerTerminalBehavior;
import gregtech.api.GTValues;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.ore.OrePrefix;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class MetaItem1 extends StandardModMetaItem {
	public static final short META_ITEM_OFFSET = 0;

	public MetaItem1() {
		super(META_ITEM_OFFSET);
	}

	@Override
	public void registerSubItems() {
		MetaItems.AE2_STOCKER_LV = addItem(0, "ae2.stocker.lv");
		MetaItems.AE2_STOCKER_MV = addItem(1, "ae2.stocker.mv");
		MetaItems.AE2_STOCKER_HV = addItem(2, "ae2.stocker.hv");
		MetaItems.AE2_STOCKER_EV = addItem(3, "ae2.stocker.ev");
		MetaItems.AE2_STOCKER_IV = addItem(4, "ae2.stocker.iv");
		MetaItems.AE2_STOCKER_LUV = addItem(5, "ae2.stocker.luv");
		MetaItems.AE2_STOCKER_ZPM = addItem(6, "ae2.stocker.zpm");
		MetaItems.AE2_STOCKER_UV = addItem(7, "ae2.stocker.uv");
		MetaItems.MACHINE_STATUS = addItem(8, "machine.status");
		MetaItems.FLUID_ENCODER = addItem(9, "fluid.encoder")
				.addComponents(new FluidEncoderBehaviour()).setMaxStackSize(1);
		MetaItems.STOCKER_TERMINAL = addItem(10, "stocker.terminal")
				.addComponents(new StockerTerminalBehavior());
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")	// If these items are missing the mod should probably fail to load
	protected void registerStocker(MetaItem<?>.MetaValueItem roboticArm, MetaItem<?>.MetaValueItem pump,
								   MetaItem<?>.MetaValueItem output, int voltage) {
		RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
				.inputs(
						roboticArm.getStackForm(),
						pump.getStackForm(),
						gregtech.common.items.MetaItems.COVER_MACHINE_CONTROLLER.getStackForm(),
						Api.INSTANCE.definitions().blocks().iface().maybeStack(1).get(),
						Api.INSTANCE.definitions().blocks().fluidIface().maybeStack(1).get(),
						Api.INSTANCE.definitions().parts().levelEmitter().maybeStack(1).get(),
						Api.INSTANCE.definitions().parts().fluidLevelEmitter().maybeStack(1).get()
				)
				.input(OrePrefix.plate, Materials.Tin, 4)
				.outputs(output.getStackForm())
				.duration(200)
				.EUt((int) GTValues.V[voltage])
				.buildAndRegister();
	}

	public void registerRecipes() {
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_LV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_LV,
				MetaItems.AE2_STOCKER_LV, GTValues.LV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_MV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_MV,
				MetaItems.AE2_STOCKER_MV, GTValues.MV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_HV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_HV,
				MetaItems.AE2_STOCKER_HV, GTValues.HV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_EV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_EV,
				MetaItems.AE2_STOCKER_EV, GTValues.EV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_IV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_IV,
				MetaItems.AE2_STOCKER_IV, GTValues.IV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_LuV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_LuV,
				MetaItems.AE2_STOCKER_LUV, GTValues.LuV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_ZPM, gregtech.common.items.MetaItems.ELECTRIC_PUMP_ZPM,
				MetaItems.AE2_STOCKER_ZPM, GTValues.ZPM);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_UV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_UV,
				MetaItems.AE2_STOCKER_UV, GTValues.UV);

		RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
				.input(OrePrefix.plate, Materials.Copper, 4)
				.input(OrePrefix.dust, Materials.Redstone, 2)
				.outputs(MetaItems.MACHINE_STATUS.getStackForm())
				.duration(200)
				.EUt((int) GTValues.V[GTValues.MV])
				.buildAndRegister();

		RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
				.inputs(
						gregtech.common.items.MetaItems.INTEGRATED_CIRCUIT.getStackForm(),
						new ItemStack(Items.BUCKET)
				)
				.outputs(MetaItems.FLUID_ENCODER.getStackForm())
				.duration(200)
				.EUt((int) GTValues.V[GTValues.MV])
				.buildAndRegister();

		RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
				.input("itemIlluminatedPanel", 1)
				.inputs(
						Api.INSTANCE.definitions().materials().engProcessor().maybeStack(1).get(),
						MetaItems.AE2_STOCKER_MV.getStackForm()
				)
				.outputs(MetaItems.STOCKER_TERMINAL.getStackForm())
				.duration(200)
				.EUt((int) GTValues.V[GTValues.MV])
				.buildAndRegister();
	}
}
