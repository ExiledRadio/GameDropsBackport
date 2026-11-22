package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.babies.client.render.BabyWrapper;
import com.exiledradio.gamedropsbackport.poplar.boat.BoatWood;
import com.exiledradio.backportcore.boat.EntityWoodBoat;
import com.exiledradio.gamedropsbackport.poplar.wood.WoodSign;
import com.exiledradio.gamedropsbackport.softblocks.EntityCushion;
import com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCavesContent;
import com.exiledradio.gamedropsbackport.sulfurcube.EntitySulfurCube;
import com.exiledradio.backportcore.worldgen.UndergroundBiomes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The content half of the client self-test: every block state and item of this mod has a baked model with real
 * textures and a name, every item draws in a GUI, every block stands in the world, the sulfur cube, cushion, poplar
 * boat and vanilla's babies render with our renderers, and the sign edit, straw bed sleep and cushion seat work from
 * the player's client.
 */
final class ClientScenes {

    private static final String[] BABIES = {"cow", "pig", "chicken", "sheep", "wolf", "ocelot", "rabbit", "horse", "donkey",
            "mule", "llama", "polar_bear", "mooshroom", "skeleton_horse", "zombie_horse", "zombie", "husk",
            "zombie_villager", "zombie_pigman", "villager"};

    /** Where the client test registers an underground biome: beside the cubes, under the floor. */
    private static final BlockPos UNDER = new BlockPos(-20, 2, 20);

    private final ClientSelfTest t;
    private final Minecraft mc;
    private final List<Entity> spawned = new ArrayList<Entity>();
    private final List<ItemStack> stacks = new ArrayList<ItemStack>();

    ClientScenes(ClientSelfTest t, Minecraft mc) {
        this.t = t;
        this.mc = mc;
    }

    private WorldServer server() {
        return this.mc.getIntegratedServer().worlds[0];
    }

    private EntityPlayerMP serverPlayer() {
        return this.mc.getIntegratedServer().getPlayerList().getPlayerByUUID(this.mc.player.getUniqueID());
    }

    private void onServer(Runnable r) {
        this.mc.getIntegratedServer().addScheduledTask(r);
    }

    private static boolean ours(ResourceLocation id) {
        return id != null && GameDropsBackport.MOD_ID.equals(id.getNamespace());
    }

    private static boolean missingTexture(IBakedModel model) {
        return model.getParticleTexture() == null || model.getParticleTexture().getIconName().contains("missingno");
    }

