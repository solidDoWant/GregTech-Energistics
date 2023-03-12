package com.soliddowant.gregtechenergistics.gui.widgets;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.widgets.SimpleTextWidget;
import gregtech.api.util.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

import java.util.function.Supplier;

public class NestedTextWidget extends SimpleTextWidget {
    public NestedTextWidget(int xPosition, int yPosition, String formatLocale, int color, Supplier<String> textSupplier) {
        super(xPosition, yPosition, formatLocale, color, textSupplier);
    }

    public NestedTextWidget(int xPosition, int yPosition, String formatLocale, Supplier<String> textSupplier) {
        super(xPosition, yPosition, formatLocale, textSupplier);
    }

    @Override
    public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String text = getDisplayText();
        Position position = getPosition();
        fontRenderer.drawString(text,
                position.x - fontRenderer.getStringWidth(text) / 2,
                position.y - fontRenderer.FONT_HEIGHT / 2, color);
        GlStateManager.color(1.0f, 1.0f, 1.0f);
    }

    public String getDisplayText() {
        if(formatLocale.isEmpty())
            if(I18n.hasKey(lastText))
                return I18n.format(lastText);
            else
                return lastText;
        else
            return I18n.format(formatLocale, I18n.format(lastText));
    }
}
