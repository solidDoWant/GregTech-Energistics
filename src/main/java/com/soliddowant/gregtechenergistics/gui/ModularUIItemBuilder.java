package com.soliddowant.gregtechenergistics.gui;

import gregtech.api.gui.ModularUI.Builder;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.SlotWidget;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public class ModularUIItemBuilder extends Builder {
    public ModularUIItemBuilder(TextureArea background, int width, int height) {
        super(background, width, height);
    }

    // Prevents the currently held item from being removed from the hotbar while GUI is open.
    @Override
    public Builder bindPlayerHotbar(InventoryPlayer inventoryPlayer, TextureArea imageLocation, int x, int y) {
        PlayerMainInvWrapper inventoryWrapper = new PlayerMainInvWrapper(inventoryPlayer);
        for (int slot = 0; slot < 9; slot++) {
            boolean isCurrentlyHeldItem = slot == inventoryPlayer.currentItem;
            this.widget(new SlotWidget(inventoryWrapper, slot, x + slot * 18, y, !isCurrentlyHeldItem, !isCurrentlyHeldItem)
                    .setBackgroundTexture(imageLocation)
                    .setLocationInfo(true, true));
        }
        return this;
    }
}
