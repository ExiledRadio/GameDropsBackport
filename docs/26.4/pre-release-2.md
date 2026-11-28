# 26.4 Pre-Release 2 against Pre-Release 1

Sources: `E:/MCDownloads/minecraft-26.4-pre-2/` (client and server jars, asset index 36, Vineflower 1.11.1 source in
`src/`) against `E:/MCDownloads/minecraft-26.4-pre-1/`. The raw lists are next to the jars:
`diff-pre1-pre2-client.txt` (every changed jar entry) and `diff-pre1-pre2-src.txt` (the source diff of the 95 changed
classes). Released 2026-11-27. Data version 5131 -> 5132, data pack format 128.1 -> 128.2, snapshot protocol 349 -> 350.

## What changed

**Data:** 1619 changed jar entries. 1511 are structure templates whose only change is the data version (checked byte by
byte after decompressing). The rest:
- `sulfur_cube_archetype/cold.json`: `push_sound` `entity.sulfur_cube.fast_sliding.hit` -> `fast_sliding.push`.
- `block_sound_set/straw_bed.json`: `break_sound` `block.straw_bed.break` -> `block.straw_bed.break_leave`.
- New block tag `wall_side_shape_override` (`honey_block`): a wall under one of these takes its side shapes from the
  block's outline shape, not its collision shape.
- `assets/minecraft/lang/en_us.json`: 2 new keys, `narration.slot.usage.{focused,hovered}.store` (bundle narration).
- `textures/gui/sprites/spectator/teleport_to_{player,team}.png` redrawn.
- No recipe, loot table, advancement, tag (but the one above), worldgen, biome, feature, model, blockstate, item
  definition or texture of ours changed. Asset index: only the lang files changed (the translators' updates); no sound
  or sounds.json change.

**Code, in Game Drops' 26.4 scope:**
- `Blocks.ICICLE`: map color `COLOR_LIGHT_BLUE` -> `ICE`.
- `IceBallItem`: the throw sound's source `NEUTRAL` -> `PLAYERS`.
- `ThrowableBallProjectile.onHit`: the hit sound is skipped when the projectile is silent.
- `SulfurCubeArchetypes`: the cold archetype's push sound `SULFUR_CUBE_FAST_SLIDING_PUSH` (matches the data).
- `JukeboxBlockEntity.notifyItemChangedInJukebox`: `BLOCK_CHANGE` is emitted on removal in playlist mode too
  (`!wasInserted || !isInPlaylistMode`); insertion in playlist mode still emits nothing.
- `ItemBasedSteering` (carrot and warped fungus on a stick): `tickBoost` sets the total to 0 when a boost ends;
  `onSynced` boosts only when the synced total is above 0; a load with `boosting` false reads the total as 0.
- `SpeleothemBlock.isTip` / `isSpeleothemWithDirection`: also require `instanceof SpeleothemBlock` (a non-speleothem
  in `#speleothems` no longer throws).
- `IcicleBlock`: an annotation only (`@LevelEvent.Value`); the landing level event stays 1055.

**Code, outside the 26.4 plan but in Game Drops:**
- `StrawBedBlock` (26.3's straw bed): `destroyOnUse` and `destroyOnLeave` use `level.destroyBlock(pos, false)` (break
  particles and the sound set's break sound, now `break_leave`); `spawnDestroyParticles` uses the normal particles while
  the other half is still a straw bed, level event 2014 otherwise.
- `SulfurCube.readyForShearing`: not while primed.

**Code, not ours:** first-person map and spear swing animations, item display and slot tooltips, bundle narration,
friends list narration, sign edit focus, report screen reopen, recipe book overlay close (clears focus), moving block
outline brightness, `/effect` counting non-living targets, flint and steel and fire charges in the sneaking
"use on block" check, fire and water buckets above the build height, lily pad build-limit message, camel and turtle
sounds (`playSound`, silent turtles), fireballs, wither skulls and wind charges dying 30 above the build height
(fireballs and skulls now explode there), splash particles at the fluid's own block, furnace experience from
non-cooking recipes, jigsaw child-space bookkeeping, `RandomizedIntStateProvider` range checks, saved data upgrades,
the data version.

## What Game Drops did

- Icicle map color ice (`BlockIcicle`); the ice ball's throw sound in the players category (`ItemIceBall`); no break
  sound from a silent ice ball (`EntityIceBall`).
- The cold sulfur cube's push sound is fast sliding's push (`SulfurCubes`).
- A playlist jukebox that loses its disc sends `block_change` (`Playlists`), through Core's game events.
- A finished pig boost syncs a total of 0, and the client ignores a synced 0 (`DeepFreezeChanges`).
- Straw beds break with `break_leave` and break through `destroyBlock` (particles and sound) (`SoftBlocks`,
  `BlockStrawBed`).
- Sulfur cube shearing: already refused while primed (`processInteract` returns first); 1.12.2's dispensers can't
  shear it. No change.
- Self-test checks for each (`coldcube`, `icecaves`, `changes264`, `softblocks`).
- The generators (`tools/iceblocks`, `icecaves`, `frostbite`, `jukebox`, `changes264`, `lookfeel264`) read
  `26.4-pre-2` by default and were rerun: every asset came out the same; `vanilla_text_264/` took the translators'
  new wording in 12 languages (41 lines).
- Languages: `tools/lang/langs.json` reads `26.4-pre-2` first.

## Backport Core

Nothing in Core's 26.4 systems changed: freeze ticks (`Entity`, `LivingEntity`, `PowderSnowBlock` and the freezing
effect are untouched), Improved Fog, the recipe book's tab names (no lang change), the Repeat Recipe key and Other
Players (narration only). Core's speleothem already treats only `BlockSpeleothem` as a speleothem, which is
Pre-Release 2's new guard. The recipe book's `closeOverlay` focus fix has no 1.12.2 counterpart (no keyboard focus).
Optional, outside the 26.4 plan: `#wall_side_shape_override` (honey block) for `ModernWall`.
