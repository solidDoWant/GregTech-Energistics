package com.soliddowant.gregtechenergistics.mixins;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.common.Loader;

/**
 * Mixin config plugin that conditionally loads mixins based on mod presence.
 * This prevents crashes when optional dependencies like JEI are not installed.
 * 
 * <p>
 * This plugin assumes that JEI-specific mixins are placed in a ".jei"
 * subpackage
 * under the main mixin package. For example, if the mixin package is
 * "com.soliddowant.gregtechenergistics.mixins", then JEI mixins should be in
 * "com.soliddowant.gregtechenergistics.mixins.jei".
 * </p>
 */
public class GregTechEnergisticsMixinPlugin implements IMixinConfigPlugin {

    private static final String JEI_PACKAGE_SUFFIX = ".jei.";
    private static final String JEI_MOD_ID = "jei";

    private String mixinPackage;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only gate JEI-related mixins behind JEI presence check
        // Check if the mixin is in the jei subpackage
        String jeiMixinPackage = mixinPackage + JEI_PACKAGE_SUFFIX;
        if (mixinClassName.startsWith(jeiMixinPackage)) {
            return Loader.isModLoaded(JEI_MOD_ID);
        }

        // Apply all other mixins unconditionally
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No special target handling needed
    }

    @Override
    public List<String> getMixins() {
        // Return null to let the config file handle mixin listing
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No pre-apply processing needed
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No post-apply processing needed
    }
}