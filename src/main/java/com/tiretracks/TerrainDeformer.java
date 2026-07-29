package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Turns repeated wheel contact into a road.
 *
 * <p>Ground wears down one stage at a time and the current stage is stored in
 * the world itself, as the block that is standing there:</p>
 *
 * <pre>
 * turf -> dirt path -> coarse dirt -> loose fill -> puddle
 * </pre>
 *
 * <p>The loose fill depends on the weather: mud when wet, sand in hot dry
 * biomes, gravel otherwise. Only a wet, fully worn rut can become a puddle, and
 * in a freezing biome that puddle is ice.</p>
 *
 * <p>Because the stage is the block, progress survives restarts and chunk
 * unloads without a single byte of extra save data.</p>
 */
public final class TerrainDeformer {

    public static final int STAGE_NONE = -1;
    public static final int STAGE_TURF = 0;
    public static final int STAGE_PATH = 1;
    public static final int STAGE_COARSE = 2;
    public static final int STAGE_LOOSE = 3;
    public static final int STAGE_PUDDLE = 4;

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

        VehicleClass vehicleClass = getVehicleClass(vehicleMass);

        /*
         * Thin snow has no collision, so the wheel raycast usually reports the
         * block below it. Check above first.
         */
        BlockPos abovePos = pos.above();
        BlockState aboveState = server.getBlockState(abovePos);

        if (Surfaces.isThinSnow(aboveState)) {
            packSnow(server, abovePos, aboveState);
            return;
        }

        if (Surfaces.isSnow(state)) {
            packSnow(server, pos, state);
            return;
        }

