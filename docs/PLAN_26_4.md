# Game Drops Backport: 26.4 (Deep Freeze) plan

26.4 is the winter game drop, "Deep Freeze": ice caves, the frostbite, the Freezing effect, ice and snow building
blocks, a calcite set, jukebox playlists and a sulfur cube archetype. Status on 2026-11-24: 10 snapshots
(Sept 22 to Nov 17) and Pre-Release 1 (Nov 24); release planned for Q4 2026, no date yet. No code or assets until
this plan is approved.

**When to build:** content counts as 26.4 when it ships outside experiments in the 26.4 release (the rule in
`PLAN.md`). Planning and test design start now; assets, names and numbers come from the release's client jar.
Streams whose content hasn't changed since Snapshot 7 ("final features") can start at the release candidate.

**Sources:** minecraft.wiki's *Java Edition 26.4* and *26.4/Development versions* pages (read 2026-11-24), checked
against **26.4 Pre-Release 1**: its client jar, Vineflower source and a diff against 26.3 are in
`G:\MCDownloads\minecraft-26.4-pre-1\` (`client.jar`, `src/`, `diff-26.3.txt`, `sounds.json`). 26.x jars aren't
obfuscated, so the source reads as Mojang's names. At release, diff the release jar against the pre-release and
switch `tools/assets.py` to 26.4.

**What the pre-release contains** (diff against 26.3): 25 new blocks, 28 new item definitions, 5 item textures,
the frostbite's 3 entity textures, the Freezing effect icon and 6 particle frames, 58 new sound events, 1 biome
(`ice_caves`), 21 features, 26 placed features, 2 surface rules, 1 noise, the `cold` sulfur cube archetype, 2
wandering trader trade files, 13 brewing recipes, the `flurry` jukebox song and 52 recipe advancements. Everything
in the stream lists below is in it; nothing in it is missing from them.

**Version:** Game Drops 1.1.0 (a new drop). Backport Core bumps for the look and feel pieces (stream 8). Every
release follows Core's `docs/RELEASING.md`.

## Scope at a glance

| In | Out (and why) |
| --- | --- |
| Ice caves, crevasses, icicles, ice crystals, brittle ice | Vulkan, DXGI, shader and launcher changes (not in 1.12.2) |
| Frostbite, Freezing, ice balls, Freezing potions and arrows | Data pack, resource pack and network format changes |
| Packed ice bricks, chiseled packed ice, snow bricks, the calcite set, chiseled soulshale | New registries (`block_sound_set`, `dispense_behavior`) as data; their player-visible results are in |
| Cold sulfur cube; new fast-sliding blocks | Commands-only changes (`/fillbiome` accuracy, `/time` limit, 2D local coordinates) |
| Jukebox playlists, the "Flurry" disc | Friend requests, reporting and Realms (needs Mojang's services) |
| Changes to existing things players see (ice sounds and faces, mushrooms, snowball knockback, carrot on a stick, stray spawns, conversions) | Ender dragon fixes that patch 1.21+ code with no 1.12.2 equivalent (checked one by one in stream 7) |
| 26.4's look and feel (Other Players screen, F3 categories, fog, keyboard navigation, Repeat Recipe key, panorama) | |

## Content owned by another mod of the family (Rule 1) and its integrations (Rule 2)

| Needed here | Owner | What this mod does |
| --- | --- | --- |
| Powder snow (frostbite conversion; husk and drowned conversions) | Caves & Cliffs | Rule 2: conversions only with Caves & Cliffs. The frostbite and ice caves work without it. |
| Freezing's freeze ticks (the frozen-ticks counter, its damage every 40 ticks, the frost overlay, leather and mob immunity) | **Backport Core**, moved from Caves & Cliffs (decision 3) | 26.4's Freezing effect does nothing of its own: `FreezingMobEffect.applyEffectTick` only calls `freezeForTicks(1)`, and `LivingEntity` freezes a mob that is in powder snow **or** has the effect. Game Drops feeds Core's freeze ticks; Caves & Cliffs' powder snow does too. |
| Calcite (the calcite set; ice caves are mostly packed ice and calcite) | Caves & Cliffs | The calcite set only with Caves & Cliffs. Without it, ice caves use packed ice where vanilla has calcite. |
| Deepslate (ice cave ore blobs below 0) | Caves & Cliffs | Stone blobs without it, as the sulfur caves do. |
| Ancient cities (chiseled soulshale loot) | Wild Update | Only with Wild Update; in Creative only through commands, as in vanilla. |
| Blue ice (cold sulfur cube; water next to blue ice turns to brittle ice) | Update Aquatic (Rule 1 table) | Uses whichever blue ice is installed. |
| Strays in more cold biomes | Caves & Cliffs (frozen peaks, jagged peaks, snowy slopes) | Vanilla's frozen ocean and frozen river here; the peaks only with Caves & Cliffs. |
| Wandering trader (ice crystal and icicle trades) | Village & Pillage | Rule 2, through Core's `TradeRegistry`. |
| Cold farm animals, frogs, snow foxes, white rabbits in ice caves | Tricky Trials (variants), Wild Update (frogs), Village & Pillage (foxes) | Each only with its owner; through tags, as vanilla does. |
| Trial chambers, mineshafts, strongholds and ruined portals in ice caves | Tricky Trials; vanilla | Ice caves don't block them. |
| Sulfur caves under upgraded old worlds (26.4 Snapshot 1) | Caves & Cliffs (`upgradeworld`) | Caves & Cliffs adds Game Drops' sulfur caves to its below-zero retrogen when Game Drops is installed. |
| Copper chests feeding a jukebox | Tricky Trials | Any copper chest counts only with Tricky Trials. |
| Adventuring Time (ice caves), Monster Hunter (frostbite) | Backport Core (`advancement.AdventuringTime`) | Registered through Core. |

## Streams

### 1. Ice caves
- **Where:** an underground region under cold biomes, placed where vanilla places the biome. As decided for sulfur caves: vanilla's heights with Caves & Cliffs' extended height, the proportional 0-255 placement otherwise, and our own underground region under any biome, since 1.12.2 has no cave biomes.
- **What's in them:** mostly packed ice and calcite, large packed-ice icicles on floor and ceiling, ice crystals, snow patches and no glow lichen.
- **Ore placements:** the 22 `ice_cave_ore_*` placements, each placing a stone or deepslate patch before the ore.
- **Crevasses:** winding cracks covered with brittle ice over water, using vanilla's feature fields (width 2-8, length 4-16, twistiness, tapering).
- **Spawns:** frostbites, strays and the common cave monsters; snowy villagers in villages that reach them.
- **Structures:** strongholds are biased toward ice caves (`#stronghold_biased_to` lists `ice_caves`); mineshafts, standard ruined portals and trial chambers also generate in them.
- **Blocks:**
  - **Icicles** (`IcicleBlock`): speleothems that grow under packed ice (`MAX_GROWING_LENGTH = 4`). They melt when the block light is above 4 (block light only, not sky light), above any block in `#melts_icicle_above` (magma block), and in the Nether. They fall like pointed dripstone, and a trident breaks them. Shares Caves & Cliffs' pointed dripstone code where installed, its own copy otherwise (open question 1).
  - **Ice crystals** (`IceCrystalBlock`): light 4, attach to any full side, ambient sound over packed ice.
  - **Brittle ice** (`BrittleIceBlock`): ages 0-3. Stepping on it schedules the next crack 24 ticks later, so frostbites and strays (`#does_not_break_brittle_ice`) don't break it. Only living entities crack it: other mobs only with `mobGriefing`, players only outside spawn protection (`shouldEntityIncreaseCracking`). Shulkers crack the brittle ice they sit on. Iron golems never spawn on it (`SpawnUtil`). Lily pads can be placed on it (`#supports_lily_pad`: ice, brittle ice, frosted ice). It doesn't melt and drops only with Silk Touch. Water next to blue ice becomes brittle ice.
  - **Crevasses** (`CrevasseFeature`).
