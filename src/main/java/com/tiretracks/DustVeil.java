package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Thick dust kicked up by fast driving on dry ground.
 *
 * <p>The cloud is not only cosmetic: anything caught inside it is briefly
 * blinded, which turns a chase into a question of distance and racing line.</p>
 *
 * <p>Own crew protection is a distance heuristic. Nothing in the physics API
 * tells us who is riding which vehicle, so entities closer than
 * {@code dustVeilSelfRadius} to the wheel are treated as being on board and are
 * spared, while anything further out inside {@code dustVeilRadius} is
 * considered a follower. Both values are configurable for oversized
 * builds.</p>
 */
public final class DustVeil {

    private DustVeil() {
    }

    public static void trigger(
            ServerLevel level,
            BlockPos pos,
            BlockState surface,
            double speedBlocksPerSecond
    ) {
        if (!TireTracksConfig.dustVeil()) {
            return;
        }

        if (speedBlocksPerSecond < TireTracksConfig.dustVeilMinSpeed()) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.2D;
        double z = pos.getZ() + 0.5D;

        if (TireTracksConfig.spawnParticles()) {
            int density = Math.max(1, TireTracksConfig.sprayDensity());

            level.sendParticles(
                    ParticleTypes.DUST_PLUME,
                    x,
                    y,
                    z,
                    density * 6,
                    0.7D,
                    0.45D,
                    0.7D,
                    0.015D
            );

            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.FALLING_DUST,
                            surface
                    ),
                    x,
                    y,
                    z,
                    density * 2,
                    0.6D,
                    0.3D,
                    0.6D,
                    0.01D
            );
        }

        int duration = Math.max(1, TireTracksConfig.dustVeilDurationTicks());

        double radius = TireTracksConfig.dustVeilRadius();

        if (radius <= 0.0D) {
            return;
        }

        double selfRadius = Math.min(
                Math.max(0.0D, TireTracksConfig.dustVeilSelfRadius()),
                radius
        );

        double radiusSqr = radius * radius;
        double selfRadiusSqr = selfRadius * selfRadius;

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(pos).inflate(radius)
        );

        for (LivingEntity entity : nearby) {
            if (entity.isSpectator()) {
                continue;
            }

            double distanceSqr = entity.distanceToSqr(
                    x,
                    pos.getY() + 0.5D,
                    z
            );

            if (distanceSqr < selfRadiusSqr || distanceSqr > radiusSqr) {
                continue;
            }

            /*
             * Refresh only once the previous puff has mostly worn off, so a
             * long dusty straight does not stack into permanent blindness.
             */
            MobEffectInstance existing = entity.getEffect(MobEffects.BLINDNESS);

            if (existing != null && existing.getDuration() > duration / 2) {
                continue;
            }

            entity.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    duration,
                    0,
                    false,
                    false,
                    true
            ));
        }
    }
}
