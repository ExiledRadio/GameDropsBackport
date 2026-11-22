# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.3) or from another family mod through Core.
Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

## Su-1. Jukebox comparator signal for discs added after 1.12.2 (stream 2)

**Done** (branch `gd-hookups`): Backport Core 1.4.4's patch P41 (`mechanics.JukeboxSignal`) answers with `comparatorSignal()` when the disc's class has it; Bounce gives 8. Tricky Trials (Decor-1) and Caves & Cliffs (Mu-1) only need the same method on their discs.

1.12.2's `BlockJukebox.getComparatorInputOverride` counts the signal from the disc's item id, right for vanilla's
twelve discs only; Bounce gives a number far above 15. Tricky Trials (Decor-1) and Caves & Cliffs (Mu-1) ask for the
same. **Want:** Core's jukebox patch answering with the disc's own signal when the record has one, read by name:
`int comparatorSignal()` on the `ItemRecord` subclass (`sulfur.ItemSulfurDisc` returns 26.3's 8).
**Meanwhile:** the disc plays, names and stacks as vanilla's; only the comparator signal is 1.12.2's.
## 1. Add blocks to `#dampens_vibrations` (stream 6, soft blocks)

**Done** (`gd-hookups`): Core 1.4.4's `GameEventTags.addDampener(Block)`; `SoftBlocks.init` calls it directly. Wild Update's sensors read Core's tags, so nothing changes there.

26.3 adds `#wool_stairs` and `#wool_slabs` to `#dampens_vibrations` (blocks and items). Core 1.4.3's
`gameevent.GameEventTags.dampens(IBlockState)` and `dampens(Entity)` hard-code wool and carpet, with no way to add.

Wanted: `GameEventTags.addDampener(Block)`, counting the block in both `dampens(IBlockState)` and `dampens(Entity)`
(a dropped item of that block), as `addResonator` does for resonators.

Meanwhile: `softblocks.SoftBlocks.init` calls `addDampener` by reflection when it exists, so wool stairs and slabs
start dampening once Core has it; until then they do not.
## Stream 1 (babies)

**Item 4 partly done** (`gd-hookups`): Wild Update 1.1.2 leaves the name tag out of ancient city chests when this mod is installed. The master librarian (Village & Pillage) is not changed: V&P already replaces the name tag with candles where a candle mod exists, and what a 26.x master librarian sells without one is V&P's call.

**Item 2 done, item 1 done for Trails & Tales** (`gd-babies`): Core 1.4.4's `mechanics.AgeLockable` (`isBaby`,
`getAge`/`setAge`, `startAge`; the lock is `AgeLocked` in the mob's Forge data, `AgeLockable.isLocked`) and
`VanillaRevisions.babyModels()` (26.1 installed, Core's textures switch on, and this mod's `babies.babyModels`, handed
over by `BabiesContent.preInit` through `setBabyModelSwitch`). `babies.AgeLock` holds any `AgeLockable` as it holds an
`EntityAgeable`, and `BabyWrapper` asks `babyModels()` too. Wild Update 1.1.3's tadpole locks (start age 0, no
slimeballs while locked, the bucket keeps `AgeLocked`); Trails & Tales 1.0.3's snifflet starts again at -48000, and
its camel, sniffer and armadillo draw 26.3's baby models (`BabyCamelModel`, the `sniffer_baby` layer, which 26.3 draws
with the adult's `SnifferModel` animations, and `BabyArmadilloModel`), animations (`tools/babies/animations.py`) and
`*_baby.png`/`snifflet.png` textures while `babyModels()` is true. The armadillo is Trails & Tales', not Tricky
Trials'. Items 1 (Village & Pillage, Buzzy Bees, Update Aquatic, Nether Update, Caves & Cliffs) and 3 are still open.

1. **Baby models of other family mods' mobs.** 26.1 redrew every baby but the nautilus and the ghastling, each with
   a new model, not only a texture, so none can go into Core's modern pack: each owner needs its own baby renderer
   (26.3's `Baby*Model` classes and `*_baby.png` textures). Owners: Village & Pillage (fox, cat, panda, its
   villagers' biome overlays), Buzzy Bees (bee), Update Aquatic (dolphin, turtle), Nether Update (strider, hoglin,
   zoglin, piglin, zombified piglin), Caves & Cliffs (goat, axolotl, glow squid), Trails & Tales (camel, sniffer,
   armadillo: done). Meanwhile: this mod draws only vanilla 1.12.2's babies and the owners' that are done.
