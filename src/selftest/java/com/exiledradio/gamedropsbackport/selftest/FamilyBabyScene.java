package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.mechanics.AgeLockable;
import com.exiledradio.backportcore.presentation.VanillaRevisions;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.babies.AgeLock;
import com.exiledradio.gamedropsbackport.babies.BabiesContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * The other backports' babies with this mod installed (CORE_REQUESTS stream 1, items 1 and 2), when their dev jars are
 * on the client's classpath: Trails &amp; Tales' camel, sniffer and armadillo, an adult and a baby of each, draw the
 * baby with 26.1's texture (their renderers ask Core's {@code VanillaRevisions.babyModels()}, which this mod turns on),
 * and Wild Update's tadpole, locked with a golden dandelion on the server, is seen locked by the client. Photographed
 * as {@code family_babies.png}. Trails &amp; Tales' wolf variant renderer, which replaces this mod's baby wrapper, draws
 * its own 26.1 puppies: a pale adult and four puppies (pale, ashen, tamed woods, angry striped) are checked for their
 * {@code _baby} textures and baby model, photographed as {@code family_wolves.png}. Without those mods each check is skipped and logged.
 */
final class FamilyBabyScene {

    private static final String TT = "trailsandtalesbackport";
    private static final String WU = "wildupdatebackport";
    /** Trails &amp; Tales' mob -> the file name its baby's 26.1 texture ends with. */
    private static final String[][] TT_MOBS = {{"camel", "camel_baby.png"}, {"sniffer", "snifflet.png"},
            {"armadillo", "armadillo_baby.png"}};

    /**
     * Trails &amp; Tales' wolves (its variant renderer replaces this mod's baby wrapper): variant, baby, tamed, angry,
     * the texture file name expected.
     */
    private static final Object[][] TT_WOLVES = {
        {"PALE", false, false, false, "wolf.png"},
        {"PALE", true, false, false, "wolf_baby.png"},
        {"ASHEN", true, false, false, "wolf_ashen_baby.png"},
        {"WOODS", true, true, false, "wolf_woods_tame_baby.png"},
        {"STRIPED", true, false, true, "wolf_striped_angry_baby.png"}};

    private final ClientSelfTest t;
    /** Trails &amp; Tales' wolves on the server: UUID -> its row of {@link #TT_WOLVES}. */
    private final java.util.Map<java.util.UUID, Object[]> ttWolves = new java.util.HashMap<java.util.UUID, Object[]>();
    private final Minecraft mc;
    private final List<Entity> spawned = new ArrayList<Entity>();

    FamilyBabyScene(ClientSelfTest t, Minecraft mc) {
        this.t = t;
        this.mc = mc;
    }

    private WorldServer server() {
        return this.mc.getIntegratedServer().worlds[0];
    }

    private EntityPlayerMP serverPlayer() {
        return this.mc.getIntegratedServer().getPlayerList().getPlayerByUUID(this.mc.player.getUniqueID());
    }

    private Entity spawn(WorldServer w, String modid, String name, boolean baby, double x, double z) {
        Entity e = EntityList.createEntityByIDFromName(new ResourceLocation(modid, name), w);
        if (e == null) {
            return null;
        }
        // Three-quarters toward the camera (south of it, looking north-east): without AI the body keeps the yaw it
        // is given.
        e.setLocationAndAngles(x, 4.0D, z, 150.0F, 0.0F);
        if (e instanceof net.minecraft.entity.EntityLivingBase) {
            net.minecraft.entity.EntityLivingBase l = (net.minecraft.entity.EntityLivingBase) e;
            l.renderYawOffset = l.prevRenderYawOffset = l.rotationYawHead = l.prevRotationYawHead = 150.0F;
        }
        if (e instanceof EntityLiving) {
            ((EntityLiving) e).setNoAI(true);
            ((EntityLiving) e).enablePersistence();
        }
        if (e instanceof EntityAgeable) {
            ((EntityAgeable) e).setGrowingAge(baby ? -24000 : 0);
        }
        w.spawnEntity(e);
        this.spawned.add(e);
        return e;
    }

