# 1.1.0

Minecraft's 26.4 game drop, Deep Freeze, and fixes. 26.4 is still in pre-release: this follows 26.4 Pre-Release 2, and an update will match the full release.

*   **Ice caves** under frozen oceans and snowy land, with icicles, ice crystals, brittle ice and crevasses. `/locate biome minecraft:ice_caves` finds them.
*   **Caves & Cliffs worlds**: with Caves & Cliffs Backport 1.2.0, new worlds place ice caves exactly where 26.4 does. Older Caves & Cliffs worlds keep the ice caves they had.
*   **The frostbite**, a cold zombie that throws ice balls. Zombies turn into frostbites in powder snow; husks and drowned turn into zombies.
*   **Ice balls**, the **Freezing** effect and its potions and tipped arrows.
*   **Packed ice bricks**, chiseled packed ice and **snow bricks** in every shape. With Caves & Cliffs Backport, the calcite pillar, polished calcite and polished calcite bricks in every shape. With Wild Update Backport, chiseled soulshale in ancient city chests.
*   **The cold sulfur cube**: a cube that swallows blue ice slides like packed ice and freezes the water under it.
*   **Jukebox playlists**: a jukebox under a chest plays the chest's discs in order, and loops. The jukebox has 26.4's look.
*   **The music disc Flurry**, in igloo chests.
*   **26.4's changes**: ice, packed ice, frosted ice and blue ice have their own sounds; snow settles on packed ice; mushrooms grow in any light; snowballs push players; carrot on a stick boosts are shorter; a perching ender dragon ignores thrown things; strays spawn in more cold biomes; straw beds break with their own sound.
*   A primed sulfur cube swallows nothing, so a dispenser can no longer swap the TNT out of a lit one.
*   Blocks and heads worn by baby zombies, husks, frostbites, zombie villagers, villagers and zombified piglins are drawn smaller, as in 26.4.
*   A silent baby makes no sound when a golden dandelion is used on it.
*   Silent mobs stay silent when they get Freezing, and a silent cushion is placed without a sound.
*   **The wandering trader** sells ice crystals and icicles (with Village & Pillage Backport).
*   **Menus**: the Deep Freeze title screen panorama, and 26.4's wording of vanilla text.
*   **Endermen follow 26.3's rules**: they no longer teleport onto signs or waterlogged blocks, an arrow they dodge flies on instead of bouncing back, and they never put their block down on bedrock or where an item frame or other entity is. They place their blocks again on Forge versions that stopped them.
*   **Smaller 26.x fixes**: a lone dark oak sapling can't be bone mealed, players can't trample farmland inside a server's spawn protection, and a named skeleton trap horse no longer vanishes.
*   **Sulfur cubes, geysers and straw beds**: boats no longer scoop up sulfur cubes, geysers leave the ender dragon alone, a hoe is the straw bed's tool, and sleeping in a straw bed counts as "Times Slept in a Straw Bed".
*   **Mobs on a lead or riding something no longer despawn**, as in 26.1: leashed squid and ocelots, and the other backports' leashed mobs, stay put.
*   **Teleport trails**: endermen, shulkers and players eating chorus fruit leave a trail of portal particles where they teleport, as in 26.3.
*   **Dispensers and sulfur cubes**: a dispenser with shears cuts the block out of a sulfur cube in front of it, and a dispenser no longer puts armor on a sulfur cube, as in 26.2.
*   **With Backport Core 1.5.0**: placing a cushion and a dispenser shearing a sulfur cube are heard by sculk sensors (with Wild Update Backport), and Adventuring Time asks for the sulfur caves and the dappled forest, as in 26.2 and 26.3.
*   **Slimmer dynamic poplars**: with Dynamic Trees - Backport, the poplars a sapling or an abandoned camp grows get shelf mushrooms only on their bottom two logs, with a whole log behind them that tapers above, as Dynamic Trees - Backport's own poplars do.
*   **Fixed: dappled forests lost their fallen trees, mushrooms and shrubs** where Dynamic Trees - Backport grows their trees. They keep them now.
*   **Fixed: hot sulfur cubes now burn like 26.2's.** Their contact damage is fire damage: a shield no longer blocks it, you are not knocked back, mobs that panic run from it, and blazes, wither skeletons, Fire Resistance and Frost Walker boots are safe from it.
*   **Fixed: piglins never spawned on Peaceful** with Nether Update Backport. They now spawn in the Nether on Peaceful, as in 26.2, and a dispenser can use a piglin or shulker spawn egg there, as 26.2 allows. Piglin brutes still don't.
*   **Fixed: calves, piglets and chicks had no 26.1 baby model with Tricky Trials Backport.** They now get it, in all three farm animal variants with 26.1's cold and warm baby textures. Adults keep Tricky Trials' look.
*   Every new feature has its own setting.
*   Translated into 18 languages.

Needs Backport Core 1.5.0 or newer.