- **Tests:** placement and depth with and without Caves & Cliffs, and with RTG and OTG (Caves & Cliffs' new hybrid worlds), F3 and `/locate biome`.

### 2. Frostbite, Freezing and ice balls
- **Frostbite:** a zombie type in snowy biomes and ice caves, with a baby form.
  - Fights in melee (which applies Freezing) or throws ice balls, switching by distance.
  - Immune to Freezing; doesn't sink in powder snow; burns in sunlight.
  - 24 max health, speed 0.21.
  - Drops 0-2 rotten flesh and 0-3 ice balls (Looting adds 0-1 of each per level); ice balls stack to 16.
  - Becomes a zombie after 45 s underwater (the zombie's drowning tracker: 600 ticks in water, then a 300-tick conversion).
- **Freezing effect:** damage over time and slowness, and the player's screen shakes.
  - Leather armor stops the damage but not the effect; fire or lava counters it.
  - Blazes, magma cubes and striders take 5x damage. Undead, snow golems, polar bears and the ender dragon are immune.
- **Ice ball** (`IceBall`): a throwable projectile; damage is `ceil(speed × 1.3)` (`BASE_DAMAGE = 1.3F`).
- **Potions and arrows:** Potion of Freezing 900 ticks (45 s), long 1800 ticks (90 s), brewed from an awkward potion and an ice ball, with splash, lingering and Arrows of Freezing as vanilla scales them; the 13 brewing recipes in the pre-release.
- **Freezing in 1.12.2:** the effect only feeds freeze ticks (see the owners table), so the frost overlay, the damage and the immunities come from whoever owns freezing (open question 3).
- **Conversions:** zombie to frostbite after 22 s in powder snow (140 ticks, then a 300-tick conversion), husk and drowned to zombie (Rule 2, Caves & Cliffs). Leather armor prevents them, and now also stops a skeleton becoming a stray (`Skeleton`: `isWearingFreezeImmuneWearables`).
- **Advancements:** "What Goes Around Comes Around", Monster Hunter, Monsters Hunted, and the two that need Freezing.
- **Sounds:** frostbite, ice ball and conversion sounds; the parrot imitates a frostbite.

### 3. Ice and snow building blocks
- **Packed ice bricks:** stairs, slab and wall. Not slippery; a pickaxe is needed to drop them.
- **Chiseled packed ice.**
- **Snow bricks:** slab, stairs and wall. Shovel only; no stonecutter.
- **Recipes:** crafting and stonecutter recipes as vanilla's (Core's stonecutter where installed).
- **Waterlogging:** stairs, slabs and walls use Core's waterlogging, as every family stairs, slab and wall does.

### 4. Calcite set and chiseled soulshale (Rule 2)
- **Calcite set:** calcite pillar, slab, stairs and wall; polished calcite and its slab, stairs and wall; polished calcite bricks and their slab, stairs and wall. Recipes as vanilla's. Only with Caves & Cliffs' calcite.
- **Chiseled soulshale:** ancient city loot, mined without a tool. Only with Wild Update.

### 5. Sulfur cube
- **Cold archetype** (`data/minecraft/sulfur_cube_archetype/cold.json`, absorbing `#sulfur_cube_archetype/cold`, i.e. blue ice):
  - **Attributes:** knockback resistance +0.5, explosion knockback resistance +0.5, bounciness +0.1, friction ×0.05 and air drag ×0.01. Knockback is horizontal 0.6625 and vertical 0.09.
  - **Frosted ice:** while on the ground and not ridden, it turns water sources below it within radius 3 into frosted ice (age 0), only where there's air above, and sends a block-place game event. There's no floating, and nothing happens over flowing water (Snapshot 8 removed both).
  - **Sounds:** the fast-sliding hit sound for hits and pushes.
- **Fast-sliding archetype:** now also packed ice bricks, chiseled packed ice and snow bricks.
- **Slow bouncy archetype:** now also calcite pillar, polished calcite, polished calcite bricks (Caves & Cliffs' calcite set, Rule 2) and chiseled soulshale (with Wild Update). Calcite was already in it in 26.3.
- **Config:** extends the `sulfurCube.extraBlocks` kinds with `cold`.

### 6. Jukebox playlists and "Flurry"
- **Playlists:** a jukebox plays the discs in a chest, trapped chest or copper chest above it (`#jukebox_containers`), in slot order; single chests only, never half of a double chest. It skips anything that isn't a disc, loops to the start, and swaps the finished disc for the next. Ejecting skips to the next disc.
- **Textures:** the jukebox gets new textures.
- **"Flurry":** the music disc, with a 30% chance in igloo basement chests and comparator output 7.

### 7. Changes to existing things
- **Ice blocks:**
  - Ice, packed ice, blue ice and frosted ice each get their own sounds, and the ice ambient sound.
  - No internal faces between ice blocks.
  - Frosted ice sends a vibration (Core's game events).
- **Snow:** can be placed on packed ice. Creative groups snow, snow blocks and snow bricks.
- **Mushrooms:** placeable whatever the light on any block whose top face has a sturdy center (`canSupportCenter`, so end rods count). Spreading still needs light under 13.
- **Honey blocks:** support blocks as a full block (`getBlockSupportShape` is a full cube), so what goes on a slime block goes on honey; mobs never spawn on them (`isValidSpawn` never).
- **Projectiles:** decorated pots and chorus flowers break only when the projectile `mayInteract` and `mayBreak` there; sulfur spikes and icicles have their own `onProjectileHit` (`SpeleothemBlock`). What `mayBreak` allows a mob's arrow is checked at release (Snapshot 5 tied it to `mobGriefing`). Pots are Trails & Tales'.
- **Items:**
  - Snowballs knock back players.
  - Carrot and warped fungus on a stick boost for 7-35 s (warped fungus only with Nether Update).
- **Mobs:**
  - Strays spawn in more cold biomes (see the owners table).
  - Pointed dripstone drips lava into cauldrons 3x as often (Caves & Cliffs).
- **Ender dragon fixes** (Snapshots 8-9): each checked against 1.12.2's dragon; only those that fix behavior 1.12.2 shares.

### 8. Look and feel (Backport Core, per the 1.4.7 rule)
Core owns each version's look and feel, so these pieces go into Core as the 26.4 version; Game Drops keeps only its
panorama and music.
- **Other Players** (key P, a pause menu icon): online and recent players, each opening Player Options. Mute works locally. Friend requests and reporting need Mojang's services and are left out.
- **F3:** the debug screen's categories and new layout.
- **Fog:** the render-distance edge fades opacity instead of turning opaque; the Improved Fog option, on by default and off in the Fast preset.
- **Keyboard:** navigation and narration for gameplay screens, the recipe book and the Creative inventory; the Repeat Recipe/Trade key (R); recipe book tab tooltips.
- **Text:** the advancement renames ("Oooh, Shiny!", "Uh-Oh"), 26.4's splashes and wording, and the Javanese and Chavacano languages where Core adds languages.
- **Panorama:** the Deep Freeze panorama (Game Drops).

## Order
1. **Done (2026-11-24):** this plan; Pre-Release 1 downloaded, decompiled and diffed, and the numbers above taken from it.
2. **At release:** diff the release jar against Pre-Release 1, update any number that changed, and switch `tools/assets.py` to 26.4.
3. **Streams 3 and 4 first:** blocks only, little risk. Alongside them, **freeze ticks move into Backport Core** (decision 3), with a Caves & Cliffs release that hands its powder snow freezing to Core.
4. **Then 2:** frostbite and Freezing.
5. **Then 1:** ice caves, which need 2's spawns, 3's blocks and Caves & Cliffs' heights.
6. **Then 5, 6 and 7.**
7. **Stream 8:** in Core, alongside.
8. **Release checks** as for 1.0.0: `tools/release_suite.sh` alone and with `FAMILY=1`, the client scenes, ScanAll and boot-server, then Core's `docs/RELEASING.md`.

## Open questions
1. **Icicles: decided 2026-11-24 by the user, shared.** Caves & Cliffs' pointed dripstone code becomes a speleothem helper in Backport Core (growth, falling, landing damage, the trident break), as vanilla shares it. Pointed dripstone and icicles both use it.
2. **When to start: decided 2026-11-24 by the user, wait.** No stream starts before the 26.4 release, the block streams included.
3. **Who owns freezing: decided 2026-11-24 by the user, Backport Core.** 26.4's Freezing effect is built on 1.17's
   freeze ticks, which Caves & Cliffs owned with powder snow. Freeze ticks become Core's: their damage and slowness,
   the frost overlay and the immunities (leather, freeze-immune mobs, the undead from 26.4).
   - Caves & Cliffs' powder snow feeds Core's freeze ticks, and so does Game Drops' Freezing.
   - Caves & Cliffs' `powderSnow.*` config keys keep working, mirrored as Core's settings move.
   - Worlds keep their frozen mobs' state.
   - This is a Core stream of its own (Core plus a Caves & Cliffs release) and goes before stream 2.
