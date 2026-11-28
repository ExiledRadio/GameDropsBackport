# Game Drops Backport: 26.4 (Deep Freeze) plan

26.4 is the winter game drop, "Deep Freeze": ice caves, the frostbite, the Freezing effect, ice and snow building
blocks, a calcite set, jukebox playlists and a cold sulfur cube.

**Status (2026-11-27): built on 26.4 Pre-Release 1, updated to Pre-Release 2, branch `gd-26-4`, on Backport Core 1.5.0
(`core-1.5.0-deepfreeze`).** 26.4 is not released yet (newest: 26.4-pre-2, 2026-11-27). On 2026-11-26 the user chose to
build on the pre-release instead of waiting (open question 2 below). Pre-Release 2's changes are in
[`26.4/pre-release-2.md`](26.4/pre-release-2.md) and applied. **Every number here must be diffed against the release
when it ships** (Order, step 2). All 18 languages are complete. Not merged, pushed or uploaded.

**Sources:** the 26.4 Pre-Release 1 and 2 client jars, decompiled with Vineflower (Mojang names), and their diffs
against 26.3 and each other, in `E:\MCDownloads\minecraft-26.4-pre-2\`, `minecraft-26.4-pre-1\` and `minecraft-26.3\` (G:
was not mounted on 2026-11-26; `tools/assets.py` finds either). The generators read `26.4-pre-2`. The exact numbers,
file and line for each, are in the checklists next to this plan (Pre-Release 2's changes marked in them):
- [`26.4/blocks-and-changes.md`](26.4/blocks-and-changes.md): the building blocks, the cold sulfur cube, jukebox
  playlists, Flurry, every change to existing things, the language diff;
- [`26.4/ice-caves.md`](26.4/ice-caves.md): the biome, its placement, every feature and placed feature, the bands, the
  icicle, ice crystal and brittle ice;
- [`26.4/frostbite-and-freezing.md`](26.4/frostbite-and-freezing.md): the frostbite, Freezing, ice balls, potions,
  advancements, strays and every new sound event;
- [`26.4/pre-release-2.md`](26.4/pre-release-2.md): Pre-Release 2 against Pre-Release 1, and what changed here.

**What the pre-release contains** (diff against 26.3): 25 new blocks, 28 new item definitions, the frostbite's 3 entity
textures, the Freezing icon and 6 particle frames, 1 biome (`ice_caves`), 21 placed ice cave ores and 5 decoration
features, 2 surface rules, 1 noise, the `cold` sulfur cube archetype, 2 wandering trader trades, 13 brewing recipes, the
`flurry` jukebox song, 52 recipe advancements, the Deep Freeze panorama, 26.4's own ice sound sets.

**Version:** Game Drops 1.1.0 (a new drop), requiring Backport Core 1.5.0.

## Streams, as built

Packages: `iceblocks` (3, 4), `frostbite` (2), `icecaves` (1), `jukebox` (6), and in the existing `sulfurcube` (5),
`changes` (7) and `lookfeel` (8). Entity ids 16-17 (frostbite, ice ball). Self-tests: `iceblocks`, `icecaves`,
`icecavesgen`, `frostbite`, `coldcube`, `jukebox`, `changes264`; `-Pselftest=all` runs them with the rest.

### 1. Ice caves (`icecaves`) - built
- [x] Biome `gamedropsbackport:ice_caves` (temperature 0, snow, water and fog `#14559b`, sky `#7fa1ff`, the sulfur caves'
      music), 26.4's spawns in order (creeper 100, skeleton 90, stray 10, frostbite 50, slime 100, spider 100, zombie 50,
      enderman 10, witch 5, zombie villager 5; bats 10). Under ground through Backport Core's `UndergroundBiomes` (F3,
      colors) and `/locate biome minecraft:ice_caves` (Core's `BiomeLocate`, chained with the sulfur caves').
