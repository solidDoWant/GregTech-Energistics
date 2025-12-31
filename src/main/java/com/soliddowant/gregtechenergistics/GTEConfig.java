package com.soliddowant.gregtechenergistics;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Tags.MODID)
public class GTEConfig {

    @Config.Comment("Settings for the AE2 Stocker Cover")
    @Config.Name("Stocker Cover")
    public static final StockerCover stockerCover = new StockerCover();

    public static class StockerCover {
        @Config.Comment("How often the stocker cover updates, in ticks. Lower values = faster updates (lower latency, slightly higher throughput) but more server load. Default: 5")
        @Config.Name("Update Interval (ticks)")
        @Config.RangeInt(min = 1, max = 100)
        public int updateIntervalTicks = 5;
    }

    @Mod.EventBusSubscriber(modid = Tags.MODID)
    public static class ConfigSyncHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (!event.getModID().equals(Tags.MODID))
                return;
            ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
        }
    }
}
