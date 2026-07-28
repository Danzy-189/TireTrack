package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * All the "wheels ruin the ground" logic. Fed by the wheel mount mixin with the exact block
 * the suspension raycast is standing on.
 */
public final class TerrainDeformer {

    private TerrainDeformer() {}

    /** What turf can turn into. Equal weights: sand, dirt, mud, gravel, coarse dirt. */
    private static final Block[] CHURNED = {
            Blocks.SAND,
            Blocks.DIRT,
            Blocks.MUD,
            Blocks.GRAVEL,
            Blocks.COARSE_DIRT
    };

    /**
     * @param level the level the wheel raycast hit in
     * @param pos   the block the wheel is resting on
     */
    public static void deformAt(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server)) return;
        if (pos == null || state == null || state.isAir()) return;
        if (!server.isLoaded(pos)) return;

        // A single snow layer has no collision, so the suspension raycast reports the block
        // UNDER it. Check upwards first so thin snow still gets eaten.
        BlockPos above = pos.above();
        BlockState aboveState = server.getBlockState(above);
        if (isSnow(aboveState)) {
            crushSnow(server, above, aboveState);
            return;
        }

        if (isSnow(state)) {
            crushSnow(server, pos, state);
            return;
        }

        if (isTurf(state)) {
            churnTurf(server, pos, state);
        }
    }

    // ------------------------------------------------------------------ turf

    /** Only actual turf. Mycelium / podzol are deliberately left alone. */
    private static boolean isTurf(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK);
    }

    private static void churnTurf(ServerLevel level, BlockPos pos, BlockState state) {
        RandomSource rand = level.getRandom();
        if (rand.nextDouble() >= TireTracksConfig.turfChance()) return;

        Block result = CHURNED[rand.nextInt(CHURNED.length)];
        level.setBlock(pos, result.defaultBlockState(), Block.UPDATE_ALL);
        clearVegetation(level, pos.above());
        effects(level, pos, state);
    }

    /** Grass / flowers sitting on the turf get shredded with it. */
    private static void clearVegetation(ServerLevel level, BlockPos above) {
        BlockState s = level.getBlockState(above);
        if (s.isAir()) return;
        if (s.canBeReplaced() && s.getFluidState().getType() == Fluids.EMPTY) {
            level.destroyBlock(above, false);
        }
    }

    // ------------------------------------------------------------------ snow

    private static boolean isSnow(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
    }

    /**
     * Driving over snow shaves off one layer per pass. When the last layer goes, the ground
     * underneath may be churned into mud.
     */
    private static void crushSnow(ServerLevel level, BlockPos pos, BlockState state) {
        if (!TireTracksConfig.eatSnow()) return;
        RandomSource rand = level.getRandom();

        // Full snow / powder snow blocks collapse into a tall layer stack first.
        if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), Block.UPDATE_ALL);
            effects(level, pos, state);
            return;
        }

        int layers = state.getValue(SnowLayerBlock.LAYERS);
        if (layers > 1) {
            level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1), Block.UPDATE_ALL);
            effects(level, pos, state);
            return;
        }

        // Last layer: remove it, and sometimes leave churned mud behind.
        level.removeBlock(pos, false);
        effects(level, pos, state);

        if (rand.nextDouble() < TireTracksConfig.snowToMudChance()) {
            BlockPos below = pos.below();
            if (isMuddyable(level.getBlockState(below))) {
                level.setBlock(below, Blocks.MUD.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Ground that can be trampled into mud once the snow is gone. */
    private static boolean isMuddyable(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.CLAY);
    }

    // --------------------------------------------------------------- effects

    private static void effects(ServerLevel level, BlockPos pos, BlockState broken) {
        if (TireTracksConfig.spawnParticles()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, broken),
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    6, 0.25D, 0.05D, 0.25D, 0.02D);
        }
        if (TireTracksConfig.playSounds()) {
            level.playSound(null, pos, broken.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.35F,
                    0.8F + level.getRandom().nextFloat() * 0.3F);
        }
    }
}