2. **Age lock for babies that are not `EntityAgeable`.** The golden dandelion works on every `EntityAgeable` baby,
   any mod's. 26.1 also locks tadpoles (Wild Update) and gives snifflets a 48000-tick start age (Trails & Tales).
   Proposal: Core hosts a small interface (`isBaby`, `getAge`/`setAge`, `startAge`) that owners implement, and this
   mod's `babies.AgeLock` uses it. Done (see above).
3. **Piglins and the golden dandelion** (Nether Update): 26.3 lists it in `piglin_loved`; piglins inspect it and take
   it. Needs a way for another mod to add to Nether Update's piglin-loved list. Meanwhile: piglins ignore it.
4. **Name tags out of the master librarian's trades** (Village & Pillage) and **out of ancient city chests** (Wild
   Update): 26.1 removed both. Owners' tables. Meanwhile: unchanged. (The woodland mansion, 1.12.2's own, is done here.)
## 1. Poplar hanging sign (Trails & Tales) — stream 5

**Done** (`gd-hookups`): Trails & Tales 1.0.2, `HangingWood.POPLAR` (recipe from `gamedropsbackport:stripped_poplar_log`); its entity texture is laid out from 26.3's by `tools/hangingsign/poplar.py`, which maps the two layouts from vanilla's own woods.

**Want:** Trails & Tales' `hangingsign.HangingWood` to gain `POPLAR("poplar", "hanging_sign", "gamedropsbackport", -1)`,
its recipe taking `gamedropsbackport:stripped_poplar_log` (26.3: `X X / ### / ###`, X iron chain, 6 signs), present
when `gamedropsbackport:poplar_planks` is registered. Trails & Tales adds hanging signs for every family wood itself
(it already does mangrove, crimson, warped), so this mod registers none.

**Note:** 26.3 draws signs and hanging signs as block models from a 32x32 `block/poplar_hanging_sign.png` (plus
`gui/hanging_signs/poplar.png`); the old 64x32 entity layout is gone. This mod rearranges its standing sign's texture
into the old layout in `tools/poplar/generate.py` (`sign_texture`); the hanging sign needs the same treatment.

**Meanwhile:** no poplar hanging sign.

## 2. Poplar shelf (Tricky Trials) — stream 5

**Done** (`gd-hookups`): Tricky Trials 1.0.1 (unreleased), `ShelfWood.POPLAR` (`tools/shelf/poplar.py`).

**Want:** Tricky Trials' `shelf.ShelfWood` to gain
`POPLAR("poplar", "gamedropsbackport", "poplar_planks", "stripped_poplar_log", MapColor.SILVER, true)`
(26.3: `COLOR_LIGHT_GRAY`, flammable 30/20, the usual `### / / ###` of stripped logs, 6 shelves).

**Meanwhile:** no poplar shelf.

## 3. Leaf litter in the dappled forest (Tricky Trials, Core) — stream 5

**Done** (`gd-hookups`): Tricky Trials' `FloraClient.dryFoliage` gives `#8C3A04` for the dappled forest; Core's `BlockOwnership` lists `leaf_litter` under `trickytrialsbackport`. Decision: no Rule 1 copy here. It would mean copying four Tricky Trials classes, a smelting recipe and a color map for a ground cover; without Tricky Trials the poplars drop no litter, which costs nothing else.

**Want:**
- Tricky Trials' `FloraClient.dryFoliage` to return `#8C3A04` for `gamedropsbackport:dappled_forest` (26.3's
  `dry_foliage_color`), as it does for the pale garden.
- `leaf_litter` in `BlockOwnership`'s static table under `trickytrialsbackport`, so a copy here could be remapped.
- A decision: should this mod register a Rule 1 copy of leaf litter when Tricky Trials is absent? It needs Tricky
  Trials' `BlockSegmented`, `BlockVegetation`, `ItemSegmented`, the leaves-to-litter smelting recipe and the dry
  foliage color map.

**Meanwhile:** the dappled forest's poplars drop Tricky Trials' leaf litter when it is installed, and none without it.

## 4. The dappled forest on Caves & Cliffs' 1.18 terrain — stream 5

