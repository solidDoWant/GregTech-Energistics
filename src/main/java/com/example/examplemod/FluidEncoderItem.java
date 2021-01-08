package com.example.examplemod;

import appeng.util.ReadableNumberConverter;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ClickButtonWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.SimpleTextWidget;
import gregtech.api.items.gui.ItemUIFactory;
import gregtech.api.items.gui.PlayerInventoryHolder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class FluidEncoderItem extends Item implements ItemUIFactory {
    public FluidEncoderItem() {
        setRegistryName("fluid_encoder");
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack heldItem = player.getHeldItem(hand);
        if (!world.isRemote) {
            PlayerInventoryHolder holder = new PlayerInventoryHolder(player, hand);
            holder.openUI();
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
    }

    @Override
    public ModularUI createUI(PlayerInventoryHolder holder, EntityPlayer entityPlayer) {
        return ModularUI.builder(GuiTextures.BACKGROUND, 194, 60)
                .label(9, 8, "metaitem.fluid.encoder.title")
                .widget(new ImageWidget(59, 24, 76, 20, GuiTextures.DISPLAY))
                .widget(new SimpleTextWidget(97, 34, "metaitem.fluid.encoder.amount", 0xFFFFFF,
                        () -> ReadableNumberConverter.INSTANCE.toWideReadableForm(getFluidAmount(holder.getCurrentItem()))))
                .widget(new ClickButtonWidget(9, 24, 30, 20, "-100", data -> adjustConfiguration(holder, -100, data.isShiftClick)))
                .widget(new ClickButtonWidget(39, 24, 20, 20, "-1", data -> adjustConfiguration(holder, -1, data.isShiftClick)))
                .widget(new ClickButtonWidget(135, 24, 20, 20, "+1", data -> adjustConfiguration(holder, +1, data.isShiftClick)))
                .widget(new ClickButtonWidget(155, 24, 30, 20, "+100", data -> adjustConfiguration(holder, +100, data.isShiftClick)))
                .bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 8, 170)
                .build(holder, entityPlayer);
    }

    public int getFluidAmount(ItemStack stack) {
        if (!stack.hasTagCompound())
            return 0;

        NBTTagCompound tag = stack.getTagCompound();

        if (!tag.hasKey("Amount"))
            return 0;

        return tag.getInteger("Amount");
    }

    public void setFluidAmount(ItemStack stack, int fluidAmount) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setInteger("Amount", fluidAmount);
        stack.setTagCompound(tag);
    }

    protected void adjustConfiguration(PlayerInventoryHolder holder, int amount, boolean shouldScale) {
        int incrementAmount = shouldScale ? 10 * amount : amount;
        setFluidAmount(holder.getCurrentItem(),
                MathHelper.clamp(getFluidAmount(holder.getCurrentItem()) + incrementAmount, 0, Integer.MAX_VALUE));
        holder.markAsDirty();
    }
}
