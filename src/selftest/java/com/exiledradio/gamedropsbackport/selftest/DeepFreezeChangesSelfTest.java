package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.asm.DeepFreezeTransformer;
import com.exiledradio.gamedropsbackport.changes.DeepFreezeChanges;
import com.exiledradio.gamedropsbackport.changes.DeepFreezeHooks;

import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.dragon.phase.PhaseList;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

/**
 * 26.4's changes to existing things (stream 7): the patches P8-P11 applied, snow on packed ice, mushrooms in light,
 * the ice sound sets, snowball knockback on players, the 140-700 tick boost and the perching dragon's thrown immunity.
 */
final class DeepFreezeChangesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "changes264";
    }

    @Override
    public void run(Check check) throws Exception {
        check.ok(DeepFreezeTransformer.appliedP8, "P8 applied (snow on packed ice)");
        check.ok(DeepFreezeTransformer.appliedP9, "P9 applied (mushrooms)");
        check.ok(DeepFreezeTransformer.appliedP11, "P11 applied (frosted ice cracks)");
        WorldServer w = check.world;
        BlockPos at = w.getSpawnPoint().add(-200, 0, -200);
        at = new BlockPos(at.getX(), 210, at.getZ());
        w.getChunk(at);
        for (BlockPos p : BlockPos.getAllInBox(at.add(-2, -1, -2), at.add(2, 3, 2))) {
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
        w.setBlockState(at.down(), Blocks.PACKED_ICE.getDefaultState(), 2);
        check.ok(Blocks.SNOW_LAYER.canPlaceBlockAt(w, at), "snow layers sit on packed ice (26.4)");
        w.setBlockState(at.down(), Blocks.ICE.getDefaultState(), 2);
        check.ok(!Blocks.SNOW_LAYER.canPlaceBlockAt(w, at), "snow layers still refuse ice");

        w.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 2);
        w.setBlockState(at.east(), Blocks.GLOWSTONE.getDefaultState(), 3);
        w.checkLight(at);
        check.ok(w.getLight(at) >= 13, "lit spot for the mushroom (" + w.getLight(at) + ")");
        check.ok(Blocks.BROWN_MUSHROOM.canBlockStay(w, at, Blocks.BROWN_MUSHROOM.getDefaultState()), "mushrooms stay in full light (26.4)");
        check.ok(Blocks.BROWN_MUSHROOM.canPlaceBlockAt(w, at), "mushrooms can be placed in full light");
        check.ok(!DeepFreezeHooks.mushroomSpreads((net.minecraft.block.BlockMushroom) Blocks.BROWN_MUSHROOM, w, at, Blocks.BROWN_MUSHROOM.getDefaultState()),
                "mushrooms still spread only in light under 13");
        w.setBlockState(at.down(), Blocks.GLASS.getDefaultState(), 2);
        check.ok(Blocks.BROWN_MUSHROOM.canBlockStay(w, at, Blocks.BROWN_MUSHROOM.getDefaultState()), "mushrooms stay on glass (center support)");
        w.setBlockState(at.down(), Blocks.AIR.getDefaultState(), 2);
        check.ok(!Blocks.BROWN_MUSHROOM.canBlockStay(w, at, Blocks.BROWN_MUSHROOM.getDefaultState()), "not on air");
        w.setBlockToAir(at.east());

        check.ok(Blocks.ICE.getSoundType() == DeepFreezeChanges.ICE, "ice has 26.4's own sounds");
        check.ok(Blocks.FROSTED_ICE.getSoundType() == DeepFreezeChanges.FROSTED_ICE, "frosted ice has its own sounds");
        check.ok(Blocks.PACKED_ICE.getSoundType().getBreakSound().getRegistryName().getPath().equals("block.packed_ice.break"),
                "packed ice has its own sounds");

        // Snowballs push players.
        w.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 2);
        FakePlayer player = FakePlayerFactory.getMinecraft(w);
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.motionX = player.motionY = player.motionZ = 0.0D;
        player.hurtResistantTime = 0;
        player.capabilities.disableDamage = false;
        EntitySnowball ball = new EntitySnowball(w, at.getX() + 0.5, at.getY() + 1, at.getZ() - 2.0);
        ball.motionZ = 1.5D;
        // A fake player is invulnerable, so the push is asked for directly.
        DeepFreezeChanges.Events.push(player, ball);
        check.ok(player.motionZ > 0.0D, "a snowball pushes a player along its flight (" + player.motionZ + ")");
        check.eq(player.getMaxHealth(), player.getHealth(), "and does no damage");

        // Pig boosts.
        int over = 0;
        for (int i = 0; i < 300; i++) {
            EntityPig pig = new EntityPig(w);
            pig.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            pig.boost();
            pig.onUpdate();
            int total = pig.getDataManager().get(boostParam());
            if (total > DeepFreezeChanges.BOOST_MAX || total < DeepFreezeChanges.BOOST_MIN) {
                over++;
            }
        }
        check.eq(0, over, "every boost lasts 140-700 ticks (26.4)");
        EntityPig done = new EntityPig(w);
        done.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        done.boost();
        done.onUpdate();
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(EntityPig.class, done, false, "boosting", "field_184765_bx");
        done.onUpdate();
        check.eq(0, done.getDataManager().get(boostParam()), "a finished boost syncs a total of 0 (26.4 Pre-Release 2)");

        // The perching dragon.
        EntityDragon dragon = new EntityDragon(w);
        dragon.setPosition(at.getX() + 0.5, at.getY() + 20, at.getZ() + 0.5);
        dragon.getPhaseManager().setPhase(PhaseList.SITTING_SCANNING);
        float hp = dragon.getHealth();
        EntitySnowball thrown = new EntitySnowball(w, at.getX(), at.getY() + 22, at.getZ());
        dragon.attackEntityFromPart(dragon.dragonPartHead, DamageSource.causeThrownDamage(thrown, player), 6.0F);
        check.eq(hp, dragon.getHealth(), "a perching dragon takes no thrown damage (26.4)");
        dragon.getPhaseManager().setPhase(PhaseList.HOLDING_PATTERN);
        dragon.hurtResistantTime = 0;
        dragon.attackEntityFromPart(dragon.dragonPartHead, DamageSource.causeThrownDamage(thrown, player), 6.0F);
        check.ok(dragon.getHealth() < hp, "a flying dragon still does");
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.network.datasync.DataParameter<Integer> boostParam() {
        return (net.minecraft.network.datasync.DataParameter<Integer>) net.minecraftforge.fml.relauncher.ReflectionHelper
                .getPrivateValue(EntityPig.class, null, "BOOST_TIME", "field_191520_bx");
    }
}
