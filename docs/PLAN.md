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
| 8 | Look and feel | 26.1-26.3 | `lookfeel` | `lookAndFeel` | none | 40-44 | Backport Core 1.4.4 (pause icon row) |
| 8 | Changes to existing things | 26.1-26.3 | `changes` | `changes` | none | 40-44 | nothing |

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

**Built (2026-11-21, `babies`):** everything above, no patches. Notes:
- The age lock works on every `EntityAgeable` baby (any mod's) except villagers and skeleton and zombie horses; a
  locked baby's age holds at -24000. A locked baby horse still eats food that heals it (1.12.2's `handleEating`
  uses the item), but does not grow.
- Sound variants are chosen when a mob first joins: random for a spawned mob, classic for a bred baby, an egg
  chick and a mob from an older world. Pigs play 26.1's eat sound when fed. Baby cat, chicken, horse, pig and
  wolf sounds included. Saved in the mob's Forge data (`gamedropsbackport:SoundVariant`).
- Woodland mansion chests lose their name tags, as in 26.1.
- Village & Pillage's polar bears never attack foxes, so nothing is needed for the baby rule. Nether Update's
  strider riders already share their mount's warmth.
- Baby models: our own renderers for 1.12.2's chicken, cow, mooshroom, pig, sheep, wolf, ocelot and cat, rabbit,
  horse, donkey, mule, skeleton and zombie horse, llama, polar bear, villager (not with Village & Pillage), zombie,
  husk, zombie villager and pig zombie (not with Nether Update), switch `babies.babyModels`. The rest are for
  their owner mods (`docs/CORE_REQUESTS.md`).

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
- **Built** (stream 3, 0.1.0): `sulfurcube.SulfurCubes` registers it (entity id 1, no packets). The 12 archetypes'
  values are in `Archetype`; `ArchetypeItems` maps 26.3's item tags to 1.12.2's blocks, stream 2's sulfur and
  cinnabar, and other family mods' blocks by name. Spawning: monster category, weight 100, groups of 2-4, any light,
  where the biome is `gamedropsbackport:sulfur_caves` or underground on sulfur, cinnabar or potent sulfur (stream 4
  can rely on either). The `bounce` game event is Core 1.4.4's `GameEvent.BOUNCE` (Core request 2). A head is held as
  26.3 holds one: no archetype tag lists a skull, so none is swallowable, but one given by command (`body_item`) stops
  the cube with no archetype (`Archetype.NONE`: base attributes, the default knockback and sounds) and is drawn by
  26.3's custom head layer (scale 0.842, 0.625 down, 1.1875). Not like vanilla: a dispenser feeds a cube in front of it
  before the item's own behavior, which runs when no cube is there.

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
  condition). Work past 1.12's population window (far spikes, a large spring's outer tuff) waits in Core 1.4.4's
  `DeferredPlacement` until the chunks there are ready; every sulfur cave column is registered with Core's
  `UndergroundBiomes` (F3's biome, grass, foliage and water colors at the block). Deviations and the Caves & Cliffs
  request: `docs/streams/sulfurcaves.md`.

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

**Built (2026-11-21).** `tools/camps/generate.py` copies 26.3's structure set, 18 structures, pools, 297 templates,
biome and structure tags and three loot tables unchanged. `camps.CampLayout` is 26.3's jigsaw placer (expansion hack,
nameless feature jigsaws); `AbandonedCampStructure` picks the variant per candidate chunk as `createStructures` does
(weighted draws, a fresh start random per try, the column's biome checked before the ground). Checked against vanilla
26.3's own starts for seed 1013 (`tools/camps/oracle/`, 24 starts): 62 of 62 pieces match. `/locate structure
minecraft:abandoned_camp_<biome>` and the 23 `#on_*` structure tags are Core's. The 16 explorer map items
(`ExplorerMaps`) are made by the camp loot's `exploration_map`, only where `/locate` finds the structure; old filled
explorer maps (1.12.2's cartographer, the other backports) become 26.3's items in a player's inventory, an opened
container or a villager's offers (26.3's `ExplorerMapItemFix`, live). Self-test `camps`.

Where it differs from 26.3:
- Heights are 1.12.2's terrain's (the start is projected onto it); biomes are 1.12.2's per-column biome (Caves &
  Cliffs' 3D noise biome where its terrain runs). A camp exists only where its biome's owner is installed.
- Trees: 1.12.2's generators for the vanilla trees; cherry, pale oak and poplar grow from their owner's sapling (a
  poplar's color is the sapling's). Bee nests on the `_bees` trees are left out.
- Blocks no installed mod has are left out (Core's palette); barrels and copper chests fall back to chests (loot
  kept), white wool stairs to white wool, cushions are left out without the soft blocks stream.
- Container loot has no position in 1.12.2: the opening player's block is the origin (biome check, map search).
  Maps search Core's 100 rings, not 26.3's 50, and do not skip structures already on a map.
- Explorer maps cannot be copied in a crafting grid or cartography table (1.12.2's recipe takes only filled maps).

### 8. Look and feel (26.1-26.3; needs Backport Core)

Backport Core's `presentation` module owns every single-slot piece (panorama, splash list, menu music, pause and
options layouts, loading screen) and shows the newest installed version's. This mod registers 26.1, 26.2 and 26.3,
so it is the newest backport and its pieces win over Tricky Trials' 1.21.11. Core's "Newest Look" can still pick an
older version, and Core's Look & Feel page switches each area off.

The versions are registered whatever the switches say. Like every other backport's, they also reach Core's content
code (`Presentation.installedNewest`): with this mod alone, worlds now generate with Core's modern biome rules (1.13+
biome point, layer rules) instead of 1.12.2's, as they already did with any other backport installed.

