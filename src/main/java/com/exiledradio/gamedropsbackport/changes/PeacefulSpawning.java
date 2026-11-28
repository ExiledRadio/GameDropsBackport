package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Ocelots on Peaceful (26.2, MC-1788). 26.x's natural spawner runs the monster category on every difficulty (only the
 * {@code spawn_monsters} rule stops it) and refuses each monster on Peaceful by itself ({@code isAllowedInPeaceful},
 * {@code checkMonsterSpawnRules}), so the jungles' ocelots, listed with the monsters, still spawn. 1.12.2 skips the
 * whole category on Peaceful ({@code spawnHostileMobs} false). Here, on Peaceful, 1.12.2's own spawner runs that
 * category once more after each world tick, with the same caps, chunks, packs and rules, and Forge's spawn check refuses
 * every {@link IMob} it picks but those 26.2 allows on Peaceful ({@link #ALLOWED_MONSTERS}): what is left is what 26.2
 * lets through (vanilla's ocelots, Nether Update's piglins, and any other mod's non-monster the monster lists hold). The dedicated server's {@code spawn-monsters=false} and {@code doMobSpawning} still stop it,
 * as they stop the category on other difficulties.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class PeacefulSpawning {

    private static final WorldEntitySpawner SPAWNER = new WorldEntitySpawner();
    /** True while our pass runs (server thread): its monsters are refused. */
    public static boolean inPass;
    /** For the self-test: entities our pass let spawn, and monsters it refused. */
    public static int spawned;
    public static int refused;

    /**
     * The family's {@link IMob} mobs whose 26.2 entity type is allowed on Peaceful (not built {@code notInPeaceful()}):
     * Nether Update's piglin (26.2 snapshot 4; its brute stays off), named so Nether Update stays optional, and 1.12.2's
     * shulker (no biome lists it; its spawn egg works on Peaceful). 26.2's other allowed monster-category types need no
     * name: the sulfur cube, the zombie horse and Tricky Trials' zombie nautilus are no {@link IMob}, Tricky Trials has
     * no camel husk, and the dragon has no egg and no spawn list.
     */
    public static final Set<ResourceLocation> ALLOWED_MONSTERS = Collections.unmodifiableSet(new HashSet<ResourceLocation>(
            Arrays.asList(new ResourceLocation("netherupdatebackport", "piglin"), new ResourceLocation("shulker"))));

    private PeacefulSpawning() {
    }

    /** 26.2's {@code !EntityType.isAllowedInPeaceful()} for 1.12.2: an {@link IMob}, unless one of {@link #ALLOWED_MONSTERS}. */
    public static boolean refusedOnPeaceful(Class<? extends Entity> type, ResourceLocation id) {
        return type != null && IMob.class.isAssignableFrom(type) && (id == null || !ALLOWED_MONSTERS.contains(id));
    }

    public static boolean refusedOnPeaceful(Entity entity) {
        return refusedOnPeaceful(entity.getClass(), EntityList.getKey(entity));
    }

    public static boolean active(WorldServer world) {
        return Changes.on(ModConfig.changes.ocelotsPeaceful) && world.getDifficulty() == EnumDifficulty.PEACEFUL
                && world.getGameRules().getBoolean("doMobSpawning")
                && world.getWorldInfo().getTerrainType() != WorldType.DEBUG_ALL_BLOCK_STATES
                && world.getMinecraftServer() != null && world.getMinecraftServer().allowSpawnMonsters();
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        if (!active(world)) {
            return;
        }
        runPass(world);
    }

    /** One monster-category pass of 1.12.2's spawner, monsters refused. @return how many spawned */
    public static int runPass(WorldServer world) {
        inPass = true;
        try {
            int n = SPAWNER.findChunksForSpawning(world, true, false, false);
            spawned += n;
            return n;
        } finally {
            inPass = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (inPass && !event.isSpawner() && refusedOnPeaceful(event.getEntityLiving())) {
            event.setResult(Event.Result.DENY);
            refused++;
        }
    }

    /** Lower-priority listeners (another mod's spawn rules) must not let a refused monster back in. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCheckSpawnLast(LivingSpawnEvent.CheckSpawn event) {
        if (inPass && !event.isSpawner() && refusedOnPeaceful(event.getEntityLiving())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
