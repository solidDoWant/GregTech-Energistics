package com.soliddowant.gregtechenergistics.render;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Custom baked model for fluid encoder that renders the actual fluid texture
 * in the droplet area, not just a color tint.
 */
public class FluidEncoderBakedModel implements IBakedModel {

    private final IBakedModel baseModel;
    private final TextureAtlasSprite baseSprite;
    private final TextureAtlasSprite maskSprite;
    private final FluidEncoderOverrideList overrideList;

    // Gray color for empty encoder (RGB components as floats)
    private static final float EMPTY_R = 0.6f;
    private static final float EMPTY_G = 0.6f;
    private static final float EMPTY_B = 0.6f;

    public FluidEncoderBakedModel(IBakedModel baseModel, TextureAtlasSprite baseSprite, TextureAtlasSprite maskSprite) {
        this.baseModel = baseModel;
        this.baseSprite = baseSprite;
        this.maskSprite = maskSprite;
        this.overrideList = new FluidEncoderOverrideList(this);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        // Return base model quads by default (for empty encoder or non-GUI rendering)
        return baseModel.getQuads(state, side, rand);
    }

    /**
     * Get quads for a specific fluid (called from override list)
     */
    public List<BakedQuad> getQuadsForFluid(@Nullable FluidStack fluidStack, @Nullable EnumFacing side, long rand) {
        if (side != null) {
            return ImmutableList.of();
        }

        List<BakedQuad> quads = new ArrayList<>();
        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();

        // Layer 0: Fluid texture (or gray for empty) in droplet shape
        if (fluidStack != null && fluidStack.getFluid() != null) {
            Fluid fluid = fluidStack.getFluid();
            ResourceLocation fluidTexture = fluid.getStill(fluidStack);
            TextureAtlasSprite fluidSprite = textureMap.getAtlasSprite(fluidTexture.toString());
            int fluidColor = fluid.getColor(fluidStack);

            // Add fluid quad using the mask shape
            quads.addAll(getQuadsForSprite(0, maskSprite, fluidSprite, fluidColor));
        } else {
            // Empty - render gray droplet
            quads.addAll(getQuadsForSolidColor(0, maskSprite, EMPTY_R, EMPTY_G, EMPTY_B));
        }

        // Layer 1: Base frame (with transparent droplet hole)
        quads.addAll(getQuadsForSprite(1, baseSprite, baseSprite, 0xFFFFFFFF));

        return quads;
    }

    /**
     * Create quads for a sprite with texture from another sprite and color tint
     */
    private List<BakedQuad> getQuadsForSprite(int layerIndex, TextureAtlasSprite shape,
            TextureAtlasSprite texture, int color) {
        List<BakedQuad> quads = new ArrayList<>();

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        // Create a simple quad for the item face
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
        builder.setQuadTint(layerIndex);
        builder.setTexture(texture);
        builder.setQuadOrientation(EnumFacing.SOUTH);

        // Vertex positions for a standard item quad (16x16 mapped to 0-1)
        float z = layerIndex * 0.001f; // Slight z-offset per layer
        putVertex(builder, 0, 0, z, texture.getMinU(), texture.getMaxV(), r, g, b, a);
        putVertex(builder, 1, 0, z, texture.getMaxU(), texture.getMaxV(), r, g, b, a);
        putVertex(builder, 1, 1, z, texture.getMaxU(), texture.getMinV(), r, g, b, a);
        putVertex(builder, 0, 1, z, texture.getMinU(), texture.getMinV(), r, g, b, a);

        quads.add(builder.build());
        return quads;
    }

    /**
     * Create quads for a solid color using the shape sprite
     */
    private List<BakedQuad> getQuadsForSolidColor(int layerIndex, TextureAtlasSprite shape,
            float r, float g, float b) {
        List<BakedQuad> quads = new ArrayList<>();

        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
        builder.setQuadTint(layerIndex);
        builder.setTexture(shape);
        builder.setQuadOrientation(EnumFacing.SOUTH);

        float z = layerIndex * 0.001f;
        putVertex(builder, 0, 0, z, shape.getMinU(), shape.getMaxV(), r, g, b, 1.0f);
        putVertex(builder, 1, 0, z, shape.getMaxU(), shape.getMaxV(), r, g, b, 1.0f);
        putVertex(builder, 1, 1, z, shape.getMaxU(), shape.getMinV(), r, g, b, 1.0f);
        putVertex(builder, 0, 1, z, shape.getMinU(), shape.getMinV(), r, g, b, 1.0f);

        quads.add(builder.build());
        return quads;
    }

    private void putVertex(UnpackedBakedQuad.Builder builder, float x, float y, float z,
            float u, float v, float r, float g, float b, float a) {
        VertexFormat format = DefaultVertexFormats.ITEM;
        for (int e = 0; e < format.getElementCount(); e++) {
            switch (format.getElement(e).getUsage()) {
                case POSITION:
                    builder.put(e, x, y, z, 1.0f);
                    break;
                case COLOR:
                    builder.put(e, r, g, b, a);
                    break;
                case UV:
                    if (format.getElement(e).getIndex() == 0) {
                        builder.put(e, u, v, 0f, 1f);
                    } else {
                        builder.put(e, 0f, 0f, 0f, 1f);
                    }
                    break;
                case NORMAL:
                    builder.put(e, 0f, 0f, 1f, 0f);
                    break;
                default:
                    builder.put(e);
            }
        }
    }

    @Override
    public boolean isAmbientOcclusion() {
        return baseModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseSprite;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrideList;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        return Pair.of(this, baseModel.handlePerspective(cameraTransformType).getRight());
    }

    /**
     * Override list that returns a fluid-specific model based on the ItemStack's NBT
     */
    private static class FluidEncoderOverrideList extends ItemOverrideList {
        private final FluidEncoderBakedModel parent;

        public FluidEncoderOverrideList(FluidEncoderBakedModel parent) {
            super(ImmutableList.of());
            this.parent = parent;
        }

        @Override
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack,
                @Nullable World world, @Nullable EntityLivingBase entity) {
            if (!FluidEncoderBehaviour.hasStackBehavior(stack)) {
                return originalModel;
            }

            FluidStack fluidStack = FluidEncoderBehaviour.getFluidStack(stack);

            // Return a wrapper that provides the fluid-specific quads
            return new IBakedModel() {
                @Override
                public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
                    return parent.getQuadsForFluid(fluidStack, side, rand);
                }

                @Override
                public boolean isAmbientOcclusion() { return parent.isAmbientOcclusion(); }
                @Override
                public boolean isGui3d() { return parent.isGui3d(); }
                @Override
                public boolean isBuiltInRenderer() { return false; }
                @Override
                public TextureAtlasSprite getParticleTexture() { return parent.getParticleTexture(); }
                @Override
                public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }

                @Override
                @SuppressWarnings("deprecation")
                public ItemCameraTransforms getItemCameraTransforms() {
                    return parent.baseModel.getItemCameraTransforms();
                }
            };
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseModel.getItemCameraTransforms();
    }
}
