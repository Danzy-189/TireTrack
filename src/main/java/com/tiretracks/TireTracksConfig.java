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
        public final ModConfigSpec.DoubleValue veryHeavyVehicleMinMass;

        public final ModConfigSpec.DoubleValue lightChance;
        public final ModConfigSpec.DoubleValue mediumChance;
        public final ModConfigSpec.DoubleValue heavyChance;
        public final ModConfigSpec.DoubleValue veryHeavyChance;

        public final ModConfigSpec.IntValue tickInterval;

        public final ModConfigSpec.BooleanValue spawnParticles;
        public final ModConfigSpec.DoubleValue particleVolumeMultiplier;

        public final ModConfigSpec.IntValue lightMaxStage;
        public final ModConfigSpec.IntValue mediumMaxStage;
        public final ModConfigSpec.IntValue heavyMaxStage;
        public final ModConfigSpec.IntValue veryHeavyMaxStage;

        public final ModConfigSpec.BooleanValue puddles;
        public final ModConfigSpec.DoubleValue wetChanceMultiplier;
        public final ModConfigSpec.DoubleValue dryChanceMultiplier;
        public final ModConfigSpec.DoubleValue dryBiomeTemperature;

        public final ModConfigSpec.DoubleValue stoneCrackMultiplier;
        public final ModConfigSpec.DoubleValue stoneCrushChance;
        public final ModConfigSpec.DoubleValue stoneBrickCrackChance;
        public final ModConfigSpec.DoubleValue groundCollapseChance;
        public final ModConfigSpec.IntValue sandSinkDepth;

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

        private Common(ModConfigSpec.Builder builder) {
            builder.comment(
                    "TireTracks terrain deformation settings.",
                    "Mass is measured in kilograms on the same scale Create Aeronautics (Sable) uses,",
                    "the same number the sub level debug dump prints as 'Mass:'.",
                    "Default profiles: 0-45 kg light, 46-80 kg medium, 81-149 kg heavy, 150 kg and up very heavy.",
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
                    "Vehicles above lightVehicleMaxMass up to and including this value use the medium profile."
            ).defineInRange(
                    "mediumVehicleMaxMass",
                    80.0D,
                    0.0D,
                    100000.0D
            );

            veryHeavyVehicleMinMass = builder.comment(
                    "Mass in kilograms from which a vehicle counts as very heavy.",
                    "Very heavy machines get their own destructive behaviour: they crack stone bricks,",
                    "can collapse soil into a hole, crush cobble into gravel and sink into sand.",
                    "Anything between mediumVehicleMaxMass and this value uses the heavy profile."
            ).defineInRange(
                    "veryHeavyVehicleMinMass",
                    150.0D,
                    0.0D,
                    100000.0D
            );

            lightChance = builder.comment(
                    "Chance per check that a light vehicle wears the ground one stage deeper."
            ).defineInRange(
                    "lightChance",
                    0.06D,
                    0.0D,
                    1.0D
            );

            mediumChance = builder.comment(
                    "Chance per check that a medium vehicle wears the ground one stage deeper."
            ).defineInRange(
                    "mediumChance",
                    0.15D,
                    0.0D,
                    1.0D
            );

            heavyChance = builder.comment(
                    "Chance per check that a heavy vehicle wears the ground one stage deeper."
            ).defineInRange(
                    "heavyChance",
                    0.30D,
                    0.0D,
                    1.0D
            );

            veryHeavyChance = builder.comment(
                    "Chance per check that a very heavy vehicle wears the ground one stage deeper."
            ).defineInRange(
                    "veryHeavyChance",
                    0.45D,
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

            particleVolumeMultiplier = builder.comment(
                    "Global multiplier for the amount of particles in every puff:",
                    "spray, dust, splashes, snow, flying clods and deformation puffs alike.",
                    "1.0 is the old amount, 1.5 is the new default, lower it if particles cost you frames."
            ).defineInRange(
                    "particleVolumeMultiplier",
                    1.5D,
                    0.1D,
                    5.0D
            );

            builder.pop();

            builder.comment(
                    "Progressive ruts.",
                    "Ground wears down one stage at a time and the stage is the block itself:",
                    "  0 turf (anything in #tiretracks:turf)",
                    "  1 coarse dirt",
                    "  2 loose fill: mud when wet, sand in hot dry biomes, gravel otherwise",
                    "  3 puddle: water, or ice in a freezing biome",
                    "Dirt paths are no longer part of the chain: wheels churn ground up, they do not",
                    "tamp a tidy footpath. Existing dirt paths still count as stage 0 and wear onward.",
                    "Heavier vehicles are allowed to reach deeper stages."
            ).push("ruts");

            lightMaxStage = builder.comment(
                    "Deepest stage a light vehicle can reach."
            ).defineInRange(
                    "lightMaxStage",
                    1,
                    0,
                    3
            );

            mediumMaxStage = builder.comment(
                    "Deepest stage a medium vehicle can reach."
            ).defineInRange(
                    "mediumMaxStage",
                    2,
                    0,
                    3
            );

            heavyMaxStage = builder.comment(
                    "Deepest stage a heavy vehicle can reach."
            ).defineInRange(
                    "heavyMaxStage",
                    3,
                    0,
                    3
            );

            veryHeavyMaxStage = builder.comment(
                    "Deepest stage a very heavy vehicle can reach."
            ).defineInRange(
                    "veryHeavyMaxStage",
                    3,
                    0,
                    3
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
                    "Hard surfaces and very heavy machines.",
                    "Rock does not churn like soil, so it has its own much smaller chances:",
                    "most passes leave it exactly as it was."
            ).push("hardground");

            stoneCrackMultiplier = builder.comment(
                    "Multiplier applied to the vehicle chance when driving on stone.",
                    "Stone is stubborn: at the default 0.25 a heavy vehicle cracks it into",
                    "cobblestone or andesite on roughly one check in thirteen, so a rock road",
                    "takes real traffic to appear instead of one lap.",
                    "Light vehicles never mark stone at all."
            ).defineInRange(
                    "stoneCrackMultiplier",
                    0.25D,
                    0.0D,
                    1.0D
            );

            stoneCrushChance = builder.comment(
                    "Chance per check that a very heavy vehicle crushes cracked rock",
                    "(cobblestone or andesite) further into gravel.",
                    "Only very heavy vehicles do this, and only where the gravel has solid ground",
                    "beneath it, so nothing falls through the world."
            ).defineInRange(
                    "stoneCrushChance",
                    0.05D,
                    0.0D,
                    1.0D
            );

            stoneBrickCrackChance = builder.comment(
                    "Chance per check that a very heavy vehicle cracks stone bricks into",
                    "cracked stone bricks. Cracked bricks are the end of the line: a paved road",
                    "gets scarred, never destroyed."
            ).defineInRange(
                    "stoneBrickCrackChance",
                    0.08D,
                    0.0D,
                    1.0D
            );

            groundCollapseChance = builder.comment(
                    "Chance per check that soil simply gives way under a very heavy vehicle and",
                    "leaves an open hole one block deep.",
                    "Deliberately tiny: this is the rare moment a 150 kg machine drops a wheel",
                    "into the ground, not a digging tool. Only fires where there is solid ground",
                    "below, so it can never open a shaft into a cave."
            ).defineInRange(
                    "groundCollapseChance",
                    0.02D,
                    0.0D,
                    1.0D
            );

            sandSinkDepth = builder.comment(
                    "How many sand blocks a very heavy vehicle can punch through in one go.",
                    "Heavy vehicles always remove a single block; very heavy ones dig this deep,",
                    "so the sand above caves in and the machine genuinely buries itself.",
                    "Digging stops as soon as the column runs out of sand or solid ground."
            ).defineInRange(
                    "sandSinkDepth",
                    2,
                    1,
                    4
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
                    "Water and rain give splashes and bubbles, sand and gravel give dust plumes,",
                    "snow gives snowflakes, soil gives flying clods.",
                    "Grass and moss spray plain dirt, never green bits.",
                    "Requires spawnParticles = true. Amounts are scaled by particleVolumeMultiplier."
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
                    "Base particle amount per puff, before particleVolumeMultiplier is applied."
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
        }
    }

    public static double lightVehicleMaxMass() {
        return COMMON.lightVehicleMaxMass.get();
    }

    public static double mediumVehicleMaxMass() {
        return COMMON.mediumVehicleMaxMass.get();
    }

    public static double veryHeavyVehicleMinMass() {
        return COMMON.veryHeavyVehicleMinMass.get();
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

    public static double veryHeavyChance() {
        return COMMON.veryHeavyChance.get();
    }

    public static int tickInterval() {
        return COMMON.tickInterval.get();
    }

    public static boolean spawnParticles() {
        return COMMON.spawnParticles.get();
    }

    public static double particleVolumeMultiplier() {
        return COMMON.particleVolumeMultiplier.get();
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

    public static int veryHeavyMaxStage() {
        return COMMON.veryHeavyMaxStage.get();
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

    public static double stoneCrackMultiplier() {
        return COMMON.stoneCrackMultiplier.get();
    }

    public static double stoneCrushChance() {
        return COMMON.stoneCrushChance.get();
    }

    public static double stoneBrickCrackChance() {
        return COMMON.stoneBrickCrackChance.get();
    }

    public static double groundCollapseChance() {
        return COMMON.groundCollapseChance.get();
    }

    public static int sandSinkDepth() {
        return COMMON.sandSinkDepth.get();
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
}
