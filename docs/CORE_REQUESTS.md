# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.3) or from another family mod through Core.
Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

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
