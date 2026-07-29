package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * Per wheel bookkeeping: cooldowns, an estimated speed and the material stuck
 * to the tread.
 *
 * <p>Speed is derived from how long the wheel needed to move from one block to
 * the next. That costs nothing, needs no access to the physics engine, and is
 * accurate enough to decide whether spray should be a puff or a cloud.</p>
 */
public final class WheelState {

    private static final long UNSET = Long.MIN_VALUE;

    /**
     * Longer gaps than this mean the wheel was parked, not crawling.
     */
    private static final long MAX_MEASURED_GAP = 60L;

    /**
     * Ticks on the same block after which the wheel counts as standing still.
     */
    private static final long IDLE_TICKS = 10L;

    private static final double SMOOTHING = 0.6D;

    private long lastBlockPos = UNSET;
    private long lastMoveTick = UNSET;
    private double speedBlocksPerTick;

    private int sprayCooldown;
    private int deformCooldown;

    private Block carriedBlock;
    private int carryRemaining;

    /**
     * @return true when the wheel entered a new block since the last contact.
     */
    public boolean updatePosition(BlockPos pos, long gameTime) {
        long packed = pos.asLong();

        if (packed == lastBlockPos) {
            if (lastMoveTick != UNSET
                    && gameTime - lastMoveTick > IDLE_TICKS) {
                speedBlocksPerTick = 0.0D;
            }

            return false;
        }

        if (lastMoveTick != UNSET) {
            long gap = gameTime - lastMoveTick;

            if (gap <= 0L) {
                /*
                 * More than a block within a single tick: physics can run
                 * several substeps per game tick, so clamp instead of dividing
                 * by zero.
                 */
                speedBlocksPerTick = 1.0D;
            } else if (gap <= MAX_MEASURED_GAP) {
                double instant = 1.0D / gap;

                speedBlocksPerTick = speedBlocksPerTick <= 0.0D
                        ? instant
                        : speedBlocksPerTick * SMOOTHING
                        + instant * (1.0D - SMOOTHING);
            } else {
                speedBlocksPerTick = 0.0D;
            }
        }

        lastBlockPos = packed;
        lastMoveTick = gameTime;

        return true;
    }

    public double speedBlocksPerSecond() {
        return speedBlocksPerTick * 20.0D;
    }

    public boolean spraySlotReady() {
        if (--sprayCooldown > 0) {
            return false;
        }

        sprayCooldown = Math.max(1, TireTracksConfig.sprayInterval());

        return true;
    }

    public boolean deformSlotReady() {
        if (--deformCooldown > 0) {
            return false;
        }

        deformCooldown = Math.max(1, TireTracksConfig.tickInterval());

        return true;
    }

    public void pickUp(Block block) {
        carriedBlock = block;
        carryRemaining = Math.max(0, TireTracksConfig.carryDistance());
    }

    public void washOff() {
        carriedBlock = null;
        carryRemaining = 0;
    }

    /**
     * Takes one block worth of material off the tread.
     *
     * @return the carried block, or null when the tread is clean.
     */
    public Block consumeCarried() {
        if (carriedBlock == null || carryRemaining <= 0) {
            carriedBlock = null;
            carryRemaining = 0;
            return null;
        }

        Block carried = carriedBlock;
        carryRemaining--;

        if (carryRemaining <= 0) {
            carriedBlock = null;
        }

        return carried;
    }
}
