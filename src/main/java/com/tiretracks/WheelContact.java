package com.tiretracks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Everything that happens when a wheel touches a block, in one place.
 *
 * <p>The mixin stays a thin hook: it only owns the per wheel state and forwards
 * here, so all behaviour lives in normal, readable classes.</p>
 */
public final class WheelContact {

    private WheelContact() {
    }

    public static void onWheelContact(
            Object wheelBlockEntity,
            Level level,
            BlockPos pos,
            BlockState state,
            WheelState wheelState
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (pos == null || state == null || wheelState == null) {
            return;
        }

        if (!server.isLoaded(pos)) {
            return;
        }

        long gameTime = server.getGameTime();

        boolean moved = wheelState.updatePosition(pos, gameTime);
        double speed = wheelState.speedBlocksPerSecond();

        /*
         * Spray only while actually rolling, otherwise a parked vehicle would
         * fountain particles forever.
         */
        if (moved && wheelState.spraySlotReady()) {
            WheelSpray.sprayAt(server, pos, state, speed);
        }

        MaterialCarry.onContact(server, pos, state, wheelState, moved);

        /*
         * Deformation is not gated on movement: a wheel spinning in place is
         * supposed to dig itself in.
         */
        if (wheelState.deformSlotReady()) {
            double mass = SableAccess.vehicleMass(wheelBlockEntity);

            TerrainDeformer.deformAt(server, pos, state, mass);
        }
    }
}
