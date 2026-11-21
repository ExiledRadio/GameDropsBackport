# Stream 8: look and feel (26.1 to 26.3)

Package `lookfeel` (client side in `lookfeel/client`), config category `lookAndFeel` (five switches, all on), packet
ids 40-44 (none used), dev-server port 25928. Branch `claude/gd-lookfeel-48b73a`. Requires Backport Core 1.4.6
(branch `gd-lookfeel`: the 26.2 pause row and `MenuBackground.contentFrame`).

Backport Core shows the newest installed version's single pieces. This mod registers 26.1, 26.2 and 26.3, so with it
installed the newest version is 26.3 and its pieces win over Tricky Trials' 1.21.11. Core's "Newest Look" still picks
an older one, and Core's Look & Feel page lists this mod's five switches under their sections. Core's 26.3 texture
layer (5 textures) also turns on now, since a backport registers 26.3.

Survey and the list of what is and is not built: `docs/PLAN.md`, stream 8. Sources: the 1.21.11, 26.1, 26.1.2, 26.2
and 26.3 client jars and asset indexes, and 26.3's decompiled `PauseScreen`, `WorldOptionsScreen`, `OptionsScreen`
(`G:/MCDownloads/gd-lookfeel-decomp`, Vineflower on Java 25).

## Changelog section

*   **Title screen**: 26.3's panorama, the dappled forest with an abandoned camp; the 26.x splashes ("Java 16 + 1 +
    4 * 2 = 25!", "Now on Vulkan 1.2!", "Music by fingerspit!", and straight quotes in two older ones).
*   **Menu music**: fingerspit's Ebb, Home, Memories, Nightly and Shores, in place of 1.21.6's five, as in 26.2.
*   **Pause menu**: Report Bugs and Send Feedback are one row of icon buttons (26.2), and World Options... takes Open
    to LAN's place (26.3).
*   **World Options**: Default Game Mode, Personal Game Mode, Allow Cheats, Difficulty and its lock; LAN with a port
    of your choice, Command Access and Force Game Mode for other players. Changes apply with Apply Changes.
    Difficulty is no longer on the Options screen in a world.
*   Vanilla text as 26.3 words it: "Isn't It Iron Pick?", "Build, light, and enter a Nether Portal" and others.

## How it works

*   `LookAndFeel.preInit` (both sides) hands Core one `Contribution` per drop: 26.1 (splash), 26.2 (splashes, the
    menu tracks, the two pause sprites), 26.3 (panorama and overlay, splash fix, `vanilla_text/`). 1.21.6's menu
    tracks are removed by path, whichever backport plays them.
*   `lookfeel.client.LookAndFeelClient` (events, no patch): on `GuiIngameMenu`, button 7 (which Core places) reads
    World Options... and opens `WorldOptionsScreen`; on `GuiOptions` in a world, Difficulty (108) and its lock (109)
    are removed. Both only while `worldOptions` is on and the menus' look is 26.3 (`WorldOptionsRules.active`).
*   `WorldOptionsScreen`: 26.3's layout (header 33, footer 33, two two-column grids, 308-wide game mode rows,
    bold underlined headings), its rules in `WorldOptionsRules` (shared with the self-test). Applying runs on the
    integrated server's thread with 1.12.2's own calls: `setAllowCommands`, `setGameType` (/defaultgamemode's),
    `EntityPlayerMP.setGameType`, `setDifficultyForAllWorlds`, the difficulty lock, `PlayerList.setCommandsAllowedForAll`,
    and `PlayerList.setGameType` (null for Force Game Mode off: guests keep their own mode), then each player's
    permission level. `Lan.publish` is `shareToLAN` with the chosen port.
*   Assets: `tools/lookfeel/generate.py` (panorama faces from the asset index, since the jar has placeholders;
    sprites; the three tracks the sulfur caves stream did not already ship; splash files diffed from the jars;
    vanilla text; names). Lang: Mojang's own 26.3 keys (forced in `vanilla_map.txt` where `.tooltip` would make them
    ours); `tools/lang/vanilla_text.py` (copied from Tricky Trials) for `vanilla_text/`.

## Where it differs from 26.3

*   Game Rules... and Restrictions... are left out of World Options: 1.12.2 has neither screen.
*   Once open to LAN, the LAN switch and port stay: 1.12.2's server cannot stop listening.
*   Not a host: Difficulty is shown inactive (1.12.2 has no packet to change a server's difficulty).
*   The pause icon row has Report Bugs and Send Feedback only: Friends and Player Reporting are online services.

## Tested

*   `cmd //c "start /affinity FFFF0000 /b /wait bash tools/lookfeel_suite.sh"` (fresh world, seed 1013, Core 1.4.6):
    **`SELFTEST DONE pass=1531 fail=0`**; `check_assets.py`: 83 blocks, 2890 states, 150 item models, 722 sound files,
    0 errors; `build_langs.py check`: 18/18, 0 errors, 0 warnings; `vanilla_text.py check`: 18 languages, 0 errors.
    Release jar: 0 `/selftest/` entries.
*   Headless (`lookfeel`): the three versions are ours, the panorama is the newest (six square faces), the overlay,
    Core's splash pool with our files, Core's menu pool with a 1.21.6 track installed before us (menu1 and the five),
    the tracks in the jar, the two sprites served through Core, the pause grid's numbers, vanilla text in 18
    languages, and every `WorldOptionsRules` decision.
*   Client (`./gradlew runClient -Pselftest=client`): **`CLIENT SELFTEST DONE pass=28 fail=0`**: title screen panorama;
    pause menu (World Options... in Open to LAN's place, two 20-wide icons in the third row); Options without
    Difficulty; World Options before and after changes (Allow Cheats, Personal Game Mode to Adventure, Difficulty to
    Hard applied on the server); port 80 refused; LAN opened on a free port with Command Access; LAN stays on after.
    Screenshots: `eclipse/gdclient/screenshots`.
*   Registering 26.x makes Core use its modern biome rules for a world with this mod alone (see PLAN). Two poplar
    checks depended on seed 1013's 1.12.2 layout; the poplar test now populates the fallen-tree area first and picks
    a cell deep in the dappled forest.
