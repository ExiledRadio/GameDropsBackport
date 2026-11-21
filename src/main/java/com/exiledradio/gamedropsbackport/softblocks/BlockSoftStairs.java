package com.exiledradio.gamedropsbackport.softblocks;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Wool or concrete stairs in two colors (bit 8 of the metadata picks one; {@link SoftColors}). Everything else is
 * vanilla's {@link BlockStairs}: corners, upside-down placement, strength and sound from the full block.
 * Waterloggable, as 26.x's stairs (Backport Core, with Fluidlogged API).
 */
public class BlockSoftStairs extends BlockStairs implements Waterloggable {

    /** Set just before construction: {@code createBlockState} runs inside the super constructor. */
    private static EnumDyeColor[] pending;

    final EnumDyeColor[] colors;
    final PropertyEnum<EnumDyeColor> color;
    private final Block base;

    static BlockSoftStairs create(Block base, EnumDyeColor[] colors) {
        pending = colors;
        try {
            return new BlockSoftStairs(base, colors);
        } finally {
            pending = null;
        }
    }

    private BlockSoftStairs(Block base, EnumDyeColor[] colors) {
        super(base.getStateFromMeta(colors[0].getMetadata()));
        this.base = base;
        this.colors = colors;
        this.color = colorProperty();
        this.useNeighborBrightness = true;
        String tool = base.getHarvestTool(base.getDefaultState());
        if (tool != null) {
            setHarvestLevel(tool, base.getHarvestLevel(base.getDefaultState()));
        }
        setDefaultState(getDefaultState().withProperty(this.color, colors[0]));
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
        return new BlockStateContainer(this, FACING, HALF, SHAPE, SoftColors.property(pending));
    }

    public EnumDyeColor colorOf(IBlockState state) {
        return state.getValue(this.color);
    }

    public IBlockState withColor(EnumDyeColor c) {
        return getDefaultState().withProperty(this.color, c);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta & 7).withProperty(this.color, this.colors[(meta >> 3) & 1]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return super.getMetaFromState(state) | (SoftColors.indexOf(this.colors, state.getValue(this.color)) << 3);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
        EnumDyeColor c = EnumDyeColor.byMetadata(meta);
        return SoftColors.indexOf(this.colors, c) >= 0 ? state.withProperty(this.color, c) : state;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(this.color).getMetadata();
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this, 1, damageDropped(state));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (EnumDyeColor c : this.colors) {
            items.add(new ItemStack(this, 1, c.getMetadata()));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.base.getMapColor(this.base.getStateFromMeta(state.getValue(this.color).getMetadata()), world, pos);
    }
}
