package com.exiledradio.gamedropsbackport.sulfurcaves;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Inside the sulfur caves, the biome's spawns replace the column's (26.3: sulfur cubes, creepers, skeletons, slimes,
 * cave spiders instead of spiders, zombies, endermen, witches, zombie villagers; bats as ambient), and each player is
 * told whether they are inside (music and fog, {@link SulfurNet}).
 */
public final class SulfurSpawns {

    private final Map<EntityPlayerMP, Boolean> told = new WeakHashMap<EntityPlayerMP, Boolean>();

    @SubscribeEvent(priority = EventPriority.LOW)
    public void spawns(WorldEvent.PotentialSpawns event) {
        World world = event.getWorld();
        EnumCreatureType type = event.getType();
        if ((type != EnumCreatureType.MONSTER && type != EnumCreatureType.AMBIENT) || SulfurCavesContent.biome == null
                || !world.isBlockLoaded(event.getPos()) || !SulfurCaves.at(world, event.getPos())) {
            return;
        }
        List<Biome.SpawnListEntry> list = event.getList();
        list.clear();
        list.addAll(SulfurCavesContent.biome.getSpawnableList(type));
    }

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || event.player.ticksExisted % 20 != 0) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        boolean inside = SulfurCaves.at(player.world, player.getPosition());
        Boolean was = told.get(player);
        if (was == null || was != inside) {
            told.put(player, inside);
            SulfurNet.tell(player, inside);
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        told.remove(event.player);
    }
}