# 1.0.3

*   **Less lag in Caves & Cliffs worlds**: mob spawning asks the sulfur caves about one block, not a whole column, and remembers it per 4x4 area.

# 1.0.2

*   **Fixed: on a server, opening a villager's trades could kick you** ("Internal server error") when an old explorer map in them needed updating. Singleplayer was not affected.
*   **Dynamic Trees**: abandoned camps' oaks, birches, spruces, pines, jungle trees and acacias are Dynamic Trees' where it grows the biome's trees. Camps had normal trees among dynamic ones.
*   **Dynamic Trees in dappled forests**: with Dynamic Trees - Backport, its poplars are dynamic poplars, with their leaf litter and shelf mushrooms (the trunk grows a full log thick where a mushroom sits), and its spruces are Dynamic Trees' spruces. Shelf mushrooms stay on a dynamic trunk as it grows.

Needs Backport Core 1.4.9 or newer.

# 1.0.1

*   **Sulfur cubes swallow other mods' blocks** that those mods list in the ore dictionary as stone, cobblestone, logs, planks, wool, ores or metal blocks, as 26.3's item tags take other mods' blocks. The new `sulfurCube.extraBlocks` setting adds any other block, with the kind of cube it makes (`modid:block=kind`).
*   **Fixed: the straw bed sat off-center in inventory slots**, higher than the other beds. It now lines up with them, as in 26.3.
*   **Fixed: straw beds in abandoned camps came apart.** In a camp built turned to the side, the two halves of a bed pointed the wrong way, so each broke off as soon as anything near it changed.
*   **Fixed: trees and plants inside abandoned camps.** Leaves, trunks, grass and reeds that stood where a camp was built stayed inside its tents; they are cleared now, as in 26.3, where camps are built before trees grow.
*   **Fixed: floating abandoned camps** in worlds without Caves & Cliffs Backport's 1.18 terrain. The ground under a camp's floor is filled in, as 26.3 shapes the ground under it.

# 1.0.0

First release: Minecraft's 26.1, 26.2 and 26.3 game drops on Minecraft 1.12.2.

*   **Realistic Terrain Generation** (and RTG Unofficial): dappled forests generate in RTG worlds, on RTG's forest terrain, instead of turning into plains. Needs Backport Core 1.4.5.
*   **The golden dandelion**: keeps a baby animal a baby until you feed it another, makes yellow dye and a suspicious stew of Saturation. Piglins love it.
*   **The babies**: 26.1's baby models and textures for animals and zombies, 26.1's cat, pig, cow and chicken sound variants, and crafted name tags. The other backports' babies get 26.1's models too, and the golden dandelion keeps tadpoles and snifflets small.
*   **Sulfur and cinnabar** in every stone shape, potent sulfur, sulfur spikes and the music disc Bounce.
*   **The sulfur cube**, with 12 kinds by the block it swallows and 26.3's knockback.
*   **Sulfur caves** and sulfur springs, named on the debug screen and with their own fog, water and plant colors. `/locate biome minecraft:sulfur_caves` finds them, also in worlds without Caves & Cliffs Backport's 1.18 terrain.
*   **fingerspit's music**: Ebb, Home, Memories, Nightly and Shores on the title screen, in survival and creative, and in the biomes 26.x plays them in.
*   **Poplar** in every wood shape, with signs and boats, red shrubs, shelf mushrooms and the dappled forest.
*   **Wool and concrete stairs and slabs**, cushions you can sit on and switch between, and straw beds.
*   **Abandoned camps** in 18 biomes, and 26.3's explorer maps, which lead to camps, vanilla structures and the other backports' structures, and copy in a crafting grid.
*   **Menus and screens**: 26.3's title screen panorama and its row of Friends, Language and Accessibility icons (with Backport Core 1.4.5), menu music and splashes, the pause menu with its icon buttons and World Options, the Game Rules screen with search, Optimize World, borderless fullscreen and 26.3's creative tab order; with Backport Core 1.4.5, 26.3's question, "Saving world", disconnected and out of memory screens, check box and End credits, 26.3's High Contrast art for its High Contrast option, and 26.2's flat sign editing for every vanilla wood (with Trails & Tales).
*   **Spawn eggs** with their newer textures, for vanilla's and the backports' mobs.
*   **26.x's smaller changes** to things 1.12.2 already has: `/time` with world clocks, `/swing`, `/unpublish`, bed bounce, riding endermen, ocelots on Peaceful, separate chat and command spam limits, today's `/advancement` and `/team` replies, and more.
*   **Waterlogging** with Fluidlogged API (optional): the new stairs, slabs, walls, sulfur spikes, and poplar fences, trapdoors, signs and leaves hold water.
*   Works with every other backport in the family, and with OptiFine and shaders.
*   Every feature has its own setting in Backport Core's settings screen.
*   Translated into 18 languages.
*   Requires Backport Core 1.4.5 or newer.