- [x] **Where** (`IceCaves`): 26.4's box (continentalness -1.05 to -0.19, weirdness 0.5 to 1.1, depth 0.2 to 0.9) over
      Caves & Cliffs' 1.18 climate noise, under its frozen and deep frozen oceans (26.4's temperature -1 to -0.8 band);
      elsewhere our patches (one 512 cell in 3, radius 20-69, ragged) under snowy oceans, or any snowy biome without
      Update Aquatic, whose frozen oceans 1.12.2 hardly makes; 26.4's depth below the ground, proportional in 0-255
      worlds; superflat by the preset's biome.
- [x] **Bands** (`ice_cave_bands`): calcite -0.6 to -0.5, packed ice to 0.2, calcite to 0.3, packed ice to 0.5, calcite to
      0.7 of `ice_cave_gradient` (first octave -4, amplitudes 1, 0, 1). Calcite is Caves & Cliffs'; packed ice without it.
- [x] **Features**, 26.4's counts and heights: `large_icicle` (48-64), the 21 `ice_cave_ore_*` (each ore inside a size-20
      stone or deepslate host; andesite, diorite, gravel plain), `snow_layers` (16-24 x 32), `ice_crystals` (48-64 x 2,
      on packed ice), `icicle_cluster` (24-64, ceiling only, attached bases, radius 6, height 3-5), `ice_crevasse` (16:
      two 8-step twisting branches, width 4 tapering 0.6, brittle ice over air over water, walls patched).
- [x] **Icicles** on Core's shared speleothem: packed ice, down only, no tip merging, `MAX_GROWING_LENGTH` 4, melt above
      block light 4, in the Nether or over magma; fall as stalactites; land with `block.icicle.land`; hardness 0.5;
      map color ice (Pre-Release 2);
      snow dust drips. **Ice crystals**: light 4, 7 high and 10 wide, need a sturdy face, air without one, chime over
      packed ice (1/250). **Brittle ice**: ages 0-3, a crack per 24 ticks under any living thing but frostbites and
      strays (mobs only with `mobGriefing`, players unless spawn-protected), breaks without a drop, Silk Touch only,
      friction 0.98. Water beside any mod's blue ice becomes brittle ice.
- [x] Wandering trader (Village & Pillage, Core's `TradeRegistry`): ice crystal for 2 emeralds, 2 icicles for 1; 5 uses.

### 2. Frostbite, Freezing and ice balls (`frostbite`) - built
- [x] **Frostbite**: 24 health, speed 0.21, the zombie's damage 3, armor 2, follow 35; 25% with an ice ball; throws
      within 16 blocks (ranged from 5 out, back to melee at 3; intervals Easy 60, Normal 20-40, Hard 15-30 ticks;
      velocity 1.6, uncertainty `14 - 4 x difficulty`), 30-tick melee that gives Freezing for 60 ticks; burns in
      daylight; frostbite reinforcements; drowns into a zombie after 600 + 300 ticks; drops 0-2 rotten flesh and 0-3 ice
      balls (+0-1 per Looting); cold-variant spawn rule (sky through powder snow, except in ice caves). Renderer with the
      drowned-style 64x64 body and the 0.25 outer layer; the baby uses the babies stream's 26.1 baby zombie model.
- [x] **Spawns**: snowy plains 10, ice spikes 25, frozen river 25, frozen ocean 25, Update Aquatic's deep frozen ocean 25,
      Caves & Cliffs' frozen peaks 25, jagged peaks 10, snowy slopes 10 (the last three by the 1.18 terrain's biome),
      ice caves 50; strays newly 95 in frozen river, frozen ocean, deep frozen ocean and the three peaks.
- [x] **Freezing** (effect 0x9EEEEE): one freeze tick a tick on Backport Core's freeze ticks; holds the counter; 1.5
      damage at full; refused by what can't freeze; `event.mob_effect.freezing` when added; 26.4's icon and freezing
      particle (on entities whose only effect is Freezing).
- [x] **Ice ball**: `ceil(speed x 1.3)` thrown damage, no freezing; stack 16; thrown at 1.5, inaccuracy 1; dispensers;
      throw sound in the players category, break sound (none when silent) and 8 item particles (Pre-Release 2).
