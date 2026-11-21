package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.sulfur.BlockPotentSulfur;
import com.exiledradio.gamedropsbackport.sulfur.BlockSulfurSpike;
import com.exiledradio.gamedropsbackport.sulfur.ItemSulfurDisc;
import com.exiledradio.gamedropsbackport.sulfur.PotentState;
import com.exiledradio.gamedropsbackport.sulfur.Reg;
import com.exiledradio.gamedropsbackport.sulfur.SulfurContent;
import com.exiledradio.gamedropsbackport.sulfur.SulfurEvents;
import com.exiledradio.gamedropsbackport.sulfur.Thickness;
import com.exiledradio.gamedropsbackport.sulfur.TilePotentSulfur;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Stream 2: every block and item under 26.3's names with 26.3's numbers, the recipes, potent sulfur's states and
 * tickers, the spike's shapes, falling, growth and damage, and the disc. Also writes {@code selftest-states.txt} and
 * {@code selftest-items.txt} for {@code tools/check_assets.py}.
 */
final class SulfurSelfTest implements SelfTest {

    static final String[] STONE_NAMES = {
            "sulfur", "sulfur_stairs", "sulfur_slab", "sulfur_wall", "polished_sulfur", "polished_sulfur_stairs",
            "polished_sulfur_slab", "polished_sulfur_wall", "sulfur_bricks", "sulfur_brick_stairs", "sulfur_brick_slab",
            "sulfur_brick_wall", "chiseled_sulfur",
            "cinnabar", "cinnabar_stairs", "cinnabar_slab", "cinnabar_wall", "polished_cinnabar", "polished_cinnabar_stairs",
            "polished_cinnabar_slab", "polished_cinnabar_wall", "cinnabar_bricks", "cinnabar_brick_stairs",
            "cinnabar_brick_slab", "cinnabar_brick_wall", "chiseled_cinnabar",
            "potent_sulfur",
    };

