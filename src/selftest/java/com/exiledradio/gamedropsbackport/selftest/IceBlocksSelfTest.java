package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.iceblocks.IceBlocksContent;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * 26.4 streams 3 and 4: every block under 26.4's names with 26.4's numbers (Pre-Release 1), the tool rules, the
 * recipes, and the calcite set and chiseled soulshale present exactly when their owners are.
 */
final class IceBlocksSelfTest implements SelfTest {

    static final String[] ALWAYS = {
            "packed_ice_bricks", "packed_ice_brick_stairs", "packed_ice_brick_slab", "packed_ice_brick_double_slab",
            "packed_ice_brick_wall", "chiseled_packed_ice",
            "snow_bricks", "snow_brick_stairs", "snow_brick_slab", "snow_brick_double_slab", "snow_brick_wall",
    };
    static final String[] CALCITE = {
            "calcite_pillar", "calcite_stairs", "calcite_slab", "calcite_double_slab", "calcite_wall",
            "polished_calcite", "polished_calcite_stairs", "polished_calcite_slab", "polished_calcite_double_slab",
            "polished_calcite_wall", "polished_calcite_bricks", "polished_calcite_brick_stairs", "polished_calcite_brick_slab",
            "polished_calcite_brick_double_slab", "polished_calcite_brick_wall",
    };

    @Override
    public String name() {
        return "iceblocks";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(IceBlocksContent.enabled(), "iceBlocks stream enabled")) {
            return;
        }
        for (String n : ALWAYS) {
            check.ok(block(n) != null, "registered: " + n);
        }
        boolean cc = IceBlocksContent.calcite();
        for (String n : CALCITE) {
            check.eq(cc, block(n) != null, "calcite set only with Caves & Cliffs: " + n);
        }
        check.eq(IceBlocksContent.soulshale(), block("chiseled_soulshale") != null, "chiseled soulshale only with Wild Update");

        numbers(check, "packed_ice_bricks", 0.5F, 0.5F, "pickaxe", Material.PACKED_ICE);
        numbers(check, "packed_ice_brick_slab", 0.5F, 0.5F, "pickaxe", Material.PACKED_ICE);
        numbers(check, "chiseled_packed_ice", 0.5F, 0.5F, "pickaxe", Material.PACKED_ICE);
        numbers(check, "snow_bricks", 0.6F, 0.6F, "shovel", Material.CRAFTED_SNOW);
        numbers(check, "snow_brick_wall", 0.6F, 0.6F, "shovel", Material.CRAFTED_SNOW);
        if (cc) {
            numbers(check, "polished_calcite", 0.75F, 0.75F, "pickaxe", Material.ROCK);
            numbers(check, "calcite_stairs", 0.75F, 0.75F, "pickaxe", Material.ROCK);
            numbers(check, "calcite_pillar", 0.75F, 0.75F, "pickaxe", Material.ROCK);
        }
        if (IceBlocksContent.soulshale()) {
            Block s = block("chiseled_soulshale");
            check.eq(2.0F, s.getBlockHardness(s.getDefaultState(), check.world, BlockPos.ORIGIN), "chiseled soulshale hardness");
            check.ok(Math.abs(s.getExplosionResistance(null) - 6.0F) < 1e-4, "chiseled soulshale resistance 6");
            check.ok(s.getCreativeTab() == null, "chiseled soulshale in no creative tab (26.4)");
        }
        check.eq(0.6F, IceBlocksContent.packedIceBricks.slipperiness, "packed ice bricks are not slippery");

