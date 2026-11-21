# Game Drops Backport

Minecraft's 26.x game drops on 1.12.2: 26.1's golden dandelion and new babies, 26.2's sulfur and cinnabar, the sulfur cube and sulfur caves, 26.3's poplar and the dappled forest, wool and concrete shapes, cushions, straw beds and abandoned camps, and every later 26.x drop. Every part can be switched off on its own.

Brings the drops through 26.3. Later drops join it as they ship.

## Install

1. Download `GameDropsBackport-<version>.jar` and `BackportCore-<version>.jar` from [Releases](https://github.com/ExiledRadio/GameDropsBackport/releases).
2. Put both in your `mods` folder (Forge 1.12.2, 14.23.5.2847 or newer). Client and server both need them.
3. Optional: [Fluidlogged API](https://www.curseforge.com/minecraft/mc-mods/fluidlogged-api) 3.3.3 or newer, for waterlogged blocks.

Back up your worlds before adding it to one you play in.

## Building

Needs Java 8. Put Backport Core's `-dev` jar in `deps/` (named `BackportCore-<core_version>-dev.jar`, see `gradle.properties`), then run `./gradlew build`. The jar is in `build/libs/`.

## Bugs

[Discord](https://discord.gg/kxQvMDJBTN)

Not affiliated with Mojang.
