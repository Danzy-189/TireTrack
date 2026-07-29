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

        public final ModConfigSpec.BooleanValue puddlesEvaporate;
        public final ModConfigSpec.DoubleValue puddleEvaporationDays;
        public final ModConfigSpec.DoubleValue hotBiomeEvaporationMultiplier;
        public final ModConfigSpec.BooleanValue mudHeals;
        public final ModConfigSpec.DoubleValue mudHealChance;
        public final ModConfigSpec.DoubleValue mudHealDays;

        private Common(ModConfigSpec.Builder builder) {
            builder.comment(
                    "TireTracks terrain deformation settings.",
                    "Mass is measured in kilograms on the same scale Create Aeronautics (Sable) uses,",
                    "the same number the sub level debug dump prints as 'Mass:'.",
                    "Default profiles: 0-45 kg light, 46-80 kg medium, above 80 kg heavy.",
                    "If mass cannot be read, the medium profile is used as fallback.",
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
                    "Heavier vehicles are allowed to reach deeper stages, but the defaults assume",
                    "real Create Aeronautics vehicles, which usually weigh 40-80 kg: a medium",
                    "vehicle must be able to reach the loose fill, otherwise most builds would",
                    "never dig past coarse dirt at all."
            ).push("ruts");

            lightMaxStage = builder.comment(
                    "Deepest stage a light vehicle can reach."
            ).defineInRange(
                    "lightMaxStage",
                    2,
                    0,
                    4
            );

            mediumMaxStage = builder.comment(
                    "Deepest stage a medium vehicle can reach."
            ).defineInRange(
                    "mediumMaxStage",
                    3,
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
                    "Passes shave layers off, then the last layer is driven into the ground as ice,",
                    "which further passes polish into packed ice. Both only happen in biomes cold",
                    "enough to keep them, so no melting holes appear in a temperate lawn."
            ).push("snow");

            eatSnow = builder.comment(
                    "Whether wheels affect snow at all."
            ).define(
                    "eatSnow",
                    true
            );

            packSnow = builder.comment(
                    "Whether snow is packed into an ice track instead of simply disappearing.",
                    "Turn off for the old behaviour, where the last layer vanishes."
            ).define(
                    "packSnow",
                    true
            );

            snowToIceChance = builder.comment(
                    "Chance per check that a groomed ice track is polished into packed ice."
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
                    "Road healing: abandoned roads slowly return to nature.",
                    "Puddles evaporate in dry weather, mud heals back to turf during rain.",
                    "Actively used roads stay as they are; only neglected stretches heal.",
                    "Time is measured in Minecraft days (1 day = 24000 ticks = 20 minutes real time)."
            ).push("healing");

            puddlesEvaporate = builder.comment(
                    "Whether puddles (water and ice in ruts) evaporate in dry weather, turning back into mud."
            ).define(
                    "puddlesEvaporate",
                    true
            );

            puddleEvaporationDays = builder.comment(
                    "Minecraft days without rain before a puddle evaporates. Raining on the block resets the timer.",
                    "1 Minecraft day = 24000 ticks = 20 minutes real time.",
                    "Hot biomes (base temperature >= dryBiomeTemperature) evaporate faster, see hotBiomeEvaporationMultiplier."
            ).defineInRange(
                    "puddleEvaporationDays",
                    1.0D,
                    0.01D,
                    30.0D
            );

            hotBiomeEvaporationMultiplier = builder.comment(
                    "How much faster puddles evaporate in hot biomes. 2.0 means twice as fast (half the days)."
            ).defineInRange(
                    "hotBiomeEvaporationMultiplier",
                    2.0D,
                    1.0D,
                    10.0D
            );

            mudHeals = builder.comment(
                    "Whether mud, coarse dirt and dirt paths slowly heal back to grass during rain,",
                    "if they have not been touched by wheels for a while."
            ).define(
                    "mudHeals",
                    true
            );

            mudHealChance = builder.comment(
                    "Chance per check that an abandoned rut heals one stage back toward turf.",
                    "Only applies during rain, after mudHealDays have passed without wheel contact."
            ).defineInRange(
                    "mudHealChance",
                    0.6D,
                    0.0D,
                    1.0D
            );

            mudHealDays = builder.comment(
                    "Minecraft days a rut must be untouched before it can start healing.",
                    "1 Minecraft day = 24000 ticks = 20 minutes real time.",
                    "Wheels reset the timer, so active roads never heal."
            ).defineInRange(
                    "mudHealDays",
                    3.0D,
                    0.01D,
                    30.0D
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

    public static boolean puddlesEvaporate() {
        return COMMON.puddlesEvaporate.get();
    }

    public static double puddleEvaporationDays() {
        return COMMON.puddleEvaporationDays.get();
    }

    public static double hotBiomeEvaporationMultiplier() {
        return COMMON.hotBiomeEvaporationMultiplier.get();
    }

    public static boolean mudHeals() {
        return COMMON.mudHeals.get();
    }

    public static double mudHealChance() {
        return COMMON.mudHealChance.get();
    }

    public static double mudHealDays() {
        return COMMON.mudHealDays.get();
    }
}
