package com.tiretracks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Block tags used to classify surfaces.
 *
 * <p>Nothing about which block behaves how is hardcoded any more: every list
 * lives in {@code data/tiretracks/tags/block/} and can be extended or replaced
 * by any datapack or modpack without touching the code.</p>
 */
public final class TireTracksTags {

    private TireTracksTags() {
    }

    /**
     * Blocks this mod must never touch. Free blacklist for modpacks.
     */
    public static final TagKey<Block> IMMUNE = block("immune");

    /**
     * Entry point of the rut chain: pristine ground that can still be churned.
     */
    public static final TagKey<Block> TURF = block("turf");

    /**
     * Loose soil that throws up flying clods.
     */
    public static final TagKey<Block> SOFT = block("soft");

    /**
     * Dry granular ground that raises a dust plume, and the only ground that
     * can raise a dust veil at speed.
     */
    public static final TagKey<Block> DUSTY = block("dusty");

    /**
     * Wet ground: splashes instead of dust, and material that sticks to tyres.
     */
    public static final TagKey<Block> WET = block("wet");

    /**
     * Anything treated as a snow surface, including the ice a groomed track
     * turns into.
     */
    public static final TagKey<Block> SNOW = block("snow");

    /**
     * Ground that the last snow layer may turn into mud.
     */
    public static final TagKey<Block> MUDDYABLE = block("muddyable");

    /**
     * Ground that snow may be packed into, turning it into an ice track.
     */
    public static final TagKey<Block> PACKABLE_GROUND = block("packable_ground");

    private static TagKey<Block> block(String path) {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(
                        TireTracks.MODID,
                        path
                )
        );
    }
}
