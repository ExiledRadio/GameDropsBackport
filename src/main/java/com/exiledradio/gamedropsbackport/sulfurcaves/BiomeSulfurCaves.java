package com.exiledradio.gamedropsbackport.sulfurcaves;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.2's sulfur caves as a 1.12.2 biome ({@code worldgen/biome/sulfur_caves.json}): temperature 0.8, downfall 0.4,
 * rain, grass {@code #aba64f}, foliage {@code #77ab2f}, water {@code #34bf89}. Never placed in a 1.12.2 biome map;
 * a superflat preset may use it, and its spawn lists are the ones the sulfur caves use ({@link SulfurSpawns}).
 */
public class BiomeSulfurCaves extends Biome {

    public BiomeSulfurCaves() {
        super(new BiomeProperties("Sulfur Caves").setTemperature(0.8F).setRainfall(0.4F).setWaterColor(0x34BF89));
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        this.spawnableCaveCreatureList.add(new SpawnListEntry(EntityBat.class, 10, 8, 8));
        this.spawnableMonsterList.clear();
        this.spawnableMonsterList.addAll(monsters());
    }

    /** 26.3's monster spawns in order; the sulfur cube only once it is registered (stream 3). */
    @SuppressWarnings("unchecked")
    static List<SpawnListEntry> monsters() {
        List<SpawnListEntry> list = new ArrayList<SpawnListEntry>();
        EntityEntry cube = ForgeRegistries.ENTITIES.getValue(new ResourceLocation("gamedropsbackport", "sulfur_cube"));
        if (cube != null && EntityLiving.class.isAssignableFrom(cube.getEntityClass())) {
            list.add(new SpawnListEntry((Class<? extends EntityLiving>) cube.getEntityClass(), 100, 2, 4));
        }
        list.add(new SpawnListEntry(EntityCreeper.class, 50, 2, 2));
        list.add(new SpawnListEntry(EntitySkeleton.class, 50, 2, 2));
        list.add(new SpawnListEntry(EntitySlime.class, 25, 1, 1));
        list.add(new SpawnListEntry(EntityCaveSpider.class, 20, 1, 1));
        list.add(new SpawnListEntry(EntityZombie.class, 50, 2, 2));
        list.add(new SpawnListEntry(EntityEnderman.class, 10, 1, 1));
        list.add(new SpawnListEntry(EntityWitch.class, 1, 1, 1));
        list.add(new SpawnListEntry(EntityZombieVillager.class, 5, 1, 1));
        return list;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getGrassColorAtPos(BlockPos pos) {
        return 0xABA64F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getFoliageColorAtPos(BlockPos pos) {
        return 0x77AB2F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSkyColorByTemp(float temperature) {
        return 0x78A7FF;
    }
}
