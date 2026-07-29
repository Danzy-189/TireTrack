package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spray thrown up by a rolling wheel.
 *
 * <p>Surface classification comes from the datapack tags in
 * {@link TireTracksTags}, so the particles always match what the terrain
 * deformer thinks the ground is. Amount scales with how fast the wheel is
 * actually travelling: a crawl produces a puff, full speed produces a
 * cloud.</p>
 *
 * <p>Everything here is cosmetic with one exception: dusty ground at speed also
 * raises a {@link DustVeil}, which does affect gameplay.</p>
 */
public final class WheelSpray {

    /**
     * Density floor and ceiling relative to the configured base density.
     */
    private static final double MIN_DENSITY_FACTOR = 0.35D;
    private static final double MAX_DENSITY_FACTOR = 1.6D;

    private WheelSpray() {
    }

    public static void sprayAt(
            Level level,
            BlockPos contactPos,
            BlockState contactState,
            double speedBlocksPerSecond
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (contactPos == null || contactState == null) {
            return;
        }

        if (!server.isLoaded(contactPos)) {
            return;
        }

        boolean particles = TireTracksConfig.spawnParticles()
                && TireTracksConfig.wheelSpray();

        BlockPos abovePos = contactPos.above();
        BlockState aboveState = server.getBlockState(abovePos);

        int density = scaledDensity(speedBlocksPerSecond);

        boolean handled = true;

        /*
         * The block above the contact point is checked first: a wheel driving
         * through shallow water or thin snow reports the solid block below it.
         */
        if (Surfaces.isWater(aboveState)) {
            if (particles) {
                spawnWater(server, abovePos, density);
            }
        } else if (Surfaces.isWater(contactState)) {
            if (particles) {
                spawnWater(server, contactPos, density);
            }
        } else if (Surfaces.isSnow(aboveState)) {
            if (particles) {
                spawnSnow(server, abovePos, density);
            }
        } else if (Surfaces.isSnow(contactState)) {
            if (particles) {
                spawnSnow(server, contactPos, density);
            }
        } else if (Surfaces.isDusty(contactState)) {
            if (particles) {
                spawnDust(server, contactPos, contactState, density);
            }

            /*
             * Gated by its own config option rather than by wheelSpray, since a
             * blinding cloud is a gameplay effect and not decoration.
             */
            DustVeil.trigger(
                    server,
                    contactPos,
                    contactState,
                    speedBlocksPerSecond
            );
        } else if (Surfaces.isSoft(contactState)) {
            if (particles) {
                spawnClods(server, contactPos, contactState, density);
            }
        } else if (!contactState.isAir()
                && contactState.getFluidState().isEmpty()) {
            /*
             * Stone, wood, concrete and so on: just a faint scuff.
             */
            if (particles) {
                spawnDust(
                        server,
                        contactPos,
                        contactState,
                        Math.max(1, density / 2)
                );
            }
        } else {
            handled = false;
        }

        /*
         * Rain adds water spray on top of whatever the surface throws up.
         */
        if (particles && server.isRainingAt(abovePos)) {
            spawnRainSpray(
                    server,
                    contactPos,
                    handled
                            ? Math.max(1, density / 2)
                            : density
            );
        }
    }

    /**
     * Base density scaled by wheel speed, clamped so slow driving still shows
     * something and top speed cannot flood the client with particles.
     */
    private static int scaledDensity(double speedBlocksPerSecond) {
        int density = Math.max(1, TireTracksConfig.sprayDensity());

        double fullSpeed = Math.max(
                0.1D,
                TireTracksConfig.sprayFullSpeed()
        );

        double factor = Mth.clamp(
                speedBlocksPerSecond / fullSpeed,
                MIN_DENSITY_FACTOR,
                MAX_DENSITY_FACTOR
        );

        return Math.max(1, (int) Math.round(density * factor));
    }

    /*
     * Water: splash arcs plus a few bubbles from the wheel cutting the surface.
     */
    private static void spawnWater(
            ServerLevel level,
            BlockPos pos,
            int density
    ) {
        level.sendParticles(
                ParticleTypes.SPLASH,
                pos.getX() + 0.5D,
                pos.getY() + 0.9D,
                pos.getZ() + 0.5D,
                density * 3,
                0.3D,
                0.1D,
                0.3D,
                0.12D
        );

        level.sendParticles(
                ParticleTypes.BUBBLE,
                pos.getX() + 0.5D,
                pos.getY() + 0.6D,
                pos.getZ() + 0.5D,
                density,
                0.25D,
                0.05D,
                0.25D,
                0.02D
        );
    }

    /*
     * Snow: a puff of snowflakes plus torn up snow bits.
     */
    private static void spawnSnow(
            ServerLevel level,
            BlockPos pos,
            int density
    ) {
        level.sendParticles(
                ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.5D,
                pos.getY() + 0.4D,
                pos.getZ() + 0.5D,
                density * 3,
                0.3D,
                0.15D,
                0.3D,
                0.05D
        );

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        Blocks.SNOW_BLOCK.defaultBlockState()
                ),
                pos.getX() + 0.5D,
                pos.getY() + 0.3D,
                pos.getZ() + 0.5D,
                density,
                0.25D,
                0.05D,
                0.25D,
                0.04D
        );
    }

    /*
     * Sand, gravel, dirt paths: a rising dust plume tinted with the block.
     */
    private static void spawnDust(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            int density
    ) {
        level.sendParticles(
                ParticleTypes.DUST_PLUME,
                pos.getX() + 0.5D,
                pos.getY() + 1.05D,
                pos.getZ() + 0.5D,
                density * 2,
                0.35D,
                0.05D,
                0.35D,
                0.02D
        );

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        state
                ),
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                density,
                0.3D,
                0.05D,
                0.3D,
                0.03D
        );
    }

    /*
     * Turf, dirt, mud: chunks kicked backwards, wet surfaces also splash.
     */
    private static void spawnClods(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            int density
    ) {
        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        state
                ),
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                density * 2,
                0.3D,
                0.1D,
                0.3D,
                0.06D
        );

        if (Surfaces.isWet(state)) {
            level.sendParticles(
                    ParticleTypes.SPLASH,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 0.5D,
                    density,
                    0.25D,
                    0.05D,
                    0.25D,
                    0.05D
            );

            return;
        }

        level.sendParticles(
                ParticleTypes.DUST_PLUME,
                pos.getX() + 0.5D,
                pos.getY() + 1.05D,
                pos.getZ() + 0.5D,
                Math.max(1, density / 2),
                0.25D,
                0.05D,
                0.25D,
                0.02D
        );
    }

    /*
     * Rain: droplets flicked off the tread, even on solid ground.
     */
    private static void spawnRainSpray(
            ServerLevel level,
            BlockPos pos,
            int density
    ) {
        level.sendParticles(
                ParticleTypes.SPLASH,
                pos.getX() + 0.5D,
                pos.getY() + 1.1D,
                pos.getZ() + 0.5D,
                density * 2,
                0.35D,
                0.1D,
                0.35D,
                0.1D
        );

        level.sendParticles(
                ParticleTypes.RAIN,
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                density,
                0.3D,
                0.05D,
                0.3D,
                0.05D
        );
    }
}
