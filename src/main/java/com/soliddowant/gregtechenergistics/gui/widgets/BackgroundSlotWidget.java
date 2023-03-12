package com.soliddowant.gregtechenergistics.gui.widgets;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.IGuiTexture;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.util.GTUtility;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import java.util.Arrays;
import java.util.List;

public class BackgroundSlotWidget extends SlotWidget {
    protected TextureArea BorderTexture = GuiTextures.SLOT;
    protected ItemStack foregroundItemStack;
    public BackgroundSlotWidget(IItemHandler itemHandler, int slotIndex, int xPosition, int yPosition, boolean canTakeItems, boolean canPutItems) {
        super(itemHandler, slotIndex, xPosition, yPosition, canTakeItems, canPutItems);
    }

    @SuppressWarnings("unused")
    public BackgroundSlotWidget(IItemHandler itemHandler, int slotIndex, int xPosition, int yPosition) {
        this(itemHandler, slotIndex, xPosition, yPosition, true, true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
        if(!isEnabled())
            return;

        Position pos = getPosition();
        Size size = getSize();

        BorderTexture.draw(pos.x, pos.y, size.width, size.height);

        if (isMouseOverElement(mouseX, mouseY)) {
            Size slotSize = this.getSize();
            drawSelectionOverlay(pos.getX() + 1, pos.getY() + 1, slotSize.getWidth(), slotSize.getHeight());
        }

        if(backgroundTexture == null || foregroundItemStack != null)
            return;

        for (IGuiTexture backgroundTexture : this.backgroundTexture) {
            backgroundTexture.draw(pos.x + 1, pos.y + 1, size.width - 2, size.height - 2);
            backgroundTexture.draw(pos.x + 1, pos.y + 1, size.width - 2, size.height - 2);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        if(foregroundItemStack == null)
            return;

        Position slotPosition = this.getPosition();
        Widget.drawItemStack(foregroundItemStack, slotPosition.getX() + 1, slotPosition.getY() + 1, null);

        if (!isMouseOverElement(mouseX, mouseY))
            return;

        List<String> tooltip = getItemToolTip(foregroundItemStack);
        drawHoveringText(foregroundItemStack, tooltip, -1, mouseX, mouseY);
    }

    public void setForegroundItemStack(ItemStack foregroundItemStack) {
        this.foregroundItemStack = foregroundItemStack;
    }

    public void clearForegroundItemStack() {
        setForegroundItemStack(null);
    }
}