    @Override
    public String name() {
        return "sulfur";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(SulfurContent.enabled(), "sulfur stream enabled")) {
            return;
        }
        registry(check);
        recipes(check);
        potent(check);
        spike(check);
        disc(check);
        dump(check.world);
    }

    // ------------------------------------------------------------------------------- registry

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, name));
    }

    private static Item item(String name) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, name));
    }

    private void registry(Check check) {
        for (String name : STONE_NAMES) {
            Block b = block(name);
            if (!check.ok(b != null && b != Blocks.AIR, "block " + name)) {
                continue;
            }
            check.ok(item(name) != null, "item " + name);
            IBlockState s = b.getDefaultState();
            check.eq(1.5F, s.getBlockHardness(check.world, BlockPos.ORIGIN), name + " hardness");
            check.eq(6.0F, b.getExplosionResistance(null), name + " blast resistance");
            check.eq("pickaxe", b.getHarvestTool(s), name + " tool");
            check.ok(!s.getMaterial().isToolNotRequired(), name + " needs a tool");
            if (name.endsWith("_slab")) {
                check.ok(block(name.replace("_slab", "_double_slab")) != null, name + " double slab");
            }
            if (name.endsWith("_slab") || name.endsWith("_stairs") || name.endsWith("_wall")) {
                check.ok(b instanceof Waterloggable, name + " waterloggable");
            }
        }
        check.eq(27, STONE_NAMES.length, "28 stone blocks less the separate potent sulfur");
        Block spike = block("sulfur_spike");
        check.ok(spike == SulfurContent.sulfurSpike, "sulfur_spike registered");
        check.eq(1.5F, spike.getDefaultState().getBlockHardness(check.world, BlockPos.ORIGIN), "spike hardness");
        check.eq(3.0F, spike.getExplosionResistance(null), "spike blast resistance");
        check.ok(spike instanceof Waterloggable, "spike waterloggable");
        check.ok(spike.canHarvestBlock(check.world, BlockPos.ORIGIN, null), "spike needs no tool");
        check.ok(!(SulfurContent.potentSulfur instanceof Waterloggable), "potent sulfur not waterloggable");
        check.eq("gold", mapColorName(SulfurContent.potentSulfur), "potent sulfur map color");
    }

    private static String mapColorName(Block b) {
        return b.getDefaultState().getMapColor(null, BlockPos.ORIGIN) == net.minecraft.block.material.MapColor.GOLD ? "gold" : "other";
    }

    private void recipes(Check check) {
        result(check, "polished_sulfur", "polished_sulfur", 4);
        result(check, "sulfur_bricks", "sulfur_bricks", 4);
        result(check, "chiseled_sulfur", "chiseled_sulfur", 1);
        result(check, "sulfur_stairs", "sulfur_stairs", 4);
        result(check, "sulfur_slab", "sulfur_slab", 6);
        result(check, "cinnabar_brick_wall", "cinnabar_brick_wall", 6);
        result(check, "polished_cinnabar", "polished_cinnabar", 4);
        result(check, "chiseled_cinnabar", "chiseled_cinnabar", 1);
        result(check, "potent_sulfur", "potent_sulfur", 1);
        result(check, "sulfur_from_sulfur_spikes", "sulfur", 1);
        IRecipe potent = CraftingManager.getRecipe(Reg.id("potent_sulfur"));
        check.eq(9, potent == null ? 0 : potent.getIngredients().size(), "potent sulfur takes 9 sulfur");
    }

    private static void result(Check check, String recipe, String out, int count) {
        IRecipe r = CraftingManager.getRecipe(Reg.id(recipe));
        if (check.ok(r != null, "recipe " + recipe)) {
            ItemStack o = r.getRecipeOutput();
            check.ok(o.getItem() == item(out) && o.getCount() == count, "recipe " + recipe + " makes " + count + " " + out);
        }
    }

    // ------------------------------------------------------------------------------- potent sulfur

    private static final BlockPos AREA = new BlockPos(1000, 120, 1000);

    private static void clear(WorldServer w, BlockPos center, int r, int h) {
        for (BlockPos p : BlockPos.getAllInBox(center.add(-r, -2, -r), center.add(r, h, r))) {
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
        for (Entity e : w.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(center.add(-r - 2, -4, -r - 2), center.add(r + 2, h + 2, r + 2)))) {
            if (!(e instanceof net.minecraft.entity.player.EntityPlayer)) {
                e.setDead();
            }
        }
    }

    private static PotentState state(WorldServer w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return s.getBlock() instanceof BlockPotentSulfur ? s.getValue(BlockPotentSulfur.STATE) : null;
    }

    private void potent(Check check) {
        WorldServer w = check.world;
        w.getChunk(AREA);
        BlockPos p = AREA;
        clear(w, p, 4, 10);
        IBlockState potent = SulfurContent.potentSulfur.getDefaultState();
        w.setBlockState(p, potent, 3);
        check.eq(PotentState.DRY, state(w, p), "dry with air above");
        w.setBlockState(p.up(), Blocks.WATER.getDefaultState(), 3);
        check.eq(PotentState.WET, state(w, p), "wet with water above");
        w.setBlockState(p.down(), Blocks.MAGMA.getDefaultState(), 3);
        check.eq(PotentState.DORMANT, state(w, p), "dormant over magma");
        w.setBlockState(p.down(), Blocks.LAVA.getDefaultState(), 3);
        check.eq(PotentState.CONTINUOUS, state(w, p), "continuous over a lava source");
        w.setBlockState(p.down(), Blocks.FLOWING_LAVA.getDefaultState().withProperty(net.minecraft.block.BlockLiquid.LEVEL, 3), 2);
        w.setBlockState(p.up(), Blocks.WATER.getDefaultState(), 3);
        w.neighborChanged(p, Blocks.WATER, p.up());
        check.eq(PotentState.WET, state(w, p), "flowing lava below is no geyser");
        w.setBlockState(p.down(), Blocks.STONE.getDefaultState(), 3);
        w.setBlockState(p.up(), Blocks.AIR.getDefaultState(), 3);
        check.eq(PotentState.DRY, state(w, p), "dry again when the water goes");

        // The pool: 1-4 water blocks, then an open block.
        for (int water = 1; water <= 5; water++) {
            for (int i = 1; i <= 6; i++) {
                w.setBlockState(p.up(i), i <= water ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
            }
            BlockPos source = TilePotentSulfur.findGasSource(w, p);
            if (water <= 4) {
                check.eq(p.up(water + 1), source, "pool surface over " + water + " water");
            } else {
                check.eq(null, source, "no pool over 5 water");
            }
        }
        for (int i = 1; i <= 6; i++) {
            w.setBlockState(p.up(i), i == 1 ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
        }
        w.neighborChanged(p, Blocks.WATER, p.up());
        check.eq(PotentState.WET, state(w, p), "wet under one water block");
        TilePotentSulfur te = (TilePotentSulfur) w.getTileEntity(p);
        if (!check.ok(te != null, "potent sulfur has its block entity")) {
            return;
        }

        // Nausea: an armor stand with its eyes just over the pool.
        EntityArmorStand stand = new EntityArmorStand(w);
        stand.setNoGravity(true);
        stand.setPosition(p.getX() + 1.5, p.getY() + 2.5 - stand.getEyeHeight(), p.getZ() + 0.5);
        w.setBlockState(p.add(1, 0, 0), Blocks.STONE.getDefaultState(), 2);
        w.setBlockState(p.add(1, 1, 0), Blocks.WATER.getDefaultState(), 2);
        w.spawnEntity(stand);
        long t0 = (w.getTotalWorldTime() / 20 + 1) * 20;
        w.getWorldInfo().setWorldTotalTime(t0);
        te.update();
        check.ok(stand.isPotionActive(MobEffects.NAUSEA), "nausea within the gas");
        check.eq(80, stand.isPotionActive(MobEffects.NAUSEA) ? stand.getActivePotionEffect(MobEffects.NAUSEA).getDuration() : 0,
                "nausea lasts 4 s");
        EntityArmorStand far = new EntityArmorStand(w);
        far.setNoGravity(true);
        far.setPosition(p.getX() + 0.5, p.getY() + 5.5 - far.getEyeHeight(), p.getZ() + 0.5);
        w.spawnEntity(far);
        te.update();
        check.ok(!far.isPotionActive(MobEffects.NAUSEA), "no nausea with no water under the eyes");
        stand.setDead();
        far.setDead();

        // Dormant: a countdown of 15-30 s for one water block.
        w.setBlockState(p.down(), Blocks.MAGMA.getDefaultState(), 3);
        check.eq(PotentState.DORMANT, state(w, p), "dormant after magma goes under");
        te = (TilePotentSulfur) w.getTileEntity(p);
        check.eq(-1, te.waitingCountdown, "fresh geyser countdown reset");
        w.getWorldInfo().setWorldTotalTime(t0 + 20);
        te.update();
        check.ok(te.waitingCountdown >= 14 && te.waitingCountdown <= 29, "dormant countdown 15-30 s, got " + te.waitingCountdown);
        te.waitingCountdown = 1;
        w.getWorldInfo().setWorldTotalTime(t0 + 40);
        te.update();
        check.eq(PotentState.ERUPTING, state(w, p), "erupts when the countdown ends");
        te = (TilePotentSulfur) w.getTileEntity(p);
        check.ok(te != null && w.getTileEntity(p) == te, "block entity kept through the state change");
        // Launch: an item in the plume gets 0.2 upward speed.
        w.getWorldInfo().setWorldTotalTime(t0 + 41);
        EntityItem drop = new EntityItem(w, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, new ItemStack(Blocks.DIRT));
        drop.motionY = 0.0;
        w.spawnEntity(drop);
        te.update();
        check.ok(Math.abs(drop.motionY - 0.2) < 1e-4, "geyser lifts an item by 0.2, got " + drop.motionY);
        drop.motionY = 0.5;
        te.update();
        check.ok(Math.abs(drop.motionY - 0.5) < 1e-4, "no lift once rising at 0.4 or more");
        drop.setDead();

        w.getWorldInfo().setWorldTotalTime(t0 + 60);
        te.update();
        check.ok(te.waitingCountdown >= 0 && te.waitingCountdown <= 1, "eruption lasts 1-2 s, got " + te.waitingCountdown);
        if (state(w, p) == PotentState.ERUPTING) {
            te.waitingCountdown = 1;
            w.getWorldInfo().setWorldTotalTime(t0 + 80);
            te.update();
        }
        check.eq(PotentState.DORMANT, state(w, p), "back to dormant after the eruption");
        clear(w, p, 4, 10);
    }

    // ------------------------------------------------------------------------------- spikes

    private void spike(Check check) {
        WorldServer w = check.world;
        BlockSulfurSpike spike = SulfurContent.sulfurSpike;
        BlockPos top = AREA.up(8);
        clear(w, AREA, 4, 12);
        w.setBlockState(top, SulfurContent.sulfur.getDefaultState(), 3);
        BlockPos a = top.down();
        w.setBlockState(a, spike.placementState(w, a, EnumFacing.DOWN, true), 3);
        check.eq(Thickness.TIP, w.getBlockState(a).getValue(BlockSulfurSpike.THICKNESS), "one stalactite is a tip");
        check.eq(EnumFacing.DOWN, w.getBlockState(a).getValue(BlockSulfurSpike.VERTICAL_DIRECTION), "hangs down");
        BlockPos b = a.down();
        w.setBlockState(b, spike.placementState(w, b, EnumFacing.DOWN, true), 3);
        check.eq(Thickness.FRUSTUM, w.getBlockState(a).getValue(BlockSulfurSpike.THICKNESS), "upper segment of two is a frustum");
        check.eq(Thickness.TIP, w.getBlockState(b).getValue(BlockSulfurSpike.THICKNESS), "lower segment is the tip");
        BlockPos c = b.down();
        w.setBlockState(c, spike.placementState(w, c, EnumFacing.DOWN, true), 3);
        check.eq(Thickness.BASE, w.getBlockState(a).getValue(BlockSulfurSpike.THICKNESS), "root of three is the base");
        check.eq(Thickness.FRUSTUM, w.getBlockState(b).getValue(BlockSulfurSpike.THICKNESS), "middle of three is a frustum");
        check.ok(!BlockSulfurSpike.isValidPlacement(w, AREA.up(3), EnumFacing.DOWN), "nothing to hang from in the air");

        // Merging tips.
        BlockPos floor = AREA.up(2);
        w.setBlockState(floor, Blocks.STONE.getDefaultState(), 3);
        BlockPos up1 = floor.up();
        w.setBlockState(up1, spike.placementState(w, up1, EnumFacing.UP, true), 3);
        BlockPos up2 = up1.up();
        IBlockState merged = spike.placementState(w, up2, EnumFacing.UP, true);
        check.eq(Thickness.TIP, w.getBlockState(up1).getValue(BlockSulfurSpike.THICKNESS), "stalagmite tip");

        // Falling: take the sulfur away; the stalactite falls on its scheduled tick.
        w.setBlockState(top, Blocks.AIR.getDefaultState(), 3);
        check.ok(w.isUpdateScheduled(a, spike), "falling is scheduled");
        spike.updateTick(w, a, w.getBlockState(a), new Random(1));
        check.ok(w.isAirBlock(a) && w.isAirBlock(b) && w.isAirBlock(c), "the whole stalactite leaves its place");
        List<EntityFallingBlock> falling = w.getEntitiesWithinAABB(EntityFallingBlock.class, new AxisAlignedBB(c.down()).union(new AxisAlignedBB(a.up())));
        check.eq(3, falling.size(), "three falling segments");
        float tipHurt = 0;
        int hurting = 0;
        for (EntityFallingBlock f : falling) {
            NBTTagCompound tag = new NBTTagCompound();
            f.writeToNBT(tag);
            if (tag.getBoolean("HurtEntities")) {
                hurting++;
                tipHurt = tag.getFloat("FallHurtAmount");
                check.eq(40, tag.getInteger("FallHurtMax"), "falling tip damage cap");
            }
            check.eq(1, tag.getInteger("Time"), "falling segment no longer looks for its block");
            f.setDead();
        }
        check.eq(1, hurting, "only the tip hurts");
        check.eq(6.0F, tipHurt, "tip damage per block: its length, at least 6");

        // A fallen segment that cannot hang where it landed breaks into its item.
        BlockPos land = AREA.up(4);
        w.setBlockState(land, spike.state(EnumFacing.DOWN, Thickness.TIP), 2);
        spike.onEndFalling(w, land, spike.state(EnumFacing.DOWN, Thickness.TIP), Blocks.AIR.getDefaultState());
        check.ok(w.isAirBlock(land), "a fallen spike breaks");
        List<EntityItem> items = w.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(land).grow(1));
        check.ok(!items.isEmpty() && items.get(0).getItem().getItem() == Item.getItemFromBlock(spike), "and drops itself");
        for (EntityItem i : items) {
            i.setDead();
        }

        // An unsupported stalagmite breaks.
        w.setBlockState(floor, Blocks.AIR.getDefaultState(), 3);
        spike.updateTick(w, up1, w.getBlockState(up1), new Random(1));
        check.ok(w.isAirBlock(up1), "an unsupported stalagmite breaks");
        check.ok(merged.getValue(BlockSulfurSpike.THICKNESS) != null, "placement state computed");

        // Falling damage is a falling stalactite.
        EntityPig pig = new EntityPig(w);
        pig.setPosition(AREA.getX() + 0.5, AREA.getY() + 1, AREA.getZ() + 0.5);
        w.setBlockState(AREA, Blocks.STONE.getDefaultState(), 2);
        w.spawnEntity(pig);
        EntityFallingBlock f = new EntityFallingBlock(w, pig.posX, pig.posY, pig.posZ, spike.state(EnumFacing.DOWN, Thickness.TIP));
        w.spawnEntity(f);
        float before = pig.getHealth();
        pig.attackEntityFrom(DamageSource.FALLING_BLOCK, 3.0F);
        check.eq(before - 3.0F, pig.getHealth(), "the hit still lands");
        check.ok(pig.getCombatTracker().getDeathMessage() != null
                        && pig.getCombatTracker().getDeathMessage().getUnformattedComponentText() != null
                        && lastSource(pig) == SulfurEvents.FALLING_STALACTITE,
                "as a falling stalactite");
        pig.setDead();
        f.setDead();

        // Growth: hanging from sulfur, one more segment (a fresh column: the first one's scheduled ticks are pending).
        clear(w, AREA, 4, 12);
        top = top.east(2);
        a = top.down();
        b = a.down();
        w.setBlockState(top, SulfurContent.sulfur.getDefaultState(), 3);
        w.setBlockState(a, spike.state(EnumFacing.DOWN, Thickness.TIP), 3);
        Random down = new Random() {
            @Override
            public boolean nextBoolean() {
                return true;
            }
        };
        grow(spike, w, a, down);
        check.ok(BlockSulfurSpike.isSpike(w.getBlockState(b)), "a stalactite under sulfur grows down");
        check.eq(Thickness.FRUSTUM, w.getBlockState(a).getValue(BlockSulfurSpike.THICKNESS), "and the old tip thickens");
        w.setBlockState(top, Blocks.STONE.getDefaultState(), 3);
        w.setBlockState(b, Blocks.AIR.getDefaultState(), 3);
        grow(spike, w, a, down);
        check.ok(w.isAirBlock(b), "no growth under stone");
        clear(w, AREA, 4, 12);
    }

    private static DamageSource lastSource(EntityPig pig) {
        try {
            java.lang.reflect.Field f = net.minecraft.util.CombatTracker.class.getDeclaredFields()[0];
            for (java.lang.reflect.Field field : net.minecraft.util.CombatTracker.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    f = field;
                }
            }
            f.setAccessible(true);
            List<?> entries = (List<?>) f.get(pig.getCombatTracker());
            if (entries.isEmpty()) {
                return null;
            }
            return ((net.minecraft.util.CombatEntry) entries.get(entries.size() - 1)).getDamageSrc();
        } catch (Exception e) {
            return null;
        }
    }

    private static void grow(BlockSulfurSpike spike, WorldServer w, BlockPos pos, Random random) {
        try {
            java.lang.reflect.Method m = BlockSulfurSpike.class.getDeclaredMethod("growIfPossible", IBlockState.class,
                    net.minecraft.world.World.class, BlockPos.class, Random.class);
            m.setAccessible(true);
            m.invoke(spike, w.getBlockState(pos), w, pos, random);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------------------- disc

    private void disc(Check check) {
        ItemSulfurDisc disc = SulfurContent.bounce;
        check.ok(item("music_disc_bounce") == disc, "music_disc_bounce registered");
        check.eq(8, disc.comparatorSignal(), "Bounce comparator signal");
        check.eq(EnumRarity.UNCOMMON, disc.getRarity(new ItemStack(disc)), "Bounce rarity");
        check.eq(1, disc.getItemStackLimit(new ItemStack(disc)), "a disc stacks to 1");
        check.eq(Reg.id("music_disc.bounce"), disc.music().getRegistryName(), "Bounce sound event");
    }

    // ------------------------------------------------------------------------------- asset check input

    private static void dump(WorldServer world) throws Exception {
        PrintWriter out = SelfTestRunner.statesOut();
        for (Block block : Reg.blocks()) {
            String kind = Reg.multipartFiles().contains(block) ? "multipart" : "variants";
            for (IBlockState state : block.getBlockState().getValidStates()) {
                out.println(block.getRegistryName() + "\t" + variant(state) + "\t" + kind);
            }
        }
        out.close();
        PrintWriter items = SelfTestRunner.itemsOut();
        for (Map.Entry<Item, String> e : Reg.itemModels().entrySet()) {
            ItemStack stack = new ItemStack(e.getKey());
            items.println(e.getKey().getRegistryName() + "\t0\t" + e.getValue() + "\t" + e.getKey().getTranslationKey(stack) + ".name");
        }
        items.close();
    }

    /** Vanilla's DefaultStateMapper string: properties sorted by name, name=value joined by commas. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static String variant(IBlockState state) {
        Map<String, String> sorted = new TreeMap<String, String>();
        IProperty<?>[] ignored = Reg.ignoredProperties().get(state.getBlock());
        List<IProperty<?>> skip = ignored == null ? java.util.Collections.<IProperty<?>>emptyList() : Arrays.asList(ignored);
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            if (skip.contains(e.getKey())) {
                continue;
            }
            IProperty p = e.getKey();
            sorted.put(p.getName(), p.getName((Comparable) e.getValue()));
        }
        if (sorted.isEmpty()) {
            return "normal";
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }
}
