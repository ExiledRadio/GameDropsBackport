# Bytecode patches

This mod's own coremod, `asm/GameDropsCore` (no MixinBooter). One transformer per patch; each one listed here with
the class and method it changes, what the hook does, and how it keeps 1.12.2's behavior while its stream is off.
Every transformer's targets are verified on a real server by `tools/release/boot-server.sh` (`PatchCheck`).

Inserted code loads method parameters only at a method's head (Mojang's ProGuard frames mark unread locals TOP); the
other patches swap one instruction for a static call with the same stack shape. A head insertion is skipped when the
method starts on a frame.

## Stream 8, changes (`asm/ChangesTransformer`, hooks in `changes/ChangeHooks` and `changes/SpamCounters`)

| # | Class and method | Change | Off |
| --- | --- | --- | --- |
| P1 | `BlockBed.onLanded` | The constant 0.66 becomes `ChangeHooks.bedBounce()` (26.2's 0.75). | Returns 0.66. |
| P2 | `EntityEnderman.attackEntityFrom` | Head: `if (endermanTakesProjectile(this, source)) return super.attackEntityFrom(source, amount);` (invokespecial `EntityMob`). | The test is false. |
| P3 | `EntityLiving.despawnEntity` | Head: `if (persistentDespawn(this)) return;` For a persistent mob, idle time is zeroed only with a player within 32 blocks. Nether Update's P10 guards the same head for riders; the two compose (each its own jump). | The test is false. |
| P4 | `EntityWitch.attackEntityWithRangedAttack` | Its `EntityPotion.shoot(DDDFF)` becomes `ChangeHooks.witchShoot` (0.45 within 2 blocks). | Calls `shoot` unchanged. |
| P5 | `EntityZombie.onLivingUpdate`, `AbstractSkeleton.onLivingUpdate` | The helmet's `ItemStack.setItemDamage(I)` becomes `ChangeHooks.sunDamage` (`attemptDamageItem`, so Unbreaking counts). | Calls `setItemDamage` unchanged. |
| P6 | `NetHandlerPlayServer.processChatMessage` | Head: `if (SpamCounters.chatPacket(this, packet)) return;` (never true; on the server thread it notes whether the message is a command). The spam limit `200` becomes `aload 0` + `SpamCounters.spamLimit(this)`: 26.2's two counters count the message and kick at their thresholds, and 1.12.2's own counter gets a limit it never passes. `this` is read again right after, so it is live in every frame. | `spamLimit` returns 200; `chatPacket` does nothing. |