    void add(List<java.util.function.IntSupplier> steps) {
        // Models and names, from the registries.
        steps.add(() -> {
            IBakedModel missing = this.mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel();
            int blocks = 0;
            int states = 0;
            for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
                if (!ours(block.getRegistryName())) {
                    continue;
                }
                blocks++;
                List<String> bad = new ArrayList<String>();
                for (IBlockState s : block.getBlockState().getValidStates()) {
                    if (s.getRenderType() != EnumBlockRenderType.MODEL) {
                        continue;
                    }
                    states++;
                    IBakedModel m = this.mc.getBlockRendererDispatcher().getModelForState(s);
                    if (m == missing || missingTexture(m)) {
                        bad.add(s.toString());
                    }
                }
                if (!bad.isEmpty()) {
                    this.t.ok(false, "model: " + block.getRegistryName() + " missing for " + bad.size() + " states, e.g. " + bad.get(0));
                }
            }
            this.t.ok(blocks > 50 && states > 300, "models: " + blocks + " blocks, " + states + " block states checked");
            int items = 0;
            for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
                if (!ours(item.getRegistryName())) {
                    continue;
                }
                NonNullList<ItemStack> list = NonNullList.create();
                item.getSubItems(CreativeTabs.SEARCH, list);
                if (list.isEmpty()) {
                    list.add(new ItemStack(item));
                }
                for (ItemStack stack : list) {
                    items++;
                    this.stacks.add(stack);
                    IBakedModel m = this.mc.getRenderItem().getItemModelWithOverrides(stack, null, null);
                    String name = stack.getDisplayName();
                    if (m == missing || missingTexture(m) || name.endsWith(".name") || name.contains(GameDropsBackport.MOD_ID)) {
                        this.t.ok(false, "item: " + item.getRegistryName() + "@" + stack.getMetadata() + " model or name missing (" + name + ")");
                    }
                }
            }
            this.t.ok(items > 100, "items: " + items + " stacks checked");
            this.t.ok(TileEntityRendererDispatcher.instance.getRenderer(WoodSign.Tile.class) != null, "poplar sign renderer");
            this.mc.displayGuiScreen(new ItemGrid(this.stacks));
            return 10;
        });
        steps.add(() -> {
            ItemGrid grid = (ItemGrid) this.mc.currentScreen;
            this.t.ok(grid.drawn > 0 && grid.error == null, "every item draws in a GUI"
                    + (grid.error == null ? "" : ": " + grid.error));
            this.t.shot("items.png");
            this.mc.displayGuiScreen(null);
            return 2;
        });

        // The straw bed, away from the mobs below: sleep at night, no spawn point, the sleep screen.
        steps.add(() -> {
            onServer(() -> {
                WorldServer w = server();
                EntityPlayerMP p = serverPlayer();
                BlockPos pos = new BlockPos(200, 4, 200);
                Block bed = Block.REGISTRY.getObject(new ResourceLocation(GameDropsBackport.MOD_ID, "straw_bed"));
                IBlockState foot = bed.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH);
                w.setBlockState(pos, foot.withProperty(net.minecraft.block.BlockBed.PART, net.minecraft.block.BlockBed.EnumPartType.FOOT));
                w.setBlockState(pos.south(), foot.withProperty(net.minecraft.block.BlockBed.PART, net.minecraft.block.BlockBed.EnumPartType.HEAD));
                p.connection.setPlayerLocation(200.5, 4, 198.5, 0, 30);
                w.setWorldTime(18000);
            });
            return 20;
        });
        // Night is read from the sky light, which the world updates on its next tick.
        steps.add(() -> {
            onServer(() -> {
                WorldServer w = server();
                BlockPos pos = new BlockPos(200, 4, 200);
                Block bed = w.getBlockState(pos).getBlock();
                bed.onBlockActivated(w, pos, w.getBlockState(pos), serverPlayer(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
            });
            return 30;
        });
        steps.add(() -> {
            this.t.ok(this.mc.player.isPlayerSleeping(), "straw bed: the player sleeps");
            this.t.ok(this.mc.currentScreen instanceof GuiSleepMP, "straw bed: the sleep screen");
            this.t.shot("straw_bed.png");
            onServer(() -> {
                EntityPlayerMP p = serverPlayer();
                p.wakeUpPlayer(true, true, false);
                this.t.ok(p.getBedLocation() == null, "straw bed: no spawn point set");
                server().setWorldTime(6000);
            });
            return 20;
        });

        // Every block in the world, a sign to edit, the mobs and their renderers.
        steps.add(() -> {
            this.mc.displayGuiScreen(null);
            onServer(() -> {
                WorldServer w = server();
                EntityPlayerMP p = serverPlayer();
                int i = 0;
                for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
                    if (!ours(block.getRegistryName()) || block.getDefaultState().getRenderType() == EnumBlockRenderType.INVISIBLE
                            || block.getRegistryName().getPath().contains("wall_sign") || block.getRegistryName().getPath().contains("bed")) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(-12 + (i % 25), 4 + (i / 25) * 2, 24);
                    w.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
                    w.setBlockState(pos, block.getDefaultState(), 2);
                    i++;
                }
                BlockPos sign = new BlockPos(0, 4, 4);
                w.setBlockState(sign, Block.REGISTRY.getObject(new ResourceLocation(GameDropsBackport.MOD_ID, "poplar_sign")).getDefaultState());
                for (int k = 0; k < BABIES.length; k++) {
                    Entity e = EntityList.createEntityByIDFromName(new ResourceLocation(BABIES[k]), w);
                    if (e instanceof EntityAgeable) {
                        ((EntityAgeable) e).setGrowingAge(-24000);
                    } else if (e instanceof EntityZombie) {
                        ((EntityZombie) e).setChild(true);
                    }
                    spawn(w, e, -9 + k, 4, 10);
                }
                int[] sizes = {1, 2, 4};
                // The last two hold heads, as only a command can give them (26.3 draws them by its custom head layer).
                ItemStack[] held = {new ItemStack(Blocks.SLIME_BLOCK), new ItemStack(Blocks.TNT), new ItemStack(Blocks.MAGMA), ItemStack.EMPTY,
                        new ItemStack(Items.SKULL, 1, 4), new ItemStack(Items.SKULL, 1, 1)};
                for (int k = 0; k < held.length; k++) {
                    EntitySulfurCube cube = new EntitySulfurCube(w);
                    cube.setCubeSize(sizes[k % sizes.length], true);
                    if (!held[k].isEmpty()) {
                        cube.setBodyItem(held[k]);
                    }
                    spawn(w, cube, -6 + k * 3, 4, 15);
                }
                EntityCushion cushion = new EntityCushion(w);
                cushion.setColor(EnumDyeColor.RED);
                spawn(w, cushion, 6, 4, 8);
                EntityWoodBoat boat = new EntityWoodBoat(w, 8.5, 4, 12.5, BoatWood.POPLAR.core());
                w.spawnEntity(boat);
                this.spawned.add(boat);
                p.connection.setPlayerLocation(0.5, 4, 0.5, 0, 10);
                TileEntity te = w.getTileEntity(sign);
                if (te instanceof TileEntitySign) {
                    p.openEditSign((TileEntitySign) te);
                }
            });
            return 40;
        });
        steps.add(() -> {
            this.t.ok(this.mc.currentScreen instanceof GuiEditSign, "poplar sign: the edit screen opens, " + this.mc.currentScreen);
            this.t.shot("sign_edit.png");
            this.mc.displayGuiScreen(null);
            return 40;
        });
        steps.add(() -> {
            int babies = 0;
            for (Entity e : this.mc.world.loadedEntityList) {
                Render<Entity> r = this.mc.getRenderManager().getEntityRenderObject(e);
                if (e instanceof EntityLiving && ((EntityLiving) e).isChild() && !(e instanceof EntitySulfurCube)) {
                    babies++;
                    this.t.ok(r instanceof BabyWrapper, "baby " + EntityList.getKey(e) + ": our renderer, " + (r == null ? null : r.getClass().getSimpleName()));
                }
            }
            this.t.ok(babies >= BABIES.length, "babies on the client: " + babies + " of " + BABIES.length);
            check(EntitySulfurCube.class, 6, "RenderSulfurCube");
            check(EntityCushion.class, 1, "RenderCushion");
            check(EntityWoodBoat.class, 1, "RenderWoodBoat");
            this.t.shot("entities.png");
            this.mc.player.rotationPitch = 20;
            this.mc.player.rotationYaw = 0;
            return 5;
        });
        // The heads inside their cubes, close up; and Core's underground biomes on this client (F3, grass, water).
        steps.add(() -> {
            onServer(() -> {
                serverPlayer().connection.setPlayerLocation(7.5, 4, 11.0, 0, 18);
                WorldServer w = server();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        UndergroundBiomes.set(w, UNDER.getX() + dx, UNDER.getZ() + dz, 0, 12, SulfurCavesContent.biome);
                    }
                }
                UndergroundBiomes.set(w, 7, 11, 0, 12, SulfurCavesContent.biome);
            });
            return 30;
        });
        steps.add(() -> {
            int heads = 0;
            for (Entity e : this.mc.world.loadedEntityList) {
                if (e instanceof EntitySulfurCube && ((EntitySulfurCube) e).getBodyItem().getItem() == Items.SKULL) {
                    heads++;
                }
            }
            this.t.ok(heads == 2, "cubes holding heads on the client: " + heads);
            this.t.shot("cube_heads.png");
            if (SulfurCavesContent.biome != null) {
                this.t.ok(UndergroundBiomes.at(this.mc.world, UNDER) == SulfurCavesContent.biome,
                        "underground biome spans reach the client: " + UndergroundBiomes.at(this.mc.world, UNDER));
                this.t.ok(UndergroundBiomes.at(this.mc.world, UNDER.up(20)) == null, "not above the span");
                int grass = net.minecraft.world.biome.BiomeColorHelper.getGrassColorAtPos(this.mc.world, UNDER);
                this.t.ok(grass == 0xABA64F, "grass color there is the sulfur caves' (P44): " + Integer.toHexString(grass));
                int water = net.minecraft.world.biome.BiomeColorHelper.getWaterColorAtPos(this.mc.world, UNDER);
                this.t.ok(water == 0x34BF89, "water color there is the sulfur caves': " + Integer.toHexString(water));
                int above = net.minecraft.world.biome.BiomeColorHelper.getGrassColorAtPos(this.mc.world, UNDER.up(20));
                this.t.ok(above != 0xABA64F, "and the column's own above: " + Integer.toHexString(above));
                java.util.List<String> left = new java.util.ArrayList<String>(java.util.Arrays.asList("Biome: Plains"));
                com.exiledradio.backportcore.presentation.client.DebugText.apply(
                        new com.exiledradio.backportcore.presentation.client.DebugText.Lines(left, new java.util.ArrayList<String>(), this.mc, 0F));
                this.t.ok(("Biome: " + SulfurCavesContent.biome.getBiomeName()).equals(left.get(0)), "F3 names it where the player stands: " + left.get(0));
                this.mc.gameSettings.showDebugInfo = true;
            }
            return 10;
        });
        steps.add(() -> {
            this.t.shot("f3_underground.png");
            this.mc.gameSettings.showDebugInfo = false;
            onServer(() -> {
                WorldServer w = server();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        UndergroundBiomes.set(w, UNDER.getX() + dx, UNDER.getZ() + dz, 0, 12, null);
                    }
                }
                UndergroundBiomes.set(w, 7, 11, 0, 12, null);
                serverPlayer().connection.setPlayerLocation(0.5, 4, 0.5, 0, 20);
            });
            return 30;
        });
        steps.add(() -> {
            this.t.shot("blocks.png");
            onServer(() -> {
                for (Entity e : server().loadedEntityList) {
                    if (e instanceof EntityCushion) {
                        e.processInitialInteract(serverPlayer(), EnumHand.MAIN_HAND);
                    }
                }
            });
            return 20;
        });
        steps.add(() -> {
            this.t.ok(this.mc.player.getRidingEntity() instanceof EntityCushion, "cushion: the player sits, " + this.mc.player.getRidingEntity());
            onServer(() -> {
                serverPlayer().dismountRidingEntity();
                for (Entity e : this.spawned) {
                    e.setDead();
                }
            });
            return 20;
        });
    }

    private void spawn(WorldServer w, Entity e, double x, double y, double z) {
        if (e == null) {
            return;
        }
        e.setLocationAndAngles(x + 0.5, y, z + 0.5, 180, 0);
        if (e instanceof EntityLiving) {
            ((EntityLiving) e).setNoAI(true);
            ((EntityLiving) e).enablePersistence();
        }
        w.spawnEntity(e);
        this.spawned.add(e);
    }

    private void check(Class<? extends Entity> cls, int want, String renderer) {
        int n = 0;
        String found = null;
        for (Entity e : this.mc.world.loadedEntityList) {
            if (cls.isInstance(e)) {
                n++;
                Render<Entity> r = this.mc.getRenderManager().getEntityRenderObject(e);
                found = r == null ? null : r.getClass().getSimpleName();
            }
        }
        this.t.ok(n >= want && found != null && (renderer == null || renderer.equals(found)),
                cls.getSimpleName() + ": " + n + " of " + want + " on the client, renderer " + found);
    }

    /** Draws every stack of this mod, a page at a time; a stack that throws is recorded. */
    static final class ItemGrid extends GuiScreen {
        private final List<ItemStack> stacks;
        int drawn;
        String error;

        ItemGrid(List<ItemStack> stacks) {
            this.stacks = stacks;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            RenderHelper.enableGUIStandardItemLighting();
            int per = Math.max(1, (this.width / 18) * (this.height / 18));
            int x = 0;
            int y = 0;
            for (int i = 0; i < this.stacks.size(); i++) {
                ItemStack stack = this.stacks.get(i);
                try {
                    if (i < per) {
                        this.itemRender.renderItemAndEffectIntoGUI(stack, 2 + x * 18, 2 + y * 18);
                        if (++x >= this.width / 18) {
                            x = 0;
                            y++;
                        }
                    } else {
                        // Past the screen: still render once, off screen, so every stack is drawn.
                        this.itemRender.renderItemAndEffectIntoGUI(stack, -100, -100);
                    }
                } catch (Throwable t) {
                    if (this.error == null) {
                        this.error = stack.getItem().getRegistryName() + "@" + stack.getMetadata() + ": " + t;
                    }
                }
            }
            RenderHelper.disableStandardItemLighting();
            this.drawn++;
        }
    }
}
