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

        private Common(ModConfigSpec.Builder builder) {
            builder.comment(
                    "TireTracks terrain deformation settings.",
                    "Mass is measured in kilograms when Sable exposes total vehicle mass.",
                    "If total mass cannot be read, the effective wheel mass is used as fallback."
            ).push("general");

            lightVehicleMaxMass = builder.comment(
                    "Maximum mass of a light vehicle.",
                    "Vehicles below this value use the light profile."
            ).defineInRange(
                    "lightVehicleMaxMass",
                    500.0D,
                    1.0D,
                    100000.0D
            );

            mediumVehicleMaxMass = builder.comment(
                    "Maximum mass of a medium vehicle.",
                    "Vehicles from lightVehicleMaxMass up to this value use the medium profile.",
                    "Vehicles above this value use the heavy profile."
            ).defineInRange(
                    "mediumVehicleMaxMass",
                    1500.0D,
                    1.0D,
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

            spawnParticles = builder.define(
                    "spawnParticles",
                    true
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
}