Survey: the 1.21.11, 26.1, 26.1.2, 26.2 and 26.3 client jars and asset indexes (GUI textures, lang, splashes,
`sounds.json`, decompiled `PauseScreen`, `WorldOptionsScreen`, `OptionsScreen`, `SignEditScreen`) and the wiki's
version pages. The jars' panorama faces are 69-byte placeholders; the real ones are in the asset index.

Built here (closest to 26.3). Config category `lookAndFeel`: `enabled` and one switch per row group (panorama,
splashes, menuMusic, pauseMenu, worldOptions), all on; Core's Look & Feel page lists them under their sections.

| Version | Change | Needs from Core |
| --- | --- | --- |
| 26.1 | The version only: its one splash change ("Java 16 + 1 + 4 * 2 = 25!") names a Java version, which Core leaves out on 1.12.2 | nothing |
| 26.2 | Splash "Music by fingerspit!" ("Now on Vulkan 1.2!" is shipped, but Core leaves out runtime splashes on 1.12.2) | splash pool |
| 26.2 | Menu music: fingerspit's Ebb, Home, Memories, Nightly and Shores replace 1.21.6's five (weight 1, volume 0.4); Music Toast names "fingerspit - Ebb" ... | menu pool, removal by path (has it) |
| 26.2 | Pause menu: Report Bugs and Send Feedback become 20 by 20 icon buttons in one row under Advancements and Statistics (sprites `pause_menu/bug`, `pause_menu/social_interactions`) | **new**: the 26.2 pause layout (Core 1.4.4) |
| 26.3 | Panorama: the dappled forest with an abandoned camp, and its overlay | panorama (has it) |
| 26.3 | Splash fix: straight quotes in "Don't touch your face!" and "Prepare, but don't hoard!" | splash pool |
| 26.3 | Pause menu: World Options... in place of Open to LAN. General: Default Game Mode, Personal Game Mode, Allow Commands, Difficulty and its lock. Multiplayer: LAN, Port, Command Access, Force Game Mode. Apply Changes / Cancel | the pause slot (Core places button 7) |
| 26.3 | Options: Difficulty and its lock leave the in-world Options screen (moved to World Options in 26.1, the World Options button left Options in 26.3) | nothing |
| 26.1-26.3 | Vanilla text as 26.3 words it (1.12.2 keys only): Enter the Nether's description, Isn't It Iron Pick?, the height limit, the language warning, the saves folder error | vanilla text (has it) |

Not built, and why:

- Online services: Friends list, button, toasts and options (26.2), Graphics API / Vulkan (26.2), Realms invite
  codes and buttons (26.3), chat restrictions screen (26.1), player reporting. 1.12.2 has none of them.
- World upgrade screens (26.1), Game Rules search (26.1; 1.12.2 has no game rules screen). World Options has no
  Game Rules or Restrictions button for the same reason.