        harvest(check);
        recipes(check, cc);
        RegDump.dump(com.exiledradio.gamedropsbackport.iceblocks.Reg.blocks(), com.exiledradio.gamedropsbackport.iceblocks.Reg.multipartFiles(),
                com.exiledradio.gamedropsbackport.iceblocks.Reg.ignoredProperties(), com.exiledradio.gamedropsbackport.iceblocks.Reg.itemModels());
    }

    private static Block block(String name) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("gamedropsbackport", name));
        return b == null || b == Blocks.AIR ? null : b;
    }

    private static void numbers(Check check, String name, float hardness, float resistance, String tool, Material material) {
        Block b = block(name);
        if (!check.ok(b != null, "present: " + name)) {
            return;
        }
        IBlockState s = b.getDefaultState();
        check.ok(Math.abs(b.getBlockHardness(s, check.world, BlockPos.ORIGIN) - hardness) < 1e-4, name + " hardness " + hardness);
        check.ok(Math.abs(b.getExplosionResistance(null) - resistance) < 1e-4,
                name + " resistance " + resistance + " (got " + b.getExplosionResistance(null) + ")");
        check.eq(tool, b.getHarvestTool(s), name + " tool");
        check.ok(s.getMaterial() == material, name + " material");
    }

    /** requiresCorrectToolForDrops: by hand nothing drops; a pickaxe (shovel for snow bricks) gets it. */
    private static void harvest(Check check) {
        BlockPos at = check.world.getSpawnPoint().add(60, 0, 60);
        at = new BlockPos(at.getX(), 210, at.getZ());
        FakePlayer player = FakePlayerFactory.getMinecraft(check.world);
        Object[][] cases = {
                {IceBlocksContent.packedIceBricks, Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL},
                {IceBlocksContent.packedIceBrickShapes.wall, Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL},
                {IceBlocksContent.packedIceBrickShapes.stairs, Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL},
                {IceBlocksContent.packedIceBrickShapes.slab, Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL},
                {IceBlocksContent.snowBricks, Items.WOODEN_SHOVEL, Items.WOODEN_PICKAXE},
        };
        for (Object[] c : cases) {
            Block b = (Block) c[0];
            check.world.setBlockState(at, b.getDefaultState(), 2);
            player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            check.ok(!b.canHarvestBlock(check.world, at, player), b.getRegistryName() + ": nothing by hand");
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack((net.minecraft.item.Item) c[2]));
            check.ok(!b.canHarvestBlock(check.world, at, player), b.getRegistryName() + ": nothing with the wrong tool");
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack((net.minecraft.item.Item) c[1]));
            check.ok(b.canHarvestBlock(check.world, at, player), b.getRegistryName() + ": drops with its tool");
        }
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        check.world.setBlockToAir(at);
    }

    private static void recipes(Check check, boolean cc) {
        String[][] want = {
                {"packed_ice_bricks", "4"}, {"packed_ice_brick_stairs", "4"}, {"packed_ice_brick_slab", "6"},
                {"packed_ice_brick_wall", "6"}, {"chiseled_packed_ice", "1"},
                {"snow_bricks", "4"}, {"snow_brick_stairs", "4"}, {"snow_brick_slab", "6"}, {"snow_brick_wall", "6"},
        };
        for (String[] w : want) {
            recipe(check, w[0], Integer.parseInt(w[1]));
        }
        if (cc) {
            String[][] calcite = {
                    {"calcite_pillar", "2"}, {"calcite_stairs", "4"}, {"calcite_slab", "6"}, {"calcite_wall", "6"},
                    {"polished_calcite", "4"}, {"polished_calcite_bricks", "4"}, {"polished_calcite_brick_wall", "6"},
            };
            for (String[] w : calcite) {
                recipe(check, w[0], Integer.parseInt(w[1]));
            }
        }
    }

    private static void recipe(Check check, String name, int count) {
        IRecipe r = CraftingManager.getRecipe(new ResourceLocation("gamedropsbackport", name));
        if (check.ok(r != null, "recipe " + name)) {
            check.eq(count, r.getRecipeOutput().getCount(), "recipe " + name + " count");
            check.eq(name, r.getRecipeOutput().getItem().getRegistryName().getPath(), "recipe " + name + " output");
        }
    }
}
