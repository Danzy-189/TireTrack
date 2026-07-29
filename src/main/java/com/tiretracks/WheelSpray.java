package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cosmetic particles thrown up by a rolling wheel.
 * Purely visual: nothing here changes blocks or physics.
 */
public final class WheelSpray {

    private WheelSpray() {
    }

    public static void sprayAt(
            Level level,
            BlockPos contactPos,
            BlockState contactState
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (!TireTracksConfig.spawnParticles()
                || !TireTracksConfig.wheelSpray()) {
            return;
        }

        if (contactPos == null || contactState == null) {
            return;
        }

        if (!server.isLoaded(contactPos)) {
            return;
        }

        BlockPos abovePos = contactPos.above();
        BlockState aboveState = server.getBlockState(abovePos);

        int density = Math.max(1, TireTracksConfig.sprayDensity());

        boolean handled = true;

        /*
         * The block above the contact point is checked first: a wheel driving
         * through shallow water or thin snow reports the solid block below it.
         */
        if (isWater(aboveState)) {
            spawnWater(server, abovePos, density);
        } else if (isWater(contactState)) {
            spawnWater(server, contactPos, density);
        } else if (isSnow(aboveState)) {
            spawnSnow(server, abovePos, density);
        } else if (isSnow(contactState)) {
            spawnSnow(server, contactPos, density);
        } else if (isDusty(contactState)) {
            spawnDust(server, contactPos, contactState, density);
        } else if (isSoft(contactState)) {
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

    private static boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    private static boolean isSnow(BlockState state) {
        return state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean isDusty(BlockState state) {
        return state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL);
    }

    private static boolean isSoft(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.FARMLAND);
    }

    private static boolean isWet(BlockState state) {
        return state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.FARMLAND);
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
     * Snow: a puff of snowflakes plus torn-up snow bits.
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

        if (isWet(state)) {
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
