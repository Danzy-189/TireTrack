# TireTracks 3.2.0

Wheels from **Create Aeronautics** (Offroad / Sable / Simulated) leave real marks on the world.

Minecraft **1.21.1**, NeoForge **21.1.235**, Java **21**.

---

## What it does

### Roads that build themselves

Ground wears down one stage at a time, and the stage **is the block standing there**, so progress survives restarts and chunk unloads without a single byte of extra save data.

| Stage | Block |
| --- | --- |
| 0 | turf (anything in `#tiretracks:turf`) |
| 1 | coarse dirt |
| 2 | loose fill: **mud** when wet, **sand** in hot dry biomes, **gravel** otherwise |
| 3 | puddle: **water**, or **ice** in a freezing biome |

Dirt paths are **not** part of the chain. A wheel churns ground up, it does not tamp a tidy footpath. Existing paths, vanilla or from an older version, still count as stage 0 and keep wearing onward.

A puddle only forms in a rut walled in on all four sides with a solid floor, so water can never run off across the landscape.

### Weight matters

Mass is read straight from the vehicle, on the same kilogram scale the sub level debug dump prints as `Mass:`.

| Class | Mass | Reaches |
| --- | --- | --- |
| Light | 0 – 45 kg | coarse dirt |
| Medium | 46 – 80 kg | loose fill |
| Heavy | 81 – 149 kg | puddles |
| Very heavy | 150 kg and up | puddles, plus hard ground damage |

If mass cannot be read, the medium profile is used.

### Very heavy machines (150 kg+)

* **Stone bricks** crack into cracked stone bricks. Cracked is the end of the line: a paved road gets scarred, never destroyed.
* **Cobblestone and andesite** can be crushed further into gravel, only where there is solid ground below.
* **Soil** can simply give way and leave an open hole one block deep. Deliberately rare, and never over a cave.
* **Sand** does not just dent: the machine punches two blocks down and the sand above caves in behind it, so it genuinely buries itself.

### Every surface reacts differently

* **Sand and red sand** — medium packs it into sandstone, heavy punches a block out, very heavy digs a pit. Light vehicles leave no trace.
* **Stone** — stubborn on purpose. Only a fraction of the normal wear chance, cracking into cobblestone or andesite at random so the track blends into the rock instead of reading as a laid stripe.
* **Snow** — not eaten but compressed: layers are shaved off, the last one is driven into the ground as ice, and further passes polish that into packed ice. Only in biomes cold enough to keep it.

### Particles that sell the motion

Water splashes and bubbles, sand and gravel raise a dust plume, snow throws flakes, soil kicks flying clods. Amount scales with actual wheel speed. Grass, podzol and moss spray **plain dirt**, never green flecks. Driving in the rain adds droplets and popping bubbles from under the wheels.

Mud picked up on soft ground trails behind the tyre for a few blocks and is rinsed off by water. Particles only: no blocks are smeared onto anyone's road.

### Built to be tuned

Which block counts as which surface is decided by block tags in `data/tiretracks/tags/block/`, so any datapack or modpack can extend or replace the lists without touching the code:

`immune`, `turf`, `soft`, `dusty`, `wet`, `snow`, `muddyable`, `packable_ground`, `dirt_particles`.

`immune` is a free blacklist, and blocks with a block entity are always protected.

### Quiet by design

No sounds, no effects on players. Only blocks and particles.

---

## Config

`config/tiretracks-common.toml`, generated on first launch.

**Delete the old file when upgrading from 3.1.x** — the stage numbers changed and several keys were added or removed.

### `[general]`

| Key | Default | Meaning |
| --- | --- | --- |
| `lightVehicleMaxMass` | 45.0 | upper bound of the light profile, kg |
| `mediumVehicleMaxMass` | 80.0 | upper bound of the medium profile, kg |
| `veryHeavyVehicleMinMass` | 150.0 | mass from which a vehicle is very heavy |
| `lightChance` | 0.06 | wear chance per check, light |
| `mediumChance` | 0.15 | wear chance per check, medium |
| `heavyChance` | 0.30 | wear chance per check, heavy |
| `veryHeavyChance` | 0.45 | wear chance per check, very heavy |
| `tickInterval` | 4 | physics ticks between terrain checks per wheel |
| `spawnParticles` | true | master switch for every particle |
| `particleVolumeMultiplier` | 1.5 | global particle amount multiplier |

### `[ruts]`

| Key | Default |
| --- | --- |
| `lightMaxStage` | 1 |
| `mediumMaxStage` | 2 |
| `heavyMaxStage` | 3 |
| `veryHeavyMaxStage` | 3 |
| `puddles` | true |
| `wetChanceMultiplier` | 1.6 |
| `dryChanceMultiplier` | 0.85 |
| `dryBiomeTemperature` | 0.95 |

### `[hardground]`

| Key | Default | Meaning |
| --- | --- | --- |
| `stoneCrackMultiplier` | 0.25 | fraction of the vehicle chance applied on stone |
| `stoneCrushChance` | 0.05 | cobble/andesite crushed into gravel, very heavy only |
| `stoneBrickCrackChance` | 0.08 | stone bricks cracked, very heavy only |
| `groundCollapseChance` | 0.02 | soil collapses into a hole, very heavy only |
| `sandSinkDepth` | 2 | sand blocks punched through in one go, very heavy |

### `[snow]`

`eatSnow` true, `packSnow` true, `snowToIceChance` 0.35, `snowToMudChance` 0.25

### `[spray]`

`wheelSpray` true, `sprayInterval` 2, `sprayDensity` 2, `sprayFullSpeed` 10.0

### `[carry]`

`carryEnabled` true, `carryDistance` 6

---

## Compatibility

The hook is a `@Pseudo` mixin matched by class name, so a missing or renamed Offroad build makes TireTracks sit idle instead of crashing the game. All Create Aeronautics dependencies are optional.

## Building

```
gradle clean build
```

Output: `build/libs/TireTracks-3.2.0.jar`. The GitHub Actions workflow **Build TireTracks** does the same on demand.