- [x] **Potions**: Freezing 900 ticks, long 1800; awkward + ice ball, water + ice ball = mundane, + redstone = long;
      splash, lingering (a quarter) and tipped arrows (an eighth) from 1.12.2's container mixes.
- [x] **Conversions** in powder snow (Caves & Cliffs'), no leather worn: zombie to frostbite, husk to zombie, Update
      Aquatic's drowned to zombie, 140 + 300 ticks, with 26.4's sounds.
- [x] **Advancements**: What Goes Around Comes Around; the frostbite in Monster Hunter and Monsters Hunted (Core's new
      `advancement.MonsterHunter`). The parrot imitates the frostbite.

### 3 and 4. Ice and snow building blocks, the calcite set, chiseled soulshale (`iceblocks`) - built
- [x] Packed ice bricks (0.5, chime, friction 0.6, pickaxe needed), their stairs, slab and wall, chiseled packed ice;
      snow bricks (0.6, snow's sounds, shovel needed) and their shapes; recipes as 26.4's (2x2 to 4, slabs 6, stairs 4,
      walls 6, chiseled from two slabs).
- [x] With Caves & Cliffs: calcite pillar, slab, stairs, wall; polished calcite and polished calcite bricks with their
      shapes (0.75, calcite's sounds); recipes as 26.4's (pillar 2 from 2).
- [x] With Wild Update: chiseled soulshale (2.0 / 6.0, any tool, no creative tab), weight 3 in the ancient city chest
      pool.

### 5. Sulfur cube (`sulfurcube`) - built
- [x] The cold archetype (blue ice): fast sliding's numbers and sounds (Pre-Release 2: push `fast_sliding.push`, was
      `.hit`); on the ground and riding
      nothing, with `mobGriefing`, it turns water sources under it within 3 (`< 9`) with air above into frosted ice.
- [x] Fast sliding now packed ice, packed ice bricks, chiseled packed ice, snow block, snow bricks; slow bouncy gains the
      calcite set and chiseled soulshale; `extraBlocks` accepts `=cold`.

### 6. Jukebox playlists and "Flurry" (`jukebox`) - built
- [x] A jukebox under a single chest, trapped chest or copper chest plays its discs in slot order after the last one
      taken, skips non-discs, loops; an empty jukebox takes the next disc at once, so ejecting skips. Song ends from 26.4's
      lengths. Taking a disc out sends `block_change` (Pre-Release 2).
- [x] Flurry (154 s, comparator 7, uncommon), 30% in igloo chests (a pool of its own).
- [x] The jukebox's 26.4 look (new side and top, a bottom of its own), as Game Drops' 26.4 contribution to Core.

### 7. Changes to existing things (`changes`, settings each) - built
- [x] Ice, packed ice, frosted ice and blue ice: 26.4's sound sets; ice in a frozen lake resonates (1/10000).
- [x] Ice and frosted ice hide faces against ice, frosted ice and brittle ice (patch P10).
- [x] Frosted ice vibrates on each crack (P11).
- [x] Snow layers on packed ice (P8). Mushrooms stay on any center support in any light, spread only under 13 (P9).
- [x] Snowballs push players. Carrot on a stick boosts 140-700 ticks; a finished boost syncs a total of 0, which is no
      boost (Pre-Release 2). A perching dragon takes no thrown damage.
- [x] Pre-Release 2, 26.3's straw bed: breaks with `break_leave` (its sound set's break sound), through `destroyBlock`.
- [x] Checked, no change needed: the dragon's vertical steering (1.12.2 already 0.1, 26.4's fix of 26.3's 0.01);
      pointed dripstone's cauldron rates (unchanged in 26.4, against this plan's earlier note).

### 8. Look and feel - built (Pre-Release 1)
- [x] Game Drops registers 26.4 with Core: the Deep Freeze panorama, the jukebox model.
- [x] Vanilla text as 26.4 words it (`tools/lookfeel264`, `vanilla_text_264/`, 19 languages from Mojang's 26.4 files):
      185 keys, every 1.12.2 key whose English changed from 26.3 ("Cow is hurt", "60 FPS", ...), the family's prefixed
      copies of them, and copies under vanilla's own keys ("Oooh, Shiny!", "Uh-Oh"). Uh Oh's keys moved out of Game
      Drops' lang into `vanilla_text/` (26.3's "Uh Oh"), so 26.4's text can replace them.
