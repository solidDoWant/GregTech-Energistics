package com.example.examplemod;

import static com.example.examplemod.MetaItems.*;

import appeng.core.Api;
import appeng.core.api.definitions.ApiItems;
import gregtech.api.GTValues;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.StandardMetaItem;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.ore.OrePrefix;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GTCEMetaItem1 extends StandardMetaItem {
	public static final short META_ITEM_OFFSET = 0;
	private static final ModelResourceLocation MISSING_LOCATION = new ModelResourceLocation("builtin/missing",
			"inventory");

	public GTCEMetaItem1() {
		super(META_ITEM_OFFSET);
	}

	@Override
	public void registerSubItems() {
		AE2_STOCKER_LV = addItem(0, "ae2.stocker.lv");
		AE2_STOCKER_MV = addItem(1, "ae2.stocker.mv");
		AE2_STOCKER_HV = addItem(2, "ae2.stocker.hv");
		AE2_STOCKER_EV = addItem(3, "ae2.stocker.ev");
		AE2_STOCKER_IV = addItem(4, "ae2.stocker.iv");
		AE2_STOCKER_LUV = addItem(5, "ae2.stocker.luv");
		AE2_STOCKER_ZPM = addItem(6, "ae2.stocker.zpm");
		AE2_STOCKER_UV = addItem(7, "ae2.stocker.uv");
		MACHINE_STATUS = addItem(8, "machine.status");
	}

	// Re-implemented from base class to change the asset resource location to this
	// mod's.
	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels() {
		for (short itemMetaKey : metaItems.keys()) {
			MetaItem<?>.MetaValueItem metaValueItem = metaItems.get(itemMetaKey);
			int numberOfModels = metaValueItem.getModelAmount();
			if (numberOfModels > 1) {
				ModelResourceLocation[] resourceLocations = new ModelResourceLocation[numberOfModels];
				for (int i = 0; i < resourceLocations.length; i++) {
					ResourceLocation resourceLocation = new ResourceLocation(ExampleMod.MODID,
							formatModelPath(metaValueItem) + "/" + (i + 1));
					ModelBakery.registerItemVariants(this, resourceLocation);
					resourceLocations[i] = new ModelResourceLocation(resourceLocation, "inventory");
				}
				specialItemsModels.put((short) (metaItemOffset + itemMetaKey), resourceLocations);
				continue;
			}
			ResourceLocation resourceLocation = new ResourceLocation(ExampleMod.MODID, formatModelPath(metaValueItem));
			ModelBakery.registerItemVariants(this, resourceLocation);
			metaItemsModels.put((short) (metaItemOffset + itemMetaKey),
					new ModelResourceLocation(resourceLocation, "inventory"));
		}

		ModelLoader.setCustomMeshDefinition(this, itemStack -> {
			short itemDamage = formatRawItemDamage((short) itemStack.getItemDamage());
			if (specialItemsModels.containsKey(itemDamage)) {
				int modelIndex = getModelIndex(itemStack);
				return specialItemsModels.get(itemDamage)[modelIndex];
			}
			if (metaItemsModels.containsKey(itemDamage)) {
				return metaItemsModels.get(itemDamage);
			}
			return MISSING_LOCATION;
		});
	}

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
				AE2_STOCKER_LV, GTValues.LV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_MV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_MV,
				AE2_STOCKER_MV, GTValues.MV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_HV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_HV,
				AE2_STOCKER_HV, GTValues.HV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_EV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_EV,
				AE2_STOCKER_EV, GTValues.EV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_IV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_IV,
				AE2_STOCKER_IV, GTValues.IV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_LUV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_LUV,
				AE2_STOCKER_LUV, GTValues.LuV);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_ZPM, gregtech.common.items.MetaItems.ELECTRIC_PUMP_ZPM,
				AE2_STOCKER_ZPM, GTValues.ZPM);
		registerStocker(gregtech.common.items.MetaItems.ROBOT_ARM_UV, gregtech.common.items.MetaItems.ELECTRIC_PUMP_UV,
				AE2_STOCKER_UV, GTValues.UV);

		RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
				.input(OrePrefix.plate, Materials.Tin, 4)
				.input(OrePrefix.dust, Materials.Redstone, 2)
				.outputs(MACHINE_STATUS.getStackForm())
				.duration(200)
				.EUt((int) GTValues.V[GTValues.MV])
				.buildAndRegister();
	}
}
