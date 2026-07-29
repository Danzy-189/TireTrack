package com.tiretracks.mixin;

import com.tiretracks.WheelContact;
import com.tiretracks.WheelState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hooks the block lookup a wheel performs while checking the ground under it.
 *
 * <p>Deliberately thin: it owns the per wheel state and forwards to
 * {@link WheelContact}. All behaviour lives in ordinary classes, which keeps the
 * mixin easy to keep working across Offroad and Sable updates.</p>
 */
@Pseudo
@Mixin(
        targets = "dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity",
        remap = false
)
public abstract class WheelMountBlockEntityMixin {

    @Unique
    private WheelState tiretracks$wheelState;

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
        BlockState state = level.getBlockState(pos);

        try {
            if (!level.isClientSide) {
                WheelState wheelState = this.tiretracks$wheelState;

                if (wheelState == null) {
                    wheelState = new WheelState();
                    this.tiretracks$wheelState = wheelState;
                }

                WheelContact.onWheelContact(
                        this,
                        level,
                        pos,
                        state,
                        wheelState
                );
            }
        } catch (Throwable ignored) {
            /*
             * Terrain damage must never break vehicle physics.
             */
        }

        return state;
    }
}
