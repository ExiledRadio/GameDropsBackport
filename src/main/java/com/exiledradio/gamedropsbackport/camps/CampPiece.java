package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPiece;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernJigsaw;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.exiledradio.backportcore.terrain.WorldHeights;
import com.exiledradio.gamedropsbackport.softblocks.BlockStrawBed;
import com.exiledradio.gamedropsbackport.softblocks.SoftBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;
import java.util.Random;

/**
 * A piece of an abandoned camp: Backport Core's jigsaw piece (its element places itself: 26.3's legacy single
 * element skips the template's air, a feature element grows its tree), and, as 26.3's
 * {@code PoolElementStructurePiece}, it tells the terrain how it meets the ground ({@code beard_thin}).
 *
 * <p>A template is placed here rather than by Core's element only to leave out its entities, the soft blocks
 * stream's cushions, where that stream is off: 1.12.2 would log each one it cannot make. Every camp template is a
 * rigid {@code legacy_single_pool_element} with no processors, which is all this does.
 */
public class CampPiece extends JigsawPiece implements TerrainAdaptation.AdaptingPiece, ModernJigsaw.Joined {

    /** 26.3's {@code JigsawJunction}s: source x, source ground y, source z, three ints each. */
    private int[] junctions = new int[0];

    /** For loading. */
    public CampPiece() {
    }

    public CampPiece(JigsawElement element, BlockPos position, int groundLevelDelta, Rotation rotation, StructureBoundingBox box) {
        super(element, position, groundLevelDelta, rotation, box);
    }

    @Override
    public void addJunction(int x, int groundY, int z) {
        int n = this.junctions.length;
        this.junctions = Arrays.copyOf(this.junctions, n + 3);
        this.junctions[n] = x;
        this.junctions[n + 1] = groundY;
        this.junctions[n + 2] = z;
    }

