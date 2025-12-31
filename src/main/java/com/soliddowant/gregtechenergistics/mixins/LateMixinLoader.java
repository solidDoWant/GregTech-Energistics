package com.soliddowant.gregtechenergistics.mixins;

import java.util.Collections;
import java.util.List;

import zone.rong.mixinbooter.ILateMixinLoader;

/**
 * Registers our mixin configuration with MixinBooter for late loading.
 * This is required because we're targeting GregTech's classes, which are mod
 * classes
 * rather than Minecraft/Forge classes.
 */
public class LateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.gregtechenergistics.json");
    }
}
