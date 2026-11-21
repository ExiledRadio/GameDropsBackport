# Requests for Backport Core

What Game Drops Backport needs from Backport Core (`backportcore` 1.4.0) or from another family mod through Core.
Nothing here is built in Core's repo by this mod; each request says what we do meanwhile.

## 1. Add blocks to `#dampens_vibrations` (stream 6, soft blocks)

26.3 adds `#wool_stairs` and `#wool_slabs` to `#dampens_vibrations` (blocks and items). Core 1.4.3's
`gameevent.GameEventTags.dampens(IBlockState)` and `dampens(Entity)` hard-code wool and carpet, with no way to add.

Wanted: `GameEventTags.addDampener(Block)`, counting the block in both `dampens(IBlockState)` and `dampens(Entity)`
(a dropped item of that block), as `addResonator` does for resonators.

Meanwhile: `softblocks.SoftBlocks.init` calls `addDampener` by reflection when it exists, so wool stairs and slabs
start dampening once Core has it; until then they do not.
