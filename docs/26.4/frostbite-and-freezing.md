# 26.4-pre1 Frostbite / Freezing / Ice Ball, numbers from the code

Paths are relative to `E:/MCDownloads/minecraft-26.4-pre-1/src/` unless they say 26.3. `W/` = `net/minecraft/world/`, `C/` = `net/minecraft/client/`, `D/` = `data/minecraft/`. Ticks are at 20/s.

## 1. Frostbite

### Class shape
- [ ] `Frostbite extends RangedZombie extends Zombie implements RangedAttackMob`. RangedZombie is new in 26.4. Drowned does NOT use it: it keeps its own trident code. (W/entity/monster/zombie/Frostbite.java:40, RangedZombie.java:20)
- [ ] Entity id `minecraft:frostbite`, category MONSTER. (W/entity/EntityTypeIds.java:65, EntityTypes.java:486-495)

### Attributes (Frostbite.createAttributes = Zombie.createAttributes + overrides; Frostbite.java:67-69, Zombie.java:162-169; registered DefaultAttributes.java:127)
- [ ] MAX_HEALTH 24.0 (zombie has the default 20). Frostbite.java:68
- [ ] MOVEMENT_SPEED 0.21 (zombie 0.23). Frostbite.java:68
- [ ] FOLLOW_RANGE 35.0. Zombie.java:164
- [ ] ATTACK_DAMAGE 3.0. Zombie.java:166
- [ ] ARMOR 2.0. Zombie.java:167
- [ ] SPAWN_REINFORCEMENTS_CHANCE: attribute default, randomized by `randomizeReinforcementsChance()`, the same as zombie. Zombie.java:168, 565. Reinforcements are `this.getType()`, so a frostbite calls frostbites (Hard only). Zombie.java:300-309
- [ ] Knockback resistance: permanent random bonus `nextDouble()*0.05` (0-5%). Follow-range bonus `nextDouble()*1.5*difficultyMultiplier`, applied only when > 1.0, as ADD_MULTIPLIED_TOTAL. Zombie.java:566-572
- [ ] Baby: speed modifier +0.5 ADD_MULTIPLIED_BASE (0.21 becomes 0.315). Zombie.java:75-77, 225-230
- [ ] Base melee damage per difficulty (vanilla player scaling of 3.0): Easy 2.5, Normal 3, Hard 4.5. Player.java hurtServer (`min(d/2+1,d)` on Easy, `d*1.5` on Hard)

### Dimensions (EntityTypes.java:486-495)
- [ ] sized 0.6 x 1.95, eyeHeight 1.74, passengerAttachments 2.0125, ridingOffset -0.7, clientTrackingRange 8, notInPeaceful. Identical to ZOMBIE (EntityTypes.java:1142-1151)
- [ ] Baby: `EntityDimensions.scalable(0.49, 0.98)`, eyeHeight 0.775, vehicle attachment y 0.1875. Zombie.java:96-98, 471
- [ ] Baby odds 5% (`nextFloat() < 0.05F`). Zombie.java:560-561. Chicken-jockey chances come from Zombie.finalizeSpawn (0.05 checks at Zombie.java:504, 513). Frostbite does not override finalizeSpawn.
- [ ] Can-pick-up-loot `nextFloat() < 0.55*difficultyMultiplier`. Zombie.java:493

### Equipment
- [ ] `populateDefaultEquipmentSlots` is fully overridden: 25% chance (`nextFloat() > 0.75`) of an ICE_BALL in the main hand. There is NO zombie iron sword/shovel roll. Frostbite.java:92-96
- [ ] Preferred-weapon tag `#minecraft:frostbite_preferred_weapons` = [`minecraft:ice_ball`]. Frostbite.java:119-121, D/tags/item/frostbite_preferred_weapons.json