- [x] Core `Look264` / `Look264Client` (`core-1.5.0-deepfreeze`): Improved Fog (Video Settings, on; off in Fast), the
      Repeat Recipe/Trade key (R, recipe book only: the merchant screen is Village & Pillage's), recipe book tab
      tooltips, Other Players (P and the pause menu icon, inactive in a world nobody can join) with Player Options' mute.
- [x] Client test: `deepFreeze` step and the 26.4 pause icon row; 146 pass, 0 fail. The sulfur caves color check now
      reads at blend radius 1 (Core 1.5.0's 1.13 Biome Blend defaults to radius 2, so a 3x3 span blends with plains).
- [ ] Out of reach: F3's categories (1.12.2's F3 is one text block), keyboard navigation of menus, Javanese and
      Chavacano (Core has no language pipeline, and 1.12.2 lang codes differ), Repeat Trade (Village & Pillage's
      screen), Other Players' Blocked tab, friends and reporting (Mojang's online services). Splashes: unchanged in
      Pre-Release 1.

## Backport Core 1.5.0 (`core-1.5.0-deepfreeze`, off `core-1.5.0`)
- [x] `freeze/` freeze ticks (decision 3): 26.4's rules (undead immune, none on fire, leather, 140 ticks, damage every 40,
      x5 blazes, magma cubes, striders, slowness), effects that hold the counter and set the damage, sync, frost overlay,
      shaking, `freezing` config, `freezeDamage` also Game Drops'.
- [x] `speleothem/` (decision 1): 26.4's `SpeleothemBlock` as `BlockSpeleothem`, `falling_speleothem` (Core entity 2; the boats have 0 and 1),
      stalagmite and falling stalactite damage, tridents.
- [x] `advancement.MonsterHunter`, `locate.BiomeLocate.next`, the frostbite egg in the modern egg layer.
- Self-test group `freeze` (25 checks); the full Core suite passes (one timing check flaked under load, passing alone).

## Test results (2026-11-27, Pre-Release 2)
- GD `-Pselftest=all`, fresh world, Game Drops alone: 4333 pass, 0 fail (5 new Pre-Release 2 checks).
- Client test (`runClient -Pselftest=client`): 146 pass, 0 fail.

## Test results (2026-11-26)
- GD `-Pselftest=all`, fresh world, Game Drops alone: 4328 pass, 0 fail.
- The same with Village & Pillage, Update Aquatic, Caves & Cliffs, Wild Update, Trails & Tales and Tricky Trials dev
  jars: 4411 pass, 2 fail, both in 26.2's peaceful-spawning check (`changes`: a spawn answered DENY outside the pass,
  so another mod's spawn rule answers there); no crashes, clashes or linkage errors. Not yet compared with main.

## Owners (Rule 1 and 2)
Unchanged from the first plan: powder snow, calcite and deepslate are Caves & Cliffs'; ancient cities Wild Update's;
blue ice and the drowned Update Aquatic's; the trader Village & Pillage's; copper chests Tricky Trials'. Each piece
here works without its owner and joins it when installed.