    /** A Trails &amp; Tales wolf of the given variant (set through its WolfData before it joins the world). */
    private EntityWolf spawnWolf(WorldServer w, EntityPlayerMP owner, Object[] row, double x, double z, EntityWolf target) {
        EntityWolf wolf = new EntityWolf(w);
        // Three-quarters toward a camera north of them, looking south.
        wolf.setLocationAndAngles(x, 4.0D, z, 30.0F, 0.0F);
        wolf.renderYawOffset = wolf.prevRenderYawOffset = wolf.rotationYawHead = wolf.prevRotationYawHead = 30.0F;
        wolf.setNoAI(true);
        wolf.enablePersistence();
        wolf.setGrowingAge((Boolean) row[1] ? -24000 : 0);
        if ((Boolean) row[2]) {
            wolf.setTamed(true);
            wolf.setOwnerId(owner.getUniqueID());
        }
        if ((Boolean) row[3]) {
            // 1.12.2's wolf calms down on its next update without a target.
            wolf.setAngry(true);
            wolf.setAttackTarget(target);
        }
        try {
            Class<?> data = Class.forName("com.exiledradio.trailsandtalesbackport.wolf.WolfData");
            Object d = data.getMethod("of", EntityWolf.class).invoke(null, wolf);
            Class<?> variant = Class.forName("com.exiledradio.trailsandtalesbackport.wolf.WolfVariant");
            data.getField("variant").set(d, variant.getMethod("valueOf", String.class).invoke(null, (String) row[0]));
        } catch (Exception ex) {
            GameDropsBackport.LOGGER.warn("CLIENT SELFTEST note: could not set a Trails & Tales wolf variant", ex);
        }
        w.spawnEntity(wolf);
        this.spawned.add(wolf);
        this.ttWolves.put(wolf.getUniqueID(), row);
        return wolf;
    }

    /** @return the class name of the model Trails &amp; Tales' wolf renderer draws this wolf with */
    private static String wolfModel(Render<Entity> r, Entity e) {
        try {
            Object m = r.getClass().getMethod("modelFor", EntityWolf.class).invoke(r, e);
            return m == null ? null : m.getClass().getSimpleName();
        } catch (Exception ex) {
            return "none (" + ex + ")";
        }
    }

    private static ResourceLocation texture(Render<Entity> r, Entity e) {
        try {
            Method m = ReflectionHelper.findMethod(Render.class, "getEntityTexture", "func_110775_a", Entity.class);
            return (ResourceLocation) m.invoke(r, e);
        } catch (Exception ex) {
            return null;
        }
    }

