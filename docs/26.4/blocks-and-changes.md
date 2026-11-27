# 26.4 Pre-Release 1: building blocks, cold sulfur cube, jukebox playlists, Flurry, changes to existing things

Sources: `E:/MCDownloads/minecraft-26.4-pre-1/src/` (called NEW) and `E:/MCDownloads/minecraft-26.3/src/` (called OLD). Java paths are relative to `net/minecraft/`, data paths to `data/minecraft/`. Line numbers are NEW unless marked OLD.

Property inheritance (state/BlockBehaviour.java):
- `ofLegacyCopy` (line 1092) copies destroyTime, explosionResistance, collision, randomTicks, light, mapColor, sound set, friction, speed/jump/bounce, canOcclude, forceSolid, pushReaction, requiresCorrectToolForDrops, offset, requiredFeatures, instrument, replaceable.
- `ofFullCopy` (line 1077) is legacy plus jumpFactor, isRedstoneConductor, isValidSpawn, postProcess, isSuffocating, isViewBlocking, drops, descriptionId.
- `strength(x)` sets hardness = resistance = x (line 1192).
- Helpers in Blocks.java: `registerStair` (5739) = StairBlock, ofFullCopy(base) + isViewBlocking(NEAR_PLANE_INTERSECTS_OUTLINE). `registerSlab` (5745) = SlabBlock, ofLegacyCopy(base) + the same viewBlocking. `registerWall` (5761) = WallBlock, ofLegacyCopy(base).forceSolidOn().
- Default friction is 0.6, so a friction of 0.6 means not slippery.

## 1. Building blocks

