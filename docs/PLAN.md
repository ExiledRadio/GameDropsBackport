# Game Drops Backport: plan

Everything Java Edition's 26.x game drops added or changed: 26.1 (Tiny Takeover), 26.2, 26.3 and each later 26.x
release as it ships. Mojang stopped making big named updates after 1.21 and now ships "game drops"; the drop names
are this mod's sections. The user split them out of Tricky Trials Backport into this mod on 2026-11-17 (Tricky
Trials keeps 1.21 and the 1.21.2-1.21.11 drops). None of these streams had been built there, so nothing moves in
worlds. Mod id `gamedropsbackport` (permanent); the display name lives only in `gradle.properties` (`mod_name`).

Sources: minecraft.wiki's *Java Edition 26.1*, *26.2* and *26.3* pages, current Java behavior winning, and the client
jars themselves (`G:\MCDownloads\minecraft-<version>\client.jar`: 26.1, 26.1.2, 26.2, 26.3). What each drop added
was found by diffing the English language files and blockstate folders of 1.21.11 and each 26.x release (Tricky
Trials' plan did it, 2026-11). Assets and names come from 26.3, the newest release (`tools/assets.py`).

Content counts as 26.x when it first shipped **outside experiments** in a 26.x release.

## Scope at a glance

| In | Out (and why) |
| --- | --- |
| Every new block, item, mob, biome and structure of 26.x, and their changes to existing things that players see | Data-driven and technical changes (components, commands-only content, test blocks, protocol changes) |

## Content owned by another mod of the family (Rule 1) and its integrations (Rule 2)

| Needed here | Owner | What this mod does |
| --- | --- | --- |
| Copper (note block trumpet on copper) | Caves & Cliffs; Tricky Trials' copper blocks | Works with whichever copper is installed; nothing without one. |
| Foxes (baby polar bears leave them alone), the wandering trader | Village & Pillage | Rule 2: only with Village & Pillage; the trader's golden dandelion and name tag trades through Core's `TradeRegistry`. |
| Striders (inherit warmth) | Nether Update | Rule 2: only with Nether Update. |
| Chest boats (poplar chest boat) | Wild Update | Only with Wild Update. |
| Hanging signs (poplar hanging sign) | Trails & Tales | Only with Trails & Tales. |
| Shelves (poplar shelf) | Tricky Trials (stream 21 there) | Only with Tricky Trials. |
| Vibrations (wool damping) | Wild Update | Rule 2: only with Wild Update. |
| Explorer map targets: ancient city, warm ocean ruins, trial chambers, villages | Wild Update, Update Aquatic, Tricky Trials, Village & Pillage | A map exists only where its structure exists. |
| Trident (breaks sulfur spikes) | Update Aquatic | Rule 2: only with Update Aquatic. |

When a feature needs a block or item another family mod owns and the owner is absent, this mod registers the
identical copy (Rule 1), as Update Aquatic's `woodset` does.

## Streams

Each stream owns one package under `com.exiledradio.gamedropsbackport`, one config category with an `enabled`
switch (already in `ModConfig`, empty), one block of packet ids and one block of entity ids. No stream calls
another's classes.

| # | Stream | Version | Package | Config | Entity ids | Packet ids | Needs |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Golden dandelion and babies | 26.1 | `babies` | `babies` | none | 5-9 | nothing |
| 2 | Sulfur and cinnabar blocks | 26.2 | `sulfur` | `sulfur` | none | 10-14 | nothing |
| 3 | Sulfur cube | 26.2 | `sulfurcube` | `sulfurCube` | 1-5 | 15-19 | sulfur blocks by name |
| 4 | Sulfur caves and springs | 26.2 | `sulfurcaves` | `sulfurCaves` | none | 20-24 | decision 2 (heights) |
| 5 | Poplar and the dappled forest | 26.3 | `poplar` | `poplar` | 6-10 | 25-29 | decision 1 for the biome only |
| 6 | Wool and concrete shapes, cushions, straw beds | 26.3 | `softblocks` | `softBlocks` | 11-15 | 30-34 | nothing |
| 7 | Abandoned camps and explorer maps | 26.3 | `camps` | `camps` | none | 35-39 | Backport Core (templates, terrain) |

Ports (block 25920-25939): the main checkout's dev server uses 25920; a stream worktree uses `25920 + row`
(25921-25927). Later 26.x streams take the next rows, up to 25939.

### 1. Golden dandelion and babies (26.1, Tiny Takeover)

- **Golden dandelion** (a dandelion and 8 gold nuggets; potted form): used on a baby mob it stops it growing
  up, used again it lets it grow; yellow dye; a Saturation suspicious stew; sold by the wandering trader
  (Core's `TradeRegistry`, with Village & Pillage).
- Name tags craftable from paper and any nugget; the wandering trader's name tag for 1 emerald.
- 26.1's adult sound variants (royal cat, mini and big pig, moody cow, picky chicken), chosen per animal.
- Skeleton and zombie horses no longer panic when hurt and their babies never grow up; baby polar bears
  leave foxes alone (foxes are Village & Pillage's); striders inherit warmth (Nether Update's).
- The note block's trumpet on copper (by oxidation stage), with Tricky Trials' and Caves & Cliffs' copper.
- **Baby models and textures**: 26.1 redrew nearly every baby mob. New models need renderers, not just
  textures: listed for Backport Core's modern pack where a texture is enough (a Core request when this stream is
  built), and as our own renderers for vanilla's babies behind a switch.

### 2. Sulfur and cinnabar blocks (26.2)

- **Sulfur** and **cinnabar**, each with polished, bricks and chiseled forms and their stairs, slabs and walls
  (28 blocks, Tricky Trials' tuff shapes); **potent sulfur** (9 sulfur; bubbles, nausea gas over 1-4 water,
  eruptions over magma every 50 s, constantly over lava); **sulfur spikes** (stalactite and stalagmite,
  breaking unsupported, falling for damage, broken by thrown tridents; 4 make a sulfur block).
- Music disc **Bounce** (mineshaft minecart chests; comparator 8).
- **Built** (stream 2, 0.1.0): every block above under 26.3's names (`SulfurContent` lists them). The disc is
  registered; its loot (abandoned mineshaft chests, weight 10 in the 3-roll rail pool, only inside sulfur caves)
  needs the biome, so stream 4 adds it.

### 3. Sulfur cube (26.2)

- The **sulfur cube**: 8 health, hops like a slime, absorbs the block it touches (its AI stops until it is
  sheared), immune to fall, player and explosion damage while holding a block, splits in two on death; 12
  archetypes by the block held (bouncy, sticky, hot, explosive with TNT: a 6 s fuse from fire or redstone,
  0.75-3 s from an explosion, ...). Large cubes can be bucketed; small ones grow on slimeballs.
  Advancement *Uh Oh*; the hot cube's death message.

### 4. Sulfur caves and springs (26.2)

- The **sulfur caves** biome (bands of sulfur and cinnabar, sulfur pools with potent sulfur, cave spiders
  instead of spiders) and **sulfur springs** above it in four sizes. 1.12.2 has no underground biomes:
  decision 2 below.
- **Built** (stream 4): `sulfurcaves.SulfurCaves` says where (Caves & Cliffs' 1.18 terrain: 26.2's climate box
  over its climate noise at real heights, its lush caves, dripstone caves and deep dark kept; other worlds: our
  own patches under low land, as rare and as large as 26.2's, at 26.2's depth, two thirds of it in a 0-255 world;
  superflat: the preset's biome). `SulfurCavesGen` lays the bands, then 26.3's `rooted_sulfur_spring` (root system
  and the ten spring templates), `sulfur_pool`, `sulfur_spike_cluster` and `sulfur_spike` with their placed-feature
  counts. The biome's spawns (sulfur cube once stream 3 registers `sulfur_cube`), music and fog color; a tuff copy
  where no family mod has tuff. Bounce is in every mineshaft chest's rail pool (26.3's table has no biome
  condition). Deviations and the Caves & Cliffs request: `docs/streams/sulfurcaves.md`.

### 5. Poplar and the dappled forest (26.3)

- **Poplar** set: log, wood, stripped log and wood, planks, stairs, slab, fence, gate, door, trapdoor,
  button, pressure plate, sign, sapling (red, orange or yellow leaves), potted sapling, boat and chest
  boat (chest boats with Wild Update), hanging sign (with Trails & Tales), shelf (with Tricky Trials).
- **Red shrub** (spread by bone meal), **shelf mushroom** (two sizes, bounces like a bed), fallen poplars.
- The **dappled forest** biome near cold biomes: decision 1 below.

### 6. Wool and concrete shapes, cushions and straw beds (26.3)

- Wool stairs and slabs and concrete stairs and slabs in 16 colors (64 blocks plus double slabs; concrete is
  1.12.2's; Village & Pillage's stonecutter cuts concrete).
- **Cushions** (16 colors, from wool slabs): a seat that does not collide and breaks with its support.
- **Straw bed** (3 hay bales make 4): sleep through the night without setting your spawn; used up after one
  night; villagers ignore it; never in the Nether or End.
- Wool damping sound (Wild Update's vibrations, Rule 2).

### 7. Abandoned camps and explorer maps (26.3; needs Backport Core)

- **Abandoned camps** in 18 biomes, with their variants and loot.
- 26.3's **explorer maps**: abandoned camp, ancient city (Wild Update's), mineshaft, desert and jungle
  pyramids, the five villages, ocean monument, woodland mansion, swamp hut, warm ocean ruins (Update
  Aquatic's), trial chambers (Tricky Trials'), and buried variants, wherever their structure exists.
- Needs Core's template loader, as Tricky Trials' trial chambers do.

### Later 26.x

Each later 26.x release joins this plan as its own streams when it ships (26.4 is in snapshots now), with the next
config category, id blocks and port.

## Decisions (made for Tricky Trials, 2026-11-07; they carry over)

1. **New biomes are placed where vanilla places them** (Tricky Trials' open question #2): a real biome in its
   vanilla climate, in new chunks. The dappled forest goes where vanilla puts it, near cold biomes, as the pale
   garden does in the dark forest's climate.
2. **Sulfur caves' heights** (Tricky Trials' open question #7): vanilla's heights when Caves & Cliffs' extended
   height is installed (y -64 to 319, through Core's `WorldHeights`), the proportional 0-255 placement otherwise,
   the same pattern as trial chambers; our own underground regions under any biome, since 1.12.2 has no
   underground biomes.
3. **The mod** (the user, 2026-11-17): the 26.x drops are their own family mod, Game Drops Backport, not part of
   Tricky Trials.

**Standing rule (the user, 2026-11-07):** build whatever is closest to vanilla; patches are fine where vanilla's
behavior needs them. Design questions vanilla already answers are not asked.

## Shared files and rules

- **`ModConfig.java`**: one category per stream with an `enabled` switch; a stream adds fields only between its
  own markers. Category classes extend `Object`.
- **`GameDropsBackport.java`**: at most one line per lifecycle hook, calling your package.
- **`asm/`**: one transformer per patch, each in `docs/PATCHES.md`, its `TARGETS` added to
  `GameDropsCore.serverTargets()` or `clientTargets()` so `PatchCheck` verifies them on a real server.
- **`selftest/SelfTestRunner.java`**: one line in `TESTS`.
- **`en_us.lang`**: every user-facing string is a key; vanilla names through `tools/assets.py`'s `name(...)`;
  config tooltips from `python -I tools/lang/build_langs.py config`; the 18 languages from `build_langs.py build`,
  our own text translated in `tools/lang/translations/`.
- **Registration**: from your own `@Mod.EventBusSubscriber` class.

## Build and test

- `./gradlew build` never deploys unless `gdbDeployDir` is set in `~/.gradle/gradle.properties`.
- `deps/` holds Backport Core's dev jar (not committed): copy `BackportCore/build/libs/BackportCore-1.4.0-dev.jar`.
- Test: `./gradlew runServer -Pselftest=<stream>` (port in `eclipse/server.properties`), then
  `python -I tools/lang/build_langs.py check` with 0 errors, then `./gradlew build` last, then
  `tools/release/boot-server.sh G:/VBPTest/release-boot <log> build/libs/GameDropsBackport-<version>.jar <Core jar>`.
  Check free RAM first (8 GB or wait). Never `cleanRunServer`. Kill only our own server, by PID.

## Order

1. Scaffold and this plan: **done** (0.1.0).
2. The streams in release order: babies, sulfur, sulfur cube, sulfur caves, poplar, soft blocks.
3. Abandoned camps and explorer maps once Backport Core has the template loader.
