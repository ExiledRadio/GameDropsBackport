# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.0) or from another family mod through Core.
Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

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
