package com.exiledradio.gamedropsbackport.iceblocks;

import com.exiledradio.backportcore.block.ModernWall;


import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;

/**
 * The stairs, slab and wall cut from one block, registered and named the modern way:
 * {@code <prefix>_stairs}, {@code <prefix>_slab} (+ {@code <prefix>_double_slab}), {@code <prefix>_wall}.
 *
 * <p>The base block need not be registered by this mod (a copy's owner may provide the real one); the
 * shapes copy strength, sound and tool from whatever base they are given.
 */
public final class Shapes {

    public final String prefix;
    @Nullable public final BlockShapedStairs stairs;
    @Nullable public final BlockShapedSlab.Half slab;
    @Nullable public final BlockShapedSlab.Double doubleSlab;
    @Nullable public final ModernWall wall;

    /** Core's wall with this stream's tool rule. */
    public static class Wall extends ModernWall {
        private final boolean toolRequired;

        public Wall(IBlockState full, boolean toolRequired) {
            super(full);
            this.toolRequired = toolRequired;
        }

        @Override
        public boolean canHarvestBlock(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
                                       net.minecraft.entity.player.EntityPlayer player) {
            return BlockIceStone.toolRequired(this, this.toolRequired, world, pos, player);
        }
    }

    private Shapes(String prefix, BlockShapedStairs stairs, BlockShapedSlab.Half slab, BlockShapedSlab.Double doubleSlab,
                   ModernWall wall) {
        this.prefix = prefix;
        this.stairs = stairs;
        this.slab = slab;
        this.doubleSlab = doubleSlab;
        this.wall = wall;
    }

    /** Registers the asked-for shapes of a base block. */
    public static Shapes register(IForgeRegistry<Block> registry, String prefix, Block base, boolean toolRequired) {
        boolean stairs = true, slab = true, wall = true;
        IBlockState state = base.getDefaultState();
        String tool = base.getHarvestTool(state);
        BlockShapedStairs st = null;
        BlockShapedSlab.Half half = null;
        BlockShapedSlab.Double full = null;
        ModernWall wa = null;
        if (stairs) {
            st = Reg.block(registry, prefix + "_stairs", new BlockShapedStairs(state, toolRequired), tool);
        }
        if (slab) {
            half = Reg.block(registry, prefix + "_slab", new BlockShapedSlab.Half(base, state, toolRequired), tool);
            full = Reg.block(registry, prefix + "_double_slab", new BlockShapedSlab.Double(base, state, half, toolRequired), tool);
            full.setCreativeTab(null);
            Reg.ignoreProperties(half, BlockShapedSlab.VARIANT);
            Reg.ignoreProperties(full, BlockShapedSlab.VARIANT);
        }
        if (wall) {
            // Backport Core's 1.16+ wall (low and tall sides), the family's one wall class (CORE_REQUESTS #8).
            wa = Reg.block(registry, prefix + "_wall", new Wall(state, toolRequired), tool);
            Reg.multipartFile(wa);
        }
        return new Shapes(prefix, st, half, full, wa);
    }

    /** Registers the items: plain for stairs and wall, the joining {@link ItemSlab} for the slab. */
    public void registerItems(IForgeRegistry<Item> registry) {
        if (this.stairs != null) {
            Reg.blockItem(registry, this.stairs);
        }
        if (this.slab != null) {
            Reg.blockItem(registry, this.slab, new ItemSlab(this.slab, (BlockSlab) this.slab, (BlockSlab) this.doubleSlab));
        }
        if (this.wall != null) {
            Reg.blockItem(registry, this.wall);
        }
    }
}