### AI goals
- [ ] From Zombie.registerGoals: goal 4 ZombieAttackTurtleEggGoal(1.0, 3); goal 8 LookAtPlayerGoal(Player, 8.0); goal 8 RandomLookAroundGoal. Zombie.java:143-147
- [ ] addBehaviourGoals is fully replaced (no SpearUseGoal, MoveThroughVillage or WaterAvoidingStroll). Frostbite.java:52-65:
  - [ ] goal 1 RangedZombieRangedAttackGoal(speed 1.0, intervalMin(), intervalMax(), radius 16.0, chargeWeapon=false, stopWhenInRange=false). :56
  - [ ] goal 2 RangedZombieMeleeAttackGoal(speed 1.0, trackTarget=false, attackInterval 30, chargeWeapon=false). `MELEE_ATTACK_TICKS = 30`. :41, :58
  - [ ] goal 3 RandomStrollGoal(1.0) (not water-avoiding). :59
  - [ ] target 1 HurtByTargetGoal, alerts ZombifiedPiglin. target 2 NearestAttackable Player (mustSee=true, mustReach=false). target 3 AbstractVillager (mustSee=false). target 3 IronGolem (true). target 5 Turtle (interval 10, true, false, BABY_ON_LAND). :60-64
- [ ] Ranged interval in ticks = `(int)(20*k)`. min k: Peaceful/Normal 1.0, Easy 3.0, Hard 0.75. max k: Peaceful/Normal 2.0, Easy 3.0, Hard 1.5. That gives min/max 20/40 (Normal), 60/60 (Easy), 15/30 (Hard). Frostbite.java:144-158
- [ ] RangedAttackGoal timing: after a shot, `attackTime = floor(dist/16 * (max-min) + min)`. First shot `floor(lerp(dist/16, min, max))`. Needs line of sight on the firing tick. seeTime>=5 only matters with stopWhenInRange (false for frostbite, so it keeps walking toward the target). W/entity/ai/goal/RangedAttackGoal.java:87-107
- [ ] Ranged gating `shouldDoRangedAttack` (RangedZombie.java:79-103):
  - [ ] requires the main-hand item to be ICE_BALL (an empty-handed frostbite is melee only). :80
  - [ ] distance must be <= 16 blocks (maxRange² = 256). Frostbite.java:43, 104-106; RangedZombie.java:85-86
  - [ ] hysteresis: once ranged, it stays ranged while distSqr >= 9 (3 blocks). It enters ranged only when distSqr >= 25 (5 blocks). `MELEE_MODE_DISTANCE_SQR=9`, `RANGED_MODE_DISTANCE_SQR=25`. :21-22, :89-90
  - [ ] below the threshold: melee if the target is within melee reach. Otherwise ranged only if the path ends at the target's block but `!canReach()` (target unreachable). :92-96
- [ ] Projectile: `spawnProjectileUsingShoot(velocity 1.6, inaccuracy = rangedAttackUncertainty)`. Aim y = `target.getY(1/3) - projY + horizDist * trajectoryCorrectionFactor`. RangedZombie.java:59-73
  - [ ] trajectoryCorrectionFactor = gravity*4 = 0.03*4 = 0.12 for the ice ball. W/entity/projectile/Projectile.java:495-497, ThrowableProjectile.java:93-95. 26.3 hard-coded 0.2 for skeleton/drowned; 26.4 changed it to this factor (AbstractSkeleton.java:180, Drowned.java:308). Arrows have their own gravity, so their factor differs.
  - [ ] inaccuracy = `14 - difficultyId*4`: Peaceful 14, Easy 10, Normal 6, Hard 2. W/entity/monster/RangedAttackMob.java:9-11
  - [ ] throwing does NOT use up the held ice ball. If the hand isn't an ice ball it fires a fresh `new ItemStack(ICE_BALL)` (unreachable in practice because of the gating). RangedZombie.java:56-57
  - [ ] throw sound FROSTBITE_THROW, volume 1.0, pitch `0.4/(rand*0.4+0.8)`. RangedZombie.java:76; Frostbite.java:114-116
  - [ ] swings the main hand on the throw. Frostbite.java:168-171
- [ ] Melee on-hit: a successful `doHurtTarget` on a LivingEntity applies FREEZING for 60 ticks (3 s), amplifier 0. `FREEZING_TICKS_ON_MELEE = 60`. Frostbite.java:42, 135-142

