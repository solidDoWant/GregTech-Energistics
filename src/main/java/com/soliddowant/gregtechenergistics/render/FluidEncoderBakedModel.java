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
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Custom baked model for fluid encoder that renders the actual fluid texture
 * in the droplet area, not just a color tint.
 */
public class FluidEncoderBakedModel implements IBakedModel {

    // Thread-local to store the current ItemStack being rendered
    private static final ThreadLocal<ItemStack> CURRENT_STACK = new ThreadLocal<>();

    private final IBakedModel baseModel;
    private final TextureAtlasSprite baseSprite;
    private final TextureAtlasSprite maskSprite;
    private final FluidEncoderOverrideList overrideList;

    // Droplet bounds calculated from mask sprite (in 0-16 pixel coordinates)
    private final float dropletMinX;
    private final float dropletMinY;
    private final float dropletMaxX;
    private final float dropletMaxY;

    public FluidEncoderBakedModel(IBakedModel baseModel, TextureAtlasSprite baseSprite, TextureAtlasSprite maskSprite) {
        this.baseModel = baseModel;
        this.baseSprite = baseSprite;
        this.maskSprite = maskSprite;
        this.overrideList = new FluidEncoderOverrideList(this);

        // Calculate droplet bounds from mask sprite
        float[] bounds = calculateDropletBounds(maskSprite);
        this.dropletMinX = bounds[0];
        this.dropletMinY = bounds[1];
        this.dropletMaxX = bounds[2];
        this.dropletMaxY = bounds[3];
    }

    /**
     * Calculate the bounding box of opaque pixels in the mask sprite.
     * Returns [minX, minY, maxX, maxY] in pixel coordinates (0-16).
     */
    private float[] calculateDropletBounds(TextureAtlasSprite mask) {
        int width = mask.getIconWidth();
        int height = mask.getIconHeight();

        // Default to full sprite if we can't read pixels
        float minX = 0, minY = 0, maxX = 16, maxY = 16;

        // Get the first animation frame (frame 0)
        int[][] frameData = mask.getFrameTextureData(0);
        if (frameData != null && frameData.length > 0 && frameData[0] != null) {
            int[] pixels = frameData[0];

            minX = width;
            minY = height;
            maxX = 0;
            maxY = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    int alpha = (pixel >> 24) & 0xFF;

                    // If pixel is opaque (alpha > 128), include in bounds
                    if (alpha > 128) {
                        if (x < minX)
                            minX = x;
                        if (y < minY)
                            minY = y;
                        if (x + 1 > maxX)
                            maxX = x + 1;
                        if (y + 1 > maxY)
                            maxY = y + 1;
                    }
                }
            }

            // If no opaque pixels found, use full sprite
            if (maxX <= minX || maxY <= minY) {
                minX = 0;
                minY = 0;
                maxX = width;
                maxY = height;
            }

            // Scale to 16x16 coordinate space
            minX = (minX / width) * 16;
            minY = (minY / height) * 16;
            maxX = (maxX / width) * 16;
            maxY = (maxY / height) * 16;
        }

        return new float[] { minX, minY, maxX, maxY };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        // Get the current stack from thread-local (set by handleItemState)
        ItemStack currentStack = CURRENT_STACK.get();
        FluidStack fluidStack = null;

        if (currentStack != null && FluidEncoderBehaviour.hasStackBehavior(currentStack)) {
            fluidStack = FluidEncoderBehaviour.getFluidStack(currentStack);
        }

        // Clean up thread-local to prevent memory leaks
        CURRENT_STACK.remove();

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

        // Layer 0: Fluid texture or mask sprite, bounded to droplet area
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

        // Layer 0: Bounded quads for fluid/mask (only covers droplet area)
        // Need both front and back faces so item looks correct from both sides
        quads.add(buildBoundedQuad(layer0Sprite, 0,
                dropletMinX / 16f, dropletMinY / 16f,
                dropletMaxX / 16f, dropletMaxY / 16f,
                EnumFacing.SOUTH)); // Front face
        quads.add(buildBoundedQuad(layer0Sprite, 0,
                dropletMinX / 16f, dropletMinY / 16f,
                dropletMaxX / 16f, dropletMaxY / 16f,
                EnumFacing.NORTH)); // Back face

        // Layer 1: Full frame (unchanged - uses ItemLayerModel)
        quads.addAll(ItemLayerModel.getQuadsForSprite(1, baseSprite,
                DefaultVertexFormats.ITEM, Optional.empty()));

        return quads.build();
    }

    /**
     * Build a bounded quad that only covers the specified area.
     * Coordinates are in 0-1 range (e.g., 0.25 to 0.75 for center half).
     */
    private BakedQuad buildBoundedQuad(TextureAtlasSprite texture, int tintIndex,
            float minX, float minY, float maxX, float maxY, EnumFacing facing) {

        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
        builder.setQuadTint(tintIndex);
        builder.setTexture(texture);
        builder.setQuadOrientation(facing);

        // Z position - front face slightly in front, back face slightly behind
        float z = facing == EnumFacing.SOUTH ? 7.5f / 16f : 8.5f / 16f;

        // Normal direction
        float normalZ = facing == EnumFacing.SOUTH ? 1f : -1f;

        // Calculate UV coordinates - sample from the bounded region of the texture
        float u0 = texture.getInterpolatedU(minX * 16);
        float u1 = texture.getInterpolatedU(maxX * 16);
        float v0 = texture.getInterpolatedV(minY * 16);
        float v1 = texture.getInterpolatedV(maxY * 16);

        if (facing == EnumFacing.SOUTH) {
            // Front face - counter-clockwise winding
            putVertex(builder, minX, 1 - maxY, z, u0, v1, normalZ); // Bottom-left
            putVertex(builder, maxX, 1 - maxY, z, u1, v1, normalZ); // Bottom-right
            putVertex(builder, maxX, 1 - minY, z, u1, v0, normalZ); // Top-right
            putVertex(builder, minX, 1 - minY, z, u0, v0, normalZ); // Top-left
        } else {
            // Back face - clockwise winding (reversed order)
            putVertex(builder, minX, 1 - minY, z, u0, v0, normalZ); // Top-left
            putVertex(builder, maxX, 1 - minY, z, u1, v0, normalZ); // Top-right
            putVertex(builder, maxX, 1 - maxY, z, u1, v1, normalZ); // Bottom-right
            putVertex(builder, minX, 1 - maxY, z, u0, v1, normalZ); // Bottom-left
        }

        return builder.build();
    }

    /**
     * Put a vertex into the quad builder.
     */
    private void putVertex(UnpackedBakedQuad.Builder builder, float x, float y, float z, float u, float v,
            float normalZ) {
        VertexFormat format = DefaultVertexFormats.ITEM;
        for (int e = 0; e < format.getElementCount(); e++) {
            switch (format.getElement(e).getUsage()) {
                case POSITION:
                    builder.put(e, x, y, z, 1.0f);
                    break;
                case COLOR:
                    builder.put(e, 1.0f, 1.0f, 1.0f, 1.0f);
                    break;
                case UV:
                    if (format.getElement(e).getIndex() == 0) {
                        builder.put(e, u, v, 0f, 1f);
                    } else {
                        // Lightmap UVs
                        builder.put(e, 0f, 0f, 0f, 1f);
                    }
                    break;
                case NORMAL:
                    builder.put(e, 0f, 0f, normalZ, 0f);
                    break;
                default:
                    builder.put(e);
            }
        }
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
