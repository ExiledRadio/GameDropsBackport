package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.freeze.FreezeTicks;
import com.exiledradio.gamedropsbackport.frostbite.EntityFrostbite;
import com.exiledradio.gamedropsbackport.frostbite.EntityIceBall;
import com.exiledradio.gamedropsbackport.frostbite.FrostbiteContent;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTable;

/**
 * 26.4 stream 2 with Pre-Release 1's numbers: the frostbite's attributes and equipment, Freezing on Backport Core's
 * freeze ticks (1.5 damage, immunities), the potions and brewing, ice ball damage, drowning into a zombie, spawns,
 * the loot table and Monster Hunter.
 */
final class FrostbiteSelfTest implements SelfTest {

    @Override
    public String name() {
        return "frostbite";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(FrostbiteContent.enabled(), "frostbite stream enabled")) {
            return;
        }
        WorldServer w = check.world;
        BlockPos at = w.getSpawnPoint().add(120, 0, 120);
        at = new BlockPos(at.getX(), 200, at.getZ());
        w.getChunk(at);
        w.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 2);

        check.eq(new ResourceLocation("gamedropsbackport", "frostbite"), EntityList.getKey(EntityFrostbite.class), "frostbite id");
        check.eq(new ResourceLocation("gamedropsbackport", "ice_ball"), EntityList.getKey(EntityIceBall.class), "ice ball id");
        EntityFrostbite f = new EntityFrostbite(w);
        check.ok(Math.abs(f.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() - 24.0D) < 1e-9, "frostbite health 24");
        check.ok(Math.abs(f.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() - 0.21D) < 1e-9, "frostbite speed 0.21");
        check.ok(Math.abs(f.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue() - 3.0D) < 1e-9, "frostbite damage 3");
        check.ok(Math.abs(f.getEntityAttribute(SharedMonsterAttributes.ARMOR).getBaseValue() - 2.0D) < 1e-9, "frostbite armor 2");
        check.ok(Math.abs(f.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getBaseValue() - 35.0D) < 1e-9, "frostbite follow range 35");
        check.ok(!FreezeTicks.canFreeze(f), "frostbite: immune to freezing (undead)");
        f.addPotionEffect(new PotionEffect(FrostbiteContent.freezing, 100));
        check.ok(!f.isPotionActive(FrostbiteContent.freezing), "frostbite: Freezing refused (canBeAffected)");
        int balls = 0;
        for (int i = 0; i < 2000; i++) {
            EntityFrostbite g = new EntityFrostbite(w);
            g.onInitialSpawn(w.getDifficultyForLocation(at), null);
            if (g.getHeldItemMainhand().getItem() == FrostbiteContent.iceBall) {
                balls++;
            }
            check.ok(g.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.MAINHAND).isEmpty()
                    || g.getHeldItemMainhand().getItem() == FrostbiteContent.iceBall, i == 0 ? "frostbite: no sword or shovel" : "frostbite equipment");
        }
        check.ok(balls > 400 && balls < 600, "frostbite: about 25% hold an ice ball (" + balls + "/2000)");
        intervals(check, w);

        // Freezing on a pig: 1 a tick, held, 1.5 damage at full.
        EntityPig pig = new EntityPig(w);
        pig.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        pig.setNoAI(true);
        w.spawnEntity(pig);
        pig.addPotionEffect(new PotionEffect(FrostbiteContent.freezing, 200));
        pig.onUpdate();
        pig.onUpdate();
        check.eq(2, FreezeTicks.ticks(pig), "Freezing adds a freeze tick each tick");
        FreezeTicks.setTicks(pig, 140);
        pig.ticksExisted = 40;
        float hp = pig.getHealth();
        pig.hurtResistantTime = 0;
        pig.onUpdate();
        check.ok(Math.abs(hp - pig.getHealth() - 1.5F) < 1e-4, "Freezing: 1.5 damage every 40 ticks at full (" + hp + " -> " + pig.getHealth() + ")");
        pig.setDead();

        // Potions and brewing.
        check.eq(900, FrostbiteContent.freezingPotion.getEffects().get(0).getDuration(), "Potion of Freezing 900 ticks");
        check.eq(1800, FrostbiteContent.longFreezingPotion.getEffects().get(0).getDuration(), "long Freezing 1800 ticks");
        ItemStack awkward = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.AWKWARD);
        ItemStack brewed = PotionHelper.doReaction(new ItemStack(FrostbiteContent.iceBall), awkward);
        check.ok(PotionUtils.getPotionFromItem(brewed) == FrostbiteContent.freezingPotion, "awkward + ice ball = Freezing");
        ItemStack water = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
        check.ok(PotionUtils.getPotionFromItem(PotionHelper.doReaction(new ItemStack(FrostbiteContent.iceBall), water)) == PotionTypes.MUNDANE,
                "water + ice ball = mundane");
        check.ok(PotionUtils.getPotionFromItem(PotionHelper.doReaction(new ItemStack(Items.REDSTONE), brewed)) == FrostbiteContent.longFreezingPotion,
                "Freezing + redstone = long Freezing");

        // Ice ball: ceil(speed x 1.3); 2 fresh from a player (1.5), 3 from a frostbite (1.6).
        EntityIceBall ball = new EntityIceBall(w, at.getX(), at.getY() + 5, at.getZ());
        ball.motionX = 1.5D;
        check.eq(2, ball.damage(), "ice ball at 1.5: 2 damage");
        ball.motionX = 1.6D;
        check.eq(3, ball.damage(), "ice ball at 1.6: 3 damage");
        check.eq(16, FrostbiteContent.iceBall.getItemStackLimit(new ItemStack(FrostbiteContent.iceBall)), "ice balls stack to 16");

        // Drowning into a zombie: 300 more ticks once the 600 in water are done.
        EntityFrostbite drowning = new EntityFrostbite(w);
        drowning.setPosition(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        NBTTagCompound tag = new NBTTagCompound();
        drowning.writeEntityToNBT(tag);
        tag.setInteger("DrownedConversionTime", 0);
        drowning.readEntityFromNBT(tag);
        w.spawnEntity(drowning);
        drowning.onUpdate();
        check.ok(drowning.isDead, "frostbite: drowned conversion ends in a zombie");
        boolean zombie = false;
        for (EntityZombie z : w.getEntitiesWithinAABB(EntityZombie.class, new net.minecraft.util.math.AxisAlignedBB(at).grow(6))) {
            if (z.getClass() == EntityZombie.class) {
                zombie = true;
                z.setDead();
            }
        }
        check.ok(zombie, "frostbite drowns into a plain zombie, not a drowned");

        // Spawns.
        check.eq(10, weight(Biomes.ICE_PLAINS, EntityFrostbite.class), "snowy plains frostbite 10");
        check.eq(25, weight(Biomes.MUTATED_ICE_FLATS, EntityFrostbite.class), "ice spikes frostbite 25");
        check.eq(25, weight(Biomes.FROZEN_RIVER, EntityFrostbite.class), "frozen river frostbite 25");
        check.eq(25, weight(Biomes.FROZEN_OCEAN, EntityFrostbite.class), "frozen ocean frostbite 25");
        check.eq(95, weight(Biomes.FROZEN_RIVER, EntityStray.class), "frozen river stray 95 (26.4)");
        check.eq(95, weight(Biomes.FROZEN_OCEAN, EntityStray.class), "frozen ocean stray 95 (26.4)");

        // Loot and advancements.
        LootTable table = w.getLootTableManager().getLootTableFromLocation(EntityFrostbite.LOOT);
        check.ok(table != null && table != LootTable.EMPTY_LOOT_TABLE && table.getPool("ice_ball") != null && table.getPool("rotten_flesh") != null,
                "frostbite loot table: rotten flesh and ice balls");
        Advancement adv = w.getMinecraftServer().getAdvancementManager()
                .getAdvancement(new ResourceLocation("gamedropsbackport", "adventure/what_goes_around_comes_around"));
        check.ok(adv != null && adv.getCriteria().containsKey("kill_frostbite_with_ice_ball"), "What Goes Around Comes Around loads");
        Advancement hunter = w.getMinecraftServer().getAdvancementManager().getAdvancement(new ResourceLocation("adventure/kill_a_mob"));
        check.ok(hunter != null && hunter.getCriteria().containsKey("gamedropsbackport:frostbite"), "Monster Hunter counts the frostbite");
        Advancement hunted = w.getMinecraftServer().getAdvancementManager().getAdvancement(new ResourceLocation("adventure/kill_all_mobs"));
        check.ok(hunted != null && hunted.getCriteria().containsKey("gamedropsbackport:frostbite"), "Monsters Hunted needs the frostbite");
    }

    private static void intervals(Check check, WorldServer w) {
        EntityFrostbite f = new EntityFrostbite(w);
        net.minecraft.world.EnumDifficulty old = w.getDifficulty();
        Object[][] want = {{net.minecraft.world.EnumDifficulty.EASY, 60, 60}, {net.minecraft.world.EnumDifficulty.NORMAL, 20, 40},
                {net.minecraft.world.EnumDifficulty.HARD, 15, 30}};
        for (Object[] row : want) {
            w.getWorldInfo().setDifficulty((net.minecraft.world.EnumDifficulty) row[0]);
            check.eq(row[1], f.intervalMin(), "throw interval min " + row[0]);
            check.eq(row[2], f.intervalMax(), "throw interval max " + row[0]);
        }
        w.getWorldInfo().setDifficulty(old);
    }

    private static int weight(Biome biome, Class<?> type) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.MONSTER)) {
            if (e.entityClass == type) {
                return e.itemWeight;
            }
        }
        return 0;
    }
}