        churn(server, pos, state, vehicleClass);
    }

    /*
     * Mass is in kilograms, on the same scale Create Aeronautics reports and the
     * sub level debug dump shows.
     * With default config: 0-45 kg light, 46-80 kg medium, above 80 kg heavy.
     * Both bounds are inclusive.
     */
    public static VehicleClass getVehicleClass(double mass) {
        if (!Double.isFinite(mass) || mass <= 0.0D) {
            return VehicleClass.MEDIUM;
        }

        if (mass <= TireTracksConfig.lightVehicleMaxMass()) {
            return VehicleClass.LIGHT;
        }

        if (mass <= TireTracksConfig.mediumVehicleMaxMass()) {
            return VehicleClass.MEDIUM;
        }

        return VehicleClass.HEAVY;
    }

    /**
     * Which stage of wear the ground is currently at.
     */
    public static int stageOf(BlockState state) {
        if (state.is(Blocks.WATER) || state.is(Blocks.ICE)) {
            return STAGE_PUDDLE;
        }

        if (state.is(Blocks.DIRT_PATH)) {
            return STAGE_PATH;
        }

        if (state.is(Blocks.COARSE_DIRT)) {
            return STAGE_COARSE;
        }

        if (state.is(Blocks.MUD)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(BlockTags.SAND)) {
            return STAGE_LOOSE;
        }

        if (Surfaces.isTurf(state)) {
            return STAGE_TURF;
        }

        return STAGE_NONE;
    }

    /**
     * Deepest stage a vehicle class is able to reach.
     */
    public static int maxStageFor(VehicleClass vehicleClass) {
        return switch (vehicleClass) {
            case LIGHT -> TireTracksConfig.lightMaxStage();
            case MEDIUM -> TireTracksConfig.mediumMaxStage();
            case HEAVY -> TireTracksConfig.heavyMaxStage();
        };
    }

    private static void churn(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            VehicleClass vehicleClass
    ) {
        if (Surfaces.isImmune(state)) {
            return;
        }

        int stage = stageOf(state);

        if (stage == STAGE_NONE) {
            return;
        }

        if (stage >= maxStageFor(vehicleClass)) {
            return;
        }

        Weather.Moisture moisture = Weather.moistureAt(level, pos);

        double chance = getChance(vehicleClass) * moisture.chanceMultiplier();

        RandomSource random = level.getRandom();

        if (random.nextDouble() >= chance) {
            return;
        }

        BlockState result = resultFor(
                level,
                pos,
                state,
                stage + 1,
                moisture
        );

        if (result == null) {
            return;
        }

        level.setBlock(pos, result, Block.UPDATE_ALL);

        clearVegetation(level, pos.above());

        spawnBreakParticles(level, pos, state);
    }

    private static double getChance(VehicleClass vehicleClass) {
        return switch (vehicleClass) {
            case LIGHT -> TireTracksConfig.lightChance();
            case MEDIUM -> TireTracksConfig.mediumChance();
            case HEAVY -> TireTracksConfig.heavyChance();
        };
    }

    /**
     * @return the block for the next stage, or null when this step is not
     *         possible right now.
     */
    private static BlockState resultFor(
            ServerLevel level,
            BlockPos pos,
            BlockState current,
            int targetStage,
            Weather.Moisture moisture
    ) {
        switch (targetStage) {
            case STAGE_PATH:
                return Blocks.DIRT_PATH.defaultBlockState();

            case STAGE_COARSE:
                return Blocks.COARSE_DIRT.defaultBlockState();

            case STAGE_LOOSE: {
                Block loose = switch (moisture) {
                    case WET -> Blocks.MUD;
                    case DRY -> Blocks.SAND;
                    case NEUTRAL -> Blocks.GRAVEL;
                };

                /*
                 * Sand and gravel fall. Placing them over a gap would punch a
                 * hole through the terrain instead of leaving a rut.
                 */
                if ((loose == Blocks.SAND || loose == Blocks.GRAVEL)
                        && !hasSolidFloor(level, pos)) {
                    return null;
                }

                return loose.defaultBlockState();
            }

            case STAGE_PUDDLE: {
                if (!TireTracksConfig.puddles()) {
                    return null;
                }

                if (!current.is(Blocks.MUD)) {
                    return null;
                }

                if (moisture != Weather.Moisture.WET) {
                    return null;
                }

                if (!canHoldPuddle(level, pos)) {
                    return null;
                }

                return Weather.freezes(level, pos)
                        ? Blocks.ICE.defaultBlockState()
                        : Blocks.WATER.defaultBlockState();
            }

            default:
                return null;
        }
    }

    private static boolean hasSolidFloor(ServerLevel level, BlockPos pos) {
        BlockPos belowPos = pos.below();

        return level.getBlockState(belowPos)
                .isFaceSturdy(level, belowPos, Direction.UP);
    }

    /**
     * A puddle is only allowed in a rut that is walled in on all four sides and
     * has a solid floor, so water can never run off across the landscape.
     */
    private static boolean canHoldPuddle(ServerLevel level, BlockPos pos) {
        if (!hasSolidFloor(level, pos)) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbourPos = pos.relative(direction);

            BlockState neighbourState = level.getBlockState(neighbourPos);

            if (!neighbourState.isFaceSturdy(
                    level,
                    neighbourPos,
                    direction.getOpposite()
            )) {
                return false;
            }
        }

        return true;
    }

    private static void clearVegetation(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return;
        }

        if (state.canBeReplaced()
                && state.getFluidState().getType() == Fluids.EMPTY) {
            level.destroyBlock(pos, false);
        }
    }

    /**
     * Snow is compressed rather than deleted: layers are shaved off, the last
     * one is driven into the ground as ice, and further passes polish that into
     * packed ice.
     *
     * <p>Vanilla has no packed snow block, and a snow block cannot serve as the
     * packed stage either: it is already the entry point of this chain, so a
     * groomed track would collapse back into loose layers forever. Ice is flush
     * with the surface, never turns back into snow, and is genuinely faster to
     * drive on.</p>
     *
     * <p>All of it only happens in biomes cold enough to keep the result, so no
     * melting holes appear in a temperate lawn.</p>
     */
    private static void packSnow(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (!TireTracksConfig.eatSnow()) {
            return;
        }

        if (Surfaces.isImmune(state)) {
            return;
        }

        RandomSource random = level.getRandom();

        /*
         * A groomed track, or a frozen puddle, is polished one step further.
         * Packed ice is terminal and never melts.
         */
        if (state.is(Blocks.ICE)) {
            if (!TireTracksConfig.packSnow()
                    || !Weather.freezes(level, pos)) {
                return;
            }

            if (random.nextDouble()
                    >= TireTracksConfig.snowToIceChance()) {
                return;
            }

            level.setBlock(
                    pos,
                    Blocks.PACKED_ICE.defaultBlockState(),
                    Block.UPDATE_ALL
            );

            spawnBreakParticles(level, pos, state);

            return;
        }

        /*
         * Full snow and powder snow blocks collapse into a seven layer stack
         * first.
         */
        if (state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)) {
            level.setBlock(
                    pos,
                    Blocks.SNOW.defaultBlockState()
                            .setValue(SnowLayerBlock.LAYERS, 7),
                    Block.UPDATE_ALL
            );

            spawnBreakParticles(level, pos, state);

            return;
        }

        if (!state.hasProperty(SnowLayerBlock.LAYERS)) {
            return;
        }

        int layers = state.getValue(SnowLayerBlock.LAYERS);

        if (layers > 1) {
            level.setBlock(
                    pos,
                    state.setValue(SnowLayerBlock.LAYERS, layers - 1),
                    Block.UPDATE_ALL
            );

            spawnBreakParticles(level, pos, state);

            return;
        }

        level.removeBlock(pos, false);

        spawnBreakParticles(level, pos, state);

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        if (Surfaces.isImmune(belowState)) {
            return;
        }

        /*
         * Driven into the ground, the last layer leaves ice flush with the
         * surface: a groomed track without a bump for the suspension.
         */
        if (TireTracksConfig.packSnow()
                && Weather.freezes(level, pos)
                && Surfaces.isPackableGround(belowState)
                && belowState.isFaceSturdy(level, belowPos, Direction.UP)) {
            level.setBlock(
                    belowPos,
                    Blocks.ICE.defaultBlockState(),
                    Block.UPDATE_ALL
            );

            return;
        }

        if (random.nextDouble()
                >= TireTracksConfig.snowToMudChance()) {
            return;
        }

        if (Surfaces.isMuddyable(belowState)) {
            level.setBlock(
                    belowPos,
                    Blocks.MUD.defaultBlockState(),
                    Block.UPDATE_ALL
            );
        }
    }

    /**
     * A silent puff of the old block. Deformation deliberately makes no sound:
     * a block break noise under every wheel, several times a second, is noise
     * rather than feedback.
     */
    private static void spawnBreakParticles(
            ServerLevel level,
            BlockPos pos,
            BlockState brokenState
    ) {
        if (!TireTracksConfig.spawnParticles()) {
            return;
        }

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
}
