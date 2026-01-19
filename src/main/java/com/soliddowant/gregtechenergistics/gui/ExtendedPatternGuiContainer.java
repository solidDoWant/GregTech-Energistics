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
import net.minecraft.item.ItemStack;
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

        // Encode button - positioned in the pattern area
        this.encodeBtn = new GuiImgButton(this.guiLeft + 184, this.guiTop + this.ySize - 53, Settings.ACTIONS, ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        // Clear button - positioned above the input slots
        this.clearBtn = new GuiImgButton(this.guiLeft + 100, this.guiTop + this.ySize - 86, Settings.ACTIONS, ActionItems.CLOSE);
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

        // Draw pattern area labels - positioned relative to reserved space
        int patternAreaY = this.ySize - getReservedSpaceViaReflection();
        this.fontRenderer.drawString("Inputs", 8, patternAreaY - 12, 4210752);
        this.fontRenderer.drawString("Outputs", 110, patternAreaY - 12, 4210752);

        // Draw player inventory label
        this.fontRenderer.drawString(
                I18n.format("container.inventory"),
                8, this.ySize - 96 + 3, 4210752);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        // Get the number of rows in the item grid via reflection
        int rows = 0;
        try {
            java.lang.reflect.Field rowsField = this.getClass().getSuperclass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            rows = rowsField.getInt(this);
        } catch (Exception e) {
            // Default to 3 rows if we can't access the field
            rows = 3;
        }

        // Bind our custom texture
        this.bindTexture(BACKGROUND_TEXTURE);

        final int x_width = 197;

        // Draw top section (header with search box) - 18px tall
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, x_width, 18);

        // Draw view cells area on the right if present
        try {
            java.lang.reflect.Field viewCellField = this.getClass().getSuperclass().getDeclaredField("viewCell");
            viewCellField.setAccessible(true);
            boolean viewCell = viewCellField.getBoolean(this);

            if (viewCell) {
                this.drawTexturedModalRect(offsetX + x_width, offsetY, x_width, 0, 46, 128);
            }
        } catch (Exception e) {
            // Ignore
        }

        // Draw repeating middle section (item grid rows) - each row is 18px
        for (int x = 0; x < rows; x++) {
            this.drawTexturedModalRect(offsetX, offsetY + 18 + x * 18, 0, 18, x_width, 18);
        }

        // Draw bottom section (pattern area + player inventory)
        // This starts where the item grid ends and goes to the bottom
        int bottomSectionStartY = offsetY + 18 + rows * 18;
        int bottomSectionTextureY = 18 + 3 * 18; // After header (18) + 3 item rows (54) = 72
        int bottomSectionHeight = this.ySize - 18 - rows * 18;

        this.drawTexturedModalRect(offsetX, bottomSectionStartY, 0, bottomSectionTextureY, x_width, bottomSectionHeight);

        // Handle view cell updates and search field via parent logic
        try {
            java.lang.reflect.Field searchField = this.getClass().getSuperclass().getDeclaredField("searchField");
            searchField.setAccessible(true);
            Object field = searchField.get(this);
            if (field != null) {
                ((appeng.client.gui.widgets.MEGuiTextField) field).drawTextBox();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Handle view cell repository updates
        try {
            java.lang.reflect.Field viewCellField = this.getClass().getSuperclass().getDeclaredField("viewCell");
            viewCellField.setAccessible(true);
            boolean viewCell = viewCellField.getBoolean(this);

            if (viewCell) {
                java.lang.reflect.Field myCurrentViewCellsField = this.getClass().getSuperclass().getDeclaredField("myCurrentViewCells");
                myCurrentViewCellsField.setAccessible(true);
                ItemStack[] myCurrentViewCells = (ItemStack[]) myCurrentViewCellsField.get(this);

                java.lang.reflect.Field monitorableContainerField = this.getClass().getSuperclass().getDeclaredField("monitorableContainer");
                monitorableContainerField.setAccessible(true);
                appeng.container.implementations.ContainerMEMonitorable monitorableContainer =
                    (appeng.container.implementations.ContainerMEMonitorable) monitorableContainerField.get(this);

                boolean update = false;
                for (int i = 0; i < 5; i++) {
                    if (myCurrentViewCells[i] != monitorableContainer.getCellViewSlot(i).getStack()) {
                        update = true;
                        myCurrentViewCells[i] = monitorableContainer.getCellViewSlot(i).getStack();
                    }
                }

                if (update) {
                    this.repo.setViewCell(myCurrentViewCells);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    protected String getBackground() {
        // Return our custom texture that includes both terminal and pattern areas
        return "guis/extendedpattern.png";
    }

    @Override
    protected void repositionSlot(final appeng.container.slot.AppEngSlot s) {
        // Calculate offset for player-side vs pattern-side slots
        final int offsetPlayerSide = s.isPlayerSide() ? 5 : 3;

        // Reposition relative to the GUI size (pattern area is at bottom)
        s.yPos = s.getY() + this.ySize - 78 - offsetPlayerSide;
    }

    protected void bindTexture(final ResourceLocation loc) {
        this.mc.getTextureManager().bindTexture(loc);
    }
}
