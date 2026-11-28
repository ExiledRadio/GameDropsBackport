package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.dragon.phase.PhaseSittingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 26.4's changes to existing things done with events (the patched ones are {@link DeepFreezeHooks}):
 * <ul>
 *   <li>Ice, packed ice, frosted ice and any mod's blue ice get 26.4's own sound sets ({@code block.ice.*},
 *       {@code block.packed_ice.*}, ...; 1.12.2's are glass).</li>
 *   <li>Snowballs knock players back ({@code #knocks_back_players_even_with_zero_damage}): a 0-damage hit still pushes,
 *       unless a shield blocks it.</li>
 *   <li>A carrot on a stick boosts a pig for 140-700 ticks ({@code ItemBasedSteering}; 1.12.2's 140-980). A finished
 *       boost resets the synced total to 0, and a synced 0 is no boost (26.4 Pre-Release 2), so a client that starts
 *       tracking the pig later doesn't replay the last boost.</li>
 *   <li>A perching ender dragon takes no thrown damage ({@code #cannot_hurt_perching_ender_dragon} gained
 *       {@code thrown}); the projectile is set on fire for a second, as arrows are.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class DeepFreezeChanges {

    public static final int BOOST_MIN = 140;
    public static final int BOOST_MAX = 700;

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundType ICE = type("ice"), FROSTED_ICE = type("frosted_ice"), BLUE_ICE = type("blue_ice");
    /** Packed ice's set is also the ice bricks' (stream iceblocks registers it when on); made in post-init. */
    public static final SoundEvent ICE_AMBIENT = sound("block.ice.ambient");

    private static final Field TOTAL_BOOST = ReflectionHelper.findField(EntityPig.class, "totalBoostTime", "field_184767_bA");
    private static final Field BOOSTING = ReflectionHelper.findField(EntityPig.class, "boosting", "field_184765_bx");
    private static final Field BOOST_TIME = ReflectionHelper.findField(EntityPig.class, "boostTime", "field_184766_bz");
    @SuppressWarnings("unchecked")
    private static final DataParameter<Integer> BOOST_PARAM = (DataParameter<Integer>) ReflectionHelper.getPrivateValue(EntityPig.class, null,
            "BOOST_TIME", "field_191520_bx");

    private DeepFreezeChanges() {
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, name);
        SoundEvent e = new SoundEvent(id).setRegistryName(id);
        SOUNDS.add(e);
        return e;
    }

    private static SoundType type(String block) {
        return new SoundType(1.0F, 1.0F, sound("block." + block + ".break"), sound("block." + block + ".step"),
                sound("block." + block + ".place"), sound("block." + block + ".hit"), sound("block." + block + ".fall"));
    }

    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new DeepFreezeHooks.Population());
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    /** After every mod's blocks exist: the ice blocks' sound sets. */
    public static void postInit() {
        if (!Changes.on(ModConfig.changes.iceSounds)) {
            return;
        }
        setSound(Blocks.ICE, ICE);
        setSound(Blocks.PACKED_ICE, packedIce());
        setSound(Blocks.FROSTED_ICE, FROSTED_ICE);
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation id = block.getRegistryName();
            if (id != null && "blue_ice".equals(id.getPath())) {
                setSound(block, BLUE_ICE);
            }
        }
    }

    /** {@code block.packed_ice.*}: the ice blocks stream's events, or ours when that stream is off. */
    static SoundType packedIce() {
        SoundEvent[] e = new SoundEvent[5];
        String[] kinds = {"break", "step", "place", "hit", "fall"};
        for (int i = 0; i < 5; i++) {
            e[i] = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "block.packed_ice." + kinds[i]));
        }
        return new SoundType(1.0F, 1.0F, e[0], e[1], e[2], e[3], e[4]);
    }

    static void setSound(Block block, SoundType type) {
        ReflectionHelper.setPrivateValue(Block.class, block, type, "blockSoundType", "field_149762_H");
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        for (SoundEvent s : SOUNDS) {
            event.getRegistry().register(s);
        }
        if (!ModConfig.iceBlocks.enabled) {
            for (String kind : new String[] {"break", "step", "place", "hit", "fall"}) {
                ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, "block.packed_ice." + kind);
                event.getRegistry().register(new SoundEvent(id).setRegistryName(id));
            }
        }
    }

    public static final class Events {

        @SubscribeEvent
        public void attack(LivingAttackEvent event) {
            Entity direct = event.getSource().getImmediateSource();
            if (event.getEntityLiving() instanceof EntityPlayer && direct instanceof EntitySnowball && event.getAmount() <= 0.0F
                    && Changes.on(ModConfig.changes.snowballKnockback)) {
                snowballKnockback((EntityPlayer) event.getEntityLiving(), (EntitySnowball) direct, event.getSource());
            } else if (event.getEntityLiving() instanceof EntityDragon && "thrown".equals(event.getSource().getDamageType())
                    && Changes.on(ModConfig.changes.dragonPerchThrown)
                    && ((EntityDragon) event.getEntityLiving()).getPhaseManager().getCurrentPhase() instanceof PhaseSittingBase) {
                if (direct != null) {
                    direct.setFire(1);
                }
                event.setCanceled(true);
            }
        }

        /** {@code Player.hurtServer} with a tagged weapon at 0 damage: the hurt and the push, not the health. */
        static void snowballKnockback(EntityPlayer player, EntitySnowball ball, net.minecraft.util.DamageSource source) {
            if (player.world.isRemote || player.isEntityInvulnerable(source) || player.capabilities.disableDamage) {
                return;
            }
            push(player, ball);
        }

        /** The push itself: the hurt time, 0.4 along the ball's flight, unless a shield is up or the player was just hurt. */
        public static void push(EntityPlayer player, EntitySnowball ball) {
            if (player.hurtResistantTime > player.maxHurtResistantTime / 2.0F || player.isActiveItemStackBlocking()) {
                return;
            }
            player.hurtResistantTime = player.maxHurtResistantTime;
            player.knockBack(ball, 0.4F, -ball.motionX, -ball.motionZ);
            player.velocityChanged = true;
            player.world.setEntityState(player, (byte) 2);
        }

        @SubscribeEvent
        public void living(LivingEvent.LivingUpdateEvent event) {
            if (!(event.getEntityLiving() instanceof EntityPig) || !Changes.on(ModConfig.changes.steeringBoost)) {
                return;
            }
            EntityPig pig = (EntityPig) event.getEntityLiving();
            try {
                if (pig.world.isRemote) {
                    // onSynced: a synced total of 0 is no boost (1.12.2 boosts for a tick).
                    if (BOOSTING.getBoolean(pig) && TOTAL_BOOST.getInt(pig) == 0) {
                        BOOSTING.setBoolean(pig, false);
                    }
                    return;
                }
                if (!BOOSTING.getBoolean(pig) && TOTAL_BOOST.getInt(pig) > 0) {
                    // tickBoost: a finished boost sets the total to 0.
                    TOTAL_BOOST.setInt(pig, 0);
                    pig.getDataManager().set(BOOST_PARAM, 0);
                } else if (BOOSTING.getBoolean(pig) && BOOST_TIME.getInt(pig) == 0 && TOTAL_BOOST.getInt(pig) > BOOST_MAX) {
                    // A fresh 1.12.2 boost past 700 is drawn again in 140-700, which leaves 140-700 uniform.
                    int total = BOOST_MIN + pig.getRNG().nextInt(BOOST_MAX - BOOST_MIN + 1);
                    TOTAL_BOOST.setInt(pig, total);
                    pig.getDataManager().set(BOOST_PARAM, total);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