    /** 26.3's cushion entity, the camps' only entity, by the soft blocks stream's name. */
    static final ResourceLocation CUSHION = new ResourceLocation("gamedropsbackport", "cushion");

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox chunk) {
        if (this.element == null || this.element.template() == null || !this.element.rigid()) {
            return super.addComponentParts(world, random, chunk);
        }
        ModernTemplate template = ModernTemplate.load(JigsawPools.templateId(this.element.template()));
        TemplatePlacement placement = TemplatePlacement.of(this.pieceRotation, Mirror.NONE).clip(chunk).finalizeEntities(true)
                .entities(ForgeRegistries.ENTITIES.containsKey(CUSHION));
        // legacy_single_pool_element: BlockIgnoreProcessor.STRUCTURE_AND_AIR, the template's air is not placed.
        // 26.3 builds camps before any tree or plant, so its ground has none where the template is air; 1.12.2
        // populates trees and plants first, so those are taken out of the template's air here.
        placement.processor((w, p, block, r) -> {
            if ("minecraft:air".equals(block.name())) {
                clearGrowth(w, p);
                return null;
            }
            return block;
        });
        template.place(world, this.position, placement);
        StructureBoundingBox area = this.boundingBox.intersectsWith(chunk) ? new StructureBoundingBox(
                Math.max(this.boundingBox.minX, chunk.minX), this.boundingBox.minY, Math.max(this.boundingBox.minZ, chunk.minZ),
                Math.min(this.boundingBox.maxX, chunk.maxX), this.boundingBox.maxY, Math.min(this.boundingBox.maxZ, chunk.maxZ)) : null;
        if (area != null) {
            wholeBeds(world, area);
            // 26.3 shapes the terrain under a camp (beard_thin); Caves & Cliffs' 1.18 generator does that here, before
            // the chunk exists. Anywhere else the ground is already made, so the gap under the floor is filled now.
            if (!WorldHeights.isCavesAndCliffsExtended(world)) {
                groundBelow(world, area);
            }
        }
        return true;
    }

    /** A tree's or plant's block where the template has air: taken out (Core then takes the tree's cut-off leaves). */
    public static void clearGrowth(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        Block b = s.getBlock();
        Material m = s.getMaterial();
        if (m == Material.LEAVES || m == Material.PLANTS || m == Material.VINE || m == Material.CACTUS
                || m == Material.GOURD || b instanceof BlockLog || b instanceof BlockBush || b.isLeaves(s, world, pos)
                || b.isWood(world, pos)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
    }

    /**
     * A straw bed half built here gets its other half now, also where that half is in the next chunk (which exists:
     * 1.12.2 populates a chunk only once its neighbors exist). Otherwise anything placed beside the lone half before
     * the next chunk builds its part tells it its other half is gone, and it breaks.
     */
    private static void wholeBeds(World world, StructureBoundingBox area) {
        if (SoftBlocks.strawBed == null) {
            return;
        }
        for (BlockPos pos : BlockPos.getAllInBoxMutable(area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)) {
            IBlockState s = world.getBlockState(pos);
            if (s.getBlock() != SoftBlocks.strawBed) {
                continue;
            }
            BlockPos o = BlockStrawBed.other(s, pos.toImmutable());
            if (area.isVecInside(o) || !world.isBlockLoaded(o) || world.getBlockState(o).getBlock() == SoftBlocks.strawBed) {
                continue;
            }
            BlockBed.EnumPartType part = s.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT
                    ? BlockBed.EnumPartType.HEAD : BlockBed.EnumPartType.FOOT;
            world.setBlockState(o, s.withProperty(BlockBed.PART, part).withProperty(BlockBed.OCCUPIED, false), 2);
        }
    }

    /** How far down the ground is filled under a camp's floor (26.3's beard reaches 12 blocks). */
    static final int FILL_DEPTH = 12;

    /**
     * Fills the gap between a camp piece's bottom layer and the ground under it with the biome's filler, as 26.3's
     * beard gives it ground to stand on: only under columns where the piece has a block at its bottom.
     */
    private static void groundBelow(World world, StructureBoundingBox area) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = area.minX; x <= area.maxX; x++) {
            for (int z = area.minZ; z <= area.maxZ; z++) {
                p.setPos(x, area.minY, z);
                if (open(world, p)) {
                    continue;
                }
                IBlockState fill = filler(world, p);
                for (int y = area.minY - 1; y >= area.minY - FILL_DEPTH && y >= WorldHeights.minY(world); y--) {
                    p.setY(y);
                    if (!open(world, p)) {
                        break;
                    }
                    world.setBlockState(p, fill, 2);
                }
            }
        }
    }

    /** Air, liquid, or a plant, leaf or anything a block may replace: not ground. */
    private static boolean open(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        Material m = s.getMaterial();
        return s.getBlock().isAir(s, world, pos) || m.isLiquid() || m == Material.LEAVES || m == Material.PLANTS
                || m == Material.VINE || s.getBlock().isReplaceable(world, pos);
    }

    /** The biome's filler (dirt in most), sandstone for sand: nothing under a camp may fall. */
    private static IBlockState filler(World world, BlockPos pos) {
        IBlockState f = world.getBiome(pos).fillerBlock;
        if (f.getBlock() == Blocks.SAND) {
            return f.getValue(net.minecraft.block.BlockSand.VARIANT) == net.minecraft.block.BlockSand.EnumType.RED_SAND
                    ? Blocks.RED_SANDSTONE.getDefaultState() : Blocks.SANDSTONE.getDefaultState();
        }
        if (f.getBlock() instanceof BlockFalling || f.getMaterial() == Material.AIR || f.getMaterial().isLiquid()) {
            return Blocks.DIRT.getDefaultState();
        }
        return f;
    }

    @Override
    public boolean rigid() {
        return this.element == null || this.element.rigid();
    }

    @Override
    public int[] junctions() {
        return this.junctions.clone();
    }

    @Override
    public void offset(int x, int y, int z) {
        super.offset(x, y, z);
        this.pivot = this.pivot.add(x, y, z);
        for (int n = 0; n < this.junctions.length; n += 3) {
            this.junctions[n] += x;
            this.junctions[n + 1] += y;
            this.junctions[n + 2] += z;
        }
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        super.writeStructureToNBT(tag);
        tag.setIntArray("junctions", this.junctions);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager manager) {
        super.readStructureFromNBT(tag, manager);
        int[] j = tag.getIntArray("junctions");
        this.junctions = j.length % 3 == 0 ? j : new int[0];
    }
}
