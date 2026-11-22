package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPiece;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernJigsaw;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
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
        placement.processor((w, p, block, r) -> "minecraft:air".equals(block.name()) ? null : block);
        template.place(world, this.position, placement);
        return true;
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
