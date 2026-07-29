package com.tiretracks;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class TireTracksConfig {

    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(Common::new);

        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    private TireTracksConfig() {
    }

    public static final class Common {

        public final ModConfigSpec.DoubleValue lightVehicleMaxMass;
        public final ModConfigSpec.DoubleValue mediumVehicleMaxMass;

        public final ModConfigSpec.DoubleValue lightChance;
        public final ModConfigSpec.DoubleValue mediumChance;
        public final ModConfigSpec.DoubleValue heavyChance;

        public final ModConfigSpec.DoubleValue snowToMudChance;

        public final ModConfigSpec.IntValue tickInterval;

        public final ModConfigSpec.BooleanValue eatSnow;
        public final ModConfigSpec.BooleanValue playSounds;
        public final ModConfigSpec.BooleanValue spawnParticles;

        public final ModConfigSpec.BooleanValue wheelSpray;
        public final ModConfigSpec.IntValue sprayInterval;
        public final ModConfigSpec.IntValue sprayDensity;

        private Common(ModConfigSpec.Builder builder) {
            builder.comment(
                    "TireTracks terrain deformation settings.",
                    "Mass is measured in kilograms on the same scale Create Aeronautics (Sable) uses.",
                    "Default profiles: 0-45 kg light, 46-80 kg medium, above 80 kg heavy.",
                    "If total mass cannot be read, the medium profile is used as fallback."
            ).push("general");

            lightVehicleMaxMass = builder.comment(
                    "Upper mass bound of a light vehicle, in kilograms.",
                    "Vehicles up to and including this value use the light profile."
            ).defineInRange(
                    "lightVehicleMaxMass",
                    45.0D,
                    0.0D,
                    100000.0D
            );

            mediumVehicleMaxMass = builder.comment(
                    "Upper mass bound of a medium vehicle, in kilograms.",
                    "Vehicles above lightVehicleMaxMass up to and including this value use the medium profile.",
                    "Vehicles above this value use the heavy profile."
            ).defineInRange(
                    "mediumVehicleMaxMass",
                    80.0D,
                    0.0D,
                    100000.0D
            );

            lightChance = builder.comment(
                    "Chance for light vehicles to create a track.",
                    "Light vehicles can create only dirt or sand."
            ).defineInRange(
                    "lightChance",
                    0.12D,
                    0.0D,
                    1.0D
            );

            mediumChance = builder.comment(
                    "Chance for medium vehicles to create a track.",
                    "Medium vehicles can create gravel, sand or dirt path."
            ).defineInRange(
                    "mediumChance",
                    0.30D,
                    0.0D,
                    1.0D
            );

            heavyChance = builder.comment(
                    "Chance for heavy vehicles to create a track.",
                    "Heavy vehicles can create mud, coarse dirt or dirt."
            ).defineInRange(
                    "heavyChance",
                    0.50D,
                    0.0D,
                    1.0D
            );

            snowToMudChance = builder.comment(
                    "Chance that the last snow layer leaves mud underneath."
            ).defineInRange(
                    "snowToMudChance",
                    0.25D,
                    0.0D,
                    1.0D
            );

            eatSnow = builder.comment(
                    "Whether wheels remove snow layers."
            ).define(
                    "eatSnow",
                    true
            );

            tickInterval = builder.comment(
                    "Number of physics ticks between terrain checks per wheel.",
                    "Lower values create tracks more often but cost more performance."
            ).defineInRange(
                    "tickInterval",
                    4,
                    1,
                    200
            );

            playSounds = builder.define(
                    "playSounds",
                    true
            );

            spawnParticles = builder.comment(
                    "Master switch for every particle this mod spawns."
            ).define(
                    "spawnParticles",
                    true
            );

            builder.pop();

            builder.comment(
                    "Cosmetic spray thrown up by rolling wheels.",
                    "Water and rain give splashes, sand and gravel give dust plumes,",
                    "snow gives snowflakes, soil gives flying clods.",
                    "Requires spawnParticles = true."
            ).push("spray");

            wheelSpray = builder.comment(
                    "Whether wheels throw up surface particles while driving."
            ).define(
                    "wheelSpray",
                    true
            );

            sprayInterval = builder.comment(
                    "Number of physics ticks between spray puffs per wheel.",
                    "Spray is also skipped while a wheel stays on the same block, so a parked vehicle is quiet."
            ).defineInRange(
                    "sprayInterval",
                    2,
                    1,
                    200
            );

            sprayDensity = builder.comment(
                    "Particle amount multiplier per puff. Raise for thicker spray, lower for performance."
            ).defineInRange(
                    "sprayDensity",
                    2,
                    1,
                    20
            );

            builder.pop();
        }
    }

    public static double lightVehicleMaxMass() {
        return COMMON.lightVehicleMaxMass.get();
    }

    public static double mediumVehicleMaxMass() {
        return COMMON.mediumVehicleMaxMass.get();
    }

    public static double lightChance() {
        return COMMON.lightChance.get();
    }

    public static double mediumChance() {
        return COMMON.mediumChance.get();
    }

    public static double heavyChance() {
        return COMMON.heavyChance.get();
    }

    public static double snowToMudChance() {
        return COMMON.snowToMudChance.get();
    }

    public static int tickInterval() {
        return COMMON.tickInterval.get();
    }

    public static boolean eatSnow() {
        return COMMON.eatSnow.get();
    }

    public static boolean playSounds() {
        return COMMON.playSounds.get();
    }

    public static boolean spawnParticles() {
        return COMMON.spawnParticles.get();
    }

    public static boolean wheelSpray() {
        return COMMON.wheelSpray.get();
    }

    public static int sprayInterval() {
        return COMMON.sprayInterval.get();
    }

    public static int sprayDensity() {
        return COMMON.sprayDensity.get();
    }
}
