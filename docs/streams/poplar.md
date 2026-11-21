# Stream 5: poplar and the dappled forest (26.3)

Package `poplar` (wood set in `poplar/wood`, boats in `poplar/boat`, client parts in `poplar/client`). Config `poplar`
(`enabled`, `generateBiome`). Entity ids 6 (`boat`) and 7 (`chest_boat`); 8-10 and packet ids 25-29 unused. Dev
server port 25925. Assets: `python -I tools/poplar/generate.py`. Test: `tools/poplar_suite.sh`.

## Changelog section

*   **Poplar wood**: poplar logs, wood, stripped logs and stripped wood, planks, stairs, slabs, fences, fence gates,
    doors, trapdoors, pressure plates, buttons, signs and boats. Strip logs and wood with any axe. Poplar planks work in
    every recipe that takes any planks. With Wild Update installed, poplar boats with chests.
*   **Red, orange and yellow poplar leaves** drop poplar saplings and sticks, never apples, rustle beside a trunk and
    let colored leaves drift down.
*   **Poplar saplings** grow into a red, orange or yellow poplar: a tall trunk with a few short branches and a tall,
    narrow canopy. Saplings go into flower pots.
*   **Red shrub**: bone meal spreads it to the side.
*   **Shelf mushroom**: grows on the side of a block; bone meal makes it bigger, and a big one drops two. It bounces
    you like a bed unless you sneak, and halves fall damage. Rabbit stew can be made with one instead of a brown
    mushroom.
*   **Dappled forest** biome: part of the plains of cool regions is now dappled forest in newly generated chunks:
    orange grass, poplars of three colors, fallen poplars, spruces, shelf mushrooms, red shrubs, brown mushrooms and
    patches of coarse dirt. With Tricky Trials, leaf litter under the poplars.
*   With Village & Pillage, the wandering trader sells poplar saplings, logs and shelf mushrooms, and poplar leaves,
    saplings, red shrubs (30%) and shelf mushrooms (65%) go in the composter.

## How it works

*   **Wood set** (`poplar/wood`): Tricky Trials' pale oak code (Trails & Tales' `common/wood`, the family's own),
    copied in. 26.3's `BlockSetType.POPLAR` uses the defaults, 1.12.2's wooden sounds. Recipes, fuel 300/150/200/100,
    flammability 5/20 and 5/5, ore names `logWood`, `plankWood`, `slabWood`, `stairWood`, `fenceWood`,
    `fenceGateWood`, `treeLeaves`, `treeSapling`. The hanging sign and shelf are Trails & Tales' and Tricky Trials'
    to add (they do so for every family wood): `CORE_REQUESTS.md` 1-2.
*   **Signs**: our `TileEntitySign` subclass drawn by our renderer with the old 64x32 layout. 26.3's sign is a block
    model on a 32x32 texture, so the generator rearranges it into that layout.
*   **Boats**: Trails & Tales' boat code: `gamedropsbackport:boat` and `chest_boat`, the wood in synced data and saved
    by name. Chest boats are Wild Update's (1.19), so the chest boat, its item and recipe exist only with it.
*   **Tree** (`PoplarFeatures.tree`): 26.3's `TreeFeature` with `poplar_trunk_placer(7, 4, 0)` (4 above branches,
    1-4 branches), `poplar_foliage_placer` (radius 5/6/7/8 weighted 5/5/1/1, height 5-6, side holes 0.15) and
    `two_layers_feature_size(upper 2)`, in vanilla's order of random draws (a direction shuffle every trunk level,
    the rhombus rows, the logs inside the canopy). Decorators: `shelf_mushroom` (40%), and for the worldgen
    `_leaf_litter` trees two `place_on_ground` passes (96 tries radius 4, 150 tries radius 2).
*   **Fallen poplar** (`PoplarFeatures.fallenTree`): `FallenTreeFeature`: stump, a 2-5 log 2-3 blocks away with at
    most 2 gaps, brown mushrooms on top (10%), shelf mushrooms on the sides (80%).
*   **Leaves**: 1.12.2 leaves decaying at modern distance 7, untinted; `POPLAR_LEAVES` sounds (leaf litter's); the
    ambient rustle one time in 300 with a natural log and three of the same leaves beside; a falling leaf one time in a
    hundred (26.3's `PoplarProvider`, the pale oak leaf's numbers).
*   **Red shrub**: `BushBlock`: 13 high, replaceable, no collision; bone meal spreads to a free side.
*   **Shelf mushroom**: `facing` and `age` in metadata; 26.3's two boxes per size; support behind it; bounce
    restitution 0.75 (0.6 for anything not alive), fall damage halved, its bounce sound.
*   **Biome** (`BiomeDappledForest`): 26.3's numbers (temperature 0.6, downfall 0.6, grass `#DF6827`, foliage
    `#E68E30`, water `#375154`, sky `#7CA3FF`, fog `#CCD8E2` blended on the client), animals (fox with Village &
    Pillage), the plains' height. Decoration: 1.12.2's ores and disks, 26.3's vegetal step
    (`PoplarFeatures.vegetation`), springs.
*   **Placement** (`DappledForestLayer`): decision 1. 26.3 puts it at `MIDDLE_BIOMES_VARIANT[1][0]`: the cool band's
    driest slot with weirdness above 0, otherwise plains. 1.12.2's `GenLayerBiome` has that band (climate 3, the cool
    list); a layer is inserted right above it, and a cool cell it made plains becomes dappled forest one time in
    three. Warm plains stay plains; every other cell is untouched and the forest flows through vanilla's zooms,
    edges, rivers and shores. Caves & Cliffs' 1.18 terrain does not use these layers (`CORE_REQUESTS.md` 4).

## Differences from 26.3

*   No hanging sign or shelf until Trails & Tales and Tricky Trials add poplar (Rule 2 owners).
*   No leaf litter without Tricky Trials; with it, the litter is not yet tinted with the dappled forest's dry foliage
    color (`CORE_REQUESTS.md` 3).
*   Spruces in the dappled forest are 1.12.2's spruce (`WorldGenTaiga2`), not 26.3's `spruce` feature.
*   Coarse dirt uses 1.12.2's surface noise above 1.75 in place of 26.3's `small_patch` noise above 1.2.
*   No glow lichen, glow squids, cold animal variants or forest music (other mods' content, or none in 1.12.2).
*   The 1.12.2 placement replaces one cool plains cell in three; 26.3's share comes from its noise.
*   1.12.2's `Random`, so trees are not block-for-block 26.3's for a seed.