### Powder snow, sun, conversions
- [ ] Pathfinding malus 0 for POWDER_SNOW and ON_TOP_OF_POWDER_SNOW. Frostbite.java:47-48
- [ ] `makeStuckInBlock` is ignored for powder snow (no slowdown inside it). Frostbite.java:160-165
- [ ] In tag `powder_snow_walkable_mobs` (walks on top). D/tags/entity_type/powder_snow_walkable_mobs.json
- [ ] Also in tags `zombies`, `burn_in_daylight`, `no_anger_from_wind_charge`, `does_not_break_brittle_ice` (with stray). D/tags/entity_type/*.json
- [ ] Sunlight: burns. It inherits `isSunSensitive() = true` (Zombie.java:286-288) and is in the burn_in_daylight tag. Being in powder snow (or the previous tick in it) counts as a non-burnable block. W/entity/Mob.java:530
- [ ] Frostbite does not freeze-convert: `convertsWhenFreezing() = false`. Frostbite.java:173-176
- [ ] Frostbite drowns into a ZOMBIE (not a drowned): `convertsToWhenDrowning() = ZOMBIE`. Frostbite.java:129-132. Level event 1057. Frostbite.java:123-127
  - [ ] drowning tracker: eye in water for 600 ticks ("InWaterTime") starts a 300-tick conversion ("DrownedConversionTime"). Total 900 ticks = 45 s. Zombie.java:104-117. The resulting zombie can later become a drowned.
- [ ] Zombie freezing tracker, new in 26.4. Zombie.java:119-132:
  - [ ] condition `isInPowderSnow && !isWearingFreezeImmuneWearables()` (any leather armor piece blocks it). :124
  - [ ] 140 ticks of affliction ("FreezingTime"), then 300 ticks of conversion ("FrostbiteConversionTime"). Total 440 = 22 s. :125-128. Constant `FREEZING_CONVERSION_TICKS = 300`. :91
  - [ ] Leaving the snow resets afflictionTime to -1 and cancels conversion. W/entity/ConversionTracker.java:67-70. NoAI mobs never convert. :54
  - [ ] Conversion uses `ConversionParams.single(mob, keepEquipment=true, preserveCanPickUpLoot=true)`, then `handleAttributes(specialMultiplier, CONVERSION)`. ConversionTracker.java:98; Zombie.java:129-131
  - [ ] Synced flag DATA_FROSTBITE_CONVERSION_ID (boolean) and `isFreezingConverting()`. Zombie.java:88, 184-186
  - [ ] NBT is written only when `convertsWhenFreezing()`. Zombie.java:434-436, 448-450
- [ ] Conversion targets (`convertsToWhenFreezing`) and level events:
  - [ ] Zombie -> FROSTBITE, event 1056. Zombie.java:387-394
  - [ ] Husk -> ZOMBIE, event 1041 (husk-to-zombie sound). Husk.java:88-103
  - [ ] Drowned -> ZOMBIE, event 1058 (new DROWNED_CONVERTED_TO_ZOMBIE). Drowned.java:184-188, 227-230
  - [ ] ZombieVillager: no conversion (`convertsWhenFreezing=false`). ZombieVillager.java:191-194
  - [ ] ZombifiedPiglin: no conversion. ZombifiedPiglin.java:98-101
  - [ ] Frostbite: no conversion. Frostbite.java:173-176
- [ ] Level event sounds (C/renderer/LevelEventHandler.java):
  - [ ] 1056 ZOMBIE_CONVERTED_TO_FROSTBITE: vol 2.0, pitch `rand*0.1+0.9`. :231-233
  - [ ] 1057 FROSTBITE_CONVERTED_TO_ZOMBIE: vol 2.0, pitch `rand*0.1+0.9`. :234-236
  - [ ] 1058 DROWNED_CONVERTED_TO_ZOMBIE: vol 2.0, pitch `(r-r)*0.2+1.0`. :237+
  - [ ] Constants are in W/level/block/LevelEvent.java:56-58
- [ ] Skeleton -> Stray freeze conversion now also skips when wearing leather (`&& !isWearingFreezeImmuneWearables()`). Still 140 + 300, event 1048. W/entity/monster/skeleton/Skeleton.java:19-30 (26.3 had no leather check)

### Immunities
- [ ] Immune to FREEZING: `immune_to_freezing` = [polar_bear, snow_golem, #undead], and frostbite is undead through #zombies. D/tags/entity_type/immune_to_freezing.json
- [ ] Other undead behavior is inherited (Smite, inverted healing). Not specific to this mob.

### Loot (D/loot_table/entities/frostbite.json)
- [ ] Pool 1: rotten_flesh, set_count uniform 0-2, looting enchanted_count_increase uniform 0-1 per level. :4-31
- [ ] Pool 2: ice_ball, set_count uniform 0-3, looting uniform 0-1 per level. :32-59
- [ ] random_sequence `minecraft:entities/frostbite`. :61
- [ ] No rare iron/carrot/potato pool (zombies have one).

### Spawning
- [ ] SpawnPlacements: ON_GROUND, MOTION_BLOCKING_NO_LEAVES, `Monster::checkColdVariantSpawnRules`. W/entity/SpawnPlacements.java:122
- [ ] checkColdVariantSpawnRules = checkMonsterSpawnRules, plus: if the biome is in `#spawns_cold_variants_underground` (= [ice_caves]) no sky check; else walk up through powder snow and require `canSeeSky` (or a spawner reason). W/entity/monster/Monster.java:120-139; D/tags/worldgen/biome/spawns_cold_variants_underground.json
- [ ] Biome entries (`spawns_by_category.monster`, every frostbite entry has count 4). D/worldgen/biome/:
  - [ ] ice_caves (new biome): weight 50. ice_caves.json:38
  - [ ] ice_spikes: 25. ice_spikes.json:86
  - [ ] frozen_peaks: 25. frozen_peaks.json:35
  - [ ] frozen_river: 25. frozen_river.json:82
  - [ ] frozen_ocean: 25. frozen_ocean.json:76
  - [ ] deep_frozen_ocean: 25. deep_frozen_ocean.json:76
  - [ ] snowy_plains: 10. snowy_plains.json:91
  - [ ] snowy_slopes: 10. snowy_slopes.json:44
  - [ ] jagged_peaks: 10. jagged_peaks.json:35
  - [ ] Nothing else. The full 68-biome scan returned only these 9.

### Sounds (SoundEvents.java)
- [ ] entity.frostbite.ambient FROSTBITE_AMBIENT :1352; .death :1353; .hurt :1354; .step :1355 (played at vol 0.15, pitch 1.0 by Zombie.playStepSound, Zombie.java:398-399); .throw :1356; .converted_to_zombie :1363
- [ ] entity.zombie.converted_to_frostbite :1362
- [ ] entity.parrot.imitate.frostbite :845

### Client / renderer
- [ ] FrostbiteRenderer extends AbstractZombieRenderer. Textures `textures/entity/zombie/frostbite.png` and `frostbite_baby.png`, both 64x64. C/renderer/entity/FrostbiteRenderer.java:13-14
- [ ] Model layers: FROSTBITE, FROSTBITE_BABY, FROSTBITE_ARMOR (humanoid armor), FROSTBITE_BABY_ARMOR, FROSTBITE_OUTER_LAYER ("frostbite","outer"). C/model/geom/ModelLayers.java:115-119
- [ ] The body mesh is `HumanoidModel.createBodyLayerWithCustomLeftArmsAndLegs(NONE)`, the same as the drowned: standard humanoid plus left_arm at UV (32,48) box (-1,-2,-2, 4x12x4) offset (5,2,0), and left_leg at UV (16,48) box (-2,0,-2, 4x12x4) offset (1.9,12,0). Texture 64x64. C/model/HumanoidModel.java:119-129; C/model/geom/LayerDefinitions.java:257
- [ ] Outer layer: same mesh with CubeDeformation 0.25 (the drowned outer layer is also 0.25). Texture `textures/entity/zombie/frostbite_outer_layer.png` (64x64). Rendered only when NOT baby, tint -1 (white). LayerDefinitions.java:258; C/renderer/entity/layers/FrostbiteOuterLayer.java:14, 30-32
- [ ] Baby: BabyFrostbiteModel extends BabyZombieModel and uses `BabyZombieModel.createBodyLayer` (the same layer as the baby zombie). LayerDefinitions.java:256, 351
- [ ] FrostbiteModel/BabyFrostbiteModel add no code. FrostbiteModel.java:6-10
- [ ] The textures are palette PNGs (PNG colortype 3). Per memory "Keyed PNG transparency draws black", convert them to RGBA for 1.12.

### Other mobs
- [ ] Villagers panic from a frostbite within 8.0 blocks (same as zombie/husk/drowned). W/entity/ai/sensing/VillagerHostilesSensor.java:14
- [ ] Parrot imitates with PARROT_IMITATE_FROSTBITE. W/entity/animal/parrot/Parrot.java:93. Also new: parrots no longer imitate silent mobs (`!input.isSilent()` in PARROT_CAN_IMITATE_SOUND).
- [ ] Iron golem / snow golem: no frostbite-specific change. The IronGolem and SnowGolem diffs are only sound-call refactors. The frostbite targets iron golems (target goal 3).

## 2. Freezing effect

- [ ] `MobEffects.FREEZING = new FreezingMobEffect(HARMFUL, 10415854 = 0x9EEEEE)`, `.withSoundOnAdded(APPLY_EFFECT_FREEZING)` ("event.mob_effect.freezing", SoundEvents.java:1364). W/effect/MobEffects.java:133-135
- [ ] Effect particle `ParticleTypes.FREEZING`. FreezingMobEffect.java:12
- [ ] applyEffectTick: `mob.freezeForTicks(1)` every tick (`shouldApplyEffectTickThisTick` always true). Constants: TICKS_ADDED=1, DAMAGE_VALUE=1.5. FreezingMobEffect.java:8-24
- [ ] `freezeForTicks(n)`: if canFreeze, `ticksFrozen = min(140, ticksFrozen+n)`. W/entity/Entity.java:2928-2932. Powder snow now uses the same call (+1/tick). InsideBlockEffectType FREEZE.
- [ ] Constants: BASE_TICKS_REQUIRED_TO_FREEZE=140, FREEZE_HURT_FREQUENCY=40. Entity.java:206-207. `getTicksRequiredToFreeze()=140`. :2943-2945. `isFullyFrozen = ticksFrozen >= 140`. :2939-2941
- [ ] `Entity.canFreeze = !is(#immune_to_freezing) && !isOnFire()`. Being on fire, including from lava, blocks freezing. Entity.java:4055-4057 (26.3: tag only)
- [ ] `LivingEntity.canFreeze`: false for spectators and when wearing any `#freeze_immune_wearables` item (leather boots/leggings/chestplate/helmet, leather_horse_armor) in an ARMOR slot. LivingEntity.java:4006-4021
- [ ] Server tick (LivingEntity.java:3163-3174):
  - [ ] Thaw by -2/tick when `!canFreeze || (!isInPowderSnow && !hasEffect(FREEZING))`. :3164-3165. So with the effect active outside snow there is no thaw, only +1/tick. In snow with the effect it gains +2/tick.
  - [ ] Every `tickCount % 40 == 0` while fully frozen and canFreeze: freeze damage 1.5 with the effect, else 1.0. :3169-3171 (26.3: always 1.0)
  - [ ] Slowness: transient MOVEMENT_SPEED modifier "powder_snow" = `-0.05 * percentFrozen` ADD_VALUE, removed at 0 ticks. :530-540
- [ ] Extra damage: `#is_freezing` damage (= [freeze]) on `#freeze_hurts_extra_types` (strider, blaze, magma_cube) is x5.0. LivingEntity.java:1226-1228
- [ ] canBeAffected: FREEZING is rejected outright for `#immune_to_freezing`. LivingEntity.java:1040
- [ ] Tag change: `freeze_immune_entity_types` (26.3: stray, polar_bear, snow_golem, wither) was REMOVED and replaced by `immune_to_freezing` = [polar_bear, snow_golem, #undead]. So in 26.4 every undead (zombies, skeletons, phantom, wither...) stops taking powder-snow freeze damage.
- [ ] Skeleton.canFreeze still hard false (unchanged). Skeleton.java:71-73
- [ ] Damage type freeze: scaling when_caused_by_living_non_player, exhaustion 0, effects "freezing". D/damage_type/freeze.json
- [ ] Fire / extinguish: going from freezing to not freezing plays the entity extinguish sound (same as fire). Entity.java:992-993. Lava has no other special case beyond setting the entity on fire. The effect does not put fire out.
- [ ] Client: "shake" is the entity model shaking, `isShaking = state.isFullyFrozen`. C/renderer/entity/LivingEntityRenderer.java:150-152, 275. There is no camera shake. HUD powder-snow outline alpha = percentFrozen (C/gui/Hud.java:306). Frozen hearts when fully frozen (Hud.java:1448).
- [ ] FreezingParticle (C/particle/FreezingParticle.java):
  - [ ] initial velocity x/z `(r*2-1)*0.01`, y `(r*2-1)*0.01*0.25`. :23-25
  - [ ] gravity 0.125, friction 1.0, quadSize *= 0.81. :27-29
  - [ ] lifetime `max((int)(32/(r*0.8+0.2))*0.9, 10)`. :30-31
  - [ ] rotSpeed `(r-0.5)*0.1`, initial roll rotSpeed*5. :32-33
  - [ ] tick: `roll += PI*rotSpeed*2*exp(age/-8)`. On first ground contact, lifetime = age+20. `yd -= 0.003`, clamped to >= -0.14. :46-58
  - [ ] Layer OPAQUE. :38
  - [ ] Sprite chosen at random from 6 frames freezing_0..5, not animated. :80; assets/minecraft/particles/freezing.json

## 3. Ice Ball

- [ ] Item: stacksTo 16, dispensed DEFAULT_PROJECTILE (it is a ProjectileItem: dispenser fires `new IceBall` at the position). There is NO use cooldown. W/item/Items.java:1935-1937; IceBallItem.java:16, 46-48
- [ ] Player throw: sound ICE_BALL_THROW, SoundSource PLAYERS (Pre-Release 2; NEUTRAL in Pre-Release 1), vol 0.5, pitch `0.4/(r*0.4+0.8)`. `spawnProjectileFromRotation(..., roll 0, velocity 1.5, inaccuracy 1.0)`. Consumes 1, awards ITEM_USED. IceBallItem.java:17 (PROJECTILE_SHOOT_POWER 1.5), 26-42
- [ ] Entity ICE_BALL: MISC, noLootTable, 0.25x0.25, clientTrackingRange 4, updateInterval 10. EntityTypes.java:572-575. Rendered as ThrownItemRenderer (C/renderer/entity/EntityRenderers.java:116). Gravity 0.03 (ThrowableProjectile default).
- [ ] Damage: `ceil(clamp(|velocity| * 1.3, 0, 2^31))` from the projectile's current speed at impact. BASE_DAMAGE 1.3. IceBall.java:17, 36-40. Fresh throws give ceil(1.5*1.3)=2 for a player and ceil(1.6*1.3)=3 for a frostbite. Drag and gravity change the speed at impact. Damage source `thrown(this, owner)`.
- [ ] On entity hit: `hurtServer`. If damage > 0 and the target is living, `EnchantmentHelper.doPostAttackEffectsWithItemSource` with the ball's item. ThrowableBallProjectile.java:58-69 (file lines). It does NOT apply Freezing. Nothing in IceBall freezes. The Frostbite's freeze is melee only.
- [ ] Knockback: standard hurt knockback, no custom value.
- [ ] On any hit (block or entity), server side: broadcast entity event 3, play the hit sound ICE_BALL_BREAK at the hit point unless the ball is silent (Pre-Release 2), NEUTRAL, vol 0.5, pitch `0.4/(r*0.4+0.8)`, then discard. ThrowableBallProjectile.java:72-93. No block interaction (no snow layers or freezing water).
- [ ] Event 3 client: 8 `ItemParticleOption(ITEM, ice_ball stack)` particles at the position with zero velocity. ThrowableBallProjectile.java:47-55
- [ ] It is in `#impact_projectiles`. D/tags/entity_type/impact_projectiles.json
- [ ] Snowball now extends ThrowableBallProjectile. Damage 3 to Blaze, else 0. Snowball.java (diff 13770-13850). Changes:
  - [ ] the particle is always ItemParticleOption of its stack (the ITEM_SNOWBALL fallback is gone)
  - [ ] it uses hurtServer
  - [ ] it runs post-attack enchant effects when damage > 0
  - [ ] hit sound null, so it stays silent
- [ ] New item tag `knocks_back_players_even_with_zero_damage` = [snowball]:
  - [ ] Player.hurtServer no longer returns false on 0 damage if the weapon item is in the tag, so knockback and the hurt event happen. W/entity/player/Player.java:705-710
  - [ ] Shield blocking of a 0-damage tagged weapon resolves as if 1.0 damage, then 0. LivingEntity.java:1354-1357

## 4. Potions / brewing / arrows

- [ ] Potions.FREEZING = FREEZING 900 ticks (45 s). LONG_FREEZING = 1800 (1:30). No strong variant. W/item/alchemy/Potions.java:114-117
- [ ] Both are in potion tag `tradeable`. D/tags/potion/tradeable.json
- [ ] Duration scale: tipped arrow 0.125, so 112 ticks and 225 ticks. Lingering 0.25, so 225 and 450. Items.java:2462, 2471
- [ ] Lang: "Arrow of Freezing", "Potion of Freezing", "Splash Potion of Freezing", "Lingering Potion of Freezing", effect "Freezing".
- [ ] 13 brewing recipes (D/recipe/brewing/):
  - [ ] potion awkward + ice_ball -> potion freezing (potion_awkward_ice_ball)
  - [ ] splash awkward + ice_ball -> splash freezing (splash_potion_awkward_ice_ball)
  - [ ] lingering awkward + ice_ball -> lingering freezing (lingering_potion_awkward_ice_ball)
  - [ ] potion water + ice_ball -> potion mundane (potion_water_ice_ball)
  - [ ] splash water + ice_ball -> splash mundane (splash_potion_water_ice_ball)
  - [ ] lingering water + ice_ball -> lingering mundane (lingering_potion_water_ice_ball)
  - [ ] potion freezing + redstone -> potion long_freezing
  - [ ] splash freezing + redstone -> splash long_freezing
  - [ ] lingering freezing + redstone -> lingering long_freezing
  - [ ] potion freezing + gunpowder -> splash freezing
  - [ ] potion long_freezing + gunpowder -> splash long_freezing
  - [ ] splash freezing + dragon_breath -> lingering freezing
  - [ ] splash long_freezing + dragon_breath -> lingering long_freezing
  - [ ] Source: VanillaBrewingProvider.java:35-36 (`buildStartMix(ICE_BALL, FREEZING)`, `buildMix(FREEZING, REDSTONE, LONG_FREEZING)`)
- [ ] There is no fermented-spider-eye inversion.
- [ ] Tipped arrow crafting is the generic special recipe (nothing new).

## 5. Advancements

- [ ] NEW adventure/what_goes_around_comes_around, parent adventure/kill_a_mob. D/advancement/adventure/what_goes_around_comes_around.json:2
  - [ ] criterion `kill_frostbite_with_ice_ball`, trigger player_killed_entity, entity type frostbite. :4-12
  - [ ] killing_blow direct_entity type ice_ball, damage tag is_projectile true. :13-23
  - [ ] icon ice_ball, frame task (none given), no rewards, sends telemetry. :28-44
  - [ ] Title "What Goes Around Comes Around". Description "Kill a Frostbite using an Ice Ball".
- [ ] kill_a_mob (Monster Hunter): frostbite criterion added and in the single OR requirement. kill_a_mob.json:160-171, 535
- [ ] kill_all_mobs (Monsters Hunted): frostbite criterion plus its own AND requirement. kill_all_mobs.json:160-171, 560-562
- [ ] nether/all_effects (How Did We Get Here): `minecraft:freezing` added. all_effects.json:15
- [ ] nether/all_potions (A Furious Cocktail): `minecraft:freezing` added. all_potions.json:8
- [ ] adventure/adventuring_time: biome ice_caves added (criterion and requirement).
- [ ] adventure/root: icon changed from map to buried_treasure_map.
- [ ] Title renames (lang): `advancements.husbandry.uh_oh.title` "Uh Oh" -> "Uh-Oh". `advancements.nether.distract_piglin.title` "Oh Shiny" -> "Oooh, Shiny!". No other advancement key changed.
- [ ] (FYI) About 170 subtitle strings were reworded to "X is hurt", "...is equipped" etc. Not in scope.

## 6. Mob interactions
- [ ] Parrot, VillagerHostilesSensor: see section 1, "Other mobs".
- [ ] Snow golem: lang adds `subtitles.entity.snow_golem.shoot` "Snow Golem throws". There is no code change in SnowGolem.java.
- [ ] No mob flees frostbites specifically, apart from villagers (8 blocks).

## 7. Stray changes
- [ ] `Stray.checkStraySpawnRules` was removed. STRAY now uses `Monster::checkColdVariantSpawnRules` (same powder-snow sky logic, plus no sky check in ice_caves). SpawnPlacements.java:156
- [ ] Stray is now freeze-immune through #undead (it was listed explicitly in the old tag).
- [ ] Stray is in `does_not_break_brittle_ice`.
- [ ] Biomes that spawn strays NEWLY in 26.4 (count 4 each). The skeleton entries are unchanged, so skeleton weight 100 stays alongside:
  - [ ] frozen_peaks 95 (frozen_peaks.json:40)
  - [ ] jagged_peaks 95 (:40)
  - [ ] snowy_slopes 95 (:49)
  - [ ] frozen_river 95 (:87)
  - [ ] frozen_ocean 95 (:81)
  - [ ] deep_frozen_ocean 95 (:81)
  - [ ] ice_caves 10 (new biome, skeleton 90) (:33)
- [ ] Unchanged: snowy_plains stray 80 (skeleton 20), ice_spikes stray 80 (skeleton 20).
- [ ] Trial chamber spawner NBTs `ranged/stray.nbt` and `slow_ranged/stray.nbt` changed (binary). Contents UNKNOWN: not decoded.

## 8. New sound events in 26.4

The set difference of string ids, SoundEvents.java plus the new data-driven `block_sound_set` JSONs, against 26.3 SoundEvents.java:
- [ ] Frostbite: entity.frostbite.ambient/death/hurt/step/throw (:1352-1356), entity.frostbite.converted_to_zombie (:1363), entity.zombie.converted_to_frostbite (:1362), entity.parrot.imitate.frostbite (:845)
- [ ] Ice ball: entity.ice_ball.break (:1359), entity.ice_ball.throw (:1360)
- [ ] Effect: event.mob_effect.freezing APPLY_EFFECT_FREEZING (:1364)
- [ ] Conversion: entity.drowned.converted_to_zombie (:397)
- [ ] Ice blocks (SoundEvents): block.ice.ambient ICE_BLOCK_AMBIENT (:1351), block.ice_crystal.ambient (:1357), block.icicle.land (:1358), block.brittle_ice.crack (:1361)
- [ ] Block sound sets, new ids in D/block_sound_set/*.json (break/fall/hit/place/step each):
  - [ ] block.blue_ice.*
  - [ ] block.brittle_ice.*
  - [ ] block.frosted_ice.*
  - [ ] block.ice.*
  - [ ] block.ice_crystal.*
  - [ ] block.icicle.*
  - [ ] block.packed_ice.*
  - [ ] block.chiseled_soulshale.*
- [ ] Misc: music_disc.flurry (:712, Holder; igloo chest 30% roll), entity.zombie_nautilus.riding (:1288)
- [ ] Refactor note: 26.4 moved most block sound fields out of SoundEvents into `block_sound_set` data (1898 to 1322 ids in the Java file). The only id that vanished entirely is `entity.chicken.step`. It may have moved somewhere else; I did not check.

## Wiki (minecraft.wiki Frostbite, Freezing) vs code
- [ ] AGREES: 24 HP, armor 2, speed 0.21 / baby 0.315, 25% ice ball, 3 s melee freeze, 16-block range, the spawn weights and group 4, the 22 s zombie conversion with the leather rule, the drop ranges, the 45 s / 1:30 / 0:11 / 0:05 durations, 1.5 dmg every 40 ticks, x5 for blaze/magma/strider, fire nullifies.
- [ ] DISAGREES: ranged interval. The wiki says Easy 3.5 s, Normal 1.5 s, Hard 1.0 s. The code gives Easy fixed 3.0 s, Normal 1.0-2.0 s, Hard 0.75-1.5 s, scaled by distance/16. The wiki numbers look like averages, but the Easy one is wrong.
- [ ] DISAGREES: the wiki says it "switches to melee within 2 blocks". The code switches below 3 blocks while ranged (5 blocks to re-enter ranged), and only when the target is in melee reach or reachable.
- [ ] DISAGREES / ambiguous: the wiki says frostbite becomes a zombie after "30 seconds" in water. The code needs 600 ticks of affliction plus 300 of conversion = 45 s total (the 30 s is only the first stage).
- [ ] DISAGREES: the wiki gives the "conversion to zombie" sound as vol 1.6, pitch 0.8-1.2. The code (event 1057) uses vol 2.0, pitch 0.9-1.0.
- [ ] Wiki says the Freezing slowdown reaches "half speed". The code is -0.05 ADD_VALUE times percent frozen: half speed only for a 0.1-speed player, less for mobs.
- [ ] Wiki lists the ender dragon as immune. It is not in the tag; the dragon rejects effects through its own addEffect override (EnderDragon.java:846), so the outcome matches.
- [ ] UNKNOWN: the wiki history about snapshot 4 vs 8 immunity lists. Only pre-1 was available here.
