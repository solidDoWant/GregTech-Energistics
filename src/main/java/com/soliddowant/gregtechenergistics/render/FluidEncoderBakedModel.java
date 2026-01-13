package com.soliddowant.gregtechenergistics.render;

import java.util.List;
import java.util.Optional;

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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ItemLayerModel;
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

    public FluidEncoderBakedModel(IBakedModel baseModel, TextureAtlasSprite baseSprite, TextureAtlasSprite maskSprite) {
        this.baseModel = baseModel;
        this.baseSprite = baseSprite;
        this.maskSprite = maskSprite;
        this.overrideList = new FluidEncoderOverrideList(this);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        // Default: empty encoder with gray droplet
        return getQuadsForFluid(null, side, rand);
    }

    /**
     * Get quads for a specific fluid (called from override list)
     */
    public List<BakedQuad> getQuadsForFluid(@Nullable FluidStack fluidStack, @Nullable EnumFacing side, long rand) {
        if (side != null) {
            return ImmutableList.of();
        }

        ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();

        // Layer 0: Fluid texture or mask sprite
        TextureAtlasSprite layer0Sprite = maskSprite; // Default to mask

        if (fluidStack != null && fluidStack.getFluid() != null) {
            Fluid fluid = fluidStack.getFluid();
            ResourceLocation fluidStill = fluid.getStill(fluidStack);

            if (fluidStill != null) {
                // Try to get the fluid texture from the atlas
                TextureAtlasSprite fluidSprite = textureMap.getAtlasSprite(fluidStill.toString());

                // Check if we got a valid sprite (not the missing texture)
                TextureAtlasSprite missingSprite = textureMap.getMissingSprite();
                if (fluidSprite != null && fluidSprite != missingSprite) {
                    layer0Sprite = fluidSprite;
                }
                // If fluid texture not found, keep maskSprite and IItemColor will tint it
            }
        }

        // Generate quads for layer 0 using ItemLayerModel's quad builder
        quads.addAll(ItemLayerModel.getQuadsForSprite(0, layer0Sprite,
                DefaultVertexFormats.ITEM, Optional.empty()));

        // Layer 1: Base frame
        quads.addAll(ItemLayerModel.getQuadsForSprite(1, baseSprite,
                DefaultVertexFormats.ITEM, Optional.empty()));

        return quads.build();
    }

    /**
     * Check if a fluid's texture was successfully loaded
     */
    public boolean hasFluidTexture(@Nullable FluidStack fluidStack) {
        if (fluidStack == null || fluidStack.getFluid() == null) {
            return false;
        }

        Fluid fluid = fluidStack.getFluid();
        ResourceLocation fluidStill = fluid.getStill(fluidStack);
        if (fluidStill == null) {
            return false;
        }

        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite fluidSprite = textureMap.getAtlasSprite(fluidStill.toString());
        TextureAtlasSprite missingSprite = textureMap.getMissingSprite();

        return fluidSprite != null && fluidSprite != missingSprite;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
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
            return new FluidEncoderRenderedModel(parent, fluidStack);
        }
    }

    /**
     * Inner model class that renders with a specific fluid
     */
    private static class FluidEncoderRenderedModel implements IBakedModel {
        private final FluidEncoderBakedModel parent;
        private final FluidStack fluidStack;

        public FluidEncoderRenderedModel(FluidEncoderBakedModel parent, FluidStack fluidStack) {
            this.parent = parent;
            this.fluidStack = fluidStack;
        }

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

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
            return parent.handlePerspective(cameraTransformType);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseModel.getItemCameraTransforms();
    }
}
