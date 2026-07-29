package com.tiretracks.mixin;

import com.tiretracks.TerrainDeformer;
import com.tiretracks.TireTracksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(
        targets = "dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity",
        remap = false
)
public abstract class WheelMountBlockEntityMixin {

    @Shadow
    protected Level level;

    @Unique
    private int tiretracks$cooldown = 0;

    @Redirect(
            method = "sable$physicsTick",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/world/level/Level;" +
                            "getBlockState(" +
                            "Lnet/minecraft/core/BlockPos;" +
                            ")" +
                            "Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 0,
            remap = false
    )
    private BlockState tiretracks$onContactBlock(
            Level level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(pos);

        try {
            if (!level.isClientSide) {
                if (--this.tiretracks$cooldown <= 0) {
                    this.tiretracks$cooldown =
                            Math.max(
                                    1,
                                    TireTracksConfig.tickInterval()
                            );

                    double vehicleMass =
                            tiretracks$getVehicleMass(
                                    level,
                                    pos
                            );

                    TerrainDeformer.deformAt(
                            level,
                            pos,
                            state,
                            vehicleMass
                    );
                }
            }
        } catch (Throwable ignored) {
            /*
             * Terrain damage must never break vehicle physics.
             */
        }

        return state;
    }

    @Unique
    private double tiretracks$getVehicleMass(
            Level level,
            BlockPos contactPos
    ) {
        /*
         * The exact Sable API for total mass may differ between builds.
         * First try to read the mass tracker through reflection.
         */
        try {
            Class<?> sableClass =
                    Class.forName(
                            "dev.ryanhcode.sable.Sable"
                    );

            Field helperField =
                    sableClass.getField("HELPER");

            Object helper =
                    helperField.get(null);

            Method getContaining =
                    tiretracks$findMethod(
                            helper.getClass(),
                            "getContaining",
                            1
                    );

            if (getContaining != null) {
                Object subLevel =
                        getContaining.invoke(
                                helper,
                                this
                        );

                if (subLevel != null) {
                    Method getMassTracker =
                            tiretracks$findMethod(
                                    subLevel.getClass(),
                                    "getMassTracker",
                                    0
                            );

                    if (getMassTracker != null) {
                        Object massTracker =
                                getMassTracker.invoke(
                                        subLevel
                                );

                        Double totalMass =
                                tiretracks$readMassValue(
                                        massTracker
                                );

                        if (totalMass != null
                                && totalMass > 0.0D) {
                            return totalMass;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            /*
             * Use fallback below.
             */
        }

        /*
         * If this Sable version does not expose a readable total mass,
         * use a safe medium profile instead of breaking the game.
         */
        return TireTracksConfig.mediumVehicleMaxMass()
                * 0.5D;
    }

    @Unique
    private Double tiretracks$readMassValue(
            Object massTracker
    ) {
        String[] methodNames = {
                "getMass",
                "mass",
                "getTotalMass",
                "getBodyMass",
                "getNormalMass"
        };

        for (String methodName : methodNames) {
            try {
                Method method =
                        massTracker.getClass()
                                .getMethod(methodName);

                Object value =
                        method.invoke(massTracker);

                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (Throwable ignored) {
                /*
                 * Try the next possible API name.
                 */
            }
        }

        return null;
    }

    @Unique
    private Method tiretracks$findMethod(
            Class<?> type,
            String name,
            int parameterCount
    ) {
        Class<?> current = type;

        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name)
                        && method.getParameterCount()
                        == parameterCount) {
                    method.setAccessible(true);
                    return method;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }
}
