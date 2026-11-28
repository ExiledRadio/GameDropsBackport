package com.exiledradio.gamedropsbackport.poplar;

import net.minecraft.block.BlockDirt;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The dappled forest, 26.3 ({@code worldgen/biome/dappled_forest}).
 *
 * <ul>
 *   <li>Temperature 0.6, downfall 0.6, rain; grass {@code #DF6827}, foliage {@code #E68E30}, water {@code #375154},
 *       sky {@code #7CA3FF}; fog {@code #CCD8E2} (client, {@code client.PoplarClient}). The plains' height and
 *       variation (0.125, 0.05), since it takes the place of plains.</li>
 *   <li>Spawns: sheep 12, pig 10, chicken 10, cow 8 (4 each), rabbit 4 (2-4), fox 4 (2-4, Village &amp; Pillage's
 *       fox, Rule 2); 1.12.2's usual monsters, which are 26.3's (spider, zombie, zombie villager, skeleton, creeper,
 *       slime, enderman, witch); bats.</li>
 *   <li>Surface: grass and dirt, with patches of coarse dirt (26.3's {@code small_patch} noise above 1.2; here
 *       1.12.2's surface noise above 1.75, the threshold it uses for the giant taiga's coarse dirt).</li>
 *   <li>Decoration: ores, sand, clay and gravel disks as any 1.12.2 biome, then 26.3's vegetal step
 *       ({@link PoplarFeatures#vegetation}), then springs. No 1.12.2 trees, flowers, grass or mushrooms of its own.</li>
 * </ul>
 * Not here: glow squids and glow lichen in its caves (Caves &amp; Cliffs' mob and block), the cold farm animal
 * variants (Tricky Trials' animals), the forest music (1.12.2 has one overworld music).
 */
public class BiomeDappledForest extends Biome {

    public static final int GRASS = 0xDF6827;
    public static final int FOLIAGE = 0xE68E30;
    public static final int DRY_FOLIAGE = 0x8C3A04;
    public static final int SKY = 0x7CA3FF;
    public static final int FOG = 0xCCD8E2;
    public static final int WATER = 0x375154;

    public BiomeDappledForest() {
        super(new BiomeProperties("Dappled Forest").setBaseHeight(0.125F).setHeightVariation(0.05F)
                .setTemperature(0.6F).setRainfall(0.6F).setWaterColor(WATER));
        this.spawnableCreatureList.clear();
        this.spawnableCreatureList.add(new SpawnListEntry(EntitySheep.class, 12, 4, 4));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityPig.class, 10, 4, 4));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityChicken.class, 10, 4, 4));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityCow.class, 8, 4, 4));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityRabbit.class, 4, 2, 4));
        this.topBlock = Blocks.GRASS.getDefaultState();
        this.fillerBlock = Blocks.DIRT.getDefaultState();
    }

    static void addTypes(Biome biome) {
        BiomeDictionary.addTypes(biome, BiomeDictionary.Type.FOREST);
    }

    /** Init: Village &amp; Pillage's fox, weight 4 in groups of 2-4, when it is installed. */
    @SuppressWarnings("unchecked")
    void addFox() {
        EntityEntry fox = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "fox"));
        if (fox != null && EntityLiving.class.isAssignableFrom(fox.getEntityClass())) {
            this.spawnableCreatureList.add(new SpawnListEntry((Class<? extends EntityLiving>) fox.getEntityClass(), 4, 2, 4));
        }
    }

    /** Coarse dirt in patches, then 1.12.2's grass and dirt surface. */
    @Override
    public void genTerrainBlocks(World world, Random rand, ChunkPrimer primer, int x, int z, double noise) {
        this.topBlock = Blocks.GRASS.getDefaultState();
        this.fillerBlock = Blocks.DIRT.getDefaultState();
        if (noise > 1.75D) {
            this.topBlock = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
        }
        generateBiomeTerrain(world, rand, primer, x, z, noise);
    }

    @Override
    public BiomeDecorator createBiomeDecorator() {
        return getModdedBiomeDecorator(new Decorator());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSkyColorByTemp(float temperature) {
        return SKY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getGrassColorAtPos(BlockPos pos) {
        return getModdedBiomeGrassColor(GRASS);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getFoliageColorAtPos(BlockPos pos) {
        return getModdedBiomeFoliageColor(FOLIAGE);
    }

    /** 1.12.2's decoration with everything vegetal replaced by 26.3's dappled forest step. */
    public static class Decorator extends BiomeDecorator {

        @Override
        protected void genDecorations(Biome biome, World world, Random random) {
            net.minecraft.util.math.ChunkPos chunk = new net.minecraft.util.math.ChunkPos(this.chunkPos);
            MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(world, random, chunk));
            generateOres(world, random);
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.SAND)) {
                for (int i = 0; i < this.sandPatchesPerChunk; i++) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    this.sandGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(x, 0, z)));
                }
            }
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.CLAY)) {
                for (int i = 0; i < this.clayPerChunk; i++) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    this.clayGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(x, 0, z)));
                }
            }
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.SAND_PASS2)) {
                for (int i = 0; i < this.gravelPatchesPerChunk; i++) {
                    int x = random.nextInt(16) + 8;
                    int z = random.nextInt(16) + 8;
                    this.gravelGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(x, 0, z)));
                }
            }
            // Where Dynamic Trees grows this forest's trees (Dynamic Trees - Backport's biome table), it denies the
            // TREE step; the step's fallen trees, mushrooms and shrubs are still Game Drops' to place.
            if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.TREE)
                    || com.exiledradio.backportcore.compat.DynamicTrees.replacesTrees(world, this.chunkPos.add(16, 0, 16))) {
                PoplarFeatures.vegetation(world, random, this.chunkPos, biome);
            }
            if (this.generateFalls) {
                if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.LAKE_WATER)) {
                    for (int i = 0; i < 50; i++) {
                        int x = random.nextInt(16) + 8;
                        int z = random.nextInt(16) + 8;
                        int y = random.nextInt(random.nextInt(248) + 8);
                        new WorldGenLiquids(Blocks.FLOWING_WATER).generate(world, random, this.chunkPos.add(x, y, z));
                    }
                }
                if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.LAKE_LAVA)) {
                    for (int i = 0; i < 20; i++) {
                        int x = random.nextInt(16) + 8;
                        int z = random.nextInt(16) + 8;
                        int y = random.nextInt(random.nextInt(random.nextInt(240) + 8) + 8);
                        new WorldGenLiquids(Blocks.FLOWING_LAVA).generate(world, random, this.chunkPos.add(x, y, z));
                    }
                }
            }
            MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(world, random, chunk));
        }
    }
}
