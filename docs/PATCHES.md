# Bytecode patches

This mod's own coremod, `asm/GameDropsCore` (no MixinBooter). One transformer per patch; each one listed here with
the class and method it changes, what the hook does, and how it keeps 1.12.2's behavior while its stream is off.
Every transformer's targets are verified on a real server by `tools/release/boot-server.sh` (`PatchCheck`).

None yet.
