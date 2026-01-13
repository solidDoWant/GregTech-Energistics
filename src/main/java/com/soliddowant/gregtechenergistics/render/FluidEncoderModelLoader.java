package com.soliddowant.gregtechenergistics.render;

import java.util.Collection;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.soliddowant.gregtechenergistics.Tags;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;

/**
 * Custom model loader for the fluid encoder item.
 * Intercepts the model loading and returns a custom model that renders fluid textures.
 */
public class FluidEncoderModelLoader implements ICustomModelLoader {

    private static final Logger LOGGER = LogManager.getLogger("GTEnergistics");

    public static final FluidEncoderModelLoader INSTANCE = new FluidEncoderModelLoader();

    private static final ResourceLocation BASE_TEXTURE =
            new ResourceLocation(Tags.MODID, "items/metaitems/fluid.encoder");
    private static final ResourceLocation MASK_TEXTURE =
            new ResourceLocation(Tags.MODID, "items/metaitems/fluid.encoder.underlay");

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        // Only handle our specific fluid encoder model
        // GregTech registers models with path "metaitems/..." not "item/metaitems/..."
        boolean accepts = modelLocation.getNamespace().equals(Tags.MODID) &&
               modelLocation.getPath().equals("metaitems/fluid.encoder");

        if (accepts) {
            LOGGER.info("[FluidEncoder] Model loader ACCEPTING: {}", modelLocation);
        }
        return accepts;
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        LOGGER.info("[FluidEncoder] Loading model: {}", modelLocation);
        return new FluidEncoderModel();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        LOGGER.info("[FluidEncoder] Model loader reloaded");
    }

    /**
     * Custom model that bakes into our FluidEncoderBakedModel
     */
    private static class FluidEncoderModel implements IModel {

        @Override
        public Collection<ResourceLocation> getDependencies() {
            return ImmutableList.of();
        }

        @Override
        public Collection<ResourceLocation> getTextures() {
            LOGGER.info("[FluidEncoder] getTextures() called - returning: {}, {}", BASE_TEXTURE, MASK_TEXTURE);
            return ImmutableSet.of(BASE_TEXTURE, MASK_TEXTURE);
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format,
                Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
            LOGGER.info("[FluidEncoder] Baking model...");

            // Get the textures
            TextureAtlasSprite baseSprite = bakedTextureGetter.apply(BASE_TEXTURE);
            TextureAtlasSprite maskSprite = bakedTextureGetter.apply(MASK_TEXTURE);

            LOGGER.info("[FluidEncoder] Base sprite: {} ({}x{})",
                    baseSprite.getIconName(), baseSprite.getIconWidth(), baseSprite.getIconHeight());
            LOGGER.info("[FluidEncoder] Mask sprite: {} ({}x{})",
                    maskSprite.getIconName(), maskSprite.getIconWidth(), maskSprite.getIconHeight());

            // Get the default item model to use as base for transforms
            IBakedModel defaultModel;
            try {
                IModel defaultItemModel = ModelLoaderRegistry.getModel(
                        new ResourceLocation("minecraft", "item/generated"));
                defaultModel = defaultItemModel.bake(state, format, bakedTextureGetter);
                LOGGER.info("[FluidEncoder] Default item model loaded successfully");
            } catch (Exception e) {
                LOGGER.error("[FluidEncoder] Failed to load default item model", e);
                throw new RuntimeException("Failed to load default item model", e);
            }

            LOGGER.info("[FluidEncoder] Model baked successfully");
            return new FluidEncoderBakedModel(defaultModel, baseSprite, maskSprite);
        }
    }
}
