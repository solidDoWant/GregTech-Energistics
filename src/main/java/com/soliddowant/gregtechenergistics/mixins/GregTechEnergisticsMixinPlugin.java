package com.soliddowant.gregtechenergistics.mixins;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.common.Loader;

/**
 * Mixin config plugin that conditionally loads mixins based on mod presence.
 * This prevents crashes when optional dependencies like JEI are not installed.
 */
public class GregTechEnergisticsMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // No initialization needed
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only gate JEI-related mixins behind JEI presence check
        if (mixinClassName.contains(".jei.")) {
            return Loader.isModLoaded("jei");
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
