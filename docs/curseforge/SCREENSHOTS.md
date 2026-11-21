# Game Drops Backport: screenshot scenes

Hand-built scenes for the image slots in `DESCRIPTION.md` (the `TODO-*.jpg` names on branch
`claude/gd-release-7240b5`, commit c31c222). The user takes the pictures; these are the places to stand.

**World:** `Game Drops Showcase` in the Backport Showcase instance (OptiFine), seed 263, Caves & Cliffs world type.
Built on `G:\VBPTest\showcase263` (ports 25702/25703) with the instance's jars: Core 1.4.4, Game Drops 0.1.0, C&C
1.1.2, Wild 1.1.1, T&T 1.0.1, Tricky Trials 1.0.1, NU 1.1.0, UA 1.2.1, BB 1.1.0, V&P 2.7.2, Fluidlogged API 3.3.3.
Scene scripts: `G:\VBPTest\showcase263\tools\scenes\gd*.py` (shared code in `gd_common.py`). Spawn is at the baby
paddock; every scene is within 120 blocks of it.

**World rules:** `doDaylightCycle`, `doWeatherCycle`, `doMobSpawning`, `doFireTick` and `mobGriefing` are off and
`randomTickSpeed` is 0. Set the time with `/time set <n>` before each shot. Every mob is posed with `NoAI`, `Silent`,
`PersistenceRequired` and `Invulnerable`; the babies are also age-locked as the golden dandelion locks them. Fly
(creative) to the `/tp` spot and hold still; nothing is built under the cameras.

**Built from the mod's own 26.3 templates:** the spring is `sulfur_spring_large_1` and the camp is
`campsite_dappled_forest_1` joined to `tent_dappled_forest_1` at their jigsaws, placed block by block where the game
would put them. The poplars were grown from saplings (on a scratch platform, then cloned into place) and their leaves
set to the color each spot needs. The forest, cabin and camp have their biome painted `dappled_forest`, so the grass
takes its orange.

## Scenes

