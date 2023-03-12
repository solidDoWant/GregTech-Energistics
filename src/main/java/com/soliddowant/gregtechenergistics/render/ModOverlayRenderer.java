package com.soliddowant.gregtechenergistics.render;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ModOverlayRenderer extends SimpleOverlayRenderer {
    protected TextureAtlasSprite sprite;
    protected String modId;
    protected String basePath;

    public ModOverlayRenderer(String modId, String basePath) {
        super(basePath);
        this.modId = modId;
        this.basePath = basePath;

        gregtech.client.renderer.texture.Textures.iconRegisters.remove(this);
        Textures.iconRegisters.add(this);
    }

    public ModOverlayRenderer(String basePath) {
        this(GregTechEnergisticsMod.MODID, basePath);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(TextureMap textureMap) {
        this.sprite = textureMap.registerSprite(new ResourceLocation(modId, "blocks/" + basePath));
    }

    @SideOnly(Side.CLIENT)
    public void renderSided(EnumFacing side, Cuboid6 bounds, CCRenderState renderState, IVertexOperation[] pipeline,
                            Matrix4 translation) {
        gregtech.client.renderer.texture.Textures.renderFace(renderState, translation, pipeline, side, bounds, sprite, BlockRenderLayer.CUTOUT);
    }
}
