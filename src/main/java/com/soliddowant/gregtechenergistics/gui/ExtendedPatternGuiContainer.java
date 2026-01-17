package com.soliddowant.gregtechenergistics.gui;

import java.io.IOException;

import com.soliddowant.gregtechenergistics.Tags;
import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.util.AEPartLocation;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ExtendedPatternGuiContainer extends AEBaseGui {
    private static final ResourceLocation BACKGROUND_TEXTURE =
        new ResourceLocation(Tags.MODID, "textures/guis/extendedpattern.png");

    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;

    public ExtendedPatternGuiContainer(final InventoryPlayer inventoryPlayer, final ExtendedPatternTerminalPart part) {
        super(new ExtendedPatternContainer(inventoryPlayer, part));
        this.xSize = 195;
        this.ySize = 240;
    }

    public static ExtendedPatternGuiContainer getClientGuiContainer(AEPartLocation side, EntityPlayer player, World world,
            int x, int y, int z) {
        ExtendedPatternTerminalPart part = GuiProxy.getPartAtLocation(world, x, y, z, side, ExtendedPatternTerminalPart.class);
        if (part == null)
            return null;

        return new ExtendedPatternGuiContainer(player.inventory, part);
    }

    @Override
    public void initGui() {
        super.initGui();

        // Encode button
        this.encodeBtn = new GuiImgButton(this.guiLeft + 50, this.guiTop + 120, Settings.ACTIONS, ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        // Clear button
        this.clearBtn = new GuiImgButton(this.guiLeft + 68, this.guiTop + 120, Settings.ACTIONS, ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) throws IOException {
        super.actionPerformed(btn);

        if (this.encodeBtn == btn) {
            // Encode the pattern
            NetworkHandler.instance().sendToServer(
                new PacketValueConfig("PatternTerminal.Encode", "1"));
        }

        if (this.clearBtn == btn) {
            // Clear all slots
            NetworkHandler.instance().sendToServer(
                new PacketValueConfig("PatternTerminal.Clear", "1"));
        }
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        // Draw title
        this.fontRenderer.drawString(
            I18n.format("gui.gregtechenergistics.extendedpattern.title"),
            8, 6, 4210752);

        // Draw labels
        this.fontRenderer.drawString("Inputs", 8, 14, 4210752);
        this.fontRenderer.drawString("Outputs", 98, 14, 4210752);

        // Draw player inventory label
        this.fontRenderer.drawString(
            I18n.format("container.inventory"),
            8, this.ySize - 94, 4210752);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(BACKGROUND_TEXTURE);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    protected void bindTexture(final ResourceLocation loc) {
        this.mc.getTextureManager().bindTexture(loc);
    }
}
