package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mud and soil picked up by a tyre and dropped again on clean ground.
 *
 * <p>Purely cosmetic: the trail is made of particles only. Smearing real blocks
 * onto somebody's stone road would be griefing rather than flavour.</p>
 */
public final class MaterialCarry {

    private MaterialCarry() {
    }

    public static void onContact(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            WheelState wheelState,
            boolean moved
    ) {
        if (!TireTracksConfig.carryEnabled()) {
            return;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);

        /*
         * Water rinses the tread clean.
         */
        if (Surfaces.isWater(state) || Surfaces.isWater(aboveState)) {
            wheelState.washOff();
            return;
        }

        /*
         * Standing on soft or wet ground reloads the tread instead of dropping
         * anything.
         */
        if (Surfaces.sticksToTyres(state)) {
            wheelState.pickUp(state.getBlock());
            return;
        }

        if (Surfaces.sticksToTyres(aboveState)) {
            wheelState.pickUp(aboveState.getBlock());
            return;
        }

        if (!moved) {
            return;
        }

        Block carried = wheelState.consumeCarried();

        if (carried == null || !TireTracksConfig.spawnParticles()) {
            return;
        }

        spawnTrail(level, pos, carried.defaultBlockState());
    }

    private static void spawnTrail(
            ServerLevel level,
            BlockPos pos,
            BlockState carriedState
    ) {
        int density = Math.max(1, TireTracksConfig.sprayDensity());

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.FALLING_DUST,
                        carriedState
                ),
                x,
                y + 0.15D,
                z,
                density,
                0.28D,
                0.05D,
                0.28D,
                0.01D
        );

        level.sendParticles(
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        carriedState
                ),
                x,
                y,
                z,
                Math.max(1, density / 2),
                0.22D,
                0.02D,
                0.22D,
                0.02D
        );
    }
}
