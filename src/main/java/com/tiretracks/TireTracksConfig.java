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

        public final ModConfigSpec.IntValue tickInterval;

        public final ModConfigSpec.BooleanValue playSounds;
        public final ModConfigSpec.BooleanValue spawnParticles;

        public final ModConfigSpec.IntValue lightMaxStage;
        public final ModConfigSpec.IntValue mediumMaxStage;
        public final ModConfigSpec.IntValue heavyMaxStage;

        public final ModConfigSpec.BooleanValue puddles;
        public final ModConfigSpec.DoubleValue wetChanceMultiplier;
        public final ModConfigSpec.DoubleValue dryChanceMultiplier;
        public final ModConfigSpec.DoubleValue dryBiomeTemperature;

        public final ModConfigSpec.BooleanValue eatSnow;
        public final ModConfigSpec.BooleanValue packSnow;
        public final ModConfigSpec.DoubleValue snowToIceChance;
        public final ModConfigSpec.DoubleValue snowToMudChance;

        public final ModConfigSpec.BooleanValue wheelSpray;
        public final ModConfigSpec.IntValue sprayInterval;
        public final ModConfigSpec.IntValue sprayDensity;
        public final ModConfigSpec.DoubleValue sprayFullSpeed;

        public final ModConfigSpec.BooleanValue carryEnabled;
        public final ModConfigSpec.IntValue carryDistance;

        public final ModConfigSpec.BooleanValue dustVeil;
        public final ModConfigSpec.DoubleValue dustVeilMinSpeed;
        public final ModConfigSpec.DoubleValue dustVeilRadius;
        public final ModConfigSpec.DoubleValue dustVeilSelfRadius;
        public final ModConfigSpec.IntValue dustVeilDurationTicks;

        private Common(ModConfigSpec.Builder builder) {
            builder.comment(
                    "TireTracks terrain deformation settings.",
                    "Mass is measured in kilograms on the same scale Create Aeronautics (Sable) uses.",
                    "Default profiles: 0-45 kg light, 46-80 kg medium, above 80 kg heavy.",
                    "If total mass cannot be read, the medium profile is used as fallback.",
                    "Which blocks count as which surface is decided by block tags in",
                    "data/tiretracks/tags/block/, not by this file."
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
                    "Chance per check that a light vehicle wears the ground one stage deeper."
            ).defineInRange(
                    "lightChance",
                    0.12D,
                    0.0D,
                    1.0D
            );

            mediumChance = builder.comment(
                    "Chance per check that a medium vehicle wears the ground one stage deeper."
            ).defineInRange(
                    "mediumChance",
                    0.30D,
                    0.0D,
                    1.0D
            );

            heavyChance = builder.comment(
                    "Chance per check that a heavy vehicle wears the ground one stage deeper."
            ).defineInRange(
                    "heavyChance",
                    0.50D,
                    0.0D,
                    1.0D
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
                    "Progressive ruts.",
                    "Ground wears down one stage at a time and the stage is the block itself:",
                    "  0 turf (anything in #tiretracks:turf)",
                    "  1 dirt path",
                    "  2 coarse dirt",
                    "  3 loose fill: mud when wet, sand in hot dry biomes, gravel otherwise",
                    "  4 puddle: water, or ice in a freezing biome",
                    "Heavier vehicles are allowed to reach deeper stages."
            ).push("ruts");

            lightMaxStage = builder.comment(
                    "Deepest stage a light vehicle can reach. 1 means it only ever packs a footpath."
            ).defineInRange(
                    "lightMaxStage",
                    1,
                    0,
                    4
            );

            mediumMaxStage = builder.comment(
                    "Deepest stage a medium vehicle can reach."
            ).defineInRange(
                    "mediumMaxStage",
                    2,
                    0,
                    4
            );

            heavyMaxStage = builder.comment(
                    "Deepest stage a heavy vehicle can reach."
            ).defineInRange(
                    "heavyMaxStage",
                    4,
                    0,
                    4
            );

            puddles = builder.comment(
                    "Whether a fully worn wet rut may fill with water, or ice in a freezing biome.",
                    "Only happens when the rut has a solid floor and solid ground on all four sides,",
                    "so water can never run off across the landscape."
            ).define(
                    "puddles",
                    true
            );

            wetChanceMultiplier = builder.comment(
                    "Chance multiplier while it is raining on the block, or next to water."
            ).defineInRange(
                    "wetChanceMultiplier",
                    1.6D,
                    0.0D,
                    10.0D
            );

            dryChanceMultiplier = builder.comment(
                    "Chance multiplier in hot dry biomes."
            ).defineInRange(
                    "dryChanceMultiplier",
                    0.85D,
                    0.0D,
                    10.0D
            );

            dryBiomeTemperature = builder.comment(
                    "Biome base temperature from which ground counts as dry.",
                    "Desert and badlands are 2.0, savanna 1.2, plains 0.8."
            ).defineInRange(
                    "dryBiomeTemperature",
                    0.95D,
                    -1.0D,
                    5.0D
            );

            builder.pop();

            builder.comment(
                    "Snow handling.",
                    "Passes shave layers off, then the last layer is driven into the ground as",
                    "packed snow, which further passes polish into ice. Both only happen in biomes",
                    "cold enough to keep them, so no melting holes appear in a temperate lawn."
            ).push("snow");

            eatSnow = builder.comment(
                    "Whether wheels affect snow at all."
            ).define(
                    "eatSnow",
                    true
            );

            packSnow = builder.comment(
                    "Whether snow is packed into a track instead of simply disappearing.",
                    "Turn off for the old behaviour, where the last layer vanishes."
            ).define(
                    "packSnow",
                    true
            );

            snowToIceChance = builder.comment(
                    "Chance per check that packed snow is polished into ice."
            ).defineInRange(
                    "snowToIceChance",
                    0.35D,
                    0.0D,
                    1.0D
            );

            snowToMudChance = builder.comment(
                    "Chance that the last snow layer leaves mud underneath.",
                    "Only used where snow cannot be packed: warm biomes, or packSnow = false."
            ).defineInRange(
                    "snowToMudChance",
                    0.25D,
                    0.0D,
                    1.0D
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
                    "Base particle amount per puff. Raise for thicker spray, lower for performance."
            ).defineInRange(
                    "sprayDensity",
                    2,
                    1,
                    20
            );

            sprayFullSpeed = builder.comment(
                    "Wheel speed in blocks per second at which spray reaches full strength.",
                    "Slower driving thins the spray out, faster driving thickens it a little."
            ).defineInRange(
                    "sprayFullSpeed",
                    10.0D,
                    0.1D,
                    200.0D
            );

            builder.pop();

            builder.comment(
                    "Mud and soil picked up by a tyre and dropped again on clean ground.",
                    "Particles only: no blocks are placed on the surface driven over.",
                    "Driving through water rinses the tread clean."
            ).push("carry");

            carryEnabled = builder.define(
                    "carryEnabled",
                    true
            );

            carryDistance = builder.comment(
                    "How many blocks of clean ground still show a trail after leaving soft ground."
            ).defineInRange(
                    "carryDistance",
                    6,
                    0,
                    64
            );

            builder.pop();

            builder.comment(
                    "Dust veil: driving fast over dusty ground raises a cloud that briefly blinds",
                    "anyone caught inside it. Own crew protection is a distance heuristic, since",
                    "nothing tells us who is riding which vehicle: everything closer than",
                    "dustVeilSelfRadius is treated as being on board and is spared."
            ).push("dust");

            dustVeil = builder.comment(
                    "Whether the dust cloud blinds nearby entities. Set false for particles only."
            ).define(
                    "dustVeil",
                    true
            );

            dustVeilMinSpeed = builder.comment(
                    "Minimum wheel speed in blocks per second before a veil is raised."
            ).defineInRange(
                    "dustVeilMinSpeed",
                    8.0D,
                    0.0D,
                    200.0D
            );

            dustVeilRadius = builder.comment(
                    "Outer radius of the cloud in blocks."
            ).defineInRange(
                    "dustVeilRadius",
                    10.0D,
                    0.0D,
                    64.0D
            );

            dustVeilSelfRadius = builder.comment(
                    "Inner radius that is spared, meant to cover the vehicle raising the dust.",
                    "Raise it on very large builds if your own crew keeps getting blinded."
            ).defineInRange(
                    "dustVeilSelfRadius",
                    4.0D,
                    0.0D,
                    64.0D
            );

            dustVeilDurationTicks = builder.comment(
                    "Blindness duration in ticks. 20 ticks is one second."
            ).defineInRange(
                    "dustVeilDurationTicks",
                    30,
                    1,
                    600
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

    public static int tickInterval() {
        return COMMON.tickInterval.get();
    }

    public static boolean playSounds() {
        return COMMON.playSounds.get();
    }

    public static boolean spawnParticles() {
        return COMMON.spawnParticles.get();
    }

    public static int lightMaxStage() {
        return COMMON.lightMaxStage.get();
    }

    public static int mediumMaxStage() {
        return COMMON.mediumMaxStage.get();
    }

    public static int heavyMaxStage() {
        return COMMON.heavyMaxStage.get();
    }

    public static boolean puddles() {
        return COMMON.puddles.get();
    }

    public static double wetChanceMultiplier() {
        return COMMON.wetChanceMultiplier.get();
    }

    public static double dryChanceMultiplier() {
        return COMMON.dryChanceMultiplier.get();
    }

    public static double dryBiomeTemperature() {
        return COMMON.dryBiomeTemperature.get();
    }

    public static boolean eatSnow() {
        return COMMON.eatSnow.get();
    }

    public static boolean packSnow() {
        return COMMON.packSnow.get();
    }

    public static double snowToIceChance() {
        return COMMON.snowToIceChance.get();
    }

    public static double snowToMudChance() {
        return COMMON.snowToMudChance.get();
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

    public static double sprayFullSpeed() {
        return COMMON.sprayFullSpeed.get();
    }

    public static boolean carryEnabled() {
        return COMMON.carryEnabled.get();
    }

    public static int carryDistance() {
        return COMMON.carryDistance.get();
    }

    public static boolean dustVeil() {
        return COMMON.dustVeil.get();
    }

    public static double dustVeilMinSpeed() {
        return COMMON.dustVeilMinSpeed.get();
    }

    public static double dustVeilRadius() {
        return COMMON.dustVeilRadius.get();
    }

    public static double dustVeilSelfRadius() {
        return COMMON.dustVeilSelfRadius.get();
    }

    public static int dustVeilDurationTicks() {
        return COMMON.dustVeilDurationTicks.get();
    }
}