### Block properties (level/block/Blocks.java)
- [ ] **packed_ice_bricks** (3091-3100): plain `Block`. mapColor ICE, instrument CHIME, requiresCorrectToolForDrops, friction 0.6 (not slippery, unlike packed ice's 0.98), strength 0.5 (hardness 0.5 / resistance 0.5), sound set PACKED_ICE, light 0. No randomTicks, so it does not melt.
- [ ] **packed_ice_brick_slab / _stairs / _wall** (3101-3103): built by registerSlab/registerStair/registerWall from PACKED_ICE_BRICKS, so they get the same values (0.5, friction 0.6, CHIME, requires tool).
- [ ] **chiseled_packed_ice** (3104): plain `Block`, `ofFullCopy(PACKED_ICE_BRICKS)`, so identical values.
- [ ] For comparison, **packed_ice** (3082-3090): mapColor ICE, CHIME, friction 0.98, strength 0.5, sound PACKED_ICE (OLD 3120-3122 used `SoundType.GLASS`). It does not require a tool and drops only with silk touch.
- [ ] **snow_bricks** (2005-2008): plain `Block`. mapColor SNOW, friction 0.6, requiresCorrectToolForDrops, strength 0.6 (0.6 / 0.6), sound SNOW. Instrument is the default (HARP). No light, no randomTicks, so it does not melt.
- [ ] **snow_brick_slab / _stairs / _wall** (2009-2011): built from SNOW_BRICKS with the helpers.
- [ ] **calcite** (4991-4999, unchanged): mapColor TERRACOTTA_WHITE, instrument BASEDRUM, sound CALCITE, requiresCorrectToolForDrops, strength 0.75.
- [ ] **calcite_pillar** (5000): `RotatedPillarBlock`, ofFullCopy(CALCITE), so it has an `axis` property.
- [ ] **calcite_slab / _stairs / _wall** (5001-5003): built from CALCITE with the helpers.
- [ ] **polished_calcite** (5004): plain `Block`, ofFullCopy(CALCITE). Its **slab / stairs / wall** are at 5005-5007.
- [ ] **polished_calcite_bricks** (5008): plain `Block`, ofFullCopy(POLISHED_CALCITE). **polished_calcite_brick_slab / _stairs / _wall** are at 5009-5011.
- [ ] **chiseled_soulshale** (5070-5073): plain `Block`. mapColor WARPED_STEM, strength(2.0, 6.0) (hardness 2.0, resistance 6.0), sound CHISELED_SOULSHALE. It does **not** require the correct tool (drops by hand), has the default HARP instrument, default friction, and no light. No special behavior.
- [ ] No new block has a block entity, light emission or randomTicks.
- [ ] New sound sets: `block_sound_set/calcite.json` and `block_sound_set/chiseled_soulshale.json` (events `block.chiseled_soulshale.{break,step,place,hit,fall}`). Every sound set uses the default volume 1.0 and pitch 1.0 (block/sounds/BlockSoundSet.java:25-26).
- [ ] Item registrations (item/Items.java) are plain `registerBlock`:
  - calcite family 85-97
  - snow bricks 813-816
  - packed ice bricks family 1059-1063
  - chiseled_soulshale 953

### Block tags (OLD -> NEW)
- [ ] `tags/block/mineable/pickaxe.json`:
  - Added: packed_ice_bricks, packed_ice_brick_slab, packed_ice_brick_stairs, packed_ice_brick_wall, chiseled_packed_ice, every calcite_* and polished_calcite* block (pillar, slab, stairs, wall, polished, polished slab/stairs/wall, polished bricks and their slab/stairs/wall), and chiseled_soulshale. Also added: ice_crystal, brittle_ice.
  - **Removed `#minecraft:walls`**, replaced by an explicit list of the 26.3 stone walls (cobblestone, mossy_cobblestone, brick, prismarine, red_sandstone, mossy_stone_brick, granite, stone_brick, nether_brick, andesite, red_nether_brick, sandstone, end_stone_brick, diorite, blackstone, polished_blackstone_brick, polished_blackstone, cobbled_deepslate, polished_deepslate, deepslate_tile, deepslate_brick, mud_brick). This lets snow_brick_wall be shovel-only.
- [ ] `tags/block/mineable/shovel.json`: added snow_bricks, snow_brick_slab, snow_brick_wall, snow_brick_stairs.
- [ ] `needs_stone_tool`, `needs_iron_tool` and `needs_diamond_tool` are unchanged. No new block is in any of them, so any pickaxe or shovel works.
- [ ] `tags/block/slabs.json` and `tags/item/slabs.json` added: calcite_slab, polished_calcite_slab, polished_calcite_brick_slab, packed_ice_brick_slab, snow_brick_slab.
- [ ] `tags/block/stairs.json` and `tags/item/stairs.json` added: packed_ice_brick_stairs, snow_brick_stairs, calcite_stairs, polished_calcite_stairs, polished_calcite_brick_stairs.
- [ ] `tags/block/walls.json` and `tags/item/walls.json` added: packed_ice_brick_wall, snow_brick_wall, calcite_wall, polished_calcite_wall, polished_calcite_brick_wall.
- [ ] `tags/block/ice.json` added: packed_ice_bricks, chiseled_packed_ice, brittle_ice. The only Java use of `BlockTags.ICE` is `levelgen/structure/structures/OceanRuinPieces.java:375` (ocean ruin floor search).
- [ ] `tags/block/blocks_motion_no_leaves.json` added: snow_bricks, calcite_pillar, polished_calcite, polished_calcite_bricks, chiseled_soulshale. UNKNOWN why: no `net/minecraft` Java code references the tag, and the list skips their slabs and stairs.
- [ ] `tags/item/sulfur_cube_archetype/fast_sliding.json`: added packed_ice_bricks, chiseled_packed_ice, snow_bricks; **removed blue_ice** (moved to the new `cold` archetype).
- [ ] `tags/item/sulfur_cube_archetype/slow_bouncy.json`: added calcite_pillar, polished_calcite, polished_calcite_bricks, chiseled_soulshale.

### Loot tables (`loot_table/blocks/*.json`)
- [ ] Every new full block drops itself under a `survives_explosion` condition. **None needs silk touch.** This covers packed_ice_bricks, chiseled_packed_ice, the walls and stairs, snow_bricks, calcite_pillar, polished_calcite(_bricks) and chiseled_soulshale.
  - packed_ice itself stays silk-touch only.
  - snow_block stays "silk touch gives the block, otherwise 4 snowballs".
- [ ] Every new slab uses the standard slab table: 1 slab, `set_count 2` when `type=double`, plus `explosion_decay`.

### Recipes (`recipe/*.json`, all new)
Crafting (shaped):
- [ ] packed_ice_bricks: 2x2 packed_ice -> 4
- [ ] snow_bricks: 2x2 snow_block -> 4
- [ ] polished_calcite: 2x2 calcite -> 4
- [ ] polished_calcite_bricks: 2x2 polished_calcite -> 4
- [ ] calcite_pillar: 1 wide, 2 tall column of calcite -> **2**
- [ ] chiseled_packed_ice: 2 packed_ice_brick_slab stacked vertically -> 1
- [ ] Every slab: 3 in a row (`###`) of its base block -> 6
- [ ] Every stairs: `#  / ## / ###` -> 4
- [ ] Every wall: `### / ###` -> 6
- [ ] Bases by family: packed ice bricks (packed_ice_bricks), snow bricks (snow_bricks), calcite slab/stairs/wall (calcite), polished calcite slab/stairs/wall (polished_calcite), polished calcite brick slab/stairs/wall (polished_calcite_bricks).
- [ ] snow_brick_* has **no** stonecutter recipes.

Stonecutting (each gives 1 unless noted; slabs always give 2):
- [ ] From packed_ice:
  - packed_ice_bricks 1
  - packed_ice_brick_slab 2
  - packed_ice_brick_stairs 1
  - packed_ice_brick_wall 1
  - chiseled_packed_ice 1
- [ ] From packed_ice_bricks:
  - slab 2
  - stairs 1
  - wall 1
  - chiseled_packed_ice 1
- [ ] From calcite:
  - calcite_pillar 1
  - calcite_slab 2
  - calcite_stairs 1
  - calcite_wall 1
  - polished_calcite 1
  - polished_calcite_slab 2
  - polished_calcite_stairs 1
  - polished_calcite_wall 1
  - polished_calcite_bricks 1
  - polished_calcite_brick_slab 2
  - polished_calcite_brick_stairs 1
  - polished_calcite_brick_wall 1
- [ ] From polished_calcite:
  - slab 2
  - stairs 1
  - wall 1
  - polished_calcite_bricks 1
  - polished_calcite_brick_slab 2
  - polished_calcite_brick_stairs 1
  - polished_calcite_brick_wall 1
- [ ] From polished_calcite_bricks:
  - brick slab 2
  - brick stairs 1
  - brick wall 1
- [ ] chiseled_soulshale has **no recipe**; it is loot only.
- [ ] Recipe-book advancements:
  - `advancement/recipes/building_blocks/*` for blocks, slabs and stairs
  - `advancement/recipes/decorations/*` for walls

### chiseled_soulshale in loot
- [ ] `loot_table/chests/ancient_city.json` pool 0 (rolls uniform 5-10): a new entry `minecraft:chiseled_soulshale`, weight **3**, count 1 (no set_count).
  - The pool's total weight goes from 84 to **87**, so the chance per roll is 3/87.
  - It is inserted after the iron_leggings entry and before echo_shard (json lines ~229-232).
- [ ] No other loot table mentions it (grep of `loot_table/`).
- [ ] UNKNOWN whether any structure NBT places it: the `structure/*.nbt` files are gzip and were not decoded.

### Creative tabs (item/CreativeModeTabs.java)
- [ ] Building Blocks (391-415), inserted after SULFUR_BRICK_WALL (390) and before SEA_LANTERN (416), in this order:
  - PACKED_ICE, PACKED_ICE_BRICKS, PACKED_ICE_BRICK_STAIRS, PACKED_ICE_BRICK_SLAB, PACKED_ICE_BRICK_WALL, CHISELED_PACKED_ICE
  - SNOW, SNOW_BLOCK, SNOW_BRICKS, SNOW_BRICK_STAIRS, SNOW_BRICK_SLAB, SNOW_BRICK_WALL
  - CALCITE, CALCITE_PILLAR, CALCITE_STAIRS, CALCITE_SLAB, CALCITE_WALL
  - POLISHED_CALCITE, POLISHED_CALCITE_STAIRS, POLISHED_CALCITE_SLAB, POLISHED_CALCITE_WALL
  - POLISHED_CALCITE_BRICKS, POLISHED_CALCITE_BRICK_STAIRS, POLISHED_CALCITE_BRICK_SLAB, POLISHED_CALCITE_BRICK_WALL
- [ ] Packed ice, snow (layer), snow block and calcite are now also in Building Blocks; they stay in Natural Blocks too (571, 576, 588).
- [ ] Natural Blocks: BRITTLE_ICE, ICICLE, ICE_CRYSTAL go after BLUE_ICE (572-575).
- [ ] **CHISELED_SOULSHALE is in no creative tab** (CreativeModeTabs.java has no reference to it). UNKNOWN whether that is intentional in pre-1.
- [ ] Tools & Utilities: MUSIC_DISC_FLURRY goes after MUSIC_DISC_BOUNCE (1214, last disc).

## 2. Sulfur cube: cold archetype

- [ ] `sulfur_cube_archetype/cold.json` (full content), also SulfurCubeArchetypes.java:189-233. Key COLD is at line 54, made by `archetype(-0.5F, 0.1F, 0.05F, 0.01F)` (helper at 250-260):
  - attribute_modifiers:
    - knockback_resistance add_value +0.5 (id `cold_add_knockback_resistance`)
    - explosion_knockback_resistance add_value +0.5
    - bounciness add_value +0.1
    - friction_modifier add_multiplied_total -0.95
    - air_drag_modifier add_multiplied_total -0.99
  - items: `#minecraft:sulfur_cube_archetype/cold`
  - knockback_modifiers: horizontal_power 0.6625, vertical_power 0.09
  - sound_settings:
    - hit_sound `entity.sulfur_cube.fast_sliding.hit`, push_sound `entity.sulfur_cube.fast_sliding.push` (Pre-Release 2; Pre-Release 1 had `.hit` for both)
    - push_sound_cooldown 1.0 s
    - push_sound_impulse_threshold 0.05
  - floats: false. No explosion (maxFuse) and no contact damage.
  - location_based_effects (one effect): `replace_disk`
    - block_state frosted_ice age 0, radius 3.0, height 1.0, offset [0,-1,0]
    - predicate, all_of: (block above is in `#air`) AND (block is `minecraft:water`) AND (fluid is water with source=true) AND unobstructed
    - trigger_game_event `minecraft:block_place`
    - requirements: entity `this` has flag is_on_ground=true AND is NOT riding a vehicle (inverted `vehicle: {}` predicate)
- [ ] The physics are identical to fast_sliding (the same five modifiers and knockback). Only the sound and the frost effect differ.
- [ ] `tags/item/sulfur_cube_archetype/cold.json` = `["minecraft:blue_ice"]` only.
- [ ] `tags/item/sulfur_cube_swallowable.json` now also includes `#minecraft:sulfur_cube_archetype/cold`.
- [ ] Disk geometry (item/enchantment/effects/ReplaceDisk.java):
  - dist = (int)3; it checks blocks in the single layer at feet-1, within x/z ±3, where `distToCenterSqr < 9`.
  - Each placement emits `block_place`.
- [ ] **New in ReplaceDisk:41-56**: the whole effect is skipped when the entity is a `Mob` and gamerule `mobGriefing` is false. This applies to the cube and to frost walker worn by mobs.
- [ ] SulfurCubeArchetype.java:
  - New record field `List<ConditionalEffect<EnchantmentLocationBasedEffect>> locationBasedEffects`, codec `location_based_effects`, optional, default []. It is validated with `LootContextParamSets.ENCHANTED_LOCATION`.
  - `SoundSettings.hitSound` and `pushSound` became `Optional` (`optionalFieldOf`).
- [ ] SulfurCube.java:
  - New `onChangedBlock` (367-388): runs on each block-position change. For every matching archetype's location effects it builds a LootContext (THIS_ENTITY, ENCHANTMENT_LEVEL 1, ORIGIN, ENCHANTMENT_ACTIVE). If the effect matches, it calls `effect.onChangedBlock(level, 1, itemInUse, this, pos, false)`; if it no longer matches, it calls `onDeactivated`. Active effects are tracked in a `Set`.
  - When the body item changes (~453-485), the old effects are deactivated, and newly gained archetypes' effects fire immediately if they match (with `true` as the last argument).
- [ ] Other SulfurCube changes:
  - Igniting with flint and steel now plays `FlintAndSteelItem.playSound`; with a fire charge it plays `FireChargeItem.playSound` (553-557).
  - `equipItem` refuses while the cube is primed (`!isBaby() && !isPrimed()`, line 566). OLD only refused babies.
  - Shearing plays the eject sound and sets pickupTimer=100 only if the drop actually spawned (~683).
  - Hit and push sounds are skipped when absent.
- [ ] Sounds: no new sulfur-cube sound events (SoundEvents diff). The cold archetype reuses fast_sliding's hit and push.

## 3. Jukebox playlists and the Flurry disc

### Block (level/block/JukeboxBlock.java)
- [ ] New blockstate property `playlist_mode` (boolean, BlockStateProperties.java:35; JukeboxBlock:37), default false.
- [ ] It is set from `JukeboxBlockEntity.isInPlaylistMode` in three places:
  - `getStateForPlacement` (46-47)
  - `neighborChanged` (51-58, flag 2)
  - `updateShape` (60-73)
- [ ] The blockstate json does **not** use `playlist_mode`; both values use the same model.
- [ ] Ticker (155-163):
  - HAS_RECORD=true: `tickWithRecord`.
  - HAS_RECORD=false and playlist_mode=true, server side only: `tickWithoutRecord`.
  - Otherwise no ticker.
- [ ] Unchanged:
  - Use without item pops the disc.
  - Use with a disc inserts it.
  - `ownSignal` = 15 while playing (direct redstone power from the jukebox).
  - Comparator output = the song's `comparator_output`.

### Playlist mode condition (level/block/entity/JukeboxBlockEntity.java:233-240)
- [ ] The block directly above must be in `#jukebox_containers`, its block entity must be a `Container`, and if it is a `ChestBlock` its type must be `SINGLE`. A double chest disables playlist mode.
- [ ] `tags/block/jukebox_containers.json` and `tags/item/jukebox_containers.json` both list: chest, trapped_chest, copper_chest, exposed_copper_chest, weathered_copper_chest, oxidized_copper_chest, and the four waxed copper chest variants.
  - Barrels, shulker boxes and hoppers are not in the tag.
  - The item tag's Java use is UNKNOWN (not referenced outside the tags provider).

### Playlist algorithm (JukeboxBlockEntity.java)
- [ ] State: `OptionalInt playlistPosition`, saved as int NBT `playlist_position` (139-140, 154-156). It is exposed through `ContainerData` slot 0 (-1 = none, lines 39-61).
- [ ] `findNextPlaylistPosition(container, start)` (246-257): `startIndex = clamp(start, -1, size)`. For offset 1..size it checks slot `(startIndex+offset) % size` and returns the first slot with a `jukebox_playable` item. Slots are read in index order 0..26, wrapping around, and the search starts after the last played slot. Non-disc items are skipped.
- [ ] `advancePlaylist(container, newPos)` (263-267): **swaps**. The jukebox's current disc (or EMPTY) goes into the chest slot `newPos`, and that slot's disc goes into the jukebox via `setTheItem`, which starts playing.
- [ ] `tryToAdvancePlaylist(replayOtherwise)` (269-275): recomputes the position. If no disc is found and `replayOtherwise` is true, it replays the current disc (`setTheItem(this.item)`).
- [ ] `tickWithRecord` (109-115): while the song plays, it ticks the player. Once the song has stopped and `shouldLoop()` holds (jukebox holds a disc AND playlist mode), it advances with replay=true, so the playlist **loops forever**. A single disc repeats.
- [ ] `onSongChanged` (73-80): if the song is no longer playing and shouldLoop, it advances (replay=true) at once.
- [ ] `tickWithoutRecord` (117-119): an **empty** jukebox in playlist mode pulls the next disc from the chest **every server tick** (replay=false), and the disc's chest slot becomes empty.
- [ ] Ejecting (`popOutTheItem`, 91-108):
  - In playlist mode the disc spawns at block bottom-center **+2.2 Y** (above the chest), with no random XZ offset. OLD (and non-playlist): +1.01 Y with random XZ ±0.7.
  - Because the jukebox is then empty, `tickWithoutRecord` loads the next chest disc on the next tick, so ejecting effectively skips to the next song.
- [ ] Game events (82-89): in playlist mode the `BLOCK_CHANGE` game event on disc insert is **suppressed**; Pre-Release 2 emits it again on removal (`!wasInserted || !isInPlaylistMode`). JUKEBOX_PLAY / JUKEBOX_STOP_PLAY from the song player are unchanged.
- [ ] Note particles (item/JukeboxSongPlayer.java:84-90): spawn at +1.2 Y, **+1.0 more (2.2) when shouldLoop** (the BooleanSupplier passed at JukeboxBlockEntity:63).
- [ ] Hoppers and comparators: nothing in the jukebox changed.
  - The jukebox still accepts only `jukebox_playable` items, max stack 1, only when empty (`canPlaceItem` 202).
  - `canTakeItem` needs an empty target slot (207).
  - Hoppers work on the chest as on any chest. The comparator on the jukebox reads the playing disc's comparator_output.
  - No redstone input controls the playlist (no power checks). UNKNOWN whether hoppers feeding the jukebox from the side fight the playlist; nothing guards against it.

### GUI (inventory/JukeboxMenu.java NEW, client/gui/screens/inventory/JukeboxScreen.java NEW)
- [ ] How it opens: right-click the chest that sits on a jukebox. `ChestBlockEntity.createMenu` (ChestBlockEntity.java:187-189) checks whether the block entity below is a `JukeboxBlockEntity` and, if so, returns `jukebox.createMenu(id, inv, chest)` instead of `ChestMenu.threeRows`.
  - This applies to any ChestBlockEntity, including copper chests.
  - Double chests go through ChestBlock's compound provider (six rows) and are not playlist mode anyway.
  - The jukebox itself has no GUI.
- [ ] `JukeboxMenu extends ChestMenu`: `MenuType.JUKEBOX` (MenuType.java:20, id "jukebox"), 3 rows (27 slots, same layout as a single chest), plus 1 data slot (the playlist position).
  - The client constructor uses `SimpleContainer(27)` and `SimpleContainerData(1)`.
  - `getPlayingNextPos()` = `findNextPlaylistPosition(container, data[0])`.
- [ ] `JukeboxScreen extends ContainerScreen` (the chest texture and the chest's title). Each slot widget's tooltip appends `record.playingNext` = "Playing Next" when that slot is the next disc to play (lines 34-41). Registered at MenuScreens.java:79.

### Flurry disc
- [ ] `jukebox_song/flurry.json`:
  - comparator_output **7**
  - description translate `jukebox_song.minecraft.flurry` = "Hyper Potions - Flurry"
  - length_in_seconds **154.0**
  - sound_event `minecraft:music_disc.flurry`
  - Also in item/JukeboxSongs.java:36, 80 (`register(..., 154, 7)`).
- [ ] Sound event `music_disc.flurry` is at sounds/SoundEvents.java:712 (registerForHolder).
- [ ] Item `music_disc_flurry` (item/Items.java:2564-2566): stacksTo 1, Rarity UNCOMMON, jukeboxPlayable FLURRY. Name "Music Disc"; it has no `.desc` lang key (the song description supplies the subtitle line).
- [ ] Loot: `loot_table/chests/igloo_chest.json` gets a new **pool 2**: rolls 1, condition `random_chance` **0.3**, single entry music_disc_flurry (weight 1, count 1). That is a 30% chance per igloo chest.
  - The disc is in no other loot table (grep).
- [ ] Assets: `assets/minecraft/items/music_disc_flurry.json`, `models/item/music_disc_flurry.json`, `textures/item/music_disc_flurry.png` (all new).

### Jukebox texture and model
- [ ] Added `textures/block/jukebox_bottom.png` (16x16, 150 bytes).
- [ ] Modified `textures/block/jukebox_side.png` (172 -> 184 bytes) and `textures/block/jukebox_top.png` (169 -> 181 bytes).
- [ ] `models/block/jukebox.json`: parent changed from `block/cube_top` (side+top; the bottom used the side texture) to `block/cube_bottom_top` with bottom=`block/jukebox_bottom`. Datagen is at client/data/models/BlockModelGenerators.java:3699 (`TexturedModel.CUBE_BOTTOM_TOP`).

## 4. Changes to existing things

### Ice sounds
- [ ] Block sounds became data-driven `block_sound_set/*.json` (the whole `SoundType.java` was deleted, -921 lines). Ice blocks get their own sets; OLD all four used `SoundType.GLASS`:
  - **ice**: OLD Blocks.java:2078 -> NEW Blocks.java:1996, sound set `ice` = block.ice.{break,step,place,hit,fall}
  - **packed_ice**: OLD 3122 -> NEW 3089, set `packed_ice` = block.packed_ice.*
  - **blue_ice**: OLD 4203 -> NEW 4170, set `blue_ice` = block.blue_ice.*
  - **frosted_ice**: OLD 3709 -> NEW 3698, set `frosted_ice` = block.frosted_ice.*
  - Volume and pitch are 1.0 for all of them (the same as glass).
  - UNKNOWN which .ogg files each event uses; sounds.json lives in the asset index, not in the jar.
- [ ] Packed ice bricks and chiseled packed ice reuse `packed_ice`. Snow bricks reuse `snow`. The calcite family reuses `calcite`.
- [ ] Ambient ice sounds (new, client side, via `animateTick`):
  - **IceBlock** (level/block/IceBlock.java:66-69 -> block/sounds/AmbientIceBlockSoundsPlayer.java):
    - Chance 1/10000 per animate tick.
    - Requires the block below to be `minecraft:water`, the block above to be air, and every block on the horizontal rays within 8 blocks (`BlockPos.horizontalRays(pos, 8)`) to be in `#triggers_ambient_ice_block_sounds` (= `[minecraft:ice]`). In practice this is the middle of a large frozen-ice sheet.
    - Plays `block.ice.ambient` (subtitle "Ice resonates") in the AMBIENT category, volume 1, pitch 1.
    - It inherits to every IceBlock subclass, including frosted ice and brittle ice; for those the ray check fails unless they are surrounded by plain ice.
  - **IceCrystalBlock** (AmbientIceCrystalBlockSoundsPlayer): chance 1/250. Needs the block below in `#triggers_ambient_ice_crystal_sounds` (= `[minecraft:packed_ice]`). Plays `block.ice_crystal.ambient` ("Ice Crystal chimes").
- [ ] **face_occluding_ice_blocks** tag = `[ice, brittle_ice, frosted_ice]`. Used in `IceBlock.skipRendering` (IceBlock.java:31-34): an IceBlock's face is culled when the neighbor is in the tag. OLD culled only against the same block (HalfTransparentBlock), so ice next to frosted or brittle ice no longer shows internal faces.

### Frosted ice
- [ ] `FrostedIceBlock` now extends the new abstract `CrackingIceBlock` (level/block/CrackingIceBlock.java). AGE_3 moved there.
  - Unchanged numbers: tick delay 20-40 (FrostedIceBlock:79-81), first tick 60-120 on place (30), 1/3 chance or fewer than 4 neighbors (70), brightness > 11 - age - lightDampening (73-74), melts if fewer than 2 frosted neighbors on neighborChanged (46).
- [ ] **New game event**: each crack step (age +1) now emits `GameEvent.BLOCK_CHANGE` (CrackingIceBlock.java:49), so sculk sensors pick up frosted-ice aging. OLD `slightlyMelt` emitted nothing.
  - Frosted ice plays no crack sound or particle: `getCrackingIceSound` returns null and `playCrackingIceParticle` is a no-op by default. Brittle ice overrides them.
- [ ] `IceBlock.melt` (71-79) now emits `BLOCK_DESTROY` **before** the evaporate check, so it fires in the Nether too. OLD emitted it only in the non-evaporating branch (OLD IceBlock.java:59-67).
- [ ] `enchantment/frost_walker.json`: the matching_fluids predicate gains `"state": {"source": "true"}`. This is equivalent behavior, because 26.4 merged flowing_water/flowing_lava into WATER/LAVA (OLD Fluids.java:10-13 -> NEW Fluids.java:12-29; `tags/fluid/water.json` and `lava.json` drop `flowing_*`).
- [ ] Frost walker on mobs (and the cold sulfur cube) now respects `mobGriefing` (ReplaceDisk.java:44).

### Snow on packed ice
- [ ] `tags/block/cannot_support_snow_layer.json`: OLD `[ice, packed_ice, barrier]` -> NEW `[ice, barrier]`. **Snow layers can now sit on packed ice.**
  - The check is SnowLayerBlock.canSurvive (71-79): the tag rejects; `support_override_snow_layer` accepts; otherwise the top face must be full, or the block is snow with 8 layers.
  - The code is unchanged. The only SnowLayerBlock diff is `context.getClickedBlockState()` at line 117.
  - The new packed ice bricks and chiseled packed ice are not in the tag either, so snow sits on them.

### Mushrooms (level/block/MushroomBlock.java)
- [ ] `canSurvive` (71-73) is now just `canSupportCenter(level, pos.below(), UP)`. **No light check and no solid-render check.**
  - OLD: below in `#overrides_mushroom_light_requirement` OR (rawBrightness < 13 AND below.isSolidRender()).
  - Mushrooms can now be placed and stay in full light, and on any block with center support (walls, fences, glass, etc.).
- [ ] Spreading (`canSpreadTo`, 75-89) keeps the old rules: the target is empty, and below is in `#overrides_mushroom_light_requirement` OR (rawBrightness < **13** AND center support). Center support replaced the old `isSolidRender`.
  - The neighbor cap is unchanged: at most 4 of the same mushroom in a 9x3x9 box (`canSpread`, 61-66). It is now static and keyed by the block at pos. The 1/25 random-tick chance and 4 hops are unchanged.
- [ ] Worldgen compensates: every `worldgen/placed_feature/{brown,red}_mushroom_*.json` block filter changed from `air` to `air AND (below_heightmap MOTION_BLOCKING OR below in #overrides_mushroom_light_requirement)`.
- [ ] `tags/block/wall_post_override.json` adds brown_mushroom and red_mushroom, so a wall under a mushroom keeps its tall post.

### Carrot / warped fungus on a stick (entity/ItemBasedSteering.java)
- [ ] Boost duration OLD `random.nextInt(841) + 140` = **140-980 ticks** -> NEW `random.nextIntBetweenInclusive(140, 700)` = **140-700 ticks** (line 38; the constants MIN 140 / MAX 700 at 11-12 now match the code). boostFactor is unchanged: `1 + 1.15*sin(t/total*PI)` (49-51).
- [ ] Pre-Release 2: a finished boost sets `boost_time_total` to 0 (`tickBoost`), `onSynced` boosts only when the synced total is above 0, and a load without `boosting` reads the total as 0.
- [ ] The boost state now persists. A new NBT child `item_based_steering {boosting, boost_time, boost_time_total}` is saved and loaded by Pig and Strider (Pig diff +save/load; Strider.java ~138-149).
- [ ] `ItemSteerable` now extends the new `Saddleable` interface (`getSaddleEquipSound`; pig PIG_SADDLE, strider STRIDER_SADDLE, default HORSE_SADDLE). This is a refactor only.

### Snowball knockback on players
- [ ] `tags/item/knocks_back_players_even_with_zero_damage.json` = `[minecraft:snowball]`.
- [ ] `entity/player/Player.java:702-712`: OLD `damage == 0 ? false : super.hurtServer(...)`. NEW: a 0-damage hit still goes through when `source.getWeaponItem()` is in the tag. **Snowballs now knock players back** (still 0 damage).
  - The thrown ball's weapon item is its own stack (ThrowableBallProjectile.java:102-104). Snowball damage is 3 vs Blaze, 0 otherwise (Snowball.java:31-34; OLD hard-coded the same in onHitEntity).
- [ ] Shields: LivingEntity.java:~1352-1357. For damage <= 0 with a tagged weapon, a shield counts as blocking (blocked amount 0, so knockback is blocked). Untagged 0-damage hits are not blocked.

### Pointed dripstone into cauldrons
- [ ] **No numeric change.**
  - Water 0.17578125, lava 0.05859375. They moved from PointedDripstoneBlock constants (OLD) to `Fluid.Properties.dripstoneTransferProbability` (level/material/Fluids.java:25 and :41).
  - The early gate is now `randomValue <= 0.17578125` (PointedDripstoneBlock.java:~72; `MINIMUM_TRANSFER_PROBABILITY_PER_RANDOM_TICK`); then `randomValue < fluid probability`.
  - Fill delay is unchanged: 50 + fall distance ticks.
- [ ] Refactor: a `FluidInfo.source` flag replaces `canFillCauldron(fluid)`. Mud counts as a water source. Stalagmite fall damage (+2.5 distance, x2) moved into `SpeleothemBlock.fallOnDamage` (SpeleothemBlock.java:492-498), unchanged.

### Stray spawning
- [ ] `Stray.checkStraySpawnRules` was deleted. Strays (and frostbites) use the new `Monster.checkColdVariantSpawnRules` (entity/monster/Monster.java:120-139; SpawnPlacements.java:122, 156).
  - In biomes tagged `#spawns_cold_variants_underground` (= `[ice_caves]`) it is just the normal monster rules: **no sky check, so strays spawn underground in ice caves**.
  - Elsewhere it is the old rule: monster rules plus spawner OR sky visible above the powder-snow column.
- [ ] Biome spawn lists (monster category; OLD -> NEW):
  - **frozen_peaks, jagged_peaks, snowy_slopes**: stray w95 x4 added (none before), and frostbite added (w25 in frozen_peaks; w10 in jagged_peaks and snowy_slopes).
  - **frozen_ocean, deep_frozen_ocean, frozen_river**: stray w95 x4 and frostbite w25 x4 added.
  - snowy_plains: frostbite w10 added. ice_spikes: frostbite w25 added. Both keep stray w80.
  - New biome **ice_caves**: creeper 100, skeleton 90, **stray 10**, frostbite 50, slime 100, spider 100, zombie 50, enderman 10 (1-4), witch 5, zombie_villager 5, bat 10 (ambient), glow_squid 10 (underground water).
- [ ] The End biomes (the_end, end_barrens, end_highlands, end_midlands, small_end_islands) gain the attributes `visual/fog_color #181318` and `visual/sky_color #000000`.

### Ender dragon
- [ ] **Vertical steering accel x10**: EnderDragon.java:220. OLD `setDeltaMovement(getDeltaMovement().add(0, ydd*0.01, 0))` -> NEW `addDeltaMovement(0, ydd*0.1, 0)`. ydd is clamped to ±flySpeed. `addDeltaMovement` does not scale: it is a plain add (Entity.java:3916-3919, a finite check plus `getDeltaMovement().add`), so this really is a 10x vertical acceleration.
- [ ] Perching immunity (phases/AbstractDragonSittingPhase.java:18-28):
  - OLD: immune when the direct entity is an `AbstractArrow` (arrows, spectral arrows, tridents) or a `WindCharge`.
  - NEW: immune when the damage type is in `#cannot_hurt_perching_ender_dragon` = `[arrow, thrown, trident, wind_charge]`. The direct entity is still set on fire for 1 s.
  - The behavior change is that **`thrown` damage (snowballs, eggs, ice balls, ender pearls) is now also blocked** while perching.
- [ ] EnderDragonPart: `is(Entity)` became `getGraphRoot()` (a refactor). The other phase diffs are `Math.clamp` refactors.

### Mob-effect immunity tags
Code: LivingEntity.java:1037-1056, now `final`. All per-class `canBeAffected` overrides were removed.
- [ ] `immune_to_poison` = `[#undead, nautilus, zombie_nautilus, spider, cave_spider]`. Replaces OLD `ignores_poison_and_regen` (= #undead), the OLD Spider override (removed at Spider.java OLD:122-126) and the OLD AbstractNautilus override (OLD :463-465). **No behavior change.**
- [ ] `immune_to_regeneration` = `[#undead]`. Same as OLD.
- [ ] `immune_to_weakness` = `[parched]`. Same as the OLD Parched override (removed).
- [ ] `immune_to_wither` = `[wither, wither_skeleton]`. Same as the OLD WitherBoss and WitherSkeleton overrides (removed).
- [ ] `immune_to_freezing` = `[polar_bear, snow_golem, #undead]`. Replaces OLD `freeze_immune_entity_types` = `[stray, polar_bear, snow_golem, wither]`. It blocks both powder-snow freezing (`Entity.canFreeze`, Entity.java:4055) and the new FREEZING effect.
  - **Behavior change: every undead is now immune to freezing.** That includes zombies, husks, drowned, skeletons, wither skeletons, phantoms, zombified piglins and so on.
  - Exception: zombies (and husks and drowned) and skeletons still run their powder-snow conversion trackers off `isInPowderSnow && !isWearingFreezeImmuneWearables()`, not off canFreeze (Skeleton.java:17-30, Zombie.java:119-132).
- [ ] `Entity.canFreeze` also returns false while the entity is on fire (Entity.java:4055-4057).
- [ ] Freeze damage is 1.5 instead of 1.0 while the FREEZING effect is active (LivingEntity.java:3170).
- [ ] Conversions on freezing (new; same 140-tick freeze plus 300-tick conversion pattern as the skeleton-to-stray conversion):
  - **zombie -> frostbite** (Zombie.java:119-132, 387-393, level event 1056; NBT `FrostbiteConversionTime`)
  - **husk -> zombie** (Husk.java:85-91, event 1041)
  - **drowned -> zombie** (Drowned.java:186-190, 225-229, event 1058)

### Wandering trader
- [ ] `villager_trade/wandering_trader/emerald_ice_crystal.json`: wants 2 emerald, gives 1 ice_crystal, max_uses 5, reputation_discount 0.05, xp default **1** (codec default, VillagerTrade.java:39).
- [ ] `emerald_icicle.json`: wants 1 emerald, gives **2** icicle, max_uses 5, reputation_discount 0.05, xp 1. In code: item/trading/VillagerTrades.java:372, 381, 1314-1345.
- [ ] Added to `tags/villager_trade/wandering_trader/common.json`; emerald_icicle sits after emerald_sulfur_spike, and emerald_ice_crystal is appended last. That tag feeds `trade_set/wandering_trader/common.json` (`amount: 5`, unchanged), so the trader picks 5 trades at random from the common pool, now including these two.
- [ ] Other trade changes found in the diff:
  - `villager_trade/farmer/4/emerald_cake.json`: price **3 -> 1 emerald** (VillagerTrades.java:429).
  - `weaponsmith/1/emerald_enchanted_iron_sword.json`: max_uses **12 -> 3** (902).
  - `tags/potion/tradeable.json` adds freezing and long_freezing.

### Sheep dyeing
- [ ] `tags/item/sheep_wool_dyes.json` = `[#minecraft:dyes]`. The dye-sheep logic moved from `DyeItem.interactLivingEntity` (removed) to `Sheep.mobInteract` (Sheep.java:158-171), keyed by the tag plus the item's `DataComponents.DYE`.
- [ ] Small change: using a dye that matches the sheep's color now returns CONSUME, which swallows the click. OLD fell through and returned PASS.

### Other player-visible changes spotted in the diff (not exhaustive; Frostbite, Icicle, Brittle Ice, Ice Crystal, Ice Ball, Freezing and Ice Caves are out of scope)
- [ ] **Phantom** swoop attack now requires line of sight to the target (Phantom.java:517).
- [ ] **Warden** anger: damage dealt through armor stands or hanging entities (item frames, paintings) is attributed to their last attacker (Warden.java:~652-665; SculkShriekerBlockEntity.getLastDamagingEntity ~96-113). The shrieker also credits the player who broke an armor stand or hanging entity. OLD credited the item thrower (ItemEntity owner).
- [ ] **Creative players flying** and spectators are no longer pushed by explosions (Player.java:1695-1697).
- [ ] An item on cooldown can no longer be used on entities: `Player.interactOn` returns PASS when `getCooldowns().isOnCooldown(item)` (Player.java ~831-865).
- [ ] An **End portal** does not build the obsidian platform for spectators (EndPortalBlock.java:83).
- [ ] **Honey block** gives full-block support (`getBlockSupportShape = Shapes.block()`, HoneyBlock.java:49-51), so blocks can attach on top of honey.
- [ ] **Ceiling item frames** drop their item and frame 0.25 lower (ItemFrame.java ~218-230).
- [ ] **Creeper / Mob shearing / Sulfur cube**: flags such as `droppedSkulls` and the shear sound now apply only when the drop actually spawned (Creeper.java:171-175, Mob.java:597-620).
- [ ] **Experience orbs** use a synchronized random for their initial velocity and stop following removed players (ExperienceOrb.java:112-125, 164-167).
- [ ] **Shulker** standing on brittle ice triggers its `stepOn` (Shulker.java:189-196).
- [ ] Ranged mobs (skeletons, illusioner, drowned): the arrow arc term `distance*0.2F` became `distance*projectile.getTrajectoryCorrectionFactor()` (AbstractSkeleton.java:173-183). The factor is `gravity * 4.0` (Projectile.java:495-497), which is 0.2 for arrows and tridents (gravity 0.05). It is unchanged for vanilla projectiles; only custom-gravity projectiles differ.
- [ ] **Fluids**: flowing_water and flowing_lava are no longer separate fluids. `Fluids.WATER` and `Fluids.LAVA` carry the state; `isSourceOfType` is used for waterlogging. Waterlogging is still source-only.
- [ ] Fluid behavior now lives in `EntityFluidInteraction` (+145 lines). UNKNOWN whether swimming or jumping in liquid feels any different (not audited).
- [ ] **WitherBoss** heals through `healNaturally`, which skips when at max health or dead (WitherBoss.java:271, 341). Its block-break sound is skipped when the wither is silent.
- [ ] Recipes and trades aside, no other building-block behavior changes were found in Block.java, SlabBlock, StairBlock or WallBlock beyond the fluid-API refactor.

## 5. Language

- [ ] Splashes: `assets/minecraft/texts/splashes.txt` is **unchanged** (diff empty, 454 lines).
- [ ] `assets/minecraft/lang/en_us.json` diff: 248 keys added, 4 removed, 312 values changed. The full lists are below.
### Added keys, gameplay (59 of 248 added)
- [ ] `advancements.adventure.what_goes_around_comes_around.description` = "Kill a Frostbite using an Ice Ball"
- [ ] `advancements.adventure.what_goes_around_comes_around.title` = "What Goes Around Comes Around"
- [ ] `biome.minecraft.ice_caves` = "Ice Caves"
- [ ] `block.minecraft.brittle_ice` = "Brittle Ice"
- [ ] `block.minecraft.calcite_pillar` = "Calcite Pillar"
- [ ] `block.minecraft.calcite_slab` = "Calcite Slab"
- [ ] `block.minecraft.calcite_stairs` = "Calcite Stairs"
- [ ] `block.minecraft.calcite_wall` = "Calcite Wall"
- [ ] `block.minecraft.chiseled_packed_ice` = "Chiseled Packed Ice"
- [ ] `block.minecraft.chiseled_soulshale` = "Chiseled Soulshale"
- [ ] `block.minecraft.ice_crystal` = "Ice Crystal"
- [ ] `block.minecraft.icicle` = "Icicle"
- [ ] `block.minecraft.packed_ice_brick_slab` = "Packed Ice Brick Slab"
- [ ] `block.minecraft.packed_ice_brick_stairs` = "Packed Ice Brick Stairs"
- [ ] `block.minecraft.packed_ice_brick_wall` = "Packed Ice Brick Wall"
- [ ] `block.minecraft.packed_ice_bricks` = "Packed Ice Bricks"
- [ ] `block.minecraft.polished_calcite` = "Polished Calcite"
- [ ] `block.minecraft.polished_calcite_brick_slab` = "Polished Calcite Brick Slab"
- [ ] `block.minecraft.polished_calcite_brick_stairs` = "Polished Calcite Brick Stairs"
- [ ] `block.minecraft.polished_calcite_brick_wall` = "Polished Calcite Brick Wall"
- [ ] `block.minecraft.polished_calcite_bricks` = "Polished Calcite Bricks"
- [ ] `block.minecraft.polished_calcite_slab` = "Polished Calcite Slab"
- [ ] `block.minecraft.polished_calcite_stairs` = "Polished Calcite Stairs"
- [ ] `block.minecraft.polished_calcite_wall` = "Polished Calcite Wall"
- [ ] `block.minecraft.snow_brick_slab` = "Snow Brick Slab"
- [ ] `block.minecraft.snow_brick_stairs` = "Snow Brick Stairs"
- [ ] `block.minecraft.snow_brick_wall` = "Snow Brick Wall"
- [ ] `block.minecraft.snow_bricks` = "Snow Bricks"
- [ ] `container.creative_inventory` = "Creative Inventory"
- [ ] `effect.minecraft.freezing` = "Freezing"
- [ ] `entity.minecraft.frostbite` = "Frostbite"
- [ ] `entity.minecraft.ice_ball` = "Ice Ball"
- [ ] `item.minecraft.frostbite_spawn_egg` = "Frostbite Spawn Egg"
- [ ] `item.minecraft.ice_ball` = "Ice Ball"
- [ ] `item.minecraft.lingering_potion.effect.freezing` = "Lingering Potion of Freezing"
- [ ] `item.minecraft.music_disc_flurry` = "Music Disc"
- [ ] `item.minecraft.potion.effect.freezing` = "Potion of Freezing"
- [ ] `item.minecraft.splash_potion.effect.freezing` = "Splash Potion of Freezing"
- [ ] `item.minecraft.tipped_arrow.effect.freezing` = "Arrow of Freezing"
- [ ] `jukebox_song.minecraft.flurry` = "Hyper Potions - Flurry"
- [ ] `record.playingNext` = "Playing Next"
- [ ] `subtitles.block.brittle_ice.crack` = "Brittle Ice cracks"
- [ ] `subtitles.block.ice_crystal.ambient` = "Ice Crystal chimes"
- [ ] `subtitles.block.ice.ambient` = "Ice resonates"
- [ ] `subtitles.block.icicle.land` = "Icicle crashes down"
- [ ] `subtitles.block.sulfur_spike.land` = "Sulfur Spike crashes down"
- [ ] `subtitles.entity.drowned.converted_to_zombie` = "Drowned converts to Zombie"
- [ ] `subtitles.entity.frostbite.ambient` = "Frostbite groans"
- [ ] `subtitles.entity.frostbite.converted_to_zombie` = "Frostbite converts to Zombie"
- [ ] `subtitles.entity.frostbite.death` = "Frostbite dies"
- [ ] `subtitles.entity.frostbite.hurt` = "Frostbite is hurt"
- [ ] `subtitles.entity.frostbite.throw` = "Frostbite throws"
- [ ] `subtitles.entity.ice_ball.break` = "Ice Ball hits"
- [ ] `subtitles.entity.ice_ball.throw` = "Ice Ball is thrown"
- [ ] `subtitles.entity.parrot.imitate.frostbite` = "Parrot groans"
- [ ] `subtitles.entity.snow_golem.shoot` = "Snow Golem throws"
- [ ] `subtitles.entity.zombie_nautilus.riding` = "Zombie Nautilus bubbles"
- [ ] `subtitles.entity.zombie.converted_to_frostbite` = "Zombie converts to Frostbite"
- [ ] `subtitles.event.mob_effect.freezing` = "Starting to freeze"

### Added keys, UI/commands/narration (189), keys only
- [ ] `argument.pattern.not_matched`, `argument.resource.validation_failed`, `argument.time.tick_count_too_high`, `chat_screen.restricted_friends_only`, `chat_screen.restricted_friends_only.narration`, `commands.effect.clear.specific.failed.multiple`, `commands.effect.clear.specific.failed.single`, `commands.effect.clear.specific.success.multiple.new`, `commands.effect.clear.specific.success.single.new`, `commands.effect.give.failed.multiple`, `commands.effect.give.failed.single`, `commands.effect.give.success.multiple.new`, `commands.effect.give.success.single.new`, `commands.effect.no_effects`, `commands.function.error.validation`, `gui.abuseReport.discard.draft.overwrite`, `gui.abuseReport.draft.quittotitle.content.new`, `gui.abuseReport.draft.quittotitle.discard`, `gui.abuseReport.draft.quittotitle.title.new`, `gui.abuseReport.draft.quittotitle.tooltip`, `gui.abuseReport.draft.quittotitle.tooltip.chat`, `gui.abuseReport.draft.restart`, `gui.abuseReport.draft.title.new`, `gui.friends.accept.tooltip`, `gui.friends.decline.tooltip`, `gui.friends.narration.button.player_options`, `gui.friends.narration.button.send_request`, `gui.friends.pending.disabled`, `gui.friends.pending.search`, `gui.friends.pending.search.no_results`, `gui.friends.player_options`, `gui.friends.search.no_results`, `gui.friends.sort`, `gui.friends.sort.alphabetical`, `gui.friends.sort.presence`, `gui.friends.title`, `gui.loading`, `gui.player_interactions.empty`, `gui.player_interactions.empty_blocked`, `gui.player_interactions.empty_blocked.link`, `gui.player_interactions.empty_muted`, `gui.player_interactions.manage_account_footer`, `gui.player_interactions.narration.entry`, `gui.player_interactions.search`, `gui.player_interactions.search.no_results`, `gui.player_interactions.tab_all`, `gui.player_interactions.tab_blocked`, `gui.player_interactions.tab_muted`, `gui.player_interactions.title`, `gui.player_interactions.title.full`, `gui.player_options.cancel_request`, `gui.player_options.edit_report`, `gui.player_options.edit_report.tooltip`, `gui.player_options.mute`, `gui.player_options.mute.feedback`, `gui.player_options.mute.tooltip`, `gui.player_options.report`, `gui.player_options.report.tooltip`, `gui.player_options.report.tooltip.disabled`, `gui.player_options.send_request`, `gui.player_options.status.blocked`, `gui.player_options.status.muted`, `gui.player_options.title`, `gui.player_options.unfriend`, `gui.player_options.unfriend.confirm`, `gui.player_options.unmute`, `gui.player_options.unmute.feedback`, `gui.player_options.unmute.tooltip`, `gui.recipebook.filter`, `gui.recipebook.moreRecipes.keyboard`, `gui.recipebook.search_box`, `gui.recipebook.tab.all`, `gui.recipebook.tooltip`, `gui.socialInteractions.tooltip.report.skin_not_reportable`, `key.debug.environmentAttributeOptions`, `key.otherPlayers`, `key.repeatRecipeOrTrade`, `mco.upload.error.block_list_failed`, `mco.upload.error.block_upload_failed`, `mco.upload.error.block_upload_unsupported`, `mco.upload.error.complete_failed`, `mco.upload.error.file_too_large`, `mco.upload.error.invalid_archive`, `mco.upload.error.invalid_session_response`, `mco.upload.error.processing_failed`, `mco.upload.error.renew_failed`, `mco.upload.error.session_create_failed`, `mco.upload.error.single_upload_failed`, `mco.upload.error.unexpected_processing_status`, `mco.upload.error.unsupported_archive_format`, `mco.upload.processing`, `menu.custom_screen_info.input.tooltip`, `menu.disconnect.draft_report.tooltip`, `menu.other_players`, `menu.other_players.no_players`, `multiplayer.disconnect.rejected`, `multiplayer.other_players.not_available`, `narration.button.state.closed`, `narration.button.state.open`, `narration.button.usage.focused.close`, `narration.button.usage.focused.open`, `narration.button.usage.hovered.close`, `narration.button.usage.hovered.open`, `narration.count_and_item`, `narration.crafter_slot.disabled`, `narration.crafter_slot.usage.focused.disable`, `narration.crafter_slot.usage.focused.enable`, `narration.crafter_slot.usage.hovered.disable`, `narration.crafter_slot.usage.hovered.enable`, `narration.creative_item`, `narration.creative_item.located`, `narration.group_navigation.usage.direction.any`, `narration.group_navigation.usage.direction.horizontal`, `narration.group_navigation.usage.direction.vertical`, `narration.group_navigation.usage.enchantment`, `narration.group_navigation.usage.pattern`, `narration.group_navigation.usage.recipe`, `narration.group_navigation.usage.slot`, `narration.group_navigation.usage.trade`, `narration.recipe_book.current_page`, `narration.recipe.usage.close`, `narration.recipe.usage.more.keyboard`, `narration.slot_group.equipment`, `narration.slot_group.fuel`, `narration.slot_group.hotbar`, `narration.slot_group.input`, `narration.slot_group.inventory`, `narration.slot_group.result`, `narration.slot_group.second_input`, `narration.slot.empty`, `narration.slot.name.body`, `narration.slot.name.carpet`, `narration.slot.name.chest`, `narration.slot.name.feet`, `narration.slot.name.fuel`, `narration.slot.name.head`, `narration.slot.name.ingredient`, `narration.slot.name.legs`, `narration.slot.name.offhand`, `narration.slot.name.potion`, `narration.slot.name.result`, `narration.slot.name.saddle`, `narration.slot.title`, `narration.slot.title.named`, `narration.slot.title.named.located`, `narration.slot.usage.focused.destroy`, `narration.slot.usage.focused.pick_up`, `narration.slot.usage.focused.place`, `narration.slot.usage.hovered.destroy`, `narration.slot.usage.hovered.pick_up`, `narration.slot.usage.hovered.place`, `narration.tab_navigation.bottom`, `narration.tab_navigation.mac.usage`, `narration.tab_navigation.top`, `narration.trade_offer.one_cost`, `narration.trade_offer.out_of_stock`, `narration.trade_offer.two_costs`, `narrator.button.recipe_book`, `narrator.position.grid`, `narrator.position.slot`, `narrator.position.tab.index`, `narrator.position.tab.named`, `options.graphicsApi.vulkanNonExperimental`, `options.improvedFog`, `options.improvedFog.tooltip`, `options.improvedTransparency.oitWithCloudsInFog.tooltip`, `options.improvedTransparency.oitWithImprovedFog.tooltip`, `options.preferDXGIPresent.experimental`, `options.preferDXGIPresent.tooltip.experimental`, `options.sharePresence.all.description`, `options.sharePresence.all.name`, `options.sharePresence.limited.description`, `options.sharePresence.limited.name`, `options.sharePresence.none.description`, `options.sharePresence.none.name`, `recipe_book_category.minecraft.blast_furnace_blocks`, `recipe_book_category.minecraft.blast_furnace_misc`, `recipe_book_category.minecraft.crafting_building_blocks`, `recipe_book_category.minecraft.crafting_equipment`, `recipe_book_category.minecraft.crafting_misc`, `recipe_book_category.minecraft.crafting_redstone`, `recipe_book_category.minecraft.furnace_blocks`, `recipe_book_category.minecraft.furnace_food`, `recipe_book_category.minecraft.furnace_misc`, `recipe_book_category.minecraft.smoker_food`, `restrictions_screen.permission.receive_player_messages.friends_only`, `tutorial.other_players.description`, `tutorial.other_players.title`, `upgradeWorld.progress.type.regions`

### Removed keys (4)
- [ ] `item.minecraft.music_disc_bounce.desc` (was "fingerspit - Bounce")
- [ ] `item.minecraft.music_disc_lava_chicken.desc` (was "Hyper Potions - Lava Chicken")
- [ ] `item.minecraft.music_disc_tears.desc` (was "Amos Roddy - Tears")
- [ ] `upgradeWorld.progress.type.region` (was "Upgrading regions")

### Changed values, non-subtitle (118)
- [ ] `advancement.advancementNotFound`: "Unknown advancement: %s" -> "Unknown advancement '%s'"
- [ ] `advancements.husbandry.uh_oh.title`: "Uh Oh" -> "Uh-Oh"
- [ ] `advancements.nether.distract_piglin.title`: "Oh Shiny" -> "Oooh, Shiny!"
- [ ] `argument.anchor.invalid`: "Invalid entity anchor position %s" -> "Invalid entity anchor position '%s'"
- [ ] `argument.component.invalid`: "Invalid chat component: %s" -> "Invalid chat component '%s'"
- [ ] `argument.gamemode.invalid`: "Unknown game mode: %s" -> "Unknown game mode '%s'"
- [ ] `argument.id.unknown`: "Unknown ID: %s" -> "Unknown ID '%s'"
- [ ] `argument.style.invalid`: "Invalid style: %s" -> "Invalid style '%s'"
- [ ] `argument.swing_animation.invalid`: "Unknown swing animation type: %s" -> "Unknown swing animation type '%s'"
- [ ] `arguments.function.unknown`: "Unknown function %s" -> "Unknown function '%s'"
- [ ] `chat_restriction.chat_disabled_by_options.action`: "Go to the Chat Settings screen" -> "Go to the Chat Settings Screen"
- [ ] `chat_screen.title`: "Chat screen" -> "Chat Screen"
- [ ] `chat.copy.click`: "Click to Copy to Clipboard" -> "Click to copy to clipboard"
- [ ] `chat.queue.tooltip`: "Click to display next message" -> "Click to display the next message"
- [ ] `command.compute.result.named.invalid`: "%s returned invalid value (%s)" -> "%s returned an invalid value (%s)"
- [ ] `command.compute.result.unnamed.invalid`: "Number provider returned invalid value (%s)" -> "Number provider returned an invalid value (%s)"
- [ ] `command.trailing_data`: "Trailing data found: %s" -> "Trailing data found '%s'"
- [ ] `commands.data.modify.invalid_index`: "Invalid list index: %s" -> "Invalid list index '%s'"
- [ ] `commands.data.modify.invalid_substring`: "Invalid substring indices: %s to %s" -> "Invalid substring indices '%s' to '%s'"
- [ ] `commands.function.error.argument_not_compound`: "Invalid argument type: %s. Expected Compound" -> "Invalid argument type '%s'. Expected Compound"
- [ ] `commands.item.source.no_such_slot.unnamed`: "The source does not have specified slots" -> "The source does not have the specified slots"
- [ ] `commands.item.target.failed`: "No targets accepted items into specified slots" -> "No targets accepted items into the specified slots"
- [ ] `commands.item.target.failed.known_item`: "No targets accepted item %s into specified slots" -> "No targets accepted item %s into the specified slots"
- [ ] `commands.item.target.no_such_slot.unnamed`: "The target does not have specified slots" -> "The target does not have the specified slots"
- [ ] `commands.posteffect.list.success`: "Player %s has %s post effects: %s" -> "Player %s has %s post effect(s): %s"
- [ ] `commands.scoreboard.objectives.modify.objectiveFormat.clear`: "Cleared default number format of objective %s" -> "Cleared the default number format of objective %s"
- [ ] `commands.scoreboard.objectives.modify.objectiveFormat.set`: "Changed default number format of objective %s" -> "Changed the default number format of objective %s"
- [ ] `commands.scoreboard.players.display.name.clear.success.multiple`: "Cleared display name for %s entities in %s" -> "Cleared the display name for %s entities in %s"
- [ ] `commands.scoreboard.players.display.name.clear.success.single`: "Cleared display name for %s in %s" -> "Cleared the display name for %s in %s"
- [ ] `commands.scoreboard.players.display.name.set.success.multiple`: "Changed display name to %s for %s entities in %s" -> "Changed the display name to %s for %s entities in %s"
- [ ] `commands.scoreboard.players.display.name.set.success.single`: "Changed display name to %s for %s in %s" -> "Changed the display name to %s for %s in %s"
- [ ] `commands.scoreboard.players.display.numberFormat.clear.success.multiple`: "Cleared number format for %s entities in %s" -> "Cleared the number format for %s entities in %s"
- [ ] `commands.scoreboard.players.display.numberFormat.clear.success.single`: "Cleared number format for %s in %s" -> "Cleared the number format for %s in %s"
- [ ] `commands.scoreboard.players.display.numberFormat.set.success.multiple`: "Changed number format for %s entities in %s" -> "Changed the number format for %s entities in %s"
- [ ] `commands.scoreboard.players.display.numberFormat.set.success.single`: "Changed number format for %s in %s" -> "Changed the number format for %s in %s"
- [ ] `commands.spreadplayers.failed.invalid.height`: "Invalid maxHeight %s; expected higher than world minimum %s" -> "Invalid maxHeight '%s'; expected higher than world minimum %s"
- [ ] `disconnect.loginFailedInfo.invalidSession`: "Invalid session (Try restarting your game and the launcher)" -> "Invalid session (try restarting your game and the launcher)"
- [ ] `gamerule.doMobLoot.description`: "Controls resource drops from mobs, including experience orbs." -> "Controls resource drops from mobs, including Experience Orbs."
- [ ] `gamerule.doTileDrops.description`: "Controls resource drops from blocks, including experience orbs." -> "Controls resource drops from blocks, including Experience Orbs."
- [ ] `gui.abuseReport.send.json_error`: "Encountered malformed payload while sending your report." -> "Encountered a malformed payload while sending your report."
- [ ] `gui.friends.empty_nickname`: "Enter a valid profile name before sending a friend request" -> "Enter a valid profile name before sending a friend request."
- [ ] `gui.friends.error.failed.message`: "Unable to send your friend request right now" -> "Unable to send your friend request right now."
- [ ] `gui.friends.error.generic`: "Something's wrong � with this feature, not your friends!

Try again later." -> "Something's wrong - with this feature, not your friends!

Try again later."
- [ ] `gui.friends.error.user_may_lack_active_profile`: "There is no player with this profile name" -> "There is no player with this profile name."
- [ ] `gui.friends.narration.button.accept`: "Press enter to accept friend request from %s" -> "Press enter to accept the friend request from %s"
- [ ] `gui.friends.narration.button.cancel_request`: "Press enter to cancel friend request to %s" -> "Press enter to cancel the friend request to %s"
- [ ] `gui.friends.narration.button.decline`: "Press enter to decline friend request from %s" -> "Press enter to decline the friend request from %s"
- [ ] `gui.friends.narration.button.unfriend`: "Press enter to remove %s from Friends List" -> "Press enter to remove %s from your Friends List"
- [ ] `gui.friends.toast.friend_removed.message`: "%s has been removed from your friends list" -> "%s has been removed from your Friends List"
- [ ] `gui.friends.toast.invite_from_friend.message`: "%s invited you to join their world. Open the friends list by pressing "%s" to join" -> "%s invited you to join their world. Open the Friends List by pressing "%s" to join"
- [ ] `gui.friends.toast.join_request.message`: "%s wants to join your world. Open the Friends list with "%s" or from the game menu." -> "%s wants to join your world. Open the Friends List with "%s" or from the game menu."
- [ ] `gui.recipebook.moreRecipes`: "Right Click for More" -> "Right-Click for More"
- [ ] `gui.socialInteractions.tooltip.report.chat_disabled_or_blocked`: "This player can't be reported because chat is disabled or blocked" -> "This player can't be reported because chat is disabled or blocked."
- [ ] `gui.socialInteractions.tooltip.report.disabled`: "The reporting service is unavailable" -> "The reporting service is unavailable."
- [ ] `gui.socialInteractions.tooltip.report.no_messages`: "No reportable messages from player %s" -> "No reportable messages from player %s."
- [ ] `gui.socialInteractions.tooltip.report.not_reportable`: "This player can't be reported, because their chat messages can't be verified on this server" -> "This player can't be reported because their chat messages can't be verified on this server."
- [ ] `item_modifier.unknown`: "Unknown item modifier: %s" -> "Unknown item modifier '%s'"
- [ ] `jigsaw_block.final_state`: "Turns into:" -> "Turns Into:"
- [ ] `jigsaw_block.placement_priority.tooltip`: "When this Jigsaw block connects to a piece, this is the order in which that piece is processed for connections in the wider structure.

Pieces will be processed in descending priority with insertion order breaking ties." -> "When this Jigsaw Block connects to a piece, this is the order in which that piece is processed for connections in the wider structure.

Pieces will be processed in descending priority with insertion order breaking ties."
- [ ] `jigsaw_block.selection_priority.tooltip`: "When the parent piece is being processed for connections, this is the order in which this Jigsaw block attempts to connect to its target piece.

Jigsaws will be processed in descending priority with random ordering breaking ties." -> "When the parent piece is being processed for connections, this is the order in which this Jigsaw Block attempts to connect to its target piece.

Jigsaws will be processed in descending priority with random ordering breaking ties."
- [ ] `key.debug.fpsCharts`: "Fps Charts" -> "FPS Charts"
- [ ] `key.friends`: "Friends Screen" -> "Friends"
- [ ] `mco.backup.restoring`: "Restoring your Realm" -> "Restoring Your Realm"
- [ ] `mco.configure.world.closing`: "Temporarily closing the Realm..." -> "Temporarily Closing the Realm..."
- [ ] `mco.configure.world.invite_codes.subtitle`: "You can add up to %s invite codes and share them so people can join your Realm" -> "You can create up to %s invite codes and share them so people can join your Realm"
- [ ] `mco.create.world.reset.title`: "Creating world..." -> "Creating World..."
- [ ] `mco.download.preparing`: "Preparing download" -> "Preparing Download"
- [ ] `mco.reset.world.resetting.screen.title`: "Resetting world..." -> "Resetting World..."
- [ ] `mco.selectServer.joinCode`: "Join Code" -> "Invite Code"
- [ ] `mco.selectServer.joinCode.hint`: "Enter join code" -> "Enter invite code"
- [ ] `mco.selectServer.joinCode.title`: "Enter Join Code" -> "Enter Invite Code"
- [ ] `mco.snapshotRealmsPopup.message`: "Realms is now available in Snapshots starting with Snapshot 23w41a. Every Realms subscription comes with a free Snapshot Realm that is separate from your normal Java Realm!" -> "Realms is now available in snapshots starting with Snapshot 23w41a. Every Realms subscription comes with a free Snapshot Realm that is separate from your normal Java Realm!"
- [ ] `mco.snapshotRealmsPopup.title`: "Realms is now available in Snapshots" -> "Realms is now available in snapshots"
- [ ] `menu.playerReporting.no_players`: "There are no players to report in this world" -> "There are no players to report in this world."
- [ ] `multiplayer.socialInteractions.not_available`: "Social Interactions are only available in Multiplayer worlds" -> "Social Interactions are only available in multiplayer worlds"
- [ ] `multiplayer.status.unrequested`: "Received unrequested status" -> "Received an unrequested status"
- [ ] `narration.button.usage.hovered`: "Left click to activate" -> "Left-click to activate"
- [ ] `narration.checkbox.usage.hovered`: "Left click to toggle" -> "Left-click to toggle"
- [ ] `narration.checkbox.usage.hovered.check`: "Left click to check" -> "Left-click to check"
- [ ] `narration.checkbox.usage.hovered.uncheck`: "Left click to uncheck" -> "Left-click to uncheck"
- [ ] `narration.cycle_button.usage.hovered`: "Left click to switch to %s" -> "Left-click to switch to %s"
- [ ] `narration.link.usage.hovered`: "Left click to follow the link" -> "Left-click to follow the link"
- [ ] `narration.recipe.usage`: "Left click to select" -> "Left-click to select"
- [ ] `narration.recipe.usage.more`: "Right click to show more recipes" -> "Right-click to show more recipes"
- [ ] `narration.tab_navigation.usage`: "Press Ctrl and Tab to switch between tabs" -> "Press Control and Tab to switch between tabs"
- [ ] `options.accessibility.narrator_hotkey.mac.tooltip`: "Allows the Narrator to be toggled on and off with 'Cmd + B'." -> "Allows the Narrator to be toggled on and off with 'Command + B'."
- [ ] `options.accessibility.narrator_hotkey.tooltip`: "Allows the Narrator to be toggled on and off with 'Ctrl + B'." -> "Allows the Narrator to be toggled on and off with 'Control + B'."
- [ ] `options.allowFriendRequests.tooltip`: "Allow other players to send you friend requests" -> "Allow other players to send you friend requests."
- [ ] `options.ctrlClickEmulatesRightClick`: "Right Click Emulation" -> "Right-Click Emulation"
- [ ] `options.debugGuiScale.tooltip`: "Overrides the debug overlay with a different GUI scale than the rest of the game" -> "Overrides the debug overlay with a different GUI scale than the rest of the game."
- [ ] `options.directionalAudio.off.tooltip`: "Classic Stereo sound." -> "Classic stereo sound."
- [ ] `options.framerate`: "%s fps" -> "%s FPS"
- [ ] `options.fullscreen.entry`: "%sx%s@%s (%sbit)" -> "%sx%s@%s (%s-bit)"
- [ ] `options.fullscreen.unavailable`: "Setting unavailable" -> "Setting Unavailable"
- [ ] `options.hideMatchedNames.tooltip`: "3rd-party Servers may send chat messages in non-standard formats.
With this option on, hidden players will be matched based on chat sender names." -> "Third-party servers may send chat messages in non-standard formats.
With this option on, muted players will be matched based on chat sender names."
- [ ] `options.inactivityFpsLimit`: "Reduce fps when" -> "Reduce FPS When"
- [ ] `options.inGameNotification.tooltip`: "Show Friend notifications in-game" -> "Show friend notifications in-game."
- [ ] `options.macFullscreenMenuVisibility.tooltip`: "Whether the macOS Menu bar and Dock can be revealed by moving the mouse to the edge of the screen while in non-exclusive fullscreen." -> "Whether the macOS menu bar and Dock can be revealed by moving the mouse to the edge of the screen while in non-exclusive fullscreen."
- [ ] `options.quitShortcuts.tooltip`: "Whether keyboard shortcuts can close the game. When off, Alt + F4 on Windows and Linux, and Cmd + Q on macOS, will no longer quit the game." -> "Whether keyboard shortcuts can close the game. When off, Alt + F4 on Windows and Linux, and Command + Q on macOS, will no longer quit the game."
- [ ] `options.vignette.tooltip`: "This is a subtle texture over the game screen used for reducing brightness towards the edges of the screen and warning about the world border." -> "This is a subtle texture over the game screen used for reducing brightness toward the edges of the screen and warning about the world border."
- [ ] `options.worldOptions.guest.command_access.disabled.scope.tooltip`: "Cannot change the command access of players joining your world when the world's multiplayer scope is set to "Off"." -> "Cannot change the command access of players joining your world when "LAN" is set to "OFF"."
- [ ] `options.worldOptions.guest.command_access.tooltip`: "Controls whether players that join your world can use commands or not." -> "Controls whether players that join your world can use commands."
- [ ] `options.worldOptions.guest.force_game_mode.off.scope.tooltip`: "Cannot change this setting when the world's multiplayer scope is set to "Off"." -> "Cannot change this setting when "LAN" is set to "OFF"."
- [ ] `particle.notFound`: "Unknown particle: %s" -> "Unknown particle '%s'"
- [ ] `predicate.unknown`: "Unknown predicate: %s" -> "Unknown predicate '%s'"
- [ ] `recipe.notFound`: "Unknown recipe: %s" -> "Unknown recipe '%s'"
- [ ] `selectWorld.backupQuestion.experimental`: "Worlds using Experimental Settings are not supported" -> "Worlds using experimental settings are not supported"
- [ ] `snbt.parser.invalid_codepoint`: "Invalid Unicode character value: %s" -> "Invalid Unicode character value '%s'"
- [ ] `snbt.parser.no_such_operation`: "No such operation: %s" -> "No such operation '%s'"
- [ ] `snbt.parser.number_parse_failure`: "Failed to parse number: %s" -> "Failed to parse number '%s'"
- [ ] `telemetry.event.world_loaded.description`: "Knowing how players play Minecraft (such as Game Mode, client or server modded, and game version) allows us to focus game updates to improve the areas that players care about most.
The World Loaded event is paired with the World Unloaded event to calculate how long the play session has lasted." -> "Knowing how players play Minecraft (such as game mode, client or server modded, and game version) allows us to focus game updates to improve the areas that players care about most.
The World Loaded event is paired with the World Unloaded event to calculate how long the play session has lasted."
- [ ] `test.error.invalid_block_type`: "Unexpected block type found: %s" -> "Unexpected block type found '%s'"
- [ ] `test.error.sequence.invalid_tick`: "Succeeded in invalid tick: expected %s" -> "Succeeded in an invalid tick: expected %s"
- [ ] `test.error.wrong_block_entity`: "Wrong block entity type: %s" -> "Wrong block entity type '%s'"
- [ ] `test.player.coordinates`: "Player coordinates: [%s, %s, %s] in %s" -> "Player coordinates: %s, %s, %s in %s"
- [ ] `test.run.coordinates`: "Test coordinates: [%s, %s, %s] in %s" -> "Test coordinates: %s, %s, %s in %s"
- [ ] `title.multiplayer.other`: "Multiplayer (3rd-party Server)" -> "Multiplayer (Third-party Server)"
- [ ] `tutorial.bundleInsert.description`: "Right Click to add items" -> "Right-click to add items"

### Changed values, subtitles (194) - mostly passive-voice rewording ('X hurts' -> 'X is hurt', 'X placed' -> 'X is placed')
- `subtitles.block.anvil.destroy`: "Anvil destroyed" -> "Anvil breaks"
- `subtitles.block.anvil.land`: "Anvil landed" -> "Anvil lands"
- `subtitles.block.anvil.use`: "Anvil used" -> "Anvil is used"
- `subtitles.block.beacon.power_select`: "Beacon power selected" -> "Beacon power is selected"
- `subtitles.block.chest.locked`: "Chest locked" -> "Chest lock clangs"
- `subtitles.block.composter.empty`: "Composter emptied" -> "Composter empties"
- `subtitles.block.composter.fill`: "Composter filled" -> "Composter is filled"
- `subtitles.block.dispenser.dispense`: "Dispensed item" -> "Dispenser dispenses item"
- `subtitles.block.dispenser.fail`: "Dispenser failed" -> "Dispenser fails"
- `subtitles.block.enchantment_table.use`: "Enchanting Table used" -> "Enchanting Table is used"
- `subtitles.block.fire.extinguish`: "Fire extinguished" -> "Fire extinguishes"
- `subtitles.block.generic.place`: "Block placed" -> "Block is placed"
- `subtitles.block.grindstone.use`: "Grindstone used" -> "Grindstone is used"
- `subtitles.block.growing_plant.crop`: "Plant cropped" -> "Plant is cropped"
- `subtitles.block.honey_block.slide`: "Sliding down a honey block" -> "Sliding down a Honey Block"
- `subtitles.block.pointed_dripstone.land`: "Stalactite crashes down" -> "Pointed Dripstone crashes down"
- `subtitles.block.poplar_leaves.ambient`: "Leaves rustling" -> "Leaves rustle"
- `subtitles.block.shelf.place_item`: "Item placed" -> "Item is placed"
- `subtitles.block.shelf.take_item`: "Item taken" -> "Item is taken"
- `subtitles.block.smithing_table.use`: "Smithing Table used" -> "Smithing Table is used"
- `subtitles.chiseled_bookshelf.insert`: "Book placed" -> "Book is placed"
- `subtitles.chiseled_bookshelf.insert_enchanted`: "Enchanted Book placed" -> "Enchanted Book is placed"
- `subtitles.chiseled_bookshelf.take`: "Book taken" -> "Book is taken"
- `subtitles.chiseled_bookshelf.take_enchanted`: "Enchanted Book taken" -> "Enchanted Book is taken"
- `subtitles.entity.allay.hurt`: "Allay hurts" -> "Allay is hurt"
- `subtitles.entity.armadillo.hurt`: "Armadillo hurts" -> "Armadillo is hurt"
- `subtitles.entity.armor_stand.fall`: "Something fell" -> "Something falls"
- `subtitles.entity.arrow.hit_player`: "Player hit" -> "Player is hit"
- `subtitles.entity.arrow.shoot`: "Arrow fired" -> "Bow fires"
- `subtitles.entity.axolotl.hurt`: "Axolotl hurts" -> "Axolotl is hurt"
- `subtitles.entity.baby_cat.hurt`: "Kitten hurts" -> "Kitten is hurt"
- `subtitles.entity.baby_chicken.hurts`: "Chick hurts" -> "Chick is hurt"
- `subtitles.entity.baby_horse.hurt`: "Foal hurts" -> "Foal is hurt"
- `subtitles.entity.baby_nautilus.hurt`: "Baby Nautilus hurts" -> "Baby Nautilus is hurt"
- `subtitles.entity.baby_nautilus.hurt_land`: "Baby Nautilus hurts" -> "Baby Nautilus is hurt"
- `subtitles.entity.baby_pig.hurt`: "Baby Pig hurts" -> "Baby Pig is hurt"
- `subtitles.entity.baby_wolf.hurt`: "Puppy hurts" -> "Puppy is hurt"
- `subtitles.entity.bat.hurt`: "Bat hurts" -> "Bat is hurt"
- `subtitles.entity.bee.hurt`: "Bee hurts" -> "Bee is hurt"
- `subtitles.entity.blaze.hurt`: "Blaze hurts" -> "Blaze is hurt"
- `subtitles.entity.bogged.hurt`: "Bogged hurts" -> "Bogged is hurt"
- `subtitles.entity.breeze.hurt`: "Breeze hurts" -> "Breeze is hurt"
- `subtitles.entity.camel_husk.hurt`: "Camel Husk hurts" -> "Camel Husk is hurt"
- `subtitles.entity.camel_husk.saddle`: "Saddle equips" -> "Saddle is equipped"
- `subtitles.entity.camel.hurt`: "Camel hurts" -> "Camel is hurt"
- `subtitles.entity.camel.saddle`: "Saddle equips" -> "Saddle is equipped"
- `subtitles.entity.cat.hurt`: "Cat hurts" -> "Cat is hurt"
- `subtitles.entity.chicken.hurt`: "Chicken hurts" -> "Chicken is hurt"
- `subtitles.entity.cod.hurt`: "Cod hurts" -> "Cod is hurt"
- `subtitles.entity.copper_golem_oxidized.hurt`: "Copper Golem hurts" -> "Copper Golem is hurt"
- `subtitles.entity.copper_golem_weathered.hurt`: "Copper Golem hurts" -> "Copper Golem is hurt"
- `subtitles.entity.copper_golem.hurt`: "Copper Golem hurts" -> "Copper Golem is hurt"
- `subtitles.entity.cow.hurt`: "Cow hurts" -> "Cow is hurt"
- `subtitles.entity.creeper.hurt`: "Creeper hurts" -> "Creeper is hurt"
- `subtitles.entity.dolphin.hurt`: "Dolphin hurts" -> "Dolphin is hurt"
- `subtitles.entity.donkey.chest`: "Donkey Chest equips" -> "Donkey Chest is equipped"
- `subtitles.entity.donkey.hurt`: "Donkey hurts" -> "Donkey is hurt"
- `subtitles.entity.drowned.hurt`: "Drowned hurts" -> "Drowned is hurt"
- `subtitles.entity.elder_guardian.hurt`: "Elder Guardian hurts" -> "Elder Guardian is hurt"
- `subtitles.entity.ender_dragon.hurt`: "Dragon hurts" -> "Dragon is hurt"
- `subtitles.entity.enderman.hurt`: "Enderman hurts" -> "Enderman is hurt"
- `subtitles.entity.endermite.hurt`: "Endermite hurts" -> "Endermite is hurt"
- `subtitles.entity.evoker.hurt`: "Evoker hurts" -> "Evoker is hurt"
- `subtitles.entity.evoker.prepare_summon`: "Evoker prepares summoning" -> "Evoker prepares to summon"
- `subtitles.entity.evoker.prepare_wololo`: "Evoker prepares charming" -> "Evoker prepares to charm"
- `subtitles.entity.fishing_bobber.retrieve`: "Bobber retrieved" -> "Bobber is retrieved"
- `subtitles.entity.fishing_bobber.throw`: "Bobber thrown" -> "Bobber is thrown"
- `subtitles.entity.fox.aggro`: "Fox angers" -> "Fox gets angry"
- `subtitles.entity.fox.hurt`: "Fox hurts" -> "Fox is hurt"
- `subtitles.entity.frog.hurt`: "Frog hurts" -> "Frog is hurt"
- `subtitles.entity.generic.big_fall`: "Something fell" -> "Something falls"
- `subtitles.entity.generic.burn`: "Burning" -> "Something burns"
- `subtitles.entity.generic.death`: "Dying" -> "Something dies"
- `subtitles.entity.generic.drink`: "Sipping" -> "Something sips"
- `subtitles.entity.generic.eat`: "Eating" -> "Something eats"
- `subtitles.entity.generic.explode`: "Explosion" -> "Something explodes"
- `subtitles.entity.generic.hurt`: "Something hurts" -> "Something is hurt"
- `subtitles.entity.generic.splash`: "Splashing" -> "Something splashes"
- `subtitles.entity.generic.swim`: "Swimming" -> "Something swims"
- `subtitles.entity.ghast.hurt`: "Ghast hurts" -> "Ghast is hurt"
- `subtitles.entity.ghastling.hurt`: "Ghastling hurts" -> "Ghastling is hurt"
- `subtitles.entity.glow_item_frame.add_item`: "Glow Item Frame fills" -> "Glow Item Frame is filled"
- `subtitles.entity.glow_item_frame.break`: "Glow Item Frame broken" -> "Glow Item Frame breaks"
- `subtitles.entity.glow_item_frame.place`: "Glow Item Frame placed" -> "Glow Item Frame is placed"
- `subtitles.entity.glow_squid.hurt`: "Glow Squid hurts" -> "Glow Squid is hurt"
- `subtitles.entity.goat.hurt`: "Goat hurts" -> "Goat is hurt"
- `subtitles.entity.guardian.hurt`: "Guardian hurts" -> "Guardian is hurt"
- `subtitles.entity.happy_ghast.equip`: "Harness equips" -> "Harness is equipped"
- `subtitles.entity.happy_ghast.hurt`: "Happy Ghast hurts" -> "Happy Ghast is hurt"
- `subtitles.entity.happy_ghast.unequip`: "Harness unequips" -> "Harness is unequipped"
- `subtitles.entity.hoglin.hurt`: "Hoglin hurts" -> "Hoglin is hurt"
- `subtitles.entity.horse.armor`: "Horse armor equips" -> "Horse armor is equipped"
- `subtitles.entity.horse.hurt`: "Horse hurts" -> "Horse is hurt"
- `subtitles.entity.horse.saddle`: "Saddle equips" -> "Saddle is equipped"
- `subtitles.entity.husk.hurt`: "Husk hurts" -> "Husk is hurt"
- `subtitles.entity.illusioner.hurt`: "Illusioner hurts" -> "Illusioner is hurt"
- `subtitles.entity.iron_golem.hurt`: "Iron Golem hurts" -> "Iron Golem is hurt"
- `subtitles.entity.iron_golem.repair`: "Iron Golem repaired" -> "Iron Golem is repaired"
- `subtitles.entity.item_frame.add_item`: "Item Frame fills" -> "Item Frame is filled"
- `subtitles.entity.item_frame.break`: "Item Frame broken" -> "Item Frame breaks"
- `subtitles.entity.item_frame.place`: "Item Frame placed" -> "Item Frame is placed"
- `subtitles.entity.leash_knot.break`: "Leash Knot broken" -> "Leash Knot breaks"
- `subtitles.entity.leash_knot.place`: "Leash Knot tied" -> "Leash Knot is tied"
- `subtitles.entity.llama.chest`: "Llama Chest equips" -> "Llama Chest is equipped"
- `subtitles.entity.llama.hurt`: "Llama hurts" -> "Llama is hurt"
- `subtitles.entity.magma_cube.hurt`: "Magma Cube hurts" -> "Magma Cube is hurt"
- `subtitles.entity.mule.chest`: "Mule Chest equips" -> "Mule Chest is equipped"
- `subtitles.entity.mule.hurt`: "Mule hurts" -> "Mule is hurt"
- `subtitles.entity.nautilus.hurt`: "Nautilus hurts" -> "Nautilus is hurt"
- `subtitles.entity.nautilus.hurt_land`: "Nautilus hurts" -> "Nautilus is hurt"
- `subtitles.entity.painting.break`: "Painting broken" -> "Painting breaks"
- `subtitles.entity.painting.place`: "Painting placed" -> "Painting is placed"
- `subtitles.entity.panda.hurt`: "Panda hurts" -> "Panda is hurt"
- `subtitles.entity.parched.hurt`: "Parched hurts" -> "Parched is hurt"
- `subtitles.entity.parrot.hurts`: "Parrot hurts" -> "Parrot is hurt"
- `subtitles.entity.parrot.imitate.wither`: "Parrot angers" -> "Parrot gets angry"
- `subtitles.entity.phantom.hurt`: "Phantom hurts" -> "Phantom is hurt"
- `subtitles.entity.pig.hurt`: "Pig hurts" -> "Pig is hurt"
- `subtitles.entity.pig.saddle`: "Saddle equips" -> "Saddle is equipped"
- `subtitles.entity.piglin_brute.hurt`: "Piglin Brute hurts" -> "Piglin Brute is hurt"
- `subtitles.entity.piglin.hurt`: "Piglin hurts" -> "Piglin is hurt"
- `subtitles.entity.pillager.hurt`: "Pillager hurts" -> "Pillager is hurt"
- `subtitles.entity.player.burp`: "Burp" -> "Player burps"
- `subtitles.entity.player.hurt`: "Player hurts" -> "Player is hurt"
- `subtitles.entity.player.hurt_drown`: "Player drowning" -> "Player is drowning"
- `subtitles.entity.player.hurt_on_fire`: "Player burns" -> "Player is burning"
- `subtitles.entity.polar_bear.hurt`: "Polar Bear hurts" -> "Polar Bear is hurt"
- `subtitles.entity.potion.throw`: "Bottle thrown" -> "Bottle is thrown"
- `subtitles.entity.puffer_fish.hurt`: "Pufferfish hurts" -> "Pufferfish is hurt"
- `subtitles.entity.rabbit.hurt`: "Rabbit hurts" -> "Rabbit is hurt"
- `subtitles.entity.ravager.hurt`: "Ravager hurts" -> "Ravager is hurt"
- `subtitles.entity.salmon.hurt`: "Salmon hurts" -> "Salmon is hurt"
- `subtitles.entity.sheep.hurt`: "Sheep hurts" -> "Sheep is hurt"
- `subtitles.entity.shulker.hurt`: "Shulker hurts" -> "Shulker is hurt"
- `subtitles.entity.silverfish.hurt`: "Silverfish hurts" -> "Silverfish is hurt"
- `subtitles.entity.skeleton_horse.hurt`: "Skeleton Horse hurts" -> "Skeleton Horse is hurt"
- `subtitles.entity.skeleton.hurt`: "Skeleton hurts" -> "Skeleton is hurt"
- `subtitles.entity.slime.hurt`: "Slime hurts" -> "Slime is hurt"
- `subtitles.entity.small_sulfur_cube.hurt`: "Small Sulfur Cube hurts" -> "Small Sulfur Cube is hurt"
- `subtitles.entity.sniffer.hurt`: "Sniffer hurts" -> "Sniffer is hurt"
- `subtitles.entity.snow_golem.hurt`: "Snow Golem hurts" -> "Snow Golem is hurt"
- `subtitles.entity.spider.hurt`: "Spider hurts" -> "Spider is hurt"
- `subtitles.entity.squid.hurt`: "Squid hurts" -> "Squid is hurt"
- `subtitles.entity.stray.hurt`: "Stray hurts" -> "Stray is hurt"
- `subtitles.entity.strider.hurt`: "Strider hurts" -> "Strider is hurt"
- `subtitles.entity.sulfur_cube.hurt`: "Sulfur Cube hurts" -> "Sulfur Cube is hurt"
- `subtitles.entity.tadpole.hurt`: "Tadpole hurts" -> "Tadpole is hurt"
- `subtitles.entity.tropical_fish.hurt`: "Tropical Fish hurts" -> "Tropical Fish is hurt"
- `subtitles.entity.turtle.hurt`: "Turtle hurts" -> "Turtle is hurt"
- `subtitles.entity.turtle.hurt_baby`: "Baby Turtle hurts" -> "Baby Turtle is hurt"
- `subtitles.entity.vex.hurt`: "Vex hurts" -> "Vex is hurt"
- `subtitles.entity.villager.hurt`: "Villager hurts" -> "Villager is hurt"
- `subtitles.entity.vindicator.hurt`: "Vindicator hurts" -> "Vindicator is hurt"
- `subtitles.entity.wandering_trader.hurt`: "Wandering Trader hurts" -> "Wandering Trader is hurt"
- `subtitles.entity.warden.hurt`: "Warden hurts" -> "Warden is hurt"
- `subtitles.entity.witch.hurt`: "Witch hurts" -> "Witch is hurt"
- `subtitles.entity.wither_skeleton.hurt`: "Wither Skeleton hurts" -> "Wither Skeleton is hurt"
- `subtitles.entity.wither.ambient`: "Wither angers" -> "Wither gets angry"
- `subtitles.entity.wither.hurt`: "Wither hurts" -> "Wither is hurt"
- `subtitles.entity.wolf.hurt`: "Wolf hurts" -> "Wolf is hurt"
- `subtitles.entity.zoglin.hurt`: "Zoglin hurts" -> "Zoglin is hurt"
- `subtitles.entity.zombie_horse.hurt`: "Zombie Horse hurts" -> "Zombie Horse is hurt"
- `subtitles.entity.zombie_nautilus.hurt`: "Zombie Nautilus hurts" -> "Zombie Nautilus is hurt"
- `subtitles.entity.zombie_nautilus.hurt_land`: "Zombie Nautilus hurts" -> "Zombie Nautilus is hurt"
- `subtitles.entity.zombie_villager.hurt`: "Zombie Villager hurts" -> "Zombie Villager is hurt"
- `subtitles.entity.zombie.destroy_egg`: "Turtle Egg stomped" -> "Turtle Egg is stomped"
- `subtitles.entity.zombie.hurt`: "Zombie hurts" -> "Zombie is hurt"
- `subtitles.entity.zombified_piglin.hurt`: "Zombified Piglin hurts" -> "Zombified Piglin is hurt"
- `subtitles.item.armor.equip`: "Gear equips" -> "Gear is equipped"
- `subtitles.item.armor.equip_nautilus`: "Nautilus Armor equips" -> "Nautilus Armor is equipped"
- `subtitles.item.armor.unequip_nautilus`: "Nautilus Armor unequips" -> "Nautilus Armor is unequipped"
- `subtitles.item.axe.wax_off`: "Wax off" -> "Wax is removed"
- `subtitles.item.brush.brushing.gravel.complete`: "Brushing Gravel completed" -> "Brushing Gravel is completed"
- `subtitles.item.brush.brushing.sand.complete`: "Brushing Sand completed" -> "Brushing Sand is completed"
- `subtitles.item.bucket.fill_axolotl`: "Axolotl scooped" -> "Axolotl is scooped"
- `subtitles.item.bucket.fill_fish`: "Fish captured" -> "Fish is captured"
- `subtitles.item.bucket.fill_sulfur_cube`: "Sulfur Cube scooped" -> "Sulfur Cube is scooped"
- `subtitles.item.bucket.fill_tadpole`: "Tadpole captured" -> "Tadpole is captured"
- `subtitles.item.bundle.insert`: "Item packed" -> "Item is packed"
- `subtitles.item.bundle.insert_fail`: "Bundle full" -> "Bundle rejects an item"
- `subtitles.item.bundle.remove_one`: "Item unpacked" -> "Item is unpacked"
- `subtitles.item.crop.plant`: "Crop planted" -> "Crop is planted"
- `subtitles.item.flintandsteel.use`: "Flint and Steel click" -> "Flint and Steel clicks"
- `subtitles.item.honeycomb.wax_on`: "Wax on" -> "Wax is applied"
- `subtitles.item.lead.tied`: "Lead tied" -> "Lead is tied"
- `subtitles.item.lead.untied`: "Lead untied" -> "Lead is untied"
- `subtitles.item.nautilus_saddle_equip`: "Saddle equips" -> "Saddle is equipped"
- `subtitles.item.nautilus_saddle_underwater_equip`: "Saddle equips" -> "Saddle is equipped"
- `subtitles.item.nether_wart.plant`: "Crop planted" -> "Crop is planted"
- `subtitles.item.underwater_saddle.equip`: "Saddle equips" -> "Saddle is equipped"
- `subtitles.ui.cartography_table.take_result`: "Map drawn" -> "Map is drawn"
- `subtitles.ui.hud.bubble_pop`: "Breath meter dropping" -> "Breath meter drops"
- `subtitles.ui.loom.take_result`: "Loom used" -> "Loom is used"
- `subtitles.ui.stonecutter.take_result`: "Stonecutter used" -> "Stonecutter is used"
