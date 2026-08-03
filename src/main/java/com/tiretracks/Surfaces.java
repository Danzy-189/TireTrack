package com.tiretracks;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single place that answers "what kind of surface is this?".
 *
 * <p>Every answer comes from a datapack tag, so both the terrain deformer and
 * the particle code always agree about a block.</p>
 */
public final class Surfaces {

    private Surfaces() {
    }

    /**
     * Blocks that must never be modified. Block entities are always protected:
     * grinding a chest or a machine into mud is never the intent.
     */
    public static boolean isImmune(BlockState state) {
        return state.is(TireTracksTags.IMMUNE)
                || state.hasBlockEntity();
    }

    public static boolean isTurf(BlockState state) {
        return state.is(TireTracksTags.TURF);
    }

    public static boolean isSoft(BlockState state) {
        return state.is(TireTracksTags.SOFT);
    }

    public static boolean isDusty(BlockState state) {
        return state.is(TireTracksTags.DUSTY);
    }

    public static boolean isWet(BlockState state) {
        return state.is(TireTracksTags.WET);
    }

    public static boolean isSnow(BlockState state) {
        return state.is(TireTracksTags.SNOW);
    }

    public static boolean isMuddyable(BlockState state) {
        return state.is(TireTracksTags.MUDDYABLE);
    }

    public static boolean isPackableGround(BlockState state) {
        return state.is(TireTracksTags.PACKABLE_GROUND);
    }

    public static boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    /**
     * Snow that a wheel drives through rather than on top of, so the wheel
     * raycast reports the block underneath it.
     */
    public static boolean isThinSnow(BlockState state) {
        if (!isSnow(state)) {
            return false;
        }

        return state.hasProperty(SnowLayerBlock.LAYERS)
                || state.is(Blocks.POWDER_SNOW);
    }

    /**
     * Material that sticks to a tyre and gets dragged onto clean ground.
     */
    public static boolean sticksToTyres(BlockState state) {
        return isWet(state) || isSoft(state);
    }

    /**
     * The block a particle should be textured with.
     *
     * <p>Block particles take their colour from the block texture, and grass,
     * podzol, moss and friends have a green top face. A wheel tearing through a
     * lawn should throw up soil, not green confetti, so everything in
     * {@code #tiretracks:dirt_particles} sprays plain dirt instead of
     * itself.</p>
     */
    public static BlockState particleStateFor(BlockState state) {
        if (state.is(TireTracksTags.DIRT_PARTICLES)) {
            return Blocks.DIRT.defaultBlockState();
        }

        return state;
    }
}
