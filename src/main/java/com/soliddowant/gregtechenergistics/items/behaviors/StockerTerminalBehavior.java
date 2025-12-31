package com.soliddowant.gregtechenergistics.items.behaviors;

import javax.annotation.Nonnull;

import com.soliddowant.gregtechenergistics.Tags;
import com.soliddowant.gregtechenergistics.items.stats.IModelProvider;
import com.soliddowant.gregtechenergistics.items.stats.IPartProvider;
import com.soliddowant.gregtechenergistics.parts.StockerTerminalPart;

import appeng.api.AEApi;
import appeng.api.parts.IPart;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StockerTerminalBehavior implements IItemBehaviour, IPartProvider, IModelProvider {
    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemUse(@Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos,
            @Nonnull EnumHand hand, @Nonnull EnumFacing side,
            float hitX, float hitY, float hitZ) {
        ItemStack heldItem = player.getHeldItem(hand);
        EnumActionResult placeResult = AEApi.instance().partHelper().placeBus(heldItem, pos, side, player, hand, world);
        return new ActionResult<>(placeResult, heldItem);
    }

    @Override
    public IPart getPart(ItemStack stack) {
        return new StockerTerminalPart(stack);
    }

    @Override
    public ModelResourceLocation getModel() {
        AEApi.instance().registries().partModels().registerModels(StockerTerminalPart.MODELS);
        return new ModelResourceLocation(Tags.MODID + ":part/stocker.terminal");
    }
}