| # | Scene | `/tp` (x y z yaw pitch) | Time, weather | Shows | Slot in `DESCRIPTION.md` |
| --- | --- | --- | --- | --- | --- |
| 1 | `gd1_babies` | `/tp @p 159.5 89 208.5 -95 22` | 3000, clear | Standing in the open gate of an oak paddock on a meadow. Age-locked babies with 26.1's models: a calf by its mother, two chicks, a puppy, two piglets, a lamb, a foal and a bunny. Golden dandelions grow by the gate; one is potted on a fence post. Hold a golden dandelion in your hand for the shot, since the alt text says "one holding a golden dandelion". | `TODO-babies.jpg` (**The golden dandelion and the babies**) |
| 2 | `gd2_sulfur` | `/tp @p 186.5 92 155.5 180 12` | 4000, clear | A small open-fronted bathhouse. The walls are sulfur bricks on polished cinnabar, with chiseled sulfur pillars, chiseled cinnabar caps and a cinnabar brick band. It has arched windows of sulfur and cinnabar brick stairs, sulfur and cinnabar stair benches, and slab seats. In the middle is a pool of water over potent sulfur, ringed by polished sulfur slabs, which bubbles and gives off gas. Blocks of sulfur, cinnabar and potent sulfur are stacked by the door. | `TODO-sulfur-blocks.jpg` (**Sulfur and cinnabar**) |
| 3 | `gd3_sulfurcube` | `/tp @p 178.5 65 175.5 -100 10` | 6000 (lit by the shaft) | Inside the sulfur cave: a big sulfur cube holding TNT, a medium one holding cinnabar, a plain medium one and two small ones (one holding a slime block). Behind them are the potent sulfur pool and the spikes. | `TODO-sulfur-cube.jpg` (**The sulfur cube**) |
| 4 | `gd4_sulfurcave` | `/tp @p 175.5 65 176.5 -95 6` | 6000 (lit by the shaft) | A hand-carved cave with bands of sulfur and cinnabar in tuff, and sulfur and cinnabar patches on the floor. Sulfur spike clusters hang from the roof and a few stand on the floor. At the far end is a pool of water over potent sulfur with magma under it, lit by daylight through a collapsed shaft; a lava seep glows by the camera. | `TODO-sulfur-caves.jpg` (**Sulfur caves**) |
| 5 | `gd5_spring` | `/tp @p 179.5 91 195.5 175 24` | 6000, clear | A large sulfur spring in a grassy valley (26.3's template): sulfur, cinnabar, granite and tuff round a pool, with spikes rising from the water and the rim and tuff scattered round it. Its potent sulfur sits over magma and erupts every 50 seconds, so wait for a plume. The bathhouse shows in the background. | Second image for **Sulfur caves**, or use it instead of scene 4 (the alt text names a spring) |
| 6 | `gd6_forest` | `/tp @p 209.5 89 226.5 -156 -14` | 5000, clear | Low beside a fallen poplar with shelf mushrooms (both sizes), looking up the slope into red, orange and yellow poplars. The ground has orange dappled-forest grass, leaf litter, red shrubs and ferns. | `TODO-dappled-forest.jpg` (**Poplar and the dappled forest**, first image) |
| 7 | `gd7_poplarwood` | `/tp @p 228.5 94 248.5 158 12` | 7000, clear | A woodcutter's cabin of poplar at the forest edge. It has plank walls on stripped poplar log posts, a stair roof with a slab ridge, a poplar door, button and pressure plate, open trapdoor shutters, and a slab porch with a fence rail and an open gate. Outside are a sign ("Poplar Grove / Woodcutter"), a potted sapling, a woodpile of poplar logs, wood and stripped wood, shelf mushrooms on the posts, red shrubs, saplings and a poplar boat pulled up on the grass. | `TODO-poplar-wood.jpg` (**Poplar and the dappled forest**, second image) |
| 8 | `gd8_camp` | `/tp @p 203.5 90 251.5 -158 18` | 5500, clear | An abandoned camp under red poplars at the forest edge (26.3's dappled forest campsite and tent). On the left is the campsite: a cold campfire, log seats with cushions, a path and the chest. On the right is the tent of white wool stairs over straw beds, with a cauldron and a barrel. **Before the shot, open the chest by the campfire.** 26.3's camp chest always rolls one explorer map, made on opening. Hold it in your off hand (F) so the map shows beside the camp. | `TODO-abandoned-camp.jpg` (**Abandoned camps**). The alt text says "in a taiga"; this camp is the dappled forest one, so change it to "An abandoned camp at the edge of the dappled forest" |
| 9 | `gd9_softblocks` | `/tp @p 109.5 103 211.5 168 26` | 4500, clear | A spruce garden terrace on the west hill. A cyan wool-stair sofa with light gray wool slab arms and yellow, orange and pink cushions. A rug of red and white wool and wool slabs. A coffee table of white concrete slabs. Lime and magenta wool slab stools with cushions. Gray and light gray concrete stairs as steps, and concrete slab planters. Two straw beds and hay under a lean-to roofed with black concrete stairs. | `TODO-soft-blocks.jpg` (**Wool, concrete, cushions and straw beds**) |
| 10 | settings screen | in game: Options, Mod Options, Backport Core, Game Drops | n/a | The settings screen. The user captures it; nothing to build. | `TODO-settings.jpg` (**The rest of 26.x**) |

## Image lines to paste

Save each shot as JPEG, under 2 MB (CurseForge's limit), with the name below, then swap the `TODO-` names in
`DESCRIPTION.md`:

| Slot | File |
| --- | --- |
| `TODO-babies.jpg` | `gd1-babies.jpg` |
| `TODO-sulfur-blocks.jpg` | `gd2-sulfur-blocks.jpg` |
| `TODO-sulfur-cube.jpg` | `gd3-sulfur-cube.jpg` |
| `TODO-sulfur-caves.jpg` | `gd4-sulfur-caves.jpg` (and `gd5-sulfur-spring.jpg` if it gets a second image) |
| `TODO-dappled-forest.jpg` | `gd6-dappled-forest.jpg` |
| `TODO-poplar-wood.jpg` | `gd7-poplar-wood.jpg` |
| `TODO-soft-blocks.jpg` | `gd9-soft-blocks.jpg` |
| `TODO-abandoned-camp.jpg` | `gd8-abandoned-camp.jpg` |
| `TODO-settings.jpg` | `gd10-settings.jpg` |

## Things to know

* The cameras were checked only in the build server's rough preview renderer, not yet in the client with shaders.
* The sulfur cave is hand-carved under the valley. Its biome is the world's own (plains above), not 26.2's
  `sulfur_caves`, so it has the default cave fog and music. Caves & Cliffs' 3D biome can't be painted from outside.
* The sulfur cube holding a block draws its block with the held-block position fix
  (`claude/gd-cube-block-pos`, merged before the 0.1.0 jar in the instance was built).
* The camp's explorer map is rolled when the chest is first opened, from the opener's spot. It points to the nearest
  structure of the kind rolled (camps, villages, mineshafts and so on, out to Core's 100 rings). Opening the chest
  again gives nothing, so the map stays in your inventory.
* Killing a sulfur cube splits it, as with a magma cube. The scripts set `Size:0` before every `/kill`.
