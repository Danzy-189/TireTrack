# TireTracks

TireTracks makes Create Aeronautics vehicles (Offroad / Sable / Simulated) leave real marks on the world.
Ground does not just change once and stay that way: it wears down stage by stage into a rut, snow is packed
into an ice road instead of vanishing, wheels throw up surface appropriate spray, mud gets dragged onto clean
ground, and a fast run across dry ground raises a blinding dust cloud behind you.

- Minecraft 1.21.1, NeoForge 21.1.235, Java 21
- Version 3.1.0
- Server side logic only. No client mod required, no packets, no new blocks or items.

## Requirements

Offroad, Sable and Simulated are optional dependencies. The mixin targets the wheel mount class by name with
`@Pseudo`, so a missing or renamed class makes TireTracks idle instead of crashing the game.

## Progressive ruts

Repeated passes wear the ground deeper, one stage at a time. The current stage is stored in the world as the
block standing there, so progress survives restarts and chunk unloads without any extra save data.

| Stage | Block | Notes |
| --- | --- | --- |
| 0 | anything in `#tiretracks:turf` | grass, dirt, podzol, mycelium, moss, farmland |
| 1 | dirt path | a packed footpath |
| 2 | coarse dirt | a worn track |
| 3 | mud, sand or gravel | depends on the weather, see below |
| 4 | water, or ice in a freezing biome | a puddle in a soaked rut |

How deep a vehicle can dig depends on its mass class. A light quad polishes a footpath and stops there, a
loaded truck can carve the same spot all the way down to a mud hole.

| Class | Mass | Chance per check | Deepest stage |
| --- | --- | --- | --- |
| Light | 0-45 kg | 0.12 | 1, dirt path |
| Medium | 46-80 kg | 0.30 | 2, coarse dirt |
| Heavy | above 80 kg | 0.50 | 4, puddle |

Mass is read from Sable in kilograms, on the same scale Create Aeronautics reports. Both bounds are inclusive:
exactly 45 kg is light, exactly 80 kg is medium. If this Sable build does not expose a readable total mass,
every vehicle falls back to the medium profile.

Sand and gravel are gravity blocks, so they are only placed when the block below is solid. Otherwise the rut
would punch a hole through the terrain instead of staying a rut.

## Wet and dry ground

Moisture decides both how fast the ground gives way and what it turns into at stage 3.

| Condition | Chance multiplier | Loose fill |
| --- | --- | --- |
| Raining on the block, or water next to it | 1.6 | mud |
| Hot dry biome, base temperature 0.95 and up | 0.85 | sand |
| Anything else | 1.0 | gravel |

## Puddles

A fully worn mud rut in the rain can fill with water, and in a freezing biome it becomes ice instead, so last
season's track turns into this winter's slippery spot. A frozen puddle then keeps progressing along the snow
chain below and can be polished into packed ice.

A puddle is only created when the rut has a solid floor and solid ground on all four sides. Water can never run
off across the landscape or down a slope.

## Snow packing

Snow is compressed rather than deleted, which is the one mechanic here that improves the ground instead of
ruining it: drive the same line often enough and you groom yourself a fast ice road.

1. Snow blocks and powder snow collapse into a seven layer stack.
2. Each further pass shaves one layer off.
3. The last layer is driven into the ground, and the ground below becomes ice, flush with the surface, so there
   is no bump for the suspension.
4. Further passes polish that ice into packed ice, which never melts.

Vanilla has no packed snow block, and a snow block cannot play that role either: it is already the entry point
of this chain, so a groomed track would collapse back into loose layers forever. Ice never turns back into snow
and is genuinely faster to drive on.

Packing only happens in biomes cold enough to keep it (base temperature below 0.15). In warmer places, or with
`packSnow = false`, the last layer simply disappears and may leave mud behind, which is the old 3.0 behaviour.

## Wheel spray

| Surface | Particles |
| --- | --- |
| Water, shallow or deep | splashes and bubbles |
| Rain, on any surface | extra droplets flicked off the tread |
| Snow, ice, powder snow | snowflakes and snow crumbs |
| `#tiretracks:dusty` | dust plume tinted with the block |
| `#tiretracks:soft` | flying clods, plus splashes on wet ground |
| Stone, wood, concrete and so on | a faint scuff |

The block above the contact point is checked first, because shallow water and thin snow have no collision and
the wheel raycast reports the solid block underneath them.

Spray amount scales with wheel speed. Speed is derived from how long the wheel needed to travel from one block
to the next, which costs nothing and needs no access to the physics engine. Spray is also skipped while a wheel
stays inside the same block, so a parked vehicle stays quiet.

## Material carry-over

Leaving mud or soil behind on clean ground: the tread keeps the last material it touched and drops a trail of
falling dust for the next few blocks. Driving through water rinses it clean immediately.

This is particles only. No blocks are placed on the surface you drive over, because smearing real mud onto
somebody's stone road would be griefing rather than flavour.

## Dust veil

Driving fast over dusty ground raises a thick cloud that briefly blinds anything caught inside it, which turns a
chase into a question of distance and racing line: hang directly behind the leader and you drive blind.

Nothing in the physics API says who is riding which vehicle, so own crew protection is a distance heuristic:
entities closer than `dustVeilSelfRadius` to the wheel are treated as being on board and are spared, while
anything further out inside `dustVeilRadius` counts as a follower. Raise the inner radius on very large builds.
Blindness is only refreshed once the previous puff has mostly worn off, so a long dusty straight cannot stack
into permanent blindness. Set `dustVeil = false` to keep the particles and drop the effect.

## Block tags

