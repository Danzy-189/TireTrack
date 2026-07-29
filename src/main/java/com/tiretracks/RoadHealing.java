package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles road healing: puddles evaporate in dry weather, mud heals back to
 * turf during rain if untouched for long enough.
 *
 * <p>Touch time tracking is in-memory only and clears when a chunk unloads, so
 * healing timers reset on chunk reload. This is acceptable: the mechanic is
 * about ongoing activity, not forensic history.</p>
 *
 * <p>Time is measured in game ticks, not real-world time. 1 game day = 24000
 * ticks = 20 minutes real time. This makes healing observable within a play
 * session and feels Minecraft-native.</p>
 *
 * <p>Healing checks pick random positions near players instead of iterating all
 * loaded chunks. This avoids internal API access, scales with player count
 * instead of world size, and naturally focuses healing where it matters.</p>
 */
@EventBusSubscriber(modid = TireTracks.MODID)
public final class RoadHealing {

    /**
     * Checks per player per second. Each check picks a random position within
     * 128 blocks of the player.
     */
    private static final int CHECKS_PER_PLAYER_PER_SECOND = 20;

    /**
     * Horizontal radius around each player to check for healing.
     */
    private static final int CHECK_RADIUS = 128;

    /**
     * Ticks per Minecraft day.
     */
    private static final long TICKS_PER_DAY = 24000L;

    /**
     * Last touch time (in game ticks, from level.getDayTime()) for deformed
     * blocks. Keyed by chunk, then by local block position.
     */
    private static final Map<ChunkPos, Map<BlockPos, Long>> LAST_TOUCH =
            new ConcurrentHashMap<>();

    private RoadHealing() {
    }

    /**
     * Marks a block as freshly touched by a wheel. Resets its healing timer.
     */
    public static void touch(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        LAST_TOUCH
                .computeIfAbsent(chunkPos, k -> new ConcurrentHashMap<>())
                .put(pos, level.getDayTime());
    }

