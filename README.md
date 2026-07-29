# TireTracks

TireTracks makes Create Aeronautics vehicles (Offroad / Sable / Simulated) leave real marks on the world.
Ground does not just change once and stay that way: it wears down stage by stage into a rut, snow is packed
into an ice road instead of vanishing, wheels throw up surface appropriate spray, and mud gets dragged onto
clean ground for a few blocks. **Abandoned roads slowly heal**: mud heals back to grass during rain, but
actively used tracks stay as they are.

- Minecraft 1.21.1, NeoForge 21.1.235, Java 21
- Version 3.1.0
- Server side logic only. No client mod required, no packets, no new blocks or items.
- Silent, and no status effect is ever applied to a player: this mod only places blocks and spawns particles.

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

How fast the ground gives way, and how deep it can go, depends on the vehicle's mass class.

| Class | Mass | Chance per check | Deepest stage |
| --- | --- | --- | --- |
| Light | 0-45 kg | 0.12 | 2, coarse dirt |
| Medium | 46-80 kg | 0.30 | 3, loose fill |
| Heavy | above 80 kg | 0.50 | 4, puddle |

Mass is read straight off the Sable sub level, in kilograms: it is the same number the in game sub level debug
dump prints as `Mass:`. Both bounds are inclusive, so exactly 45 kg is light and exactly 80 kg is medium. If
this Sable build does not expose a readable value, every vehicle falls back to the medium profile.

The stage caps are tuned for vehicles that actually get built, which usually weigh 40-80 kg. Capping the medium
class at coarse dirt would mean almost nothing in the game ever digs a real rut. Raise or lower them freely with
`lightMaxStage`, `mediumMaxStage` and `heavyMaxStage`.

Sand and gravel are gravity blocks, so they are only placed when the block below is solid. Otherwise the rut
would punch a hole through the terrain instead of staying a rut.

## Sand and stone

### Sand (sand, red sand)

- **Light vehicles** leave no trace.
- **Medium vehicles** pack the sand down into sandstone or red sandstone, which looks like a beaten path but does
  not stand out too much against the desert.
- **Heavy vehicles** sink the sand entirely, leaving a depression in the terrain.

A heavy vehicle can only sink sand when there is solid ground below, otherwise it would cascade down through the
terrain.

### Stone

- **Light vehicles** leave no trace.
- **Medium and heavy vehicles** crack stone into cobblestone or andesite, chosen at random. The mix keeps the
  track from looking artificial.

## Road healing

Abandoned roads slowly return to nature. Actively used roads — anything touched by wheels recently — stay as
they are. Time is measured in **Minecraft days**: 1 day = 24000 ticks = 20 minutes real time.

### Mud heals back to grass

Mud, coarse dirt and dirt paths can heal back toward grass **during rain**, if they have not been touched by
wheels for **3 Minecraft days** (1 hour real time). The chance per check is 60%, so healing is reasonably quick
once the timer expires. Healed blocks turn into grass if there is grass nearby (within 3 blocks), otherwise into
plain dirt.

Wheels reset the timer, so an active road never heals. This means your main routes stay as roads, while
abandoned side paths slowly vanish.

Healing timers are in-memory and reset when a chunk unloads. This is intentional: the mechanic is about ongoing
activity, not forensic history. A chunk that stays loaded will heal as expected; a chunk that is only loaded
briefly will take longer.

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

## Block tags

No surface list is hardcoded. Everything lives in `data/tiretracks/tags/block/` and can be extended or replaced
by any datapack or modpack.

| Tag | Meaning |
| --- | --- |
| `#tiretracks:immune` | never touched by this mod. Empty by default, free blacklist |
| `#tiretracks:turf` | entry point of the rut chain |
| `#tiretracks:soft` | loose soil, throws up clods |
| `#tiretracks:dusty` | dry granular ground, raises a dust plume |
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
| `spawnParticles` | true | master switch for every particle this mod spawns |

### `[ruts]`

| Option | Default | Meaning |
| --- | --- | --- |
| `lightMaxStage` | 2 | deepest stage a light vehicle can reach |
| `mediumMaxStage` | 3 | deepest stage a medium vehicle can reach |
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

### `[healing]`

**Time is measured in Minecraft days. 1 day = 24000 ticks = 20 minutes real time.**

| Option | Default | Meaning |
| --- | --- | --- |
| `mudHeals` | true | mud/coarse dirt/paths heal back to grass during rain |
| `mudHealChance` | 0.6 | chance per check that an untouched rut heals one stage (60%) |
| `mudHealDays` | 3.0 | Minecraft days a rut must be untouched before it can heal (1 hour real time) |

Upgrading from 3.0 moves `eatSnow` and `snowToMudChance` from `[general]` into `[snow]`, and drops `playSounds`
along with the whole `[dust]` section. Delete the old config file to regenerate it cleanly, otherwise the
removed keys just sit there unused.

## Troubleshooting

1. **Nothing happens at all.** The mixin is optional by design. Check the log for a line about the Sable sub
   level API; if the wheel mount class was renamed in a newer Offroad build, the redirect simply never applies.
2. **Every vehicle behaves as medium.** The mass lookup failed, which is the intended fallback. It is logged once
   at startup.
3. **The rut stops at a certain block.** That is the stage cap for that mass class. Compare the vehicle's mass
   from the sub level debug dump against `lightVehicleMaxMass` / `mediumVehicleMaxMass`, then raise the matching
   `*MaxStage`.
4. **Too destructive.** Lower the chances, cap the stages, or add blocks to `#tiretracks:immune`.
5. **I do not want ice roads.** Set `packSnow = false`, or remove `minecraft:ice` from `#tiretracks:snow` to stop
   the polishing step while keeping the rest.
6. **Few particles at very low speed.** Expected: spray needs the wheel to enter a new block, and density scales
   with speed. Lower `sprayFullSpeed` if you want thick spray while crawling.
7. **Sand should not sink / stone should not crack.** Add `minecraft:sand`, `minecraft:red_sand`, or `minecraft:stone`
   to `#tiretracks:immune`.
8. **Roads heal too fast / too slow.** Adjust `mudHealDays` and `mudHealChance`. Set `mudHeals = false` to disable
   healing entirely.

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
