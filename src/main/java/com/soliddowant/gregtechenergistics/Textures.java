package com.soliddowant.gregtechenergistics;

import codechicken.lib.texture.TextureUtils;
import gregtech.api.render.SimpleOverlayRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class Textures {
    public static List<TextureUtils.IIconRegister> iconRegisters = new ArrayList<>();

    public static SimpleOverlayRenderer MACHINE_STATUS_OVERLAY = new ModOverlayRenderer("overlay/machine/overlay_status");
    public static SimpleOverlayRenderer STOCKER_OVERLAY = new ModOverlayRenderer("overlay/machine/overlay_stocker");

    @SideOnly(Side.CLIENT)
    public static void register(TextureMap textureMap) {
        for (TextureUtils.IIconRegister iconRegister : iconRegisters)
            iconRegister.registerIcons(textureMap);
    }
}
