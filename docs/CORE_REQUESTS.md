# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.3) or from another family mod through Core.
Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

## Su-1. Jukebox comparator signal for discs added after 1.12.2 (stream 2)

1.12.2's `BlockJukebox.getComparatorInputOverride` counts the signal from the disc's item id, right for vanilla's
twelve discs only; Bounce gives a number far above 15. Tricky Trials (Decor-1) and Caves & Cliffs (Mu-1) ask for the
same. **Want:** Core's jukebox patch answering with the disc's own signal when the record has one, read by name:
`int comparatorSignal()` on the `ItemRecord` subclass (`sulfur.ItemSulfurDisc` returns 26.3's 8).
**Meanwhile:** the disc plays, names and stacks as vanilla's; only the comparator signal is 1.12.2's.
## 1. Add blocks to `#dampens_vibrations` (stream 6, soft blocks)

26.3 adds `#wool_stairs` and `#wool_slabs` to `#dampens_vibrations` (blocks and items). Core 1.4.3's
`gameevent.GameEventTags.dampens(IBlockState)` and `dampens(Entity)` hard-code wool and carpet, with no way to add.

Wanted: `GameEventTags.addDampener(Block)`, counting the block in both `dampens(IBlockState)` and `dampens(Entity)`
(a dropped item of that block), as `addResonator` does for resonators.

Meanwhile: `softblocks.SoftBlocks.init` calls `addDampener` by reflection when it exists, so wool stairs and slabs
start dampening once Core has it; until then they do not.
## Stream 1 (babies)

1. **Baby models of other family mods' mobs.** 26.1 redrew every baby but the nautilus and the ghastling, each with
   a new model, not only a texture, so none can go into Core's modern pack: each owner needs its own baby renderer
   (26.3's `Baby*Model` classes and `*_baby.png` textures). Owners: Village & Pillage (fox, cat, panda, its
   villagers' biome overlays), Buzzy Bees (bee), Update Aquatic (dolphin, turtle), Nether Update (strider, hoglin,
   zoglin, piglin, zombified piglin), Caves & Cliffs (goat, axolotl, glow squid), Trails & Tales (camel, sniffer),
   Tricky Trials (armadillo). Meanwhile: this mod draws only vanilla 1.12.2's babies.
2. **Age lock for babies that are not `EntityAgeable`.** The golden dandelion works on every `EntityAgeable` baby,
   any mod's. 26.1 also locks tadpoles (Wild Update) and gives snifflets a 48000-tick start age (Trails & Tales).
   Proposal: Core hosts a small interface (`isBaby`, `getAge`/`setAge`, `startAge`) that owners implement, and this
   mod's `babies.AgeLock` uses it. Meanwhile: tadpoles are not lockable; a locked snifflet restarts at -24000.
3. **Piglins and the golden dandelion** (Nether Update): 26.3 lists it in `piglin_loved`; piglins inspect it and take
   it. Needs a way for another mod to add to Nether Update's piglin-loved list. Meanwhile: piglins ignore it.
4. **Name tags out of the master librarian's trades** (Village & Pillage) and **out of ancient city chests** (Wild
   Update): 26.1 removed both. Owners' tables. Meanwhile: unchanged. (The woodland mansion, 1.12.2's own, is done here.)
## 1. Poplar hanging sign (Trails & Tales) — stream 5

**Want:** Trails & Tales' `hangingsign.HangingWood` to gain `POPLAR("poplar", "hanging_sign", "gamedropsbackport", -1)`,
its recipe taking `gamedropsbackport:stripped_poplar_log` (26.3: `X X / ### / ###`, X iron chain, 6 signs), present
when `gamedropsbackport:poplar_planks` is registered. Trails & Tales adds hanging signs for every family wood itself
(it already does mangrove, crimson, warped), so this mod registers none.

