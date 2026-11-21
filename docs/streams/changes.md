# Stream 8: changes to existing things (changes)

26.1, 26.2 and 26.3's changes to commands, game rules, mobs, blocks, items and world rules that 1.12.2 already has.
Package `changes`, config category `changes` (one switch per change, on by default, under the stream's `enabled`),
self-test `-Pselftest=changes`, suite `tools/changes_suite.sh`. Patches P1-P5 (`docs/PATCHES.md`). Tricky Trials'
stream 25 (`tweaks`) is the model.

Sources: minecraft.wiki's *Java Edition 26.1*, *26.2* and *26.3* pages (Changes, and Fixes where they change
behavior on purpose), 26.3's decompiled client (`G:/MCDownloads/gd-mech-decomp/out`, Vineflower; 26.x ships
unobfuscated), its data (`world_clock`, `timeline`, tags, recipes) and the English language files of 1.21.11, 26.1.2,
26.2 and 26.3, diffed for command messages.

## Built

| Setting | Version | What changes | How |
| --- | --- | --- | --- |
| `timeCommand` | 26.1 | `/time` runs on world clocks (`minecraft:overworld`, `minecraft:the_end`): `time [of <clock>] set <time>\|<marker>`, `add`, `pause`, `resume`, `rate <0.00001-1000>`, `query time`, `query <timeline> [repetition]`, `time query gametime`. `set day`, `noon`, `night`, `midnight` go forward to the next marker (an error if already there); 1.12.2 set the time back to day 0. Times take units (`1d`, `20s`, `5t`). The default clock is the dimension's: the overworld's, the End's own, none in the Nether ("There is no default clock in dimension minecraft:the_nether"). 26.1's messages and return values (the clock's total ticks). Timelines `minecraft:day`, `moon`, `early_game`, `villager_schedule`. | `TimeCommand` replaces 1.12.2's under the same name; `WorldClocks` (saved data `gamedropsbackport_world_clocks`) keeps rate, pause and the End clock. The overworld clock is 1.12.2's world time, so the sky, sleep and every mod follow it. |
| `gameRuleMessages` | 26.3 | "Game rule %s is currently set to %s", "Game rule %s is now set to %s", and "Game rule %s is already set to %s" as an error. | `GameRuleCommand` (1.12.2's, subclassed). |
| `teamColorNames` | 26.2 | A team color is only its lowercase name with underscores (`dark_purple`); `darkpurple` and `DarkPurple` get 1.12.2's "valid values" error. | `CommandEvent` on `/scoreboard teams option <team> color`. |
| `bedBounce` | 26.2 | Beds send a falling entity back up at 0.75 of its speed (0.6 for anything not alive), not 0.66. | P1. |
| `mushroomStew` | 26.3 | Mushroom stew from a bowl and any two of `#mushrooms` (brown, red, the poplar stream's shelf mushroom). | 1.12.2's `minecraft:mushroom_stew` replaced in the recipe registry. Restart. |
| `shieldBeforeTools` | 26.3 | Hoe or shovel in the main hand, shield in the off hand, not sneaking: the shield is raised instead of tilling or making a path. The block itself is still used (a chest opens). | `RightClickBlock` denies the item use only. |
| `endermenRiding` | 26.3 | An enderman riding something takes projectile damage and never teleports. | P2; `EnderTeleportEvent`. |
| `teleportTargets` | 26.3 | Endermen never land on bedrock or touch `#dangerous_for_teleportation` (fire, magma, cactus and the family's soul fire, campfires, sweet berry bushes, wither roses, pointed dripstone, powder snow); shulkers never attach to bedrock. | `EnderTeleportEvent` (1.12.2's landing search repeated there). |
| `persistentMobsIdle` | 26.3 | A persistent mob (named, for example) counts idle time while no player is within 32 blocks, so it stops wandering as other mobs do. | P3. |
| `leaderZombieHealth` | 26.1 | A leader zombie (and husk, zombie villager, zombie pigman) spawns with its raised maximum health filled. | `EntityJoinWorldEvent`, once per zombie. |
| `witchCloseThrows` | 26.1 | Witches throw at 0.45 instead of 0.75 at 2 blocks or less, so the potion no longer flies over the target. | P4. |
| `peacefulSpawnEggs` | 26.2 | On Peaceful, a dispenser keeps a monster's spawn egg (it still clicks). | The spawn egg's dispenser behavior wrapped at post-init. |
| `sunDamageEnchantments` | 26.3 | Unbreaking works on the helmet a zombie or skeleton wears against the sun. | P5. |
| `spectatorPortals` | 26.3 | Spectators go through nether portals (to an existing portal), end portals and end gateways. | Player tick: the portal blocks a spectator overlaps. |

## Not the same as vanilla

- `/time`: the End clock counts, but nothing in 1.12.2 reads it (26.x's End flashes are 1.21.9's, Tricky Trials'). Modded
  dimensions use the overworld clock when their provider is a surface world, none otherwise. 1.12.2's `query daytime`
  and `query day`, gone in 26.1, still answer as `query minecraft:day [repetition]` so old command blocks keep working
  (not suggested). Without the `of` form, 1.12.2's `/time set` still sets every world's time, as 1.12.2 did.
- `/gamerule` with no arguments still lists the rules, and a value can still create a rule (1.12.2's forms).
- A spectator in a nether portal with no portal on the other side arrives at the scaled position; 26.3 refuses the trip.
- A shulker whose teleport is refused has already turned to its new face; it finds a valid one on its next tick.

## Not built

- **Sign click commands off by default** (26.3, `allow_op_features`): 26.3's data fix allows every sign of an older
  world; 1.12.2 cannot tell such a sign from one a command placed later, so doing it would break adventure maps' signs.
- **Off-map player markers keep their facing** (26.3): 1.12.2's off-map marker is a round dot; turning it shows nothing.
- **Named paintings show their name** (26.3): 1.12.2 paintings cannot be named (name tags work on mobs, and the painting
  item's name is not kept). Needs the item-to-entity name copy too; Tricky Trials' painting variants stream places
  paintings, so it belongs there.
- **Ocelots on Peaceful** (26.2): 1.12.2 spawns ocelots with monsters; Peaceful skips that category entirely, so it
  needs a spawner pass of its own.
- **Chat and command spam counters** (26.2 `chat-spam-threshold-seconds`, `command-spam-threshold-seconds`): 1.12.2's
  single counter already kicks at the same rate (10 s); separate counters and the server.properties keys are left.
- **Host's game mode becomes the default** (26.2): only in 26.2's World Options screen as far as the code shows; left.
- **/advancement and /team join counts** (26.2, 26.3): 1.12.2's commands are other commands with other messages.
- **Trade enchanting cost, trades appearing while the screen is open** (26.1, 26.3): villagers are Village & Pillage's.

## Owned elsewhere or already 1.12.2's

- Name tag recipe and trader trade, skeleton and zombie horses not panicking, the 26.1 sound variants: stream 1.
- Explorer map items, their names and conversion: stream 7. Wool and concrete shapes: stream 6.
- Game rule snake_case ids (`minecraft:advance_time`, ...): 1.21.11's, Tricky Trials' scope (out of it there).
- `/locate`'s "within a reasonable distance": Backport Core's `/locate` already has 26.3's text.
- Hostile spawn eggs refused for players on Peaceful: 1.21.x's, Tricky Trials'.
- Axe and shield: Tricky Trials' `axeShield` (1.21).
- Suspicious stew from any two mushrooms: its owner's (Village & Pillage).
- Already 1.12.2's: zombie reinforcements leaving creative players alone, the dragon respawn needing all 4 crystals,
  feeding a wolf the last item healing fully, baby zombie height (0.975), `/give`'s amount limit being an error,
  `/publish` without a game mode, wolves doing no damage on Peaceful.
- Technical or data-driven (skipped): data components, the World Options screen, game rules file, weather file,
  knockback resistance's minimum, brewing and furnace fuel tags, `/swing`, `/fetchprofile`, `/posteffect`.
