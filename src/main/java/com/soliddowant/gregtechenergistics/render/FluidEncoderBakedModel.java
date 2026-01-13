package com.soliddowant.gregtechenergistics.render;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger("GTEnergistics");
    private static boolean hasLoggedOnce = false;

    // Thread-local to store the current ItemStack being rendered
    private static final ThreadLocal<ItemStack> CURRENT_STACK = new ThreadLocal<>();

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
        // Get the current stack from thread-local (set by handleItemState)
        ItemStack currentStack = CURRENT_STACK.get();
        FluidStack fluidStack = null;

        if (currentStack != null && FluidEncoderBehaviour.hasStackBehavior(currentStack)) {
            fluidStack = FluidEncoderBehaviour.getFluidStack(currentStack);
            LOGGER.info("[FluidEncoder] getQuads called with stack, fluidStack={}",
                    fluidStack != null ? fluidStack.getFluid().getName() : "null");
        } else {
            LOGGER.info("[FluidEncoder] getQuads called without stack (using default)");
        }

        return getQuadsForFluid(fluidStack, side, rand);
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
        String debugInfo = "empty/null";

        if (fluidStack != null && fluidStack.getFluid() != null) {
            Fluid fluid = fluidStack.getFluid();
            ResourceLocation fluidStill = fluid.getStill(fluidStack);
            debugInfo = "fluid=" + fluid.getName() + ", still=" + fluidStill;

            if (fluidStill != null) {
                // Try to get the fluid texture from the atlas
                TextureAtlasSprite fluidSprite = textureMap.getAtlasSprite(fluidStill.toString());

                // Check if we got a valid sprite (not the missing texture)
                TextureAtlasSprite missingSprite = textureMap.getMissingSprite();
                if (fluidSprite != null && fluidSprite != missingSprite) {
                    layer0Sprite = fluidSprite;
                    debugInfo += ", FOUND sprite: " + fluidSprite.getIconName();
                } else {
                    debugInfo += ", NOT FOUND (got missing sprite)";
                }
            }
        }

        // Log once per session to avoid spam
        if (!hasLoggedOnce) {
            LOGGER.info("[FluidEncoder] getQuadsForFluid: {}", debugInfo);
            LOGGER.info("[FluidEncoder] Using layer0 sprite: {} ({}x{})",
                    layer0Sprite.getIconName(), layer0Sprite.getIconWidth(), layer0Sprite.getIconHeight());
            LOGGER.info("[FluidEncoder] Using layer1 sprite: {} ({}x{})",
                    baseSprite.getIconName(), baseSprite.getIconWidth(), baseSprite.getIconHeight());
            hasLoggedOnce = true;
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
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
            ItemCameraTransforms.TransformType cameraTransformType) {
        return Pair.of(this, baseModel.handlePerspective(cameraTransformType).getRight());
    }

    /**
     * Override list that returns a fluid-specific model based on the ItemStack's
     * NBT
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
            // Store the current stack in thread-local so getQuads can access it
            CURRENT_STACK.set(stack);

            if (!hasLoggedOnce) {
                FluidStack fluidStack = FluidEncoderBehaviour.hasStackBehavior(stack)
                        ? FluidEncoderBehaviour.getFluidStack(stack)
                        : null;
                LOGGER.info("[FluidEncoder] handleItemState called, fluidStack: {}",
                        fluidStack != null ? fluidStack.getFluid().getName() + " x" + fluidStack.amount : "null");
            }

            // Return the parent model itself - it will use CURRENT_STACK in getQuads
            return parent;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseModel.getItemCameraTransforms();
    }
}
