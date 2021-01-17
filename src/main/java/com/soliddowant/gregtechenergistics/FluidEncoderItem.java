//package com.soliddowant.gregtechenergistics;
//
//import appeng.util.ReadableNumberConverter;
//import gregtech.api.GregTechAPI;
//import gregtech.api.gui.GuiTextures;
//import gregtech.api.gui.ModularUI;
//import gregtech.api.gui.widgets.ClickButtonWidget;
//import gregtech.api.gui.widgets.ImageWidget;
//import gregtech.api.gui.widgets.PhantomFluidWidget;
//import gregtech.api.gui.widgets.SimpleTextWidget;
//import gregtech.api.items.gui.ItemUIFactory;
//import gregtech.api.items.gui.PlayerInventoryHolder;
//import net.minecraft.client.renderer.block.model.ModelResourceLocation;
//import net.minecraft.client.resources.I18n;
//import net.minecraft.client.util.ITooltipFlag;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.item.Item;
//import net.minecraft.item.ItemStack;
//import net.minecraft.nbt.NBTTagCompound;
//import net.minecraft.util.ActionResult;
//import net.minecraft.util.EnumActionResult;
//import net.minecraft.util.EnumHand;
//import net.minecraft.util.math.MathHelper;
//import net.minecraft.world.World;
//import net.minecraftforge.client.model.ModelLoader;
//import net.minecraftforge.fluids.FluidStack;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//import java.util.List;
//
//public class FluidEncoderItem extends Item implements ItemUIFactory, ICustomModel {
//    public FluidEncoderItem() {
//        setRegistryName(GregTechEnergisticsMod.MODID, "fluid.encoder");
//        setUnlocalizedName("fluid.encoder");
//        setMaxStackSize(1);
//        setCreativeTab(GregTechAPI.TAB_GREGTECH);
//    }
//
//    @SideOnly(Side.CLIENT)
//    @Override
//    public void registerModel() {
//        ModelLoader.setCustomModelResourceLocation(this, 0,
//                new ModelResourceLocation(GregTechEnergisticsMod.MODID + ":fluid.encoder"));
//    }
//
//    @Override
//    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flagIn) {
//        FluidStack containedStack = getFluidStack(stack);
//        if(containedStack != null) {
//            tooltip.add(containedStack.getLocalizedName() + ", " +
//                    I18n.format(getUnlocalizedName() + ".amount", getFluidAmount(stack)));
//        }
//    }
//
//    @Nonnull
//    @Override
//    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, EntityPlayer player, @Nonnull EnumHand hand) {
//        ItemStack heldItem = player.getHeldItem(hand);
//
//        if(player.isSneaking()) {
//            heldItem.setTagCompound(null);
//            return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
//        }
//
//        if (!world.isRemote) {
//            PlayerInventoryHolder.openHandItemUI(player, hand);
//        }
//
//        return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
//    }
//
//    @Override
//    public ModularUI createUI(PlayerInventoryHolder holder, EntityPlayer entityPlayer) {
//        return new ModularUIItemBuilder(GuiTextures.BACKGROUND, 178, 240)
//                .label(9, 8, getItemStackDisplayName(holder.getCurrentItem()))
//                .widget(new ImageWidget(61, 24, 56, 20, GuiTextures.DISPLAY))
//                .widget(new SimpleTextWidget(89, 34, getUnlocalizedName() + ".amount", 0xFFFFFF,
//                        () -> ReadableNumberConverter.INSTANCE.toWideReadableForm(getFluidAmount(holder.getCurrentItem()))))
//                .widget(new ClickButtonWidget(11, 24, 30, 20, "-100", data -> adjustConfiguration(holder, -100, data.isShiftClick)))
//                .widget(new ClickButtonWidget(41, 24, 20, 20, "-1", data -> adjustConfiguration(holder, -1, data.isShiftClick)))
//                .widget(new ClickButtonWidget(117, 24, 20, 20, "+1", data -> adjustConfiguration(holder, +1, data.isShiftClick)))
//                .widget(new ClickButtonWidget(137, 24, 30, 20, "+100", data -> adjustConfiguration(holder, +100, data.isShiftClick)))
//                .widget(new PhantomFluidWidget(44, 50, 18, 18, () -> getFluidStack(holder.getCurrentItem()), fluid -> setFluidStack(holder, fluid)))
//                .label(11, 55, getUnlocalizedName() + ".fluid")
//                .bindPlayerInventory(entityPlayer.inventory, 156)
//                .build(holder, entityPlayer);
//    }
//
//    public static boolean hasFluidStack(ItemStack heldItemStack) {
//        if (!heldItemStack.hasTagCompound())
//            return false;
//
//        NBTTagCompound tag = heldItemStack.getTagCompound();
//
//        return tag.hasKey("FluidStack");
//    }
//
//    public static FluidStack getFluidStack(ItemStack heldItemStack) {
//        if (!hasFluidStack(heldItemStack))
//            return null;
//
//        NBTTagCompound tag = heldItemStack.getTagCompound();
//
//        if (!tag.hasKey("FluidStack"))
//            return null;
//
//        return FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("FluidStack"));
//    }
//
//    public static void setFluidStack(PlayerInventoryHolder holder, FluidStack stack) {
//        ItemStack heldItemStack = holder.getCurrentItem();
//        NBTTagCompound tag = heldItemStack.hasTagCompound() ? heldItemStack.getTagCompound() : new NBTTagCompound();
//        NBTTagCompound fluidTag = new NBTTagCompound();
//        if(stack != null)
//            stack.writeToNBT(fluidTag);
//        tag.setTag("FluidStack", fluidTag);
//        heldItemStack.setTagCompound(tag);
//        holder.markAsDirty();
//    }
//
//    public static int getFluidAmount(ItemStack heldItemStack) {
//        if (!heldItemStack.hasTagCompound())
//            return 0;
//
//        NBTTagCompound tag = heldItemStack.getTagCompound();
//
//        if (!tag.hasKey("Amount"))
//            return 0;
//
//        return tag.getInteger("Amount");
//    }
//
//    public static void setFluidAmount(ItemStack heldItemStack, int fluidAmount) {
//        NBTTagCompound tag = heldItemStack.hasTagCompound() ? heldItemStack.getTagCompound() : new NBTTagCompound();
//        tag.setInteger("Amount", fluidAmount);
//        heldItemStack.setTagCompound(tag);
//    }
//
//    protected static void adjustConfiguration(PlayerInventoryHolder holder, int amount, boolean shouldScale) {
//        ItemStack currentItemStack = holder.getCurrentItem();
//        int incrementAmount = shouldScale ? 10 * amount : amount;
//        setFluidAmount(currentItemStack,
//                MathHelper.clamp(getFluidAmount(currentItemStack) + incrementAmount, 0, Integer.MAX_VALUE));
//        holder.markAsDirty();
//    }
//}
