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
 * cloud. Every count then goes through {@link Particles#count(int)}, so the
 * whole mod thickens or thins with one config value.</p>
 *
 * <p>Everything here is purely cosmetic. Nothing in this class applies an
 * effect to any entity.</p>
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

        if (!TireTracksConfig.spawnParticles()
                || !TireTracksConfig.wheelSpray()) {
            return;
        }

        BlockPos abovePos = contactPos.above();
        BlockState aboveState = server.getBlockState(abovePos);

        int density = scaledDensity(speedBlocksPerSecond);

        boolean handled = true;

        /*
         * The block above the contact point is checked first: a wheel driving
         * through shallow water or thin snow reports the solid block below it.
         */
        if (Surfaces.isWater(aboveState)) {
            spawnWater(server, abovePos, density);
        } else if (Surfaces.isWater(contactState)) {
            spawnWater(server, contactPos, density);
        } else if (Surfaces.isSnow(aboveState)) {
            spawnSnow(server, abovePos, density);
        } else if (Surfaces.isSnow(contactState)) {
            spawnSnow(server, contactPos, density);
        } else if (Surfaces.isDusty(contactState)) {
            spawnDust(server, contactPos, contactState, density);
        } else if (Surfaces.isSoft(contactState)) {
            spawnClods(server, contactPos, contactState, density);
        } else if (!contactState.isAir()
                && contactState.getFluidState().isEmpty()) {
            /*
             * Stone, wood, concrete and so on: just a faint scuff.
             */
            spawnDust(
                    server,
                    contactPos,
                    contactState,
                    Math.max(1, density / 2)
            );
        } else {
            handled = false;
        }

        /*
         * Rain adds water spray on top of whatever the surface throws up.
         */
        if (server.isRainingAt(abovePos)) {
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
                Particles.count(density * 3),
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
                Particles.count(density),
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
                Particles.count(density * 3),
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
                Particles.count(density),
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
                Particles.count(density * 2),
                0.35D,
                0.05D,
                0.35D,
                0.02D
        );

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        Surfaces.particleStateFor(state)
                ),
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                Particles.count(density),
                0.3D,
                0.05D,
                0.3D,
                0.03D
        );
    }

    /*
     * Turf, dirt, mud: chunks kicked backwards, wet surfaces also splash.
     *
     * Grass, podzol and moss spray plain dirt instead of themselves: block
     * particles take their colour from the block texture, and green flecks
     * flying out from under a tyre looked like confetti rather than soil.
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
                        Surfaces.particleStateFor(state)
                ),
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                Particles.count(density * 2),
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
                    Particles.count(density),
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
                Particles.fraction(density, 0.5D),
                0.25D,
                0.05D,
                0.25D,
                0.02D
        );
    }

    /*
     * Rain: droplets flicked off the tread, even on solid ground, plus bubbles
     * popping in the film of water the wheel is rolling through.
     *
     * BUBBLE_POP is used rather than BUBBLE on purpose: the plain bubble
     * particle deletes itself the moment it finds it is not inside water, so on
     * a wet road it would never be visible at all.
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
                Particles.count(density * 2),
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
                Particles.count(density),
                0.3D,
                0.05D,
                0.3D,
                0.05D
        );

        level.sendParticles(
                ParticleTypes.BUBBLE_POP,
                pos.getX() + 0.5D,
                pos.getY() + 1.05D,
                pos.getZ() + 0.5D,
                Particles.count(density * 2),
                0.3D,
                0.08D,
                0.3D,
                0.03D
        );
    }
}
