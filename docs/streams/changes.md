# Stream 8: changes to existing things (changes)

26.1, 26.2 and 26.3's changes to commands, game rules, mobs, blocks, items and world rules that 1.12.2 already has.
Package `changes`, config category `changes` (one switch per change, on by default, under the stream's `enabled`),
self-test `-Pselftest=changes`, suite `tools/changes_suite.sh`. Patches P1-P5 and P7 (`docs/PATCHES.md`). Tricky Trials'
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
| `ocelotsPeaceful` | 26.2 | Ocelots spawn in jungles on Peaceful (MC-1788). 26.x runs the monster category on every difficulty (only `spawn_monsters` stops it) and refuses each monster on Peaceful by itself, so the ocelots its jungles list with the monsters still spawn, at Normal's rate. | `PeacefulSpawning`: on Peaceful, after each world tick, 1.12.2's own `WorldEntitySpawner` runs the monster category once more (same caps, chunks, packs and rules); Forge's spawn check refuses every `IMob` it picks, at highest and lowest priority. `doMobSpawning` and the dedicated server's `spawn-monsters=false` still stop it. |
| `spamThresholds` | 26.2 | Chat and commands have separate spam counters (MC-302268): each message adds 20, each tick takes 1 off, and the player is kicked ("Kicked for spamming") when a message leaves its counter at 20 times `chat-spam-threshold-seconds` or `command-spam-threshold-seconds` (server.properties, 10 each, written on start; 0 never kicks). Open to LAN never kicks (26.2's `IntegratedServer` returns 0). Operators and the host are never kicked. | P7 (`SpamCounters`). |
| `hostGameModeDefault` | 26.2 | The host's `/gamemode` (and F3 + N, which runs it) also sets the world's default game mode. **Off by default**: 26.3's `GameModeCommand` dropped it when World Options split Personal Game Mode from Default Game Mode. | `HostGameModeCommand` (1.12.2's, subclassed): `setGameType` (`/defaultgamemode`'s call) after a host's change. |
| `advancementCounts` | 26.2, 26.3 | `/advancement grant\|revoke` takes any number of players (`@a`) and answers once, as 26.3's `CommandResponseTracker`: `one`/`many` advancements to `one` changed player by name or to `many` (how many changed); "many" counts the advancements asked for, the failure counts the players asked for; nothing changed is the error. 26.3's keys and English. | `AdvancementCountsCommand` (1.12.2's, subclassed; it reads its own selectors). `test` stays 1.12.2's. |
| `teamJoinCounts` | 26.3 | `/scoreboard teams join` and `leave` answer as 26.3's `/team`: the one member that joined or left by name, otherwise how many ("Added 2 members to team [x]", "Removed 0 members from any team"); members already out of a team are no error. An unknown team is "Unknown team". | `CommandEvent` (`TeamCounts`), 1.12.2's reading of the members. |
| `unpublish` | 26.2 | `/unpublish` (permission level 4): a world open to LAN is closed again; "No multiplayer game published" otherwise and on a dedicated server. | `UnpublishCommand`; the closing is `lookfeel.client.Lan.unpublish`, shared with World Options' LAN switch (see `lookfeel.md`). |

## Not the same as vanilla

- `/time`: the End clock counts, but nothing in 1.12.2 reads it (26.x's End flashes are 1.21.9's, Tricky Trials'). Modded
  dimensions use the overworld clock when their provider is a surface world, none otherwise. 1.12.2's `query daytime`
  and `query day`, gone in 26.1, still answer as `query minecraft:day [repetition]` so old command blocks keep working
  (not suggested). Without the `of` form, 1.12.2's `/time set` still sets every world's time, as 1.12.2 did.
- `/gamerule` with no arguments still lists the rules, and a value can still create a rule (1.12.2's forms).
- A spectator in a nether portal with no portal on the other side arrives at the scaled position; 26.3 refuses the trip.
- A shulker whose teleport is refused has already turned to its new face; it finds a valid one on its next tick.
- Peaceful ocelots: 1.12.2's spawner picks one list entry per pack, so ocelots come as often as on Normal, where
  zombies and skeletons take most picks; 26.x picks the same way. Another mod's non-monster listed with the monsters
  spawns on Peaceful too, as it would in 26.x.
- Spam: a chat message another mod cancels (`ServerChatEvent`) is not counted, as 1.12.2 counts it after the event.
- `/advancement`: 26.3 counts "many" as the advancements asked for, not those changed (26.2 counted those changed).
- `/scoreboard teams join` again to the same team counts the member, as 26.3's `addPlayerToTeam` (leave and re-add)
  does.

## Not built

- **Sign click commands off by default** (26.3, `allow_op_features`): 26.3's data fix allows every sign of an older
  world; 1.12.2 cannot tell such a sign from one a command placed later, so doing it would break adventure maps' signs.

## Built by other family mods

- **Off-map player markers keep their facing** (26.3): Backport Core 1.4.4 (`map.MapFacing`, `asm.MapFacingTransformer`).
- **Named paintings show their name** (26.3): Tricky Trials Backport (`decor.client.PaintingNameplate`, `PaintingTooltip`).
- **Trade enchanting cost, trades appearing while the screen is open** (26.1, 26.3): Village & Pillage Backport
  (`trade.EnchantedTrades`, `client.MerchantOverlay`).

## Owned elsewhere or already 1.12.2's

- Name tag recipe and trader trade, skeleton and zombie horses not panicking, the 26.1 sound variants: stream 1.
- Explorer map items, their names and conversion: stream 7. Wool and concrete shapes: stream 6.
- Piglins and zombie horses on Peaceful (26.2, the ocelots' bullet): Nether Update's piglins and Tricky Trials'
  zombie horses (1.12.2 spawns none naturally). `ocelotsPeaceful` lets through any non-monster a mod lists with the
  monsters, so a piglin that is not an `IMob` would already spawn.
- Game rule snake_case ids (`minecraft:advance_time`, ...): 1.21.11's, Tricky Trials' scope (out of it there).
- `/locate`'s "within a reasonable distance": Backport Core's `/locate` already has 26.3's text.
- Hostile spawn eggs refused for players on Peaceful: 1.21.x's, Tricky Trials'.
- Axe and shield: Tricky Trials' `axeShield` (1.21).
- Suspicious stew from any two mushrooms: its owner's (Village & Pillage).
- Already 1.12.2's: zombie reinforcements leaving creative players alone, the dragon respawn needing all 4 crystals,
  feeding a wolf the last item healing fully, baby zombie height (0.975), `/give`'s amount limit being an error,
  `/publish` without a game mode, wolves doing no damage on Peaceful.
- Technical or data-driven (skipped): data components, game rules file, weather file, knockback resistance's
  minimum, brewing and furnace fuel tags, `/fetchprofile`, `/posteffect`.
- `/swing` (26.x, with 26.3's `animation` and `duration`): Backport Core's, with its other commands (`commands.swing`).
- The World Options screen and closing LAN from it: the look and feel stream (`lookfeel.md`).

## Tested (gd-changes-gaps, 2026-11-21, E-cores)

- `runServer -Pselftest=all` on a fresh world (seed 1013): `SELFTEST DONE pass=1794 fail=0`; the `changes` group alone
  172/0 (`ChangesGapsSelfTest`: the Peaceful pass's spawn check, both spam counters through a real chat packet (P7),
  the server.properties keys, `/advancement` and team replies with two real server players, `/unpublish` on a
  dedicated server). `check_assets.py` 0 errors; `build_langs.py check` 18/18, 0 errors, 0 warnings.
- `runClient -Pselftest=client`: `CLIENT SELFTEST DONE pass=86 fail=0`: World Options' LAN switch off closes the
  listener (a socket to the port is refused), the announcer and guests' command access; reopening; moving to a new
  port closes the old one; `/unpublish` (and after 1.12.2's `/publish`); `hostGameModeDefault` on and off.
- `tools/release/boot-server.sh` with Backport Core 1.4.4 (`gd-changes-gaps`) and the release jar, with and without
  Fluidlogged API 3.3.3: PASS, `PATCHCHECK ok 7`; server.properties gets both spam keys. Release jar: 0 `/selftest/`.
- Not tested in a real game: an ocelot spawning on Peaceful in a jungle (the pass and its spawn check are; a world
  with a player in a jungle is not), and a real second player being kicked for spam.
