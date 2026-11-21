# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.0) or from another family mod through Core.
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

## SC-1. Sulfur caves in Caves & Cliffs' 1.18 terrain (stream 4)

26.2 adds `minecraft:sulfur_caves` to `OverworldBiomeBuilder.addUndergroundBiomes` (continentalness -0.19 to 0.55,
erosion 0.45 to 1, weirdness -1.1 to -0.85, depth 0.2 to 0.9) and its bands to the underground material rule.
Caves & Cliffs' terrain core has no 26.2 edition, so its biome source never names it.

**Want:** a Caves & Cliffs edition for Game Drops Backport (as `Editions.PALE` is for Tricky Trials): the biome line,
the bands in its surface rules before deepslate, `Biomes.byName("sulfur_caves")`, and the biome's 3D id through Core's
`ModernGeneration.biomeId` (F3, spawning). A climate query in `ModernGeneration` would also do for the placement.

**Meanwhile:** `sulfurcaves.ModernClimate` reads the terrain's climate noise by reflection and applies 26.2's box
itself, the bands are laid at population (after the terrain's ores, which they cover), and F3 shows the column's
biome. `SulfurCaves` switches to the terrain's own biome as soon as `Biomes.byName("sulfur_caves")` answers.