## Deviations from 26.4
- Icicles' `attached` is read from the neighbor (1.12.2 has no metadata left); 26.4 stores it at placement.
- Brittle ice's crack particles are block-crack particles (26.4's level event 2014 has no 1.12.2 twin).
- Frostbite: the "path ends short of the target" test compares the path's end with the target's block (1.12.2's path
  keeps no target); turtle-egg trampling and turtle targeting are left out (Update Aquatic's turtles).
- Freezing's particle shows beside 1.12.2's swirl, on entities whose only effect is Freezing.
- Ice caves: Caves & Cliffs' terrain has no temperature in its column noise, so 26.4's temperature band is its frozen
  oceans; legacy worlds use our patches. The bands come after 1.12.2's ores (which they take) and the hosted ores add
  26.4's; ores outside the bands' noise keep 1.12.2's too. Features are cut at the four chunks 1.12.2 guarantees
  (26.4's region is 3x3).
- Jukebox: ejecting in playlist mode drops the disc where 1.12.2 drops it, not 2.2 above; the chest screen's "Playing
  Next" tooltip is not shown.
- Pig boosts: a fresh 1.12.2 boost over 700 ticks is drawn again (uniform 140-700); the boost is not saved.

## Follow-ups (not this branch)
- **Caves & Cliffs release**: hand powder snow to Core's freeze ticks (`FreezeTicks.markInFreezingBlock`) and pointed
  dripstone to `BlockSpeleothem`; list the frostbite in its powder snow walkers; until then a frozen mob in C&C's powder
  snow and with Freezing runs both counters.
- **Village & Pillage**: 26.4's trade changes (farmer's cake 1 emerald, weaponsmith's iron sword 3 uses), its own table.
- **Nether Update**: warped fungus on a stick 140-700 ticks.
- **Core**: Adventuring Time's 26.4 list (ice caves); the look and feel pieces above.
- Languages: done 2026-11-27 from 26.4-pre-2's Mojang files (vanilla names) and `tools/lang/translations/` (the 32
  config keys, translated by hand in the 15 languages; es_ar, es_uy, es_ve copy es_mx). Rebuild at release.
- **Nether Update**: Pre-Release 2's steering fix (a finished boost syncs a total of 0) for the warped fungus on a stick.
- **Core (optional, not 26.4's plan)**: Pre-Release 2's `#wall_side_shape_override` (the honey block) for `ModernWall`.

## Pre-Release 1 rules added to the first plan on main (2026-11-25)
The first plan (on `main`) gained these from the Pre-Release 1 jar; check each against the built streams at release:
- Strongholds are biased toward ice caves (`#stronghold_biased_to`); mineshafts, standard ruined portals and trial
  chambers also generate in them.
- Icicles also melt above `#melts_icicle_above` (magma block).
- Brittle ice: only living entities crack it (mobs with `mobGriefing`, players outside spawn protection); shulkers
  crack the brittle ice they sit on; iron golems never spawn on it; lily pads go on it (`#supports_lily_pad`).
- Leather armor also stops a skeleton becoming a stray (`isWearingFreezeImmuneWearables`).
- Mushrooms: any block whose top has a sturdy center (`canSupportCenter`, so end rods count).
- Honey blocks support blocks as a full block, and mobs never spawn on them.
- Projectiles: decorated pots and chorus flowers break only when the projectile `mayInteract` and `mayBreak` there;
  what `mayBreak` allows a mob's arrow is checked at release (Snapshot 5 tied it to `mobGriefing`).
- Jukebox playlists: single chests only (`#jukebox_containers`), never half of a double chest.

## Order
1. **Done (2026-11-24):** the first plan.
2. **At release:** diff the release jar against Pre-Release 2 (done for Pre-Release 1 -> 2 on 2026-11-27), fix any
   number that changed, rerun the generators (`tools/iceblocks`, `icecaves`, `frostbite`, `jukebox`, `changes264`,
   `lookfeel264` with `GD_ASSETS_VERSION=26.4`), switch `tools/assets.py`'s default to 26.4, put `26.4` first in
   `tools/lang/langs.json` and rebuild the languages.
3. **Done (2026-11-26, on pre-1):** streams 1-7, the Core pieces.
4. Release checks: `tools/release_suite.sh` alone and with `FAMILY=1`, client scenes, boot-server; Core's RELEASING.md.

## Open questions
1. **Icicles: decided 2026-11-24, shared** (Core's speleothem). Built.
2. **When to start: decided 2026-11-24, wait; changed 2026-11-26 by the user: build on Pre-Release 1 now** and diff at
   release.
3. **Who owns freezing: decided 2026-11-24, Backport Core.** Built in Core; Caves & Cliffs' handover is a follow-up.
