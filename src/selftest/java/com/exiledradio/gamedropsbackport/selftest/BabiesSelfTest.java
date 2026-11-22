package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.backportcore.trade.TradeRegistry;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.babies.AgeLock;
import com.exiledradio.gamedropsbackport.babies.BabiesContent;
import com.exiledradio.gamedropsbackport.babies.SoundVariants;
import com.exiledradio.gamedropsbackport.babies.Trumpet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Stream 1: golden dandelion, age lock, undead horses, name tags, trades, sound variants, trumpet. */
public final class BabiesSelfTest implements SelfTest {

    private static final String M = GameDropsBackport.MOD_ID;

    @Override
    public String name() {
        return "babies";
    }

    @Override
    public void run(Check check) throws Exception {
        WorldServer world = check.world;
        BabiesSelfTest.world = world;
        if (!check.ok(BabiesContent.goldenDandelionItem != null, "golden dandelion registered")) {
            return;
        }
        registration(check);
        recipes(check);
        trades(check);
        BlockPos base = world.getTopSolidOrLiquidBlock(world.getSpawnPoint()).add(8, 0, 8);
        flowerAndPot(check, world, base);
        ageLock(check, world, base);
        undeadHorses(check, world, base);
        soundVariants(check, world, base);
        trumpet(check);
    }

