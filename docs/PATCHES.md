# Bytecode patches

This mod's own coremod, `asm/GameDropsCore` (no MixinBooter). One transformer per patch; each one listed here with
the class and method it changes, what the hook does, and how it keeps 1.12.2's behavior while its stream is off.
Every transformer's targets are verified on a real server by `tools/release/boot-server.sh` (`PatchCheck`).

Inserted code loads method parameters only at a method's head (Mojang's ProGuard frames mark unread locals TOP); the
other patches swap one instruction for a static call with the same stack shape. A head insertion is skipped when the
method starts on a frame.

## Stream 8, changes (`asm/ChangesTransformer`, hooks in `changes/ChangeHooks`)

| # | Class and method | Change | Off |
| --- | --- | --- | --- |
| P1 | `BlockBed.onLanded` | The constant 0.66 becomes `ChangeHooks.bedBounce()` (26.2's 0.75). | Returns 0.66. |
| P2 | `EntityEnderman.attackEntityFrom` | Head: `if (endermanTakesProjectile(this, source)) return super.attackEntityFrom(source, amount);` (invokespecial `EntityMob`). | The test is false. |
| P3 | `EntityLiving.despawnEntity` | Head: `if (persistentDespawn(this)) return;` For a persistent mob, idle time is zeroed only with a player within 32 blocks. Nether Update's P10 guards the same head for riders; the two compose (each its own jump). | The test is false. |
| P4 | `EntityWitch.attackEntityWithRangedAttack` | Its `EntityPotion.shoot(DDDFF)` becomes `ChangeHooks.witchShoot` (0.45 within 2 blocks). | Calls `shoot` unchanged. |
| P5 | `EntityZombie.onLivingUpdate`, `AbstractSkeleton.onLivingUpdate` | The helmet's `ItemStack.setItemDamage(I)` becomes `ChangeHooks.sunDamage` (`attemptDamageItem`, so Unbreaking counts). | Calls `setItemDamage` unchanged. |

## Stream 7, camps (`asm/LootOriginTransformer`, hooks in `camps/LootOrigin`)

| # | Class and method | Change | Off |
| --- | --- | --- | --- |
| P6 | `TileEntityLockableLoot.fillWithLoot` | Head: `LootOrigin.enter(this)` (only `this` read); `LootOrigin.exit()` before each return. The camp loot (`CampLoot`) takes the container's block as 26.3's `ORIGIN` (biome check, map search) instead of the opening player's; outside a container it falls back to the player. Covers every subclass that inherits it (chests, Village & Pillage's barrel). | Only records; nothing but the camp tables reads it. |

