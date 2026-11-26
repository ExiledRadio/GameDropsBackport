package com.exiledradio.gamedropsbackport.icecaves;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.4's ice caves as a 1.12.2 biome ({@code worldgen/biome/ice_caves.json}): temperature 0, downfall 0, snow, water
 * and its fog {@code #14559b}, sky {@code #7fa1ff}, the sulfur caves' music. Never in a 1.12.2 biome map; a superflat
 * preset may use it, and its spawn lists are the ones the ice caves use ({@link IceCavesEvents}).
 */
public class BiomeIceCaves extends Biome {

    public static final int WATER = 0x14559B, FOG = 0x14559B, SKY = 0x7FA1FF;

    public BiomeIceCaves() {
        super(new BiomeProperties("Ice Caves").setTemperature(0.0F).setRainfall(0.0F).setSnowEnabled().setWaterColor(WATER));
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        this.spawnableCaveCreatureList.add(new SpawnListEntry(EntityBat.class, 10, 8, 8));
        this.spawnableMonsterList.clear();
        this.spawnableMonsterList.addAll(monsters());
    }

    /** 26.4's monster list in order; the frostbite once stream 2 registers it, Caves &amp; Cliffs' glow squid is water's. */
    @SuppressWarnings("unchecked")
    static List<SpawnListEntry> monsters() {
        List<SpawnListEntry> list = new ArrayList<SpawnListEntry>();
        list.add(new SpawnListEntry(EntityCreeper.class, 100, 4, 4));
        list.add(new SpawnListEntry(EntitySkeleton.class, 90, 4, 4));
        list.add(new SpawnListEntry(EntityStray.class, 10, 4, 4));
        EntityEntry frostbite = ForgeRegistries.ENTITIES.getValue(new ResourceLocation("gamedropsbackport", "frostbite"));
        if (frostbite != null && EntityLiving.class.isAssignableFrom(frostbite.getEntityClass())) {
            list.add(new SpawnListEntry((Class<? extends EntityLiving>) frostbite.getEntityClass(), 50, 4, 4));
        }
        list.add(new SpawnListEntry(EntitySlime.class, 100, 4, 4));
        list.add(new SpawnListEntry(EntitySpider.class, 100, 4, 4));
        list.add(new SpawnListEntry(EntityZombie.class, 50, 4, 4));
        list.add(new SpawnListEntry(EntityEnderman.class, 10, 1, 4));
        list.add(new SpawnListEntry(EntityWitch.class, 5, 1, 1));
        list.add(new SpawnListEntry(EntityZombieVillager.class, 5, 1, 1));
        return list;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSkyColorByTemp(float temperature) {
        return SKY;
    }
}
