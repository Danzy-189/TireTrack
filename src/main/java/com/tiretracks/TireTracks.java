package com.tiretracks;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(TireTracks.MODID)
public class TireTracks {

    public static final String MODID = "tiretracks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TireTracks(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, TireTracksConfig.SPEC);
        LOGGER.info("[TireTracks 3.1.0] loaded - wheels will now ruin your lawn, then pave it.");
    }
}
