package com.example.examplemod;

import appeng.api.config.Upgrades;
import appeng.core.AppEng;
import appeng.items.materials.ItemMaterial;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class AE2UpgradeSlotWidget implements INBTSerializable<NBTTagCompound> {
    protected ItemStackHandler slotHandler;
    protected Upgrades[] validUpgrades;
    protected Upgrades insertedUpgrade;
    protected Consumer<Boolean> contentsChangedCallback;

    public AE2UpgradeSlotWidget(Upgrades... validUpgrades) {
        this.validUpgrades = validUpgrades;
        this.slotHandler = new ItemStackHandler(1) {
                    @Override
                    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                        return validateItemStack(stack);
                    }

                    @Override
                    public int getSlotLimit(int slot) {
                        return 1;
                    }

                    @Override
                    protected void onContentsChanged(int slot) {
                        onSlotChanged();
                    }
                };
    }

    public void initUI(int x, int y, @Nonnull Consumer<Widget> widgetGroup) {
        widgetGroup.accept(new SavableBackgroundSlotWidget<>(slotHandler, 0, x, y, true,
                true).setBackgroundTexture(new TextureArea(new ResourceLocation(AppEng.MOD_ID,
                "textures/guis/states.png"), (15.0 / 16.0), (13.0 / 16.0), (1.0 / 16.0), (1.0 / 16.0))));
    }

    public void setContentsChangedCallback(Consumer<Boolean> callback) {
        this.contentsChangedCallback = callback;
    }

    public ItemStack getSlotStack() {
        return this.slotHandler.getStackInSlot(0);
    }

    protected void onSlotChanged() {
        ItemStack slotStack = getSlotStack();
        if(slotStack != null)
            if(slotStack.isEmpty())
                onSlotRemoved();
            else
                onSlotInserted(slotStack);
    }

    protected boolean validateItemStack(@Nonnull ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof ItemMaterial))
            return false;

        Upgrades itemUpgrade = ((ItemMaterial) item).getType(stack);

        for(Upgrades validUpgrade : validUpgrades)
            if(itemUpgrade == validUpgrade)
                return true;

        return false;
    }

    protected void onSlotInserted(@Nonnull ItemStack slotStack) {
        insertedUpgrade = ((ItemMaterial) slotStack.getItem()).getType(slotStack);
        if(contentsChangedCallback != null)
            contentsChangedCallback.accept(true);
    }

    protected void onSlotRemoved() {
        insertedUpgrade = null;
        if(contentsChangedCallback != null)
            contentsChangedCallback.accept(false);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SlotHandler", slotHandler.serializeNBT());

        Upgrades installedUpgrade = getInsertedUpgrade();
        if(installedUpgrade != null)
            tag.setInteger("InstalledUpgrade", installedUpgrade.ordinal());

        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull NBTTagCompound nbt) {
        if(nbt.hasKey("SlotHandler"))
            slotHandler.deserializeNBT(nbt.getCompoundTag("SlotHandler"));

        if(nbt.hasKey("InstalledUpgrade"))
            insertedUpgrade = Upgrades.values()[nbt.getInteger("InstalledUpgrade")];
    }

    public Upgrades getInsertedUpgrade() {
        return insertedUpgrade;
    }

    public boolean isUpgradeInserted() {
        return getInsertedUpgrade() != null;
    }
}
