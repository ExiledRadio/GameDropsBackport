# Stream 4: sulfur caves and springs (26.2)

Package `sulfurcaves`, config `sulfurCaves`, packet 20 (of 20-24). Sources: the 26.3 client jar
(`worldgen/biome/sulfur_caves.json`, the placed and configured features, `structure/spring/*.nbt`,
`OverworldBiomeBuilder`, `OverworldMaterialRules`, the speleothem, lake, root system and template features) and
minecraft.wiki's *Sulfur Caves* and *Sulfur Spring*.

## Where

26.2's box: continentalness -0.19 to 0.55, erosion 0.45 to 1, weirdness -1.1 to -0.85, depth 0.2 to 0.9.

- **Caves & Cliffs' 1.18 terrain**: the box over its climate noise (1.18's, which 26.x keeps), quart by quart, at real
  heights. Its own lush caves, dripstone caves and deep dark stay (the lush box overlaps; vanilla's tie is
  arbitrary). Request SC-1 asks Caves & Cliffs for the real biome.
- **Other worlds**: measured on 1.18 noise (seeds 1, 42, 1144; 16384 square) the box holds 0.007-0.022% of columns, in
  6-12 patches of median 2-18 chunks, at most 72. Our patches: one 1024 cell in 25, a ragged disc of radius 20-69,
  under low land (no ocean, river, beach, hills, mountains; base height under 0.45) at the center and the column;
  0.02% of columns. Depth 26-115 below the ground where the world goes to -64, 17-77 in a 0-255 world.
  Caves & Cliffs' own lush and dripstone regions win where they meet.
- **Superflat**: everywhere if the preset's biome is ours (`gamedropsbackport:sulfur_caves`), with decoration on.

## What

- Bands: `sulfur_cave_gradient` thresholds as 26.2 (cinnabar -0.4 to -0.1, sulfur 0 to 0.4, cinnabar 0.4 up).
- Features with their placed counts: `rooted_sulfur_spring` (1-2), `sulfur_pool` (256 tries), `sulfur_spike_cluster`
  (48-96), `sulfur_spike` (192-256 x 1-5). Springs by 26.3's weights (small 200, medium 90, large 20, extra large 5).
- Spawns from 26.3's list; bats; music `music.overworld.sulfur_caves` (14 tracks: four are 1.12.2's own files, ten
  shipped); fog `#8cb831`, under water `#17543c`.
- Bounce: weight 10 in every mineshaft chest's rail pool. 26.3's table has no biome condition (the wiki and stream 2's
  note say sulfur caves only; the jar wins).
- Tuff: Caves & Cliffs', else Tricky Trials' copy, else ours (`gamedropsbackport:tuff`, Rule 1).

## Deviations from vanilla

- The bands' noise has 26.2's octaves and thresholds but not its values (26.x rebuilt its noise library); scaled so
  sulfur and cinnabar each come out 38%, stone 24%.
- Bands are laid at population, after 1.12's (or the 1.18 terrain's) ores and stone blobs, and cover them; 26.2's
  never land in the bands. Ores in the bands are therefore absent, as in vanilla.
- Writes stay in the four chunks 1.12 guarantees: a spike thrown more than 8 blocks outside the window and the
  outermost tuff of large springs (reach 9-10) are skipped there.
- No F3 biome name, water color or grass color under ground (1.12.2 biomes are columns). Sky color only in superflat.
- Sulfur cube spawns wait for stream 3's `gamedropsbackport:sulfur_cube`.

## Tests

`-Pselftest=sulfurcaves`: registry, heights, templates, each feature on a prepared site, a real patch generated in
new chunks (census: sulfur against cinnabar, spikes, pools, no cinnabar far outside the mask), spawns there.
