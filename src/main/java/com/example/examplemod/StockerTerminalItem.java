package com.example.examplemod;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StockerTerminalItem extends Item implements ICustomModel, IPartItem<StockerTerminalPart> {

	public StockerTerminalItem() {
		this.setRegistryName(ExampleMod.MODID, "stocker.terminal");
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side,
			float hitX, float hitY, float hitZ) {
		return AEApi.instance().partHelper().placeBus(player.getHeldItem(hand), pos, side, player, hand, world);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModel() {
		AEApi.instance().registries().partModels().registerModels(StockerTerminalPart.MODELS);
		ModelLoader.setCustomModelResourceLocation(this, 0,
				new ModelResourceLocation(ExampleMod.MODID + ":part/stocker.terminal"));
	}

	@Override
	public StockerTerminalPart createPartFromItemStack(ItemStack is) {
		return new StockerTerminalPart(is);
	}
}
