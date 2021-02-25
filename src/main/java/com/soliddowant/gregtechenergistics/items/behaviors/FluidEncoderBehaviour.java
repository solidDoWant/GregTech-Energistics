package com.soliddowant.gregtechenergistics.items.behaviors;

import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import com.soliddowant.gregtechenergistics.gui.ModularUIItemBuilder;
import com.soliddowant.gregtechenergistics.items.MetaItems;
import com.soliddowant.gregtechenergistics.items.StandardModMetaItem;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ClickButtonWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.PhantomFluidWidget;
import gregtech.api.gui.widgets.SimpleTextWidget;
import gregtech.api.items.gui.ItemUIFactory;
import gregtech.api.items.gui.PlayerInventoryHolder;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.List;

public class FluidEncoderBehaviour implements IItemBehaviour, ItemUIFactory {
    @Override
    public void addInformation(ItemStack stack, List<String> lines) {
        FluidStack containedStack = getFluidStack(stack);
        if(containedStack == null)
            return;

        MetaItem<?>.MetaValueItem heldMetaItem = MetaItems.getMetaValueItemFromStack(stack);
        String baseName = "metaitem." + heldMetaItem.unlocalizedName;
        lines.add(containedStack.getLocalizedName() + ", " +
                I18n.format(baseName + ".amount", getFluidAmount(stack)));
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack heldItem = player.getHeldItem(hand);

        if(player.isSneaking()) {
            heldItem.setTagCompound(null);
            return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
        }

        if (Platform.isServer())
            PlayerInventoryHolder.openHandItemUI(player, hand);

        return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
    }

    @Override
    public ModularUI createUI(PlayerInventoryHolder holder, EntityPlayer entityPlayer) {
        ItemStack heldItemStack = holder.getCurrentItem();
        MetaItem<?>.MetaValueItem heldMetaItem = MetaItems.getMetaValueItemFromStack(heldItemStack);
        String baseName = "metaitem." + heldMetaItem.unlocalizedName;

        return new ModularUIItemBuilder(GuiTextures.BACKGROUND, 178, 240)
                .label(9, 8, heldItemStack.getDisplayName())
                .widget(new ImageWidget(61, 24, 56, 20, GuiTextures.DISPLAY))
                .widget(new SimpleTextWidget(89, 34, baseName + ".amount", 0xFFFFFF,
                        () -> ReadableNumberConverter.INSTANCE.toWideReadableForm(getFluidAmount(holder.getCurrentItem()))))
                .widget(new ClickButtonWidget(11, 24, 30, 20, "-100", data -> adjustConfiguration(holder, -100, data.isShiftClick)))
                .widget(new ClickButtonWidget(41, 24, 20, 20, "-1", data -> adjustConfiguration(holder, -1, data.isShiftClick)))
                .widget(new ClickButtonWidget(117, 24, 20, 20, "+1", data -> adjustConfiguration(holder, +1, data.isShiftClick)))
                .widget(new ClickButtonWidget(137, 24, 30, 20, "+100", data -> adjustConfiguration(holder, +100, data.isShiftClick)))
                .widget(new PhantomFluidWidget(44, 50, 18, 18, () -> getFluidStack(holder.getCurrentItem()), fluid -> setHeldItemStackFluid(holder, fluid)))
                .label(11, 55, baseName + ".fluid")
                .bindPlayerInventory(entityPlayer.inventory, 156)
                .build(holder, entityPlayer);
    }

    public static boolean hasFluidStack(ItemStack heldItemStack) {
        if (!heldItemStack.hasTagCompound())
            return false;

        NBTTagCompound tag = heldItemStack.getTagCompound();

        return tag.hasKey("FluidStack");
    }

    public static FluidStack getFluidStack(ItemStack heldItemStack) {
        if (!hasFluidStack(heldItemStack))
            return null;

        NBTTagCompound tag = heldItemStack.getTagCompound();

        if (!tag.hasKey("FluidStack"))
            return null;

        return FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("FluidStack"));
    }

    public static void setHeldItemStackFluid(PlayerInventoryHolder holder, FluidStack stack) {
        setItemStackFluid(holder.getCurrentItem(), stack);
        holder.markAsDirty();
    }

    public static void setItemStackFluid(ItemStack stack, FluidStack fluid) {
        if(fluid == null)
            return;

        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagCompound fluidTag = new NBTTagCompound();
        fluid.writeToNBT(fluidTag);
        tag.setTag("FluidStack", fluidTag);
        stack.setTagCompound(tag);
    }

    public static int getFluidAmount(ItemStack heldItemStack) {
        if (!heldItemStack.hasTagCompound())
            return 0;

        NBTTagCompound tag = heldItemStack.getTagCompound();

        if (!tag.hasKey("Amount"))
            return 0;

        return tag.getInteger("Amount");
    }

    public static void setItemStackFluidAmount(ItemStack stack, int fluidAmount) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setInteger("Amount", fluidAmount);
        stack.setTagCompound(tag);
    }

    protected static void adjustConfiguration(PlayerInventoryHolder holder, int amount, boolean shouldScale) {
        ItemStack currentItemStack = holder.getCurrentItem();
        int incrementAmount = shouldScale ? 10 * amount : amount;
        setItemStackFluidAmount(currentItemStack,
                MathHelper.clamp(getFluidAmount(currentItemStack) + incrementAmount, 0, Integer.MAX_VALUE));
        holder.markAsDirty();
    }

    public static FluidEncoderBehaviour getStackBehaviour(ItemStack stack) {
        Item item = stack.getItem();
        if(!(item instanceof StandardModMetaItem))
            return null;

        MetaItem<?>.MetaValueItem stackMetaValueItem = ((MetaItem<?>) item).getItem(stack);
        if(stackMetaValueItem == null)
            return null;

        for(IItemBehaviour behavior : stackMetaValueItem.getBehaviours())
            if(behavior instanceof FluidEncoderBehaviour)
                return (FluidEncoderBehaviour) behavior;

        return null;
    }

    public static boolean hasStackBehavior(ItemStack stack) {
        return getStackBehaviour(stack) != null;
    }
}