    private static void registration(Check check) {
        check.eq(BabiesContent.goldenDandelion, ForgeRegistries.BLOCKS.getValue(new ResourceLocation(M, "golden_dandelion")), "block");
        check.ok(ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(M, "potted_golden_dandelion")), "potted block");
        int sounds = 0;
        for (String s : new String[]{"item.golden_dandelion.use", "item.golden_dandelion.unuse", "block.note_block.trumpet",
                "block.note_block.trumpet_oxidized", "entity.pig_mini.ambient", "entity.pig_big.eat", "entity.cow_moody.step",
                "entity.chicken_picky.hurt", "entity.cat_royal.purreow", "entity.baby_cat.hiss", "entity.baby_horse.land",
                "entity.baby_wolf.whine", "entity.baby_pig.step", "entity.baby_chicken.step", "entity.pig.eat"}) {
            if (check.ok(ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(M, s)), "sound " + s)) {
                sounds++;
            }
        }
        check.eq(15, sounds, "sounds found");
        check.eq(100, Blocks.FIRE.getFlammability(BabiesContent.goldenDandelion), "flammability");
        check.eq(60, Blocks.FIRE.getEncouragement(BabiesContent.goldenDandelion), "fire encouragement");
    }

    // ------------------------------------------------------------------------------------------

    private static void recipes(Check check) {
        ItemStack n = new ItemStack(Items.GOLD_NUGGET);
        ItemStack d = new ItemStack(Blocks.YELLOW_FLOWER, 1, BlockFlower.EnumFlowerType.DANDELION.getMeta());
        ItemStack out = craft(3, n, n, n, n, d, n, n, n, n);
        check.ok(out.getItem() == BabiesContent.goldenDandelionItem, "golden dandelion recipe: " + out);
        ItemStack poppy = new ItemStack(Blocks.RED_FLOWER);
        check.ok(craft(3, n, n, n, n, poppy, n, n, n, n).isEmpty(), "golden dandelion needs a dandelion");
        ItemStack dye = craft(3, new ItemStack(BabiesContent.goldenDandelionItem));
        check.ok(dye.getItem() == Items.DYE && dye.getMetadata() == EnumDyeColor.YELLOW.getDyeDamage(), "yellow dye: " + dye);
        ItemStack paper = new ItemStack(Items.PAPER);
        for (ItemStack nugget : new ItemStack[]{new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.IRON_NUGGET)}) {
            ItemStack tag = craft(2, ItemStack.EMPTY, nugget, paper, ItemStack.EMPTY);
            check.ok(tag.getItem() == Items.NAME_TAG, "name tag from " + nugget.getItem().getRegistryName() + ": " + tag);
        }
        check.ok(craft(2, ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), paper, ItemStack.EMPTY).isEmpty(),
                "name tag needs a nugget");
    }

    private static WorldServer world;

    private static ItemStack craft(int size, ItemStack... grid) {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, size, size);
        for (int i = 0; i < grid.length; i++) {
            inv.setInventorySlotContents(i, grid[i].copy());
        }
        IRecipe r = CraftingManager.findMatchingRecipe(inv, world);
        return r == null ? ItemStack.EMPTY : r.getCraftingResult(inv);
    }

    private static void trades(Check check) {
        boolean dandelion = false;
        boolean nameTag = false;
        for (TradeRegistry.TraderTrade t : TradeRegistry.traderTrades(TradeRegistry.TraderPool.ORDINARY)) {
            MerchantRecipe r = t.offer(new Random(1));
            if (r == null) {
                continue;
            }
            if (r.getItemToSell().getItem() == BabiesContent.goldenDandelionItem) {
                dandelion = r.getItemToBuy().getItem() == Items.EMERALD && r.getItemToBuy().getCount() == 2
                        && r.getMaxTradeUses() == 12;
            }
            if (r.getItemToSell().getItem() == Items.NAME_TAG) {
                nameTag = r.getItemToBuy().getItem() == Items.EMERALD && r.getItemToBuy().getCount() == 1
                        && r.getMaxTradeUses() == 5;
            }
        }
        check.ok(dandelion, "trader: golden dandelion for 2 emeralds, 12 uses");
        check.ok(nameTag, "trader: name tag for 1 emerald, 5 uses");

        net.minecraft.world.storage.loot.LootTable mansion = world.getLootTableManager()
                .getLootTableFromLocation(net.minecraft.world.storage.loot.LootTableList.CHESTS_WOODLAND_MANSION);
        net.minecraft.world.storage.loot.LootPool main = mansion.getPool("main");
        check.ok(main != null && main.getEntry("minecraft:name_tag") == null && main.getEntry("minecraft:lead") != null,
                "no name tag in mansion chests, the lead stays");
    }

    // ------------------------------------------------------------------------------------------

    private static void flowerAndPot(Check check, WorldServer world, BlockPos base) {
        BlockPos p = base.add(0, 0, 0);
        world.setBlockState(p.down(), Blocks.GRASS.getDefaultState());
        world.setBlockToAir(p);
        check.ok(BabiesContent.goldenDandelion.canPlaceBlockAt(world, p), "dandelion on grass");
        world.setBlockState(p.down(), Blocks.STONE.getDefaultState());
        check.ok(!BabiesContent.goldenDandelion.canPlaceBlockAt(world, p), "not on stone");

        world.setBlockState(p, Blocks.FLOWER_POT.getDefaultState());
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(BabiesContent.goldenDandelionItem, 2));
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, p, EnumFacing.UP,
                new Vec3d(0.5, 0.5, 0.5)));
        check.eq(BabiesContent.pottedGoldenDandelion, world.getBlockState(p).getBlock(), "potted");
        check.eq(1, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "one dandelion used");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        world.getBlockState(p).getBlock().onBlockActivated(world, p, world.getBlockState(p), player, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        check.eq(Blocks.FLOWER_POT, world.getBlockState(p).getBlock(), "taken out");
        check.ok(player.getHeldItem(EnumHand.MAIN_HAND).getItem() == BabiesContent.goldenDandelionItem, "given back");
        world.setBlockToAir(p);
    }

    // ------------------------------------------------------------------------------------------

    private static <T extends EntityLiving> T spawn(WorldServer world, T e, BlockPos at) {
        e.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        world.spawnEntity(e);
        return e;
    }

    /** A test baby as Wild Update's tadpole: not EntityAgeable, its age counting up from 0 in onLivingUpdate. */
    static final class TestTadpole extends net.minecraft.entity.EntityCreature
            implements com.exiledradio.backportcore.mechanics.AgeLockable {
        private int age;

        TestTadpole(net.minecraft.world.World world) {
            super(world);
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (!this.world.isRemote) {
                this.age++;
            }
        }

        @Override
        public boolean isBaby() {
            return true;
        }

        @Override
        public int getAge() {
            return this.age;
        }

        @Override
        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public int startAge() {
            return 0;
        }
    }

    /** A test baby as Trails & Tales' snifflet: an EntityAgeable starting at -48000. */
    static final class TestSnifflet extends EntityCow implements com.exiledradio.backportcore.mechanics.AgeLockable {
        TestSnifflet(net.minecraft.world.World world) {
            super(world);
        }

        @Override
        public boolean isBaby() {
            return isChild();
        }

        @Override
        public int getAge() {
            return getGrowingAge();
        }

        @Override
        public void setAge(int age) {
            setGrowingAge(age);
        }

        @Override
        public int startAge() {
            return -48000;
        }
    }

    private static void tick(Entity e, int n) {
        for (int i = 0; i < n; i++) {
            e.onUpdate();
        }
    }

    private static void ageLock(Check check, WorldServer world, BlockPos base) {
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        player.capabilities.isCreativeMode = false;
        player.setPosition(base.getX(), base.getY(), base.getZ());
        EntityCow calf = spawn(world, new EntityCow(world), base.add(2, 0, 0));
        calf.setGrowingAge(-1000);
        ItemStack dandelions = new ItemStack(BabiesContent.goldenDandelionItem, 3);
        player.setHeldItem(EnumHand.MAIN_HAND, dandelions);
        player.interactOn(calf, EnumHand.MAIN_HAND);
        check.ok(AgeLock.isLocked(calf), "calf locked");
        check.eq(-24000, calf.getGrowingAge(), "age reset to the start");
        check.eq(2, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "one dandelion used");
        check.ok(calf.isNoDespawnRequired(), "locked baby is persistent");
        tick(calf, 30);
        check.eq(-24000, calf.getGrowingAge(), "locked age holds");

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
        player.interactOn(calf, EnumHand.MAIN_HAND);
        check.eq(4, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "locked calf refuses wheat");
        check.eq(-24000, calf.getGrowingAge(), "and does not grow");

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(BabiesContent.goldenDandelionItem, 2));
        player.interactOn(calf, EnumHand.MAIN_HAND);
        check.ok(AgeLock.isLocked(calf), "2-second cooldown keeps it locked");
        check.eq(2, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "cooldown uses nothing");
        world.getWorldInfo().setWorldTotalTime(world.getTotalWorldTime() + AgeLock.COOLDOWN_TICKS);
        player.interactOn(calf, EnumHand.MAIN_HAND);
        check.ok(!AgeLock.isLocked(calf), "second dandelion unlocks");
        check.eq(1, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "second dandelion used");
        tick(calf, 10);
        check.eq(-23990, calf.getGrowingAge(), "unlocked calf ages again");

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
        player.interactOn(calf, EnumHand.MAIN_HAND);
        check.eq(3, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "unlocked calf eats wheat");

        // Backport Core's AgeLockable: a baby that is not EntityAgeable (as Wild Update's tadpole, counting up from 0)
        // and an EntityAgeable with its own start age (as Trails & Tales' snifflet, -48000).
        TestTadpole tadpole = spawn(world, new TestTadpole(world), base.add(6, 0, 0));
        tadpole.setAge(500);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(BabiesContent.goldenDandelionItem, 2));
        player.interactOn(tadpole, EnumHand.MAIN_HAND);
        check.ok(AgeLock.isLocked(tadpole), "an AgeLockable mob that is not EntityAgeable is locked");
        check.eq(0, tadpole.getAge(), "back to its own start age");
        tick(tadpole, 20);
        check.eq(0, tadpole.getAge(), "and held there");
        TestSnifflet snifflet = spawn(world, new TestSnifflet(world), base.add(8, 0, 0));
        snifflet.setGrowingAge(-1000);
        player.interactOn(snifflet, EnumHand.MAIN_HAND);
        check.ok(AgeLock.isLocked(snifflet), "an AgeLockable EntityAgeable is locked");
        check.eq(-48000, snifflet.getGrowingAge(), "back to its startAge, not -24000");
        tick(snifflet, 20);
        check.eq(-48000, snifflet.getGrowingAge(), "and held there");
        tadpole.setDead();
        snifflet.setDead();

        // Adults, villagers and undead horses are not locked.
        EntityCow cow = spawn(world, new EntityCow(world), base.add(-2, 0, 0));
        EntityVillager kid = spawn(world, new EntityVillager(world), base.add(0, 0, 2));
        kid.setGrowingAge(-24000);
        EntityZombieHorse foal = spawn(world, new EntityZombieHorse(world), base.add(0, 0, -2));
        foal.setGrowingAge(-24000);
        for (EntityAgeable e : new EntityAgeable[]{cow, kid, foal}) {
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(BabiesContent.goldenDandelionItem, 1));
            player.interactOn(e, EnumHand.MAIN_HAND);
            check.ok(!AgeLock.isLocked(e), "not locked: " + e.getName());
            check.eq(1, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "nothing used on " + e.getName());
        }

        // Creative uses nothing.
        EntityPig piglet = spawn(world, new EntityPig(world), base.add(3, 0, 3));
        piglet.setGrowingAge(-5000);
        player.capabilities.isCreativeMode = true;
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(BabiesContent.goldenDandelionItem, 1));
        player.interactOn(piglet, EnumHand.MAIN_HAND);
        check.ok(AgeLock.isLocked(piglet), "creative locks");
        check.eq(1, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "creative uses nothing");
        player.capabilities.isCreativeMode = false;
        for (Entity e : new Entity[]{calf, cow, kid, foal, piglet}) {
            e.setDead();
        }
    }

    private static void undeadHorses(Check check, WorldServer world, BlockPos base) {
        EntitySkeletonHorse skeleton = spawn(world, new EntitySkeletonHorse(world), base.add(4, 0, 0));
        EntityZombieHorse zombie = spawn(world, new EntityZombieHorse(world), base.add(-4, 0, 0));
        EntityHorse horse = spawn(world, new EntityHorse(world), base.add(0, 0, 4));
        check.ok(!hasPanic(skeleton.tasks), "skeleton horse has no panic task");
        check.ok(!hasPanic(zombie.tasks), "zombie horse has no panic task");
        check.ok(hasPanic(horse.tasks), "horse keeps its panic task");
        skeleton.setGrowingAge(-3000);
        zombie.setGrowingAge(-3000);
        horse.setGrowingAge(-3000);
        tick(skeleton, 20);
        tick(zombie, 20);
        tick(horse, 20);
        check.eq(-3000, skeleton.getGrowingAge(), "skeleton foal never grows");
        check.eq(-3000, zombie.getGrowingAge(), "zombie foal never grows");
        check.eq(-2980, horse.getGrowingAge(), "horse foal grows");
        skeleton.setDead();
        zombie.setDead();
        horse.setDead();
    }

    private static boolean hasPanic(EntityAITasks tasks) {
        for (EntityAITasks.EntityAITaskEntry e : tasks.taskEntries) {
            if (e.action instanceof EntityAIPanic) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------

    private static void soundVariants(Check check, WorldServer world, BlockPos base) {
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 24; i++) {
            EntityPig pig = spawn(world, new EntityPig(world), base.add(0, 0, 0));
            seen.add(pig.getEntityData().getString(SoundVariants.KEY));
            pig.setDead();
        }
        check.ok(seen.size() >= 2 && seen.size() <= 3 && (seen.contains("classic") || seen.contains("mini") || seen.contains("big")),
                "spawned pigs pick variants: " + seen);
        EntityPig bred = new EntityPig(world);
        bred.setGrowingAge(-24000);
        EntityPig a = new EntityPig(world);
        MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(a, a, bred));
        spawn(world, bred, base);
        check.eq("classic", bred.getEntityData().getString(SoundVariants.KEY), "bred piglet is classic");
        bred.setDead();
        EntityPig loaded = new EntityPig(world);
        loaded.addedToChunk = true;
        spawn(world, loaded, base);
        check.eq("classic", loaded.getEntityData().getString(SoundVariants.KEY), "a pig from an old world is classic");
        loaded.setDead();
        EntityMooshroom moo = spawn(world, new EntityMooshroom(world), base);
        check.ok(!moo.getEntityData().hasKey(SoundVariants.KEY), "mooshrooms have no variant");
        moo.setDead();

        EntityPig pig = new EntityPig(world);
        SoundVariants.setVariant(pig, "mini");
        check.eq(M + ":entity.pig_mini.hurt", replaced(pig, "entity.pig.hurt"), "mini pig hurt");
        check.eq(null, replaced(pig, "entity.pig.step"), "mini pig keeps the step");
        pig.setGrowingAge(-100);
        check.eq(M + ":entity.baby_pig.ambient", replaced(pig, "entity.pig.ambient"), "piglet");
        check.eq(M + ":entity.baby_pig.step", replaced(pig, "entity.pig.step"), "piglet step");

        EntityCow cow = new EntityCow(world);
        SoundVariants.setVariant(cow, "moody");
        check.eq(M + ":entity.cow_moody.step", replaced(cow, "entity.cow.step"), "moody cow step");
        check.eq(null, replaced(new EntityMooshroom(world), "entity.cow.ambient"), "mooshroom keeps the cow");

        EntityChicken hen = new EntityChicken(world);
        SoundVariants.setVariant(hen, "picky");
        check.eq(M + ":entity.chicken_picky.ambient", replaced(hen, "entity.chicken.ambient"), "picky chicken");
        check.eq(null, replaced(hen, "entity.chicken.step"), "picky chicken keeps the step");
        hen.setGrowingAge(-100);
        check.eq(M + ":entity.baby_chicken.hurt", replaced(hen, "entity.chicken.hurt"), "chick");

        EntityOcelot cat = new EntityOcelot(world);
        SoundVariants.setVariant(cat, "royal");
        String ocelot = replaced(cat, "entity.cat.hurt");
        // Not ours; Village & Pillage gives the wild ocelot its own 1.14 sounds.
        check.ok(ocelot == null || !ocelot.startsWith(M + ":"), "wild ocelot keeps its sounds: " + ocelot);
        cat.setTamed(true);
        check.eq(M + ":entity.cat_royal.purr", replaced(cat, "entity.cat.purr"), "royal cat");
        cat.setGrowingAge(-100);
        check.eq(M + ":entity.baby_cat.ambient", replaced(cat, "entity.cat.ambient"), "kitten");

        EntityWolf pup = new EntityWolf(world);
        pup.setGrowingAge(-100);
        check.eq(M + ":entity.baby_wolf.growl", replaced(pup, "entity.wolf.growl"), "puppy");
        check.eq(null, replaced(pup, "entity.wolf.shake"), "puppy keeps the shake");
        check.eq(null, replaced(new EntityWolf(world), "entity.wolf.ambient"), "adult wolf unchanged");

        EntityHorse foal = new EntityHorse(world);
        foal.setGrowingAge(-100);
        check.eq(M + ":entity.baby_horse.breathe", replaced(foal, "entity.horse.breathe"), "foal breathes");
        EntityDonkey donkey = new EntityDonkey(world);
        donkey.setGrowingAge(-100);
        check.eq(null, replaced(donkey, "entity.donkey.ambient"), "donkey foal keeps its call");
        check.eq(M + ":entity.baby_horse.step", replaced(donkey, "entity.horse.step"), "donkey foal step");
    }

    private static String replaced(Entity e, String vanilla) {
        net.minecraft.util.SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(vanilla));
        if (sound == null) {
            return "missing " + vanilla;
        }
        net.minecraftforge.event.entity.PlaySoundAtEntityEvent ev =
                new net.minecraftforge.event.entity.PlaySoundAtEntityEvent(e, sound, net.minecraft.util.SoundCategory.NEUTRAL, 1, 1);
        MinecraftForge.EVENT_BUS.post(ev);
        return ev.getSound() == sound ? null : String.valueOf(ev.getSound().getRegistryName());
    }

    // ------------------------------------------------------------------------------------------

    /** Copper is another mod's: four stand-in chains in Core's table show which shapes play the trumpet. */
    private static void trumpet(Check check) {
        Block[] copper = chain("copper_block", "exposed_copper", "weathered_copper", "oxidized_copper");
        Block waxed = new Block(Material.IRON).setRegistryName("gdbtest", "waxed_weathered_copper");
        Oxidation.waxed(copper[2], waxed);
        Block[] slabs = chain("cut_copper_slab", "exposed_cut_copper_slab", "weathered_cut_copper_slab", "oxidized_cut_copper_slab");
        Block[] grates = chain("copper_grate", "exposed_copper_grate", "weathered_copper_grate", "oxidized_copper_grate");
        for (int i = 0; i < 4; i++) {
            check.eq(i, Trumpet.stage(copper[i]), "copper block stage " + i);
            check.eq(i, Trumpet.stage(slabs[i]), "cut copper slab stage " + i);
            check.eq(-1, Trumpet.stage(grates[i]), "grate is no trumpet " + i);
        }
        check.eq(2, Trumpet.stage(waxed), "waxed weathered copper");
        check.eq(-1, Trumpet.stage(Blocks.IRON_BLOCK), "iron block");
    }

    private static Block[] chain(String... names) {
        Block[] b = new Block[names.length];
        for (int i = 0; i < names.length; i++) {
            b[i] = new Block(Material.IRON).setRegistryName("gdbtest", names[i]);
        }
        Oxidation.chain(b);
        return b;
    }
}
