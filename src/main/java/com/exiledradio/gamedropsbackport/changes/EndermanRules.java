package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 26.3's enderman rules that 1.12.2 lacks, in their own handlers (so other mods' enderman rules, Nether Update's
 * among them, stack with these):
 * <ul>
 * <li>{@code teleportTargets}: no landing on a sign (MC-58061; 26.3's {@code #entities_can_teleport_to} holds
 * hanging signs but no standing or wall sign, so the search goes on below it, which here moves the target down to the
 * sign's own block, where 1.12.2's search continues), nor on a waterlogged block (MC-220319, 26.3's
 * {@code canRandomlyTeleportTo}). Runs before {@link Changes#onEnderTeleport}, which then judges the moved target.</li>
 * <li>{@code endermenArrows}: an enderman that dodges an arrow lets it fly on (26.3 pre-release 1), where 1.12.2 sends
 * it back when every try failed and counts it stuck in the enderman when one worked.</li>
 * <li>{@code endermenPlacing}: 26.3's {@code EndermanLeaveBlockGoal.canPlaceBlock}: never onto bedrock, never where an
 * entity (an item frame, a mob, a dropped item) is. The goal is 1.12.2's own otherwise, and Forge's place event can
 * refuse it, as it can a player's placing.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class EndermanRules {

    private static final String VANILLA_PLACE_GOAL = "net.minecraft.entity.monster.EntityEnderman$AIPlaceBlock";

    private EndermanRules() {
    }

    // --- teleport targets ------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEnderTeleport(EnderTeleportEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !(entity instanceof EntityEnderman) || !Changes.on(ModConfig.changes.teleportTargets)) {
            return;
        }
        World world = entity.world;
        BlockPos start = new BlockPos(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        BlockPos ground = landing(world, start);
        if (ground == null) {
            return;
        }
        IBlockState below = world.getBlockState(ground);
        if (isSign(below)) {
            // 26.3 searches on below a sign; 1.12.2's search, started at the sign's block, does the same.
            event.setTargetY(ground.getY());
            ground = landing(world, ground);
            if (ground == null) {
                return;
            }
        }
        if (Waterlogging.hasWater(world, ground)) {
            event.setCanceled(true);
        }
    }

    /**
     * 1.12.2's {@code attemptTeleport} search: the first block below {@code start} that blocks movement, or null when
     * there is none (or the chunk is not loaded: 1.12.2 does not teleport there either).
     */
    static BlockPos landing(World world, BlockPos start) {
        if (!world.isBlockLoaded(start)) {
            return null;
        }
        BlockPos pos = start;
        while (pos.getY() > 0) {
            BlockPos below = pos.down();
            if (world.getBlockState(below).getMaterial().blocksMovement()) {
                return below;
            }
            pos = below;
        }
        return null;
    }

    /** Standing and wall signs, vanilla's and other mods'; hanging signs stay a place to land, as in 26.3. */
    public static boolean isSign(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockStandingSign || block instanceof BlockWallSign;
    }

    // --- arrows ----------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent.Arrow event) {
        RayTraceResult hit = event.getRayTraceResult();
        EntityArrow arrow = event.getArrow();
        if (arrow.world.isRemote || hit == null || !(hit.entityHit instanceof EntityEnderman)
                || !Changes.on(ModConfig.changes.endermenArrows)) {
            return;
        }
        EntityEnderman enderman = (EntityEnderman) hit.entityHit;
        // A riding enderman takes the arrow (endermenRiding, P2): left to that.
        if (enderman.isRiding()) {
            return;
        }
        event.setCanceled(true);
        dodge(enderman, arrow);
    }

    /** 26.3's {@code repeatedlyTryToTeleport} for a projectile: 1.12.2's own (an indirect hit), its result unused. */
    public static void dodge(EntityEnderman enderman, EntityArrow arrow) {
        Entity shooter = arrow.shootingEntity == null ? arrow : arrow.shootingEntity;
        enderman.attackEntityFrom(DamageSource.causeArrowDamage(arrow, shooter), 0.0F);
    }

    // --- placing the carried block ---------------------------------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityEnderman)
                || !Changes.on(ModConfig.changes.endermenPlacing)) {
            return;
        }
        replacePlaceGoal((EntityEnderman) event.getEntity());
    }

    /** Swaps 1.12.2's place-block goal for {@link PlaceBlock}, at its priority. @return whether it was there */
    public static boolean replacePlaceGoal(EntityEnderman enderman) {
        List<EntityAITasks.EntityAITaskEntry> found = new ArrayList<EntityAITasks.EntityAITaskEntry>();
        for (EntityAITasks.EntityAITaskEntry entry : enderman.tasks.taskEntries) {
            if (VANILLA_PLACE_GOAL.equals(entry.action.getClass().getName())) {
                found.add(entry);
            }
        }
        for (EntityAITasks.EntityAITaskEntry entry : found) {
            enderman.tasks.removeTask(entry.action);
            enderman.tasks.addTask(entry.priority, new PlaceBlock(enderman));
        }
        return !found.isEmpty();
    }

    /** 26.3's {@code canPlaceBlock}, with 1.12.2's air, full-cube and can-place checks for its block shapes. */
    public static boolean canPlace(World world, BlockPos pos, IBlockState carried, Entity enderman) {
        IBlockState target = world.getBlockState(pos);
        IBlockState below = world.getBlockState(pos.down());
        return target.getMaterial() == Material.AIR
                && below.getMaterial() != Material.AIR
                && below.getBlock() != Blocks.BEDROCK
                && below.isFullCube()
                && carried.getBlock().canPlaceBlockAt(world, pos)
                && world.getEntitiesWithinAABBExcludingEntity(enderman, new AxisAlignedBB(pos)).isEmpty();
    }

    /**
     * 1.12.2's {@code EntityEnderman.AIPlaceBlock} (1 in 2000 each tick while mob griefing allows, a spot within a
     * block around it), deciding by {@link #canPlace}. Forge's place event refuses it when cancelled.
     */
    public static final class PlaceBlock extends EntityAIBase {
        private final EntityEnderman enderman;

        public PlaceBlock(EntityEnderman enderman) {
            this.enderman = enderman;
        }

        @Override
        public boolean shouldExecute() {
            return this.enderman.getHeldBlockState() != null
                    && ForgeEventFactory.getMobGriefingEvent(this.enderman.world, this.enderman)
                    && this.enderman.getRNG().nextInt(2000) == 0;
        }

        @Override
        public void updateTask() {
            Random random = this.enderman.getRNG();
            World world = this.enderman.world;
            int x = MathHelper.floor(this.enderman.posX - 1.0D + random.nextDouble() * 2.0D);
            int y = MathHelper.floor(this.enderman.posY + random.nextDouble() * 2.0D);
            int z = MathHelper.floor(this.enderman.posZ - 1.0D + random.nextDouble() * 2.0D);
            place(world, new BlockPos(x, y, z));
        }

        /** @return whether the carried block went down at {@code pos} */
        public boolean place(World world, BlockPos pos) {
            IBlockState carried = this.enderman.getHeldBlockState();
            if (carried == null || !canPlace(world, pos, carried, this.enderman)) {
                return false;
            }
            if (ForgeEventFactory.onBlockPlace(this.enderman, new BlockSnapshot(world, pos, carried), EnumFacing.UP).isCanceled()) {
                return false;
            }
            world.setBlockState(pos, carried, 3);
            this.enderman.setHeldBlockState(null);
            return true;
        }
    }
}