    void add(List<IntSupplier> steps) {
        final boolean tt = Loader.isModLoaded(TT);
        final boolean wu = Loader.isModLoaded(WU);
        if (!tt && !wu) {
            GameDropsBackport.LOGGER.info("CLIENT SELFTEST note: family babies skipped, neither {} nor {} is loaded", TT, WU);
            return;
        }
        steps.add(() -> {
            this.mc.getIntegratedServer().addScheduledTask(() -> {
                WorldServer w = server();
                EntityPlayerMP p = serverPlayer();
                p.connection.setPlayerLocation(60.5D, 4.0D, 0.5D, 0.0F, 15.0F);
                if (tt) {
                    double x = 65.5D;
                    for (String[] mob : TT_MOBS) {
                        spawn(w, TT, mob[0], false, x, 11.5D);
                        spawn(w, TT, mob[0], true, x - 1.0D, 5.5D);
                        x -= 5.0D;
                    }
                    EntityWolf first = null;
                    for (int k = 0; k < TT_WOLVES.length; k++) {
                        EntityWolf wolf = spawnWolf(w, p, TT_WOLVES[k], 64.5D - k * 2.0D, -6.5D, first);
                        first = first == null ? wolf : first;
                    }
                }
                if (wu) {
                    Entity tadpole = spawn(w, WU, "tadpole", true, 61.5D, 3.5D);
                    if (tadpole instanceof EntityLiving && AgeLock.lockable(tadpole)) {
                        AgeLock.toggle((EntityLiving) tadpole, null, new ItemStack(BabiesContent.goldenDandelionItem));
                    }
                }
            });
            return 60;
        });
        steps.add(() -> {
            this.t.ok(VanillaRevisions.babyModels(), "family babies: Core's babyModels() is on with this mod");
            int babies = 0;
            int adults = 0;
            for (Entity e : this.mc.world.loadedEntityList) {
                ResourceLocation id = EntityList.getKey(e);
                if (id == null || !TT.equals(id.getNamespace()) || !(e instanceof EntityAgeable)) {
                    continue;
                }
                for (String[] mob : TT_MOBS) {
                    if (!mob[0].equals(id.getPath())) {
                        continue;
                    }
                    ResourceLocation tex = texture(this.mc.getRenderManager().getEntityRenderObject(e), e);
                    boolean babyTex = tex != null && tex.getPath().endsWith("/" + mob[1]);
                    if (((EntityAgeable) e).isChild()) {
                        babies++;
                        this.t.ok(babyTex, "family babies: baby " + id + " draws 26.1's " + mob[1] + ", " + tex);
                    } else {
                        adults++;
                        this.t.ok(!babyTex && tex != null, "family babies: adult " + id + " keeps its own texture, " + tex);
                    }
                }
            }
            int wolves = 0;
            for (Entity e : this.mc.world.loadedEntityList) {
                Object[] row = this.ttWolves.get(e.getUniqueID());
                if (row == null || !(e instanceof EntityWolf)) {
                    continue;
                }
                wolves++;
                Render<Entity> r = this.mc.getRenderManager().getEntityRenderObject(e);
                ResourceLocation tex = texture(r, e);
                String expected = (String) row[4];
                boolean baby = (Boolean) row[1];
                String what = "family babies: " + (baby ? "puppy " : "adult ") + row[0].toString().toLowerCase(java.util.Locale.ROOT)
                        + ((Boolean) row[2] ? " tamed" : "") + ((Boolean) row[3] ? " angry" : "");
                // 1.12.2 has no baby textures: every puppy's (pale's too) is Trails & Tales' own copy of 26.3's.
                String ns = baby ? TT : "minecraft";
                this.t.ok(tex != null && ns.equals(tex.getNamespace()) && tex.getPath().endsWith("/" + expected),
                        what + " draws " + ns + "'s " + expected + ", " + tex);
                String model = wolfModel(r, e);
                this.t.ok(baby == "ModelBabyWolf".equals(model), what + (baby ? " uses 26.1's baby model, " : " keeps 1.12.2's model, ") + model);
            }
            if (tt) {
                // 1.12.2's reader drops a gray PNG's tRNS: the collar would cover the whole puppy in its dye color.
                ResourceLocation collar = new ResourceLocation(TT, "textures/entity/wolf/wolf_collar_baby.png");
                int alpha = -1;
                try (net.minecraft.client.resources.IResource res = this.mc.getResourceManager().getResource(collar)) {
                    alpha = net.minecraft.client.renderer.texture.TextureUtil.readBufferedImage(res.getInputStream()).getRGB(0, 0) >>> 24;
                } catch (java.io.IOException ex) {
                    GameDropsBackport.LOGGER.warn("CLIENT SELFTEST note: could not read " + collar, ex);
                }
                this.t.ok(alpha == 0, "family babies: Trails & Tales' baby collar is clear outside the collar, alpha " + alpha);
                this.t.ok(wolves == TT_WOLVES.length, "family babies: " + TT_WOLVES.length + " Trails & Tales wolves on the client, " + wolves);
                this.t.ok(babies == 3 && adults == 3, "family babies: 3 babies and 3 adults of Trails & Tales on the client, "
                        + babies + " and " + adults);
            }
            if (wu) {
                boolean seen = false;
                for (Entity e : this.mc.world.loadedEntityList) {
                    ResourceLocation id = EntityList.getKey(e);
                    if (id != null && WU.equals(id.getNamespace()) && "tadpole".equals(id.getPath())) {
                        seen = true;
                        this.t.ok(e instanceof AgeLockable && AgeLockable.isLocked(e), "family babies: the client sees the tadpole locked");
                    }
                }
                this.t.ok(seen, "family babies: the tadpole is on the client");
                // The server's tadpole (read from here: a plain int, after 60 ticks locked).
                for (Entity e : this.spawned) {
                    if (e instanceof AgeLockable && !(e instanceof EntityAgeable)) {
                        this.t.ok(((AgeLockable) e).getAge() == 0, "family babies: the locked tadpole stayed at age 0, "
                                + ((AgeLockable) e).getAge());
                    }
                }
            }
            this.t.shot("family_babies.png");
            if (tt) {
                this.mc.getIntegratedServer().addScheduledTask(() -> serverPlayer().connection.setPlayerLocation(60.5D, 4.0D, -1.5D, 180.0F, 20.0F));
            }
            return 20;
        });
        if (tt) {
            steps.add(() -> {
                this.t.shot("family_wolves.png");
                return 5;
            });
        }
        steps.add(() -> {
            this.mc.getIntegratedServer().addScheduledTask(() -> {
                for (Entity e : this.spawned) {
                    e.setDead();
                }
            });
            return 20;
        });
    }
}