No surface list is hardcoded. Everything lives in `data/tiretracks/tags/block/` and can be extended or replaced
by any datapack or modpack.

| Tag | Meaning |
| --- | --- |
| `#tiretracks:immune` | never touched by this mod. Empty by default, free blacklist |
| `#tiretracks:turf` | entry point of the rut chain |
| `#tiretracks:soft` | loose soil, throws up clods |
| `#tiretracks:dusty` | dry granular ground, raises dust and can raise a dust veil |
| `#tiretracks:wet` | wet ground: splashes instead of dust, sticks to tyres |
| `#tiretracks:snow` | treated as a snow surface, including the ice a groomed track turns into |
| `#tiretracks:muddyable` | ground the last snow layer may turn into mud |
| `#tiretracks:packable_ground` | ground that may be turned into an ice track |

Blocks with a block entity are always protected, so a chest or a machine is never ground into mud.

## Configuration

`config/tiretracks-common.toml`

### `[general]`

| Option | Default | Meaning |
| --- | --- | --- |
| `lightVehicleMaxMass` | 45.0 | upper mass bound of a light vehicle, in kg |
| `mediumVehicleMaxMass` | 80.0 | upper mass bound of a medium vehicle, in kg |
| `lightChance` | 0.12 | chance per check that a light vehicle wears the ground deeper |
| `mediumChance` | 0.30 | same for medium |
| `heavyChance` | 0.50 | same for heavy |
| `tickInterval` | 4 | physics ticks between terrain checks per wheel |
| `playSounds` | true | crunch when the ground gives way |
| `spawnParticles` | true | master switch for every particle this mod spawns |

### `[ruts]`

| Option | Default | Meaning |
| --- | --- | --- |
| `lightMaxStage` | 1 | deepest stage a light vehicle can reach |
| `mediumMaxStage` | 2 | deepest stage a medium vehicle can reach |
| `heavyMaxStage` | 4 | deepest stage a heavy vehicle can reach |
| `puddles` | true | allow worn wet ruts to fill with water or ice |
| `wetChanceMultiplier` | 1.6 | chance multiplier in rain or next to water |
| `dryChanceMultiplier` | 0.85 | chance multiplier in hot dry biomes |
| `dryBiomeTemperature` | 0.95 | biome base temperature from which ground counts as dry |

### `[snow]`

| Option | Default | Meaning |
| --- | --- | --- |
| `eatSnow` | true | whether wheels affect snow at all |
| `packSnow` | true | pack snow into an ice track instead of removing it |
| `snowToIceChance` | 0.35 | chance per check that a groomed ice track is polished into packed ice |
| `snowToMudChance` | 0.25 | chance the last layer leaves mud, where packing is impossible |

### `[spray]`

| Option | Default | Meaning |
| --- | --- | --- |
| `wheelSpray` | true | surface particles while driving |
| `sprayInterval` | 2 | physics ticks between puffs per wheel |
| `sprayDensity` | 2 | base particle amount per puff |
| `sprayFullSpeed` | 10.0 | speed in blocks per second at which spray is at full strength |

### `[carry]`

| Option | Default | Meaning |
| --- | --- | --- |
| `carryEnabled` | true | drag mud onto clean ground |
| `carryDistance` | 6 | blocks of clean ground that still show a trail |

### `[dust]`

| Option | Default | Meaning |
| --- | --- | --- |
| `dustVeil` | true | whether the cloud blinds nearby entities |
| `dustVeilMinSpeed` | 8.0 | minimum speed in blocks per second to raise a veil |
| `dustVeilRadius` | 10.0 | outer radius of the cloud |
| `dustVeilSelfRadius` | 4.0 | inner radius that is spared, meant to cover your own vehicle |
| `dustVeilDurationTicks` | 30 | blindness duration, 20 ticks is one second |

Upgrading from 3.0 moves `eatSnow` and `snowToMudChance` from `[general]` into `[snow]`. Delete the old config
file to regenerate it cleanly, otherwise the old keys just sit there unused.

## Troubleshooting

1. **Nothing happens at all.** The mixin is optional by design. Check the log for a line about the Sable mass
   API; if the wheel mount class was renamed in a newer Offroad build, the redirect simply never applies.
2. **Every vehicle behaves as medium.** The mass lookup failed, which is the intended fallback. It is logged once
   at startup.
3. **Everything is heavy.** This Sable build may report per wheel mass rather than total vehicle mass. Raise
   `lightVehicleMaxMass` and `mediumVehicleMaxMass` to match what you actually see.
4. **Too destructive.** Lower the chances, cap the stages with `lightMaxStage` and friends, or add blocks to
   `#tiretracks:immune`.
5. **I do not want ice roads.** Set `packSnow = false`, or remove `minecraft:ice` from `#tiretracks:snow` to stop
   the polishing step while keeping the rest.
6. **Few particles at very low speed.** Expected: spray needs the wheel to enter a new block, and density scales
   with speed. Lower `sprayFullSpeed` if you want thick spray while crawling.
7. **My own driver keeps getting blinded.** Raise `dustVeilSelfRadius`, or set `dustVeil = false` to keep the
   cloud purely cosmetic.

## Building

This repository has no Gradle wrapper committed. Use a local Gradle 8.10.2 or newer:

```
gradle clean build
```

The jar lands in `build/libs/TireTracks-3.1.0.jar`. The GitHub Actions workflow `Build TireTracks` does the same
on demand.

`DUST_PLUME` particles require Minecraft 1.20.5 or newer. That is fine on 1.21.1, but a backport below that
version would need a different particle.

## License

MIT.