**Note:** 26.3 draws signs and hanging signs as block models from a 32x32 `block/poplar_hanging_sign.png` (plus
`gui/hanging_signs/poplar.png`); the old 64x32 entity layout is gone. This mod rearranges its standing sign's texture
into the old layout in `tools/poplar/generate.py` (`sign_texture`); the hanging sign needs the same treatment.

**Meanwhile:** no poplar hanging sign.

## 2. Poplar shelf (Tricky Trials) — stream 5

**Want:** Tricky Trials' `shelf.ShelfWood` to gain
`POPLAR("poplar", "gamedropsbackport", "poplar_planks", "stripped_poplar_log", MapColor.SILVER, true)`
(26.3: `COLOR_LIGHT_GRAY`, flammable 30/20, the usual `### / / ###` of stripped logs, 6 shelves).

**Meanwhile:** no poplar shelf.

## 3. Leaf litter in the dappled forest (Tricky Trials, Core) — stream 5

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

**Meanwhile:** the dappled forest is placed on 1.12.2's biome layers only (`DappledForestLayer`).

## 5. One boat registry (family) — stream 5

**Want:** Core's shared boat and chest boat (Wild Update's request 4, Tricky Trials' #9).

**Meanwhile:** the poplar boat and chest boat are this mod's own entities (`gamedropsbackport:boat`, id 6, and
`chest_boat`, id 7, the chest boat only with Wild Update), Trails & Tales' boat code, the wood saved by name.
## 1. Map decoration sprites for 26.3's explorer maps (stream 7, camps)

`MapIcons.get` draws a type from `backportcore:textures/map/decorations/<name>.png`. Core 1.4.3 ships the villages,
swamp hut, jungle temple, trial chambers and red X. 26.3 adds `abandoned_camp`, `ancient_city`, `desert_pyramid`,
`mineshaft` and `warm_ocean_ruins`. **Meanwhile:** this jar ships those five under `assets/backportcore/...`
(`tools/camps/generate.py`). Ask: ship them in Core (or let `MapIcons.get` take a sprite location), then drop ours.

## 2. Exploration map search radius and skip-known (stream 7)

26.3's `exploration_map` searches 50 rings (`search_radius`) and skips structures already claimed by a map
(`skip_existing_chunks`). `StructureLocate.nearest` always searches 100 rings and knows no claims. **Meanwhile:**
maps point at the nearest structure within 100 rings, claimed or not. Ask: `nearest(world, from, idOrTag, rings,
skipClaimed)` and a claim store.

## 3. 26.3's jigsaw rules in `Jigsaw` / `JigsawElement` (stream 7)

Core's `Jigsaw` is 1.16.5's, and `JigsawElement.Feature`'s connector is named `minecraft:bottom`. In 26.3 a feature
element's jigsaw has no name, so it attaches to any target (`JigsawBlock.canAttach`: `target.name() == null`); the
camps' `minecraft:down` joints need it. **Meanwhile:** `camps.CampLayout` is our own 26.3 placer (as Tricky Trials'
`ChamberLayout` and Trails & Tales' `TrailLayout` are). Ask: a 26.x `Jigsaw` (priorities, aliases, `Util.shuffle`,
nameless features) the three can share.

## 4. Today's item names (stream 7)

Loot read from 26.3's files names items by today's id (`cobweb`). Core maps today's block names (`Flattening`) but
not item names. **Meanwhile:** `camps.CampLoot` keeps its own one-line rename table. Ask: an item counterpart of
`Flattening`/`ModernPalette`.

## 5. Tree features owned by other mods (stream 7; requests to the owners through Core's `JigsawPools.feature`)

The camps' pools grow `cherry_checked`, `cherry_bees_005` (Trails & Tales), `pale_oak_checked` (Tricky Trials) and
`red_poplar`, `orange_poplar`, `yellow_poplar` (this mod's poplar stream). **Meanwhile:** `camps.CampTrees` grows
the owner's sapling (a poplar's leaf color is then the sapling's, not the feature's). Ask each owner to register its
own placer with `JigsawPools.feature("minecraft:<feature>", ...)` in init, which wins over ours.
