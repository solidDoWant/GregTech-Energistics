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
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
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
        // We'll set the fixed size in initGui() after parent calculates dynamic size
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

        // Force our GUI to use the fixed size from our texture
        this.ySize = 247;
        this.xSize = 249;

        // Recalculate guiTop based on our fixed size
        final int unusedSpace = this.height - this.ySize;
        this.guiTop = (int) Math.floor(unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));
        this.guiLeft = (this.width - this.xSize) / 2;

        // Encode button - positioned at original location
        this.encodeBtn = new GuiImgButton(this.guiLeft + 184, this.guiTop + 108, Settings.ACTIONS, ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        // Clear button - positioned at original location
        this.clearBtn = new GuiImgButton(this.guiLeft + 100, this.guiTop + 83, Settings.ACTIONS, ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);

        // Move buttons to the correct positions
        for (GuiButton settingsButton : this.buttonList) {
            if (settingsButton instanceof GuiTabButton) {
                // This is the crafting status button
                GuiTabButton tabBtn = (GuiTabButton) settingsButton;

                tabBtn.x = this.guiLeft + 170;
                tabBtn.y = this.guiTop - 4;
            }

            // All remaining buttons that need updating are GuiImgButtons
            if (!(settingsButton instanceof GuiImgButton))
                continue;

            GuiImgButton imgBtn = (GuiImgButton) settingsButton;
            Settings buttonSetting = imgBtn.getSetting();

            if (buttonSetting == Settings.ACTIONS)
                continue;

            // Shift the buttons to the correct position relative to the scrolling display
            settingsButton.x = this.guiLeft - 18;
            settingsButton.y -= 3 + 2 * 18;

            // Hide the terminal style button because it has no effect here
            if (buttonSetting == Settings.TERMINAL_STYLE)
                settingsButton.visible = false;
        }

        // Set search field
        this.updateSearchFieldPosition(82, 6);
    }

    // Sets the position of the search field via reflection. If it fails, the search
    // field
    // will remain at its default position, but at least the game won't crash.
    protected void updateSearchFieldPosition(int x, int y) {
        try {
            Field searchField = this.getClass().getSuperclass().getDeclaredField("searchField");
            searchField.setAccessible(true);
            Object field = searchField.get(this);
            if (field == null)
                return;

            if (!(field instanceof MEGuiTextField))
                return;

            MEGuiTextField textField = (MEGuiTextField) field;
            textField.x = this.guiLeft + x;
            textField.y = this.guiTop + y;
        } catch (Exception e) {
            // Ignore - search field position won't be set but the game won't crash
        }
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
        // Draw title at original position
        this.fontRenderer.drawString(
                I18n.format("gui.gregtechenergistics.extendedpattern.title"),
                8, 6, 4210752);

        // Draw labels at original positions
        this.fontRenderer.drawString("Inputs", 8, 72, 4210752);
        this.fontRenderer.drawString("Outputs", 110, 72, 4210752);

        // Draw player inventory label at original position
        this.fontRenderer.drawString(
                I18n.format("container.inventory"),
                8, 157, 4210752);

        // Let parent handle any additional rendering it needs to do
        // Note: We don't call super.drawFG() because it would override our labels
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        // Draw our complete custom texture (fixed size, not dynamic)
        this.bindTexture(BACKGROUND_TEXTURE);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);

        // Handle search field rendering via reflection
        try {
            java.lang.reflect.Field searchField = this.getClass().getSuperclass().getDeclaredField("searchField");
            searchField.setAccessible(true);
            Object field = searchField.get(this);
            if (field != null) {
                ((appeng.client.gui.widgets.MEGuiTextField) field).drawTextBox();
            }
        } catch (Exception e) {
            // Ignore - search field won't render but rest of GUI will work
        }

        // Handle view cell repository updates via reflection
        try {
            java.lang.reflect.Field viewCellField = this.getClass().getSuperclass().getDeclaredField("viewCell");
            viewCellField.setAccessible(true);
            boolean viewCell = viewCellField.getBoolean(this);

            if (viewCell) {
                java.lang.reflect.Field myCurrentViewCellsField = this.getClass().getSuperclass()
                        .getDeclaredField("myCurrentViewCells");
                myCurrentViewCellsField.setAccessible(true);
                ItemStack[] myCurrentViewCells = (ItemStack[]) myCurrentViewCellsField.get(this);

                java.lang.reflect.Field monitorableContainerField = this.getClass().getSuperclass()
                        .getDeclaredField("monitorableContainer");
                monitorableContainerField.setAccessible(true);
                appeng.container.implementations.ContainerMEMonitorable monitorableContainer = (appeng.container.implementations.ContainerMEMonitorable) monitorableContainerField
                        .get(this);

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
            // Ignore - view cells won't update but rest of GUI will work
        }
    }

    @Override
    protected String getBackground() {
        // Return our custom texture that includes both terminal and pattern areas
        return "guis/extendedpattern.png";
    }

    @Override
    protected void repositionSlot(final appeng.container.slot.AppEngSlot s) {
        // Don't reposition - we use absolute positions in our container
        // The slots are already at the correct positions
    }

    protected void bindTexture(final ResourceLocation loc) {
        this.mc.getTextureManager().bindTexture(loc);
    }

    @Override
    protected int getMaxRows() {
        return 3;
    }
}
