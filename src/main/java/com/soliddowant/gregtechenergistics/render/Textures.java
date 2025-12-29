package com.soliddowant.gregtechenergistics.render;

import java.util.ArrayList;
import java.util.List;

import appeng.core.AppEng;
import codechicken.lib.texture.TextureUtils;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.render.SimpleOverlayRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Textures {
    public static List<TextureUtils.IIconRegister> iconRegisters = new ArrayList<>();

    public static SimpleOverlayRenderer MACHINE_STATUS_OVERLAY = new ModOverlayRenderer(
            "overlay/machine/overlay_status");
    public static SimpleOverlayRenderer STOCKER_OVERLAY = new ModOverlayRenderer("overlay/machine/overlay_stocker");

    public static ResourceLocation AE2SpriteMap = new ResourceLocation(AppEng.MOD_ID, "textures/guis/states.png");

    @SideOnly(Side.CLIENT)
    public static void register(TextureMap textureMap) {
        for (TextureUtils.IIconRegister iconRegister : iconRegisters)
            iconRegister.registerIcons(textureMap);
    }

    public static TextureArea getAE2Sprite(int x, int y) {
        assert x <= 16 && x >= 0 && y <= 16 && y >= 0 : "The sprite map coordinates are out of bounds";
        return new TextureArea(AE2SpriteMap, ((float) x / 16.0), ((float) y / 16.0), (1.0 / 16.0), (1.0 / 16.0));
    }
}
