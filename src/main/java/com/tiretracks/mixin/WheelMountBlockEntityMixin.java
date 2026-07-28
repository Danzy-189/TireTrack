package com.tiretracks.mixin;

import com.tiretracks.TerrainDeformer;
import com.tiretracks.TireTracksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hooks Create Offroad's wheel mount so every wheel chews up the ground it rolls over.
 *
 * Upstream (Simulated 1.2.x / Sable 1.2.x) does this inside sable$physicsTick, once it knows
 * which block the suspension raycast landed on:
 *
 *     this.touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(
 *             this.level.getBlockState(extensionToTerrain.minInteractingBlock())));
 *
 * That single Level#getBlockState call is the ONLY thing we need: it hands us the exact contact
 * block, server side only, already filtered to "the wheel is actually touching this". We redirect
 * it, pass the value straight through, and deform on the side. No behaviour of the vehicle physics
 * is changed.
 *
 * The class is targeted BY NAME via @Pseudo, so this mod compiles with zero Create Aeronautics
 * jars on the classpath and simply idles if the class is missing at runtime. remap = false because
 * NeoForge 1.21.1 already runs on official Mojang mappings.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity", remap = false)
public abstract class WheelMountBlockEntityMixin {

    @Unique
    private int tiretracks$cooldown = 0;

    @Redirect(
            method = "sable$physicsTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 0,
            remap = false
    )
    private BlockState tiretracks$onContactBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        try {
            if (level != null && !level.isClientSide) {
                // Physics ticks run fast; throttle so a wheel doesn't strip a field in one second.
                if (--this.tiretracks$cooldown <= 0) {
                    this.tiretracks$cooldown = Math.max(1, TireTracksConfig.tickInterval());
                    TerrainDeformer.deformAt(level, pos, state);
                }
            }
        } catch (Throwable ignored) {
            // Never let cosmetic terrain damage break vehicle physics.
        }

        return state;
    }
}