- LAN can't be closed again once open: 1.12.2's server has no way to stop listening. The LAN switch stays on.
- Exclusive Fullscreen (26.1) and Borderless as the default (26.3): built in Core 1.4.4 (P42, Core's own option).
- Quit Shortcuts (26.3, on by default: 1.12.2 already quits on Alt+F4), Right Click Emulation and Menu/Dock
  Visibility (macOS only), the minimum window size: low value; left out.
- F3: the overlay's own GUI scale (26.3), `player_speed`, day count split (26.1), AM/AX counts (26.2): built in Core
  1.4.4 (`ModernDebugLines`, `DebugScale`).
- Options headings bold, underlined and centered (26.3): Core's option lists; a Core request.
- Sign edit screen drawn from flat `gui/signs/<wood>.png` (26.2): Trails & Tales owns the sign edit screen; a
  Trails & Tales request.
- Creative tab order (26.3's mineral order): not built. 1.12.2's tabs are 1.19.2's, in registry order, with the
  minerals scattered; 26.3 reorders runs inside its own tabs that 1.12.2 lacks (Core's `docs/PRESENTATION.md`).
- GUI textures: none changed that 1.12.2 draws (26.3's enchanting table differs by 4 unused pixels).
- In-game music: the fingerspit tracks also join the overworld biome pools (26.2); in-game music is Caves & Cliffs'
  stream with Core's game music.
- Loading screen: unchanged since 1.21.9 (Tricky Trials' drawer stays).
### 8. Changes to existing things (26.1-26.3)

26.1 to 26.3's changes to commands, game rules, mobs, blocks, items and world rules 1.12.2 already has, one setting
each under `changes` (Tricky Trials' stream 25 is the model). The full list, with what 1.12.2 does and what other
family mods already change, is `docs/streams/changes.md`; patches P1-P5 are in `docs/PATCHES.md`. Port 25928 (row 8).

| Change | Version | Setting | 1.12.2 now | Other family mods |
| --- | --- | --- | --- | --- |
| `/time` on world clocks: `set day/noon/night/midnight` go forward to the next marker; `of <clock>`, `pause`, `resume`, `rate`, `query time`, timelines; 26.1's messages | 26.1 | `timeCommand` | `set day` jumps back to tick 1000; no clocks | none (Core replaces only `/locate`, `/seed`) |
| `/gamerule` messages; setting the same value is an error | 26.3 | `gameRuleMessages` | "Game rule %s has been updated to %s" | Tricky Trials adds 1.21's rules, not the command |
| Team color names only as `dark_purple` | 26.2 | `teamColorNames` | `darkpurple`, `DarkPurple` accepted | none |
| Bed bounce 0.75 | 26.2 | `bedBounce` | 0.66 | none (the shelf mushroom has its own) |
| Mushroom stew from any two of `#mushrooms` | 26.3 | `mushroomStew` | one brown, one red | the poplar stream's shelf mushroom joins the tag |
| Shield before hoe and shovel | 26.3 | `shieldBeforeTools` | the tool is used | Tricky Trials does the axe (1.21's same rule) |
| Riding endermen take projectile damage, never teleport | 26.3 | `endermenRiding` | they dodge and teleport | none |
| Endermen avoid bedrock and dangerous blocks; shulkers bedrock | 26.3 | `teleportTargets` | any solid block | none (Trails & Tales' `EnderTeleportEvent` is for pearls) |
| Persistent mobs stop wandering with no player near | 26.3 | `persistentMobsIdle` | always wander | Nether Update's P10 guards the same method's head, for riders |
| Leader zombies spawn at full raised health | 26.1 | `leaderZombieHealth` | health stays 20 | none |
| Witches throw at 0.45 within 2 blocks | 26.1 | `witchCloseThrows` | always 0.75 | none |
| Dispensers keep monster spawn eggs on Peaceful | 26.2 | `peacefulSpawnEggs` | egg used up | none |
| Unbreaking on a helmet worn against the sun | 26.3 | `sunDamageEnchantments` | ignored | Update Aquatic's drowned and Tricky Trials' bogged are their owners' |
| Spectators use nether portals, end portals, end gateways | 26.3 | `spectatorPortals` | they pass through | none |

**Built (2026-11-21):** all of the above. Self-test `changes`. Not built, with reasons: `docs/streams/changes.md`.

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
- `deps/` holds Backport Core's dev jar (not committed): copy `BackportCore/build/libs/BackportCore-<core_version>-dev.jar`.
- Test: `./gradlew runServer -Pselftest=<stream>` (port in `eclipse/server.properties`), then
  `python -I tools/lang/build_langs.py check` with 0 errors, then `./gradlew build` last, then
  `tools/release/boot-server.sh G:/VBPTest/release-boot <log> build/libs/GameDropsBackport-<version>.jar <Core jar>`.
  Check free RAM first (8 GB or wait). Never `cleanRunServer`. Kill only our own server, by PID.

## Order

1. Scaffold and this plan: **done** (0.1.0).
2. The streams in release order: babies, sulfur, sulfur cube, sulfur caves, poplar, soft blocks.
3. Abandoned camps and explorer maps once Backport Core has the template loader.
4. Changes to existing things (stream 8): **built**.
5. **1.0.0 release checks (2026-11-21)**, all on the E-cores:
   - `tools/release_suite.sh`: 1697/0 alone; with `FAMILY=1` (Village & Pillage 2.7.2, Update Aquatic 1.2.1, Caves &
     Cliffs 1.1.2, Wild Update 1.1.1, Trails & Tales 1.0.1, Tricky Trials 1.0.1 dev jars) no crash, id clash or
     duplicate. Found there and fixed: the dappled forest never generated with Village & Pillage (its 1.14 layers are
     rebuilt at the same priority) or behind Update Aquatic's, the cherry grove's and the pale garden's wrappers;
     `DappledForestLayer` now runs last and walks every `GenLayer` field. Three test assumptions fixed (V&P's ocelot
     sounds, Caves & Cliffs' local mob cap canceling the spawn event, the version).
   - `runClient -Pselftest=client` (`ClientScenes`): every block state and item model, every item drawn, every block
     placed, the babies' renderers, sulfur cube, cushion, poplar boat, the sign edit screen, straw bed sleep, cushion
     seat, then the look and feel screens: 66/0. Found and fixed: the big shelf mushroom drew as the missing model; the
     stew recipe broke vanilla's recipe advancement.
   - ScanAll (family-notes/tools/verify), CLIENT and SERVER: all six P1-P5 targets verify vanilla and patched.
   - `tools/release/boot-server.sh` with Backport Core 1.4.4's release jar, with and without Fluidlogged API 3.3.3:
     PASS, PATCHCHECK ok 6. Release jar: 0 `/selftest/` entries.
