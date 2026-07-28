package com.tiretracks;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class TireTracksConfig {

    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static class Common {
        public final ModConfigSpec.DoubleValue turfChance;
        public final ModConfigSpec.DoubleValue snowToMudChance;
        public final ModConfigSpec.IntValue tickInterval;
        public final ModConfigSpec.BooleanValue eatSnow;
        public final ModConfigSpec.BooleanValue playSounds;
        public final ModConfigSpec.BooleanValue spawnParticles;

        Common(ModConfigSpec.Builder b) {
            b.comment("TireTracks - terrain deformation under Create Aeronautics / Offroad wheels").push("general");

            turfChance = b.comment("Chance (0..1) that a grass block under a wheel is churned into sand/dirt/mud/gravel/coarse dirt")
                    .defineInRange("turfChance", 0.30D, 0.0D, 1.0D);

            snowToMudChance = b.comment("Chance (0..1) that the ground left behind by the last removed snow layer becomes mud")
                    .defineInRange("snowToMudChance", 0.25D, 0.0D, 1.0D);

            eatSnow = b.comment("Wheels remove the snow layers they drive over")
                    .define("eatSnow", true);

            tickInterval = b.comment("Only deform once every N physics ticks per wheel (higher = cheaper, slower damage)")
                    .defineInRange("tickInterval", 4, 1, 200);

            playSounds = b.define("playSounds", true);
            spawnParticles = b.define("spawnParticles", true);

            b.pop();
        }
    }

    public static double turfChance() { return COMMON.turfChance.get(); }
    public static double snowToMudChance() { return COMMON.snowToMudChance.get(); }
    public static int tickInterval() { return COMMON.tickInterval.get(); }
    public static boolean eatSnow() { return COMMON.eatSnow.get(); }
    public static boolean playSounds() { return COMMON.playSounds.get(); }
    public static boolean spawnParticles() { return COMMON.spawnParticles.get(); }
}