    /**
     * How long ago the block was last touched, in game ticks. Returns
     * Long.MAX_VALUE if never touched (allowing it to heal immediately).
     */
    private static long ticksSinceTouch(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        Map<BlockPos, Long> chunkData = LAST_TOUCH.get(chunkPos);

        if (chunkData == null) {
            return Long.MAX_VALUE;
        }

        Long lastTouch = chunkData.get(pos);

        if (lastTouch == null) {
            return Long.MAX_VALUE;
        }

        return level.getDayTime() - lastTouch;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickLevel(level);
        }
    }

    private static void tickLevel(ServerLevel level) {
        List<ServerPlayer> players = level.players();

        if (players.isEmpty()) {
            return;
        }

        RandomSource random = level.getRandom();

        /*
         * Check a few random positions near each player. This naturally focuses
         * healing where players are active, scales with player count instead of
         * world size, and avoids iterating all loaded chunks.
         */
        double checksPerPlayerThisTick = CHECKS_PER_PLAYER_PER_SECOND / 20.0D;

        for (ServerPlayer player : players) {
            if (random.nextDouble() >= checksPerPlayerThisTick) {
                continue;
            }

            checkRandomPosNear(level, player.blockPosition(), random);
        }
    }

    private static void checkRandomPosNear(
            ServerLevel level,
            BlockPos center,
            RandomSource random
    ) {
        /*
         * Pick a random position within CHECK_RADIUS blocks of center.
         */
        int dx = random.nextInt(CHECK_RADIUS * 2 + 1) - CHECK_RADIUS;
        int dz = random.nextInt(CHECK_RADIUS * 2 + 1) - CHECK_RADIUS;

        BlockPos columnBase = center.offset(dx, 0, dz);

        if (!level.hasChunkAt(columnBase)) {
            return;
        }

        /*
         * Scan the column from bottom to top, checking each block.
         */
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int y = minY; y < maxY; y++) {
            BlockPos pos = new BlockPos(columnBase.getX(), y, columnBase.getZ());

            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            tryHeal(level, pos, state, random);
        }
    }

    private static void tryHeal(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        if (state.is(Blocks.WATER) || state.is(Blocks.ICE)) {
            tryEvaporatePuddle(level, pos, state, random);

            return;
        }

        if (state.is(Blocks.MUD)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.DIRT_PATH)) {
            tryHealMud(level, pos, state, random);
        }
    }

    /**
     * Puddles evaporate back into mud in dry weather.
     */
    private static void tryEvaporatePuddle(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        if (!TireTracksConfig.puddlesEvaporate()) {
            return;
        }

        /*
         * Puddles only evaporate when it is not raining on them.
         */
        if (level.isRainingAt(pos.above())) {
            touch(level, pos);

            return;
        }

        long sinceTouch = ticksSinceTouch(level, pos);

        double evaporationDays = TireTracksConfig.puddleEvaporationDays();

        /*
         * Hot biomes evaporate faster.
         */
        float biomeTemperature = level.getBiome(pos)
                .value()
                .getBaseTemperature();

        if (biomeTemperature >= TireTracksConfig.dryBiomeTemperature()) {
            evaporationDays /= TireTracksConfig.hotBiomeEvaporationMultiplier();
        }

        long evaporationTicks = (long) (evaporationDays * TICKS_PER_DAY);

        if (sinceTouch < evaporationTicks) {
            return;
        }

        /*
         * Evaporate: puddle becomes mud.
         */
        level.setBlock(pos, Blocks.MUD.defaultBlockState(), Block.UPDATE_ALL);

        forget(pos);
    }

    /**
     * Mud, coarse dirt and dirt paths heal back toward grass during rain if
     * untouched long enough.
     */
    private static void tryHealMud(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        if (!TireTracksConfig.mudHeals()) {
            return;
        }

        /*
         * Healing only happens during rain.
         */
        if (!level.isRainingAt(pos.above())) {
            return;
        }

        long sinceTouch = ticksSinceTouch(level, pos);

        double healDays = TireTracksConfig.mudHealDays();

        long healTicks = (long) (healDays * TICKS_PER_DAY);

        if (sinceTouch < healTicks) {
            return;
        }

        double chance = TireTracksConfig.mudHealChance();

        if (random.nextDouble() >= chance) {
            return;
        }

        /*
         * Heal one stage back toward turf. Mud and coarse dirt turn into
         * grass if there is grass nearby (within 3 blocks), otherwise into
         * plain dirt. Dirt paths also turn into grass if nearby.
         */
        BlockState healed = null;

        if (state.is(Blocks.MUD)) {
            healed = hasNearbyGrass(level, pos)
                    ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : Blocks.DIRT.defaultBlockState();
        } else if (state.is(Blocks.COARSE_DIRT)) {
            healed = hasNearbyGrass(level, pos)
                    ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : Blocks.DIRT.defaultBlockState();
        } else if (state.is(Blocks.DIRT_PATH)) {
            healed = hasNearbyGrass(level, pos)
                    ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : Blocks.DIRT.defaultBlockState();
        }

        if (healed == null) {
            return;
        }

        level.setBlock(pos, healed, Block.UPDATE_ALL);

        forget(pos);
    }

    /**
     * Whether there is grass within 3 blocks horizontally. Used to decide
     * whether healed dirt should become grass or stay plain.
     */
    private static boolean hasNearbyGrass(ServerLevel level, BlockPos center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                BlockPos neighbourPos = center.offset(dx, 0, dz);

                BlockState neighbourState = level.getBlockState(neighbourPos);

                if (neighbourState.is(Blocks.GRASS_BLOCK)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Removes a position from tracking after it heals, so the map does not
     * grow unbounded.
     */
    private static void forget(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        Map<BlockPos, Long> chunkData = LAST_TOUCH.get(chunkPos);

        if (chunkData != null) {
            chunkData.remove(pos);

            if (chunkData.isEmpty()) {
                LAST_TOUCH.remove(chunkPos);
            }
        }
    }
}
