package com.soliddowant.gregtechenergistics.gui;

import java.lang.reflect.Field;

import com.soliddowant.gregtechenergistics.Tags;
import com.soliddowant.gregtechenergistics.networking.NetworkHandler;
import com.soliddowant.gregtechenergistics.networking.PacketPatternAction;
import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.util.AEPartLocation;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ExtendedPatternGuiContainer extends GuiMEMonitorable {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(Tags.MODID,
            "textures/guis/extendedpattern.png");

    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;

    public ExtendedPatternGuiContainer(final InventoryPlayer inventoryPlayer, final ExtendedPatternTerminalPart part) {
        super(inventoryPlayer, part, new ExtendedPatternContainer(inventoryPlayer, part));
        // Reserve space for pattern encoding area (from y=84 to bottom)
        // This tells the parent to stop the item grid at this point
        setReservedSpaceViaReflection(165);  // 249 - 84 = 165 pixels for pattern area + player inv
    }

    private void setReservedSpaceViaReflection(int space) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("reservedSpace");
            field.setAccessible(true);
            field.setInt(this, space);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set reservedSpace field", e);
        }
    }

    private int getReservedSpaceViaReflection() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("reservedSpace");
            field.setAccessible(true);
            return field.getInt(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get reservedSpace field", e);
        }
    }

    public static ExtendedPatternGuiContainer getClientGuiContainer(AEPartLocation side, EntityPlayer player,
            World world,
            int x, int y, int z) {
        ExtendedPatternTerminalPart part = GuiProxy.getPartAtLocation(world, x, y, z, side,
                ExtendedPatternTerminalPart.class);
        if (part == null)
            return null;

        return new ExtendedPatternGuiContainer(player.inventory, part);
    }

    @Override
    public void initGui() {
        super.initGui();

        // Calculate where the reserved space (pattern area) starts
        int reservedStart = this.guiTop + (this.ySize - getReservedSpaceViaReflection());

        // Encode button - positioned relative to pattern area
        this.encodeBtn = new GuiImgButton(this.guiLeft + 184, reservedStart + 24, Settings.ACTIONS, ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        // Clear button - positioned relative to pattern area
        this.clearBtn = new GuiImgButton(this.guiLeft + 100, reservedStart - 1, Settings.ACTIONS, ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        if (this.encodeBtn == btn) {
            // Encode the pattern
            NetworkHandler.ServerHandlerChannel.sendToServer(
                    new PacketPatternAction(PacketPatternAction.Action.ENCODE));
        }

        if (this.clearBtn == btn) {
            // Clear all slots
            NetworkHandler.ServerHandlerChannel.sendToServer(
                    new PacketPatternAction(PacketPatternAction.Action.CLEAR));
        }
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        // Draw inherited terminal features (search box, item grid, etc.)
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        // Draw pattern area labels
        this.fontRenderer.drawString("Inputs", 8, 72, 4210752);
        this.fontRenderer.drawString("Outputs", 110, 72, 4210752);

        // Draw player inventory label
        this.fontRenderer.drawString(
                I18n.format("container.inventory"),
                8, 157, 4210752);
    }

    @Override
    protected String getBackground() {
        // Return our custom texture that includes both terminal and pattern areas
        return "guis/extendedpattern.png";
    }

    @Override
    public void bindTexture(final String file) {
        // Use our mod ID instead of AE2's mod ID for texture loading
        final ResourceLocation loc = new ResourceLocation(Tags.MODID, "textures/" + file);
        this.mc.getTextureManager().bindTexture(loc);
    }

    protected void bindTexture(final ResourceLocation loc) {
        this.mc.getTextureManager().bindTexture(loc);
    }
}
