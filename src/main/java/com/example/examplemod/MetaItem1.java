package com.example.examplemod;

import gregtech.api.GTValues;
import gregtech.api.items.metaitem.StandardMetaItem;
import gregtech.api.items.metaitem.MetaItem;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.example.examplemod.MetaItems.*;

public class MetaItem1 extends StandardMetaItem {
	public static final short META_ITEM_OFFSET = Short.MAX_VALUE - 1;
	private static final ModelResourceLocation MISSING_LOCATION = new ModelResourceLocation("builtin/missing", "inventory");

	public MetaItem1() {
		super(META_ITEM_OFFSET);
	}
	
	@Override
	public void registerSubItems() {
		AE2_INTERFACE_LV = addItem(0, "ae2.stocker.lv");
	}
	
	// Re-implemented from base class to change the asset resource location to this mod's.
	@Override
	@SideOnly(Side.CLIENT)
    public void registerModels() {
        for (short itemMetaKey : metaItems.keys()) {
        	MetaItem<?>.MetaValueItem metaValueItem = metaItems.get(itemMetaKey);
            int numberOfModels = metaValueItem.getModelAmount();
            if (numberOfModels > 1) {
                ModelResourceLocation[] resourceLocations = new ModelResourceLocation[numberOfModels];
                for (int i = 0; i < resourceLocations.length; i++) {
                    ResourceLocation resourceLocation = new ResourceLocation(GTValues.MODID, formatModelPath(metaValueItem) + "/" + (i + 1));
                    ModelBakery.registerItemVariants(this, resourceLocation);
                    resourceLocations[i] = new ModelResourceLocation(resourceLocation, "inventory");
                }
                specialItemsModels.put((short) (metaItemOffset + itemMetaKey), resourceLocations);
                continue;
            }
            ResourceLocation resourceLocation = new ResourceLocation(ExampleMod.MODID, formatModelPath(metaValueItem));
            ModelBakery.registerItemVariants(this, resourceLocation);
            metaItemsModels.put((short) (metaItemOffset + itemMetaKey), new ModelResourceLocation(resourceLocation, "inventory"));
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
}
