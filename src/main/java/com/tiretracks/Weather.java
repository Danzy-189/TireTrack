package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;

/**
 * How wet the ground under a wheel is.
 *
 * <p>Wet ground is churned faster and turns into mud, dry ground resists a
 * little and falls apart into sand instead.</p>
 */
public final class Weather {

    /**
     * Biome base temperature below which water freezes in vanilla.
     *
     * <p>Base temperature is used instead of the height aware helpers on
     * purpose: it is stable across Minecraft versions, which matters for a mod
     * that has to survive dependency updates.</p>
     */
    public static final float FREEZING_TEMPERATURE = 0.15F;

    private Weather() {
    }

    public enum Moisture {
        WET,
        NEUTRAL,
        DRY;

        /**
         * Multiplier applied to the deformation chance of the vehicle class.
         */
        public double chanceMultiplier() {
            return switch (this) {
                case WET -> TireTracksConfig.wetChanceMultiplier();
                case DRY -> TireTracksConfig.dryChanceMultiplier();
                case NEUTRAL -> 1.0D;
            };
        }
    }

    public static Moisture moistureAt(ServerLevel level, BlockPos pos) {
        if (level.isRainingAt(pos.above())) {
            return Moisture.WET;
        }

        if (hasAdjacentWater(level, pos)) {
            return Moisture.WET;
        }

        if (baseTemperature(level, pos)
                >= (float) TireTracksConfig.dryBiomeTemperature()) {
            return Moisture.DRY;
        }

        return Moisture.NEUTRAL;
    }

    /**
     * Whether standing water would freeze here. Packed snow and ice roads are
     * only created where they would survive, so warm biomes never end up with
     * melting holes in the ground.
     */
    public static boolean freezes(ServerLevel level, BlockPos pos) {
        return baseTemperature(level, pos) < FREEZING_TEMPERATURE;
    }

    private static float baseTemperature(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).value().getBaseTemperature();
    }

    /**
     * Ground next to water is soaked even under a clear sky.
     */
    private static boolean hasAdjacentWater(ServerLevel level, BlockPos pos) {
        BlockPos abovePos = pos.above();

        if (level.getFluidState(abovePos).is(FluidTags.WATER)) {
            return true;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(abovePos.relative(direction))
                    .is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }
}
