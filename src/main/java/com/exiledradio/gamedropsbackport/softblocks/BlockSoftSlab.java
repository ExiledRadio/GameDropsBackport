package com.exiledradio.gamedropsbackport.softblocks;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A wool or concrete slab. A half slab holds 8 colors (metadata: color index, 8 = top); the double slab holds all 16
 * (metadata = dye color) and drops two halves of its color. 1.12.2 has no {@code type=double}, so the double is its
 * own block, as the family's other slabs. Half slabs are waterloggable, as 26.x's; the double is not.
 */
public abstract class BlockSoftSlab extends BlockSlab implements Waterloggable {

    private static EnumDyeColor[] pending;

    final EnumDyeColor[] colors;
    final PropertyEnum<EnumDyeColor> color;
    final Block base;

    BlockSoftSlab(Block base, EnumDyeColor[] colors) {
        super(base.getDefaultState().getMaterial(), base.getDefaultState().getMapColor(null, null));
        this.base = base;
        this.colors = colors;
        this.color = colorProperty();
        IBlockState full = base.getDefaultState();
        setHardness(base.getBlockHardness(full, null, null));
        setResistance(base.getExplosionResistance(null) * 5.0F / 3.0F);
        setSoundType(base.getSoundType());
        String tool = base.getHarvestTool(full);
        if (tool != null) {
            setHarvestLevel(tool, base.getHarvestLevel(full));
        }
        IBlockState state = this.blockState.getBaseState().withProperty(this.color, colors[0]);
        if (!isDouble()) {
            state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
        }
        setDefaultState(state);
        this.useNeighborBrightness = !isDouble();
    }

    static <T> T construct(EnumDyeColor[] colors, java.util.function.Supplier<T> make) {
        pending = colors;
        try {
            return make.get();
        } finally {
            pending = null;
        }
    }

    @SuppressWarnings("unchecked")
    private PropertyEnum<EnumDyeColor> colorProperty() {
        for (IProperty<?> p : this.blockState.getProperties()) {
            if (p.getName().equals("color")) {
                return (PropertyEnum<EnumDyeColor>) p;
            }
        }
        throw new IllegalStateException("no color property");
    }

    @Override
    protected BlockStateContainer createBlockState() {
        PropertyEnum<EnumDyeColor> c = SoftColors.property(pending);
        return isDouble() ? new BlockStateContainer(this, c) : new BlockStateContainer(this, HALF, c);
    }

    @Override
    public boolean canHoldWater(IBlockState state, IBlockAccess world, BlockPos pos) {
        return !isDouble();
    }

    public EnumDyeColor colorOf(IBlockState state) {
        return state.getValue(this.color);
    }

    public IBlockState withColor(EnumDyeColor c) {
        return getDefaultState().withProperty(this.color, c);
    }

    @Override
    public String getTranslationKey(int meta) {
        return getTranslationKey();
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return this.color;
    }

    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return EnumDyeColor.byMetadata(stack.getMetadata());
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        if (isDouble()) {
            return getDefaultState().withProperty(this.color, EnumDyeColor.byMetadata(meta));
        }
        return getDefaultState().withProperty(this.color, this.colors[meta & 7])
                .withProperty(HALF, (meta & 8) != 0 ? EnumBlockHalf.TOP : EnumBlockHalf.BOTTOM);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        if (isDouble()) {
            return state.getValue(this.color).getMetadata();
        }
        return SoftColors.indexOf(this.colors, state.getValue(this.color))
                | (state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0);
    }

    /** The item's metadata is the dye's; vanilla's placement then picks the half. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, net.minecraft.util.EnumFacing facing, float hitX,
                                            float hitY, float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
        EnumDyeColor c = EnumDyeColor.byMetadata(meta);
        return SoftColors.indexOf(this.colors, c) >= 0 ? state.withProperty(this.color, c) : state;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(this.color).getMetadata();
    }

    @Override
    public int quantityDropped(Random random) {
        return isDouble() ? 2 : 1;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.base.getMapColor(this.base.getStateFromMeta(state.getValue(this.color).getMetadata()), world, pos);
    }

    /** The half slab of the same material and color, as an item. */
    abstract Half halfFor(EnumDyeColor c);

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(halfFor(state.getValue(this.color)));
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(halfFor(state.getValue(this.color)), 1, damageDropped(state));
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(halfFor(state.getValue(this.color)), 1, damageDropped(state));
    }

    /** A half slab. */
    public static final class Half extends BlockSoftSlab {
        Double full;

        Half(Block base, EnumDyeColor[] colors) {
            super(base, colors);
        }

        @Override
        public boolean isDouble() {
            return false;
        }

        @Override
        Half halfFor(EnumDyeColor c) {
            return this;
        }

        @Override
        public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
            for (EnumDyeColor c : this.colors) {
                items.add(new ItemStack(this, 1, c.getMetadata()));
            }
        }

        /** @return whether this half slab has this color */
        boolean has(EnumDyeColor c) {
            return SoftColors.indexOf(this.colors, c) >= 0;
        }
    }

    /** Two halves of one color in one space. */
    public static final class Double extends BlockSoftSlab {
        final Half[] halves;

        Double(Block base, Half... halves) {
            super(base, EnumDyeColor.values());
            this.halves = halves;
        }

        @Override
        public boolean isDouble() {
            return true;
        }

        @Override
        Half halfFor(EnumDyeColor c) {
            for (Half h : this.halves) {
                if (h.has(c)) {
                    return h;
                }
            }
            return this.halves[0];
        }

        @Override
        public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        }
    }
}
