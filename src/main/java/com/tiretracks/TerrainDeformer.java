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

public final class TerrainDeformer {

    private TerrainDeformer() {
    }

    public enum VehicleClass {
        LIGHT,
        MEDIUM,
        HEAVY
    }

    public static void deformAt(
            Level level,
            BlockPos pos,
            BlockState state,
            double vehicleMass
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (pos == null || state == null || state.isAir()) {
            return;
        }

        if (!server.isLoaded(pos)) {
            return;
        }

        VehicleClass vehicleClass =
                getVehicleClass(vehicleMass);

        /*
         * A single snow layer has no collision. The wheel raycast therefore
         * usually hits the block below it, so check the block above first.
         */
        BlockPos abovePos = pos.above();
        BlockState aboveState = server.getBlockState(abovePos);

        if (isSnow(aboveState)) {
            crushSnow(server, abovePos, aboveState);
            return;
        }

        if (isSnow(state)) {
            crushSnow(server, pos, state);
            return;
        }

        if (isTurf(state)) {
            churnTurf(
                    server,
                    pos,
                    state,
                    vehicleClass
            );
        }
    }

    public static VehicleClass getVehicleClass(double mass) {
        if (!Double.isFinite(mass) || mass <= 0.0D) {
            return VehicleClass.MEDIUM;
        }

        if (mass < TireTracksConfig.lightVehicleMaxMass()) {
            return VehicleClass.LIGHT;
        }

        if (mass < TireTracksConfig.mediumVehicleMaxMass()) {
            return VehicleClass.MEDIUM;
        }

        return VehicleClass.HEAVY;
    }

    private static boolean isTurf(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK);
    }

    private static void churnTurf(
            ServerLevel level,
            BlockPos pos,
            BlockState originalState,
            VehicleClass vehicleClass
    ) {
        RandomSource random = level.getRandom();

        double chance = getChance(vehicleClass);

        if (random.nextDouble() >= chance) {
            return;
        }

        Block[] possibleBlocks =
                getPossibleTrackBlocks(vehicleClass);

        Block result =
                possibleBlocks[random.nextInt(possibleBlocks.length)];

        level.setBlock(
                pos,
                result.defaultBlockState(),
                Block.UPDATE_ALL
        );

        clearVegetation(
                level,
                pos.above()
        );

        playEffects(
                level,
                pos,
                originalState
        );
    }

    private static double getChance(VehicleClass vehicleClass) {
        return switch (vehicleClass) {
            case LIGHT -> TireTracksConfig.lightChance();
            case MEDIUM -> TireTracksConfig.mediumChance();
            case HEAVY -> TireTracksConfig.heavyChance();
        };
    }

    private static Block[] getPossibleTrackBlocks(
            VehicleClass vehicleClass
    ) {
        return switch (vehicleClass) {
            case LIGHT -> new Block[]{
                    Blocks.DIRT,
                    Blocks.SAND
            };

            case MEDIUM -> new Block[]{
                    Blocks.GRAVEL,
                    Blocks.SAND,
                    Blocks.DIRT_PATH
            };

            case HEAVY -> new Block[]{
                    Blocks.MUD,
                    Blocks.COARSE_DIRT,
                    Blocks.DIRT
            };
        };
    }

    private static void clearVegetation(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(pos);

        if (state.isAir()) {
            return;
        }

        if (state.canBeReplaced()
                && state.getFluidState().getType() == Fluids.EMPTY) {
            level.destroyBlock(pos, false);
        }
    }

    private static boolean isSnow(BlockState state) {
        return state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static void crushSnow(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (!TireTracksConfig.eatSnow()) {
            return;
        }

        RandomSource random =
                level.getRandom();

        /*
         * Full snow and powder snow blocks become
         * a seven-layer snow block first.
         */
        if (state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)) {
            level.setBlock(
                    pos,
                    Blocks.SNOW.defaultBlockState()
                            .setValue(
                                    SnowLayerBlock.LAYERS,
                                    7
                            ),
                    Block.UPDATE_ALL
            );

            playEffects(
                    level,
                    pos,
                    state
            );

            return;
        }

        int layers =
                state.getValue(
                        SnowLayerBlock.LAYERS
                );

        if (layers > 1) {
            level.setBlock(
                    pos,
                    state.setValue(
                            SnowLayerBlock.LAYERS,
                            layers - 1
                    ),
                    Block.UPDATE_ALL
            );

            playEffects(
                    level,
                    pos,
                    state
            );

            return;
        }

        /*
         * Last snow layer disappears.
         */
        level.removeBlock(pos, false);

        playEffects(
                level,
                pos,
                state
        );

        if (random.nextDouble()
                >= TireTracksConfig.snowToMudChance()) {
            return;
        }

        BlockPos belowPos =
                pos.below();

        BlockState belowState =
                level.getBlockState(belowPos);

        if (isMuddyable(belowState)) {
            level.setBlock(
                    belowPos,
                    Blocks.MUD.defaultBlockState(),
                    Block.UPDATE_ALL
            );
        }
    }

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

    private static void playEffects(
            ServerLevel level,
            BlockPos pos,
            BlockState brokenState
    ) {
        if (TireTracksConfig.spawnParticles()) {
            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            brokenState
                    ),
                    pos.getX() + 0.5D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 0.5D,
                    6,
                    0.25D,
                    0.05D,
                    0.25D,
                    0.02D
            );
        }

        if (TireTracksConfig.playSounds()) {
            level.playSound(
                    null,
                    pos,
                    brokenState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS,
                    0.35F,
                    0.8F
                            + level.getRandom().nextFloat()
                            * 0.3F
            );
        }
    }
}