**Want:** Caves & Cliffs' 1.18 biome source (as its `Editions.PALE` does for Tricky Trials' pale garden) to place
`gamedropsbackport:dappled_forest` where 26.3 does: `OverworldBiomeBuilder.MIDDLE_BIOMES_VARIANT[1][0]` (cool band,
driest humidity, weirdness above 0, where plains are otherwise), and to call
`com.exiledradio.gamedropsbackport.poplar.PoplarFeatures.vegetation(world, rand, chunkCorner, biome)` for its vegetal
step (also `tree`, `fallenTree`). Its surface is grass with coarse dirt where `small_patch` noise is above 1.2.

**Done** (Caves & Cliffs 1.1.3, `Editions.GAME_DROPS`): its 1.18 terrain places the dappled forest in
`MIDDLE_BIOMES_VARIANT[1][0]` (its leaves are 26.3's, all 7594 compared), lays coarse dirt where 26.3's `small_patch`
is at least 1.2 (the noise compared with 26.3's own, bit for bit) and calls `PoplarFeatures.vegetation` at population.
The 1.18 terrain's biome provider builds no `GenLayer`s, so `DappledForestLayer` never reaches those worlds.

## 5. One boat registry (family) — stream 5

**Not done**: a family refactor of three mods' boat entities (Wild Update request 4, Tricky Trials #9, this mod) that has to move saved boats to new entity ids. That is not a hookup and should be its own task. This mod's boats work and are saved by wood name.

**Want:** Core's shared boat and chest boat (Wild Update's request 4, Tricky Trials' #9).

**Meanwhile:** the poplar boat and chest boat are this mod's own entities (`gamedropsbackport:boat`, id 6, and
`chest_boat`, id 7, the chest boat only with Wild Update), Trails & Tales' boat code, the wood saved by name.
## 1. Map decoration sprites for 26.3's explorer maps (stream 7, camps)

**Done** (`gd-hookups`): Core 1.4.4 ships the five sprites; this jar and `tools/camps/generate.py` no longer do.

`MapIcons.get` draws a type from `backportcore:textures/map/decorations/<name>.png`. Core 1.4.3 ships the villages,
swamp hut, jungle temple, trial chambers and red X. 26.3 adds `abandoned_camp`, `ancient_city`, `desert_pyramid`,
`mineshaft` and `warm_ocean_ruins`. **Meanwhile:** this jar ships those five under `assets/backportcore/...`
(`tools/camps/generate.py`). Ask: ship them in Core (or let `MapIcons.get` take a sprite location), then drop ours.

## 2. Exploration map search radius and skip-known (stream 7)

**Done** (`gd-hookups`): Core 1.4.4's `StructureLocate.nearest(world, from, idOrTag, rings, skipClaimed[, claim])` and `StructureClaims` (saved per dimension). `ExplorerMaps.create` takes the function's `search_radius` (50) and `skip_existing_chunks` (true), searches each member without claiming, and claims the nearest. Structures found by a finder (1.12.2's mineshafts) have no grid starts, so they are never skipped.

26.3's `exploration_map` searches 50 rings (`search_radius`) and skips structures already claimed by a map
(`skip_existing_chunks`). `StructureLocate.nearest` always searches 100 rings and knows no claims. **Meanwhile:**
maps point at the nearest structure within 100 rings, claimed or not. Ask: `nearest(world, from, idOrTag, rings,
skipClaimed)` and a claim store.

## 3. 26.3's jigsaw rules in `Jigsaw` / `JigsawElement` (stream 7)

**Not done (not worth it now)**: the three 26.x placers (`CampLayout`, `ChamberLayout`, `TrailLayout`) already generate as their versions do. Moving them onto one shared 26.x `Jigsaw` means re-checking three released structures piece by piece, and it touches Core's 1.16.5 `Jigsaw` that bastions and villages use. Worth doing when a fourth 26.x jigsaw structure arrives.

Core's `Jigsaw` is 1.16.5's, and `JigsawElement.Feature`'s connector is named `minecraft:bottom`. In 26.3 a feature
element's jigsaw has no name, so it attaches to any target (`JigsawBlock.canAttach`: `target.name() == null`); the
camps' `minecraft:down` joints need it. **Meanwhile:** `camps.CampLayout` is our own 26.3 placer (as Tricky Trials'
`ChamberLayout` and Trails & Tales' `TrailLayout` are). Ask: a 26.x `Jigsaw` (priorities, aliases, `Util.shuffle`,
nameless features) the three can share.

## 4. Today's item names (stream 7)

**Done** (`gd-hookups`): Core 1.4.4's `structure.ModernItems` (`stack`, `item`, `map`), holding every 1.12.2 item whose name or metadata changed, from Minecraft's data fixers (`tools/flattening/items.py`). `CampLoot.stack` uses it, so metadata items work too (`red_dye` is `dye:1`).

Loot read from 26.3's files names items by today's id (`cobweb`). Core maps today's block names (`Flattening`) but
not item names. **Meanwhile:** `camps.CampLoot` keeps its own one-line rename table. Ask: an item counterpart of
`Flattening`/`ModernPalette`.

## 5. Tree features owned by other mods (stream 7; requests to the owners through Core's `JigsawPools.feature`)

**Done** (`gd-hookups`): Trails & Tales registers `cherry_checked` and `cherry_bees_005` (its `CherryTree`, no bee nest), Tricky Trials `pale_oak_checked` (worldgen pale oak with moss), and the poplar stream `red_poplar`, `orange_poplar` and `yellow_poplar` (`PoplarFeatures.tree` in the feature's color). All register at init, after the camps' preInit fallback.

The camps' pools grow `cherry_checked`, `cherry_bees_005` (Trails & Tales), `pale_oak_checked` (Tricky Trials) and
`red_poplar`, `orange_poplar`, `yellow_poplar` (this mod's poplar stream). **Meanwhile:** `camps.CampTrees` grows
the owner's sapling (a poplar's leaf color is then the sapling's, not the feature's). Ask each owner to register its
own placer with `JigsawPools.feature("minecraft:<feature>", ...)` in init, which wins over ours.

## 2. The `bounce` game event (stream 3, sulfur cube)

**Done** (`gd-hookups`): Core 1.4.4's `GameEvent.BOUNCE` (frequency 2, in 1.20's vibration and warden lists, as in 26.3's tags); `EntitySulfurCube.move` emits it on every bounce.

26.2 adds `minecraft:bounce`, emitted when an entity bounces off a block or a wall (26.3's
`Entity.restituteMovementAfterCollisions`: a sulfur cube holding a block, slime blocks, beds). Core 1.4.3's
`gameevent.GameEvent` has no `BOUNCE`.

Wanted: `GameEvent.BOUNCE` (26.3's vibration frequency for it), so sculk sensors hear bouncing cubes.

Meanwhile: a cube's bounce emits nothing. Its other events (`prime_fuse`, `shear`, `entity_interact`, `entity_place`)
go through `GameEvents.emit`.

## SC-1. Sulfur caves in Caves & Cliffs' 1.18 terrain (stream 4)

26.2 adds `minecraft:sulfur_caves` to `OverworldBiomeBuilder.addUndergroundBiomes` (continentalness -0.19 to 0.55,
erosion 0.45 to 1, weirdness -1.1 to -0.85, depth 0.2 to 0.9) and its bands to the underground material rule.
Caves & Cliffs' terrain core has no 26.2 edition, so its biome source never names it.

**Want:** a Caves & Cliffs edition for Game Drops Backport (as `Editions.PALE` is for Tricky Trials): the biome line,
the bands in its surface rules before deepslate, `Biomes.byName("sulfur_caves")`, and the biome's 3D id through Core's
`ModernGeneration.biomeId` (F3, spawning). A climate query in `ModernGeneration` would also do for the placement.

**Done** (Caves & Cliffs 1.1.3, `Editions.GAME_DROPS`): the biome line after the lush caves, the bands in its surface
rules before deepslate (26.3's `sulfur_cave_gradient`, bit for bit), `Biomes.byName("sulfur_caves")` and the 3D id.
`SulfurCaves.terrainHasSulfurCaves` asks the generator's `editions()` for that edition: in such a world the terrain's
biome is the answer, `ModernClimate` and the bands at population step aside (the springs, pools and spikes stay ours).
A 1.18 world made before the edition keeps `ModernClimate` and the bands.


## Lf. Look and feel (stream 8)

**Round 2** (`gd-hookups`): items 2 (all but the scale), 3 and 5 are done; 1 and 4 are still open.

**Round 3** (Core 1.4.4, Core branch `claude/jolly-mendel-d629fd`): items 1 and 2's scale are done; 4 is answered (not
built, and why). Nothing for this mod to call: Core does each by version, from this mod's 26.1 and 26.3.

Done in Core 1.4.4 (branch `gd-lookfeel`): 26.2's pause icon row (`VanillaRevisions.pauseIconRow`, sprites from this
mod) and `MenuBackground.contentFrame` for World Options' frame. Still open:

1. **Borderless fullscreen** (26.1 added Exclusive Fullscreen, 26.3 made Borderless the default): 1.12.2's
   fullscreen is always exclusive. Want: a Video option, Borderless by default from 26.3, an undecorated window the
   size of the monitor. Meanwhile: 1.12.2's fullscreen. **Done** (Core 1.4.4, patch P42): Core's option
   `backportcore:exclusive_fullscreen` ("Exclusive Fullscreen", Display after Fullscreen, 26.1's tooltips) from 26.1,
   off by default from 26.3 (on at 26.1/26.2; the default follows the newest version until the player sets it). Off,
   `Minecraft.toggleFullscreen` makes an undecorated window the monitor's size at 0, 0 (LWJGL 2 makes a new native
   window on `Display.setDisplayMode`, keeping the GL context, and reads `org.lwjgl.opengl.Window.undecorated` then),
   and puts the window back on the way out. Below 26.1, and under Cleanroom's LWJGL 3 (option hidden), 1.12.2's
   fullscreen. Tested in Core's client self-test with a real round trip on the monitor.
2. **F3** (26.1-26.3): the overlay's own GUI scale (Debug Options, "Auto"), `player_speed`, the day count split
   from local difficulty, AM/AX mob counts. Want: these entries in Core's debug screen at 26.x. Meanwhile: 1.21.11's.
   **Done except the scale**: Core's 26.1 step (`ModernDebugLines`) adds "Day #n" (split from Local Difficulty),
   "Speed: n blocks/tick" and the spawn line with 26.1's eight category abbreviations, and Debug Options lists
   `day_count`, `player_speed` and `entity_spawn_counts` (Off in both profiles, as in 26.3). **The overlay's own
   GUI scale is done too** (Core 1.4.4, `DebugScale`, no patch): 26.3's 110-wide GUI Scale slider in Debug Options'
   footer (Unchanged, Auto by default, 1 ... the largest the window fits; buttons 4 apart), and F3's columns, the
   lagometer and Core's network charts drawn at it, never larger than the game's scale, as 26.3's `DebugScreenOverlay`.
3. **Option headings** (26.3): section headings bold, underlined and centered in the options lists. Want: Core's
   `VideoSections` headings in that style from 26.3. Meanwhile: 1.21.11's style. **Done**:
   `VanillaRevisions.centeredOptionHeaders()`, Video Settings' section headers from 26.3.
4. **Creative order** (26.3): Ingredients and Building Blocks reorder the minerals; Natural Blocks goes Overworld,
   Nether, End. Want: a 26.3 order file through `Contribution.creativeOrder`. Meanwhile: Tricky Trials' order.
   **Not built** (Core 1.4.4, reasons in Core's `docs/PRESENTATION.md`): 26.3 reorders runs of minerals inside its own
   tabs (Ingredients, Natural Blocks, 1.19.3's Building Blocks). 1.12.2's tabs are 1.19.2's and list items in registry
   order, so the ores, mineral blocks and ingots are scattered among other items and no run exists to reorder (and
   there is no Natural Blocks tab). An order file sorts every tab by one full list, so a 26.3 list would regroup each tab
   into 26.3's layout and a minerals-only list would move them to the front of every tab: neither is an order any
   vanilla version had. Tricky Trials' (1.19.2's) order stays.
5. **Sign edit screen** (26.2, a Trails & Tales request): the sign drawn flat from `gui/signs/<wood>.png` at 3.9x
   instead of the 3D model. Trails & Tales owns the screen (`sign.client.SignEditScreen`); it could draw the flat
   texture when `Presentation.isAtLeast(LookArea.MENUS, "26.2")` and a pack has the texture. Meanwhile: 1.20's.
   **Done**: Trails & Tales 1.0.2 draws `<sign's namespace>:textures/gui/signs/<wood>.png` at 3.9x when it exists.
   This mod ships poplar's and Trails & Tales cherry's and bamboo's; vanilla woods need a pack with `minecraft:` ones.
