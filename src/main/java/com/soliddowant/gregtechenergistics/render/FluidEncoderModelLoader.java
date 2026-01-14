package com.soliddowant.gregtechenergistics.render;

import java.util.Collection;
import java.util.function.Function;

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
 * Intercepts the model loading and returns a custom model that renders fluid
 * textures.
 */
public class FluidEncoderModelLoader implements ICustomModelLoader {

    public static final FluidEncoderModelLoader INSTANCE = new FluidEncoderModelLoader();

    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation(Tags.MODID,
            "items/metaitems/fluid.encoder");
    private static final ResourceLocation MASK_TEXTURE = new ResourceLocation(Tags.MODID,
            "items/metaitems/fluid.encoder.underlay");

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        // Only handle our specific fluid encoder model
        // GregTech registers models with path "metaitems/..." not "item/metaitems/..."
        return modelLocation.getNamespace().equals(Tags.MODID) &&
                modelLocation.getPath().equals("metaitems/fluid.encoder");
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        return new FluidEncoderModel();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        // No special action needed on resource reload
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
            return ImmutableSet.of(BASE_TEXTURE, MASK_TEXTURE);
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format,
                Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
            // Get the textures
            TextureAtlasSprite baseSprite = bakedTextureGetter.apply(BASE_TEXTURE);
            TextureAtlasSprite maskSprite = bakedTextureGetter.apply(MASK_TEXTURE);

            // Get the default item model to use as base for transforms
            try {
                IModel defaultItemModel = ModelLoaderRegistry
                        .getModel(new ResourceLocation("minecraft", "item/generated"));
                IBakedModel defaultModel = defaultItemModel.bake(state, format, bakedTextureGetter);
                return new FluidEncoderBakedModel(defaultModel, baseSprite, maskSprite);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load default item model for fluid encoder", e);
            }
        }
    }
}
