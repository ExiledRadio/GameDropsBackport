package com.exiledradio.gamedropsbackport.babies.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyChickenModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyEquineModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyFelineModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyHumanoidModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyLlamaModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyPolarBearModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyQuadrupedModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyRabbitModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabySheepModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyVillagerModel;
import com.exiledradio.gamedropsbackport.babies.client.model.BabyWolfModel;
import com.exiledradio.gamedropsbackport.babies.client.render.BabyArmorLayer;
import com.exiledradio.gamedropsbackport.babies.client.render.BabyHeadLayer;
import com.exiledradio.gamedropsbackport.babies.client.render.BabyHeldItemLayer;
import com.exiledradio.gamedropsbackport.babies.client.render.BabyRender;
import com.exiledradio.gamedropsbackport.babies.client.render.BabyWrapper;
import com.exiledradio.gamedropsbackport.babies.client.render.OverlayLayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderAbstractHorse;
import net.minecraft.client.renderer.entity.RenderChicken;
import net.minecraft.client.renderer.entity.RenderCow;
import net.minecraft.client.renderer.entity.RenderHorse;
import net.minecraft.client.renderer.entity.RenderHusk;
import net.minecraft.client.renderer.entity.RenderLlama;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderMooshroom;
import net.minecraft.client.renderer.entity.RenderOcelot;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.client.renderer.entity.RenderPigZombie;
import net.minecraft.client.renderer.entity.RenderPolarBear;
import net.minecraft.client.renderer.entity.RenderRabbit;
import net.minecraft.client.renderer.entity.RenderSheep;
import net.minecraft.client.renderer.entity.RenderVillager;
import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.client.renderer.entity.RenderZombie;
import net.minecraft.client.renderer.entity.RenderZombieVillager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 26.1's baby models for 1.12.2's babies, behind Core's {@code VanillaRevisions.babyModels()} (this mod hands it
 * {@code babies.babyModels}): each vanilla renderer below is wrapped
 * by a {@link BabyWrapper} sending babies to a 26.3 baby renderer. Shadows are 26.3's adult radii, which 1.12
 * halves for children as 26.3 does.
 */
public final class BabyRenderers {

    private static final ResourceLocation[] HORSE = horse("horse_white", "horse_creamy", "horse_chestnut", "horse_brown",
            "horse_black", "horse_gray", "horse_darkbrown");
    private static final ResourceLocation[] MARKINGS = horse(null, "horse_markings_white", "horse_markings_whitefield",
            "horse_markings_whitedots", "horse_markings_blackdots");
    private static final ResourceLocation[] LLAMA = {BabyRender.tex("llama/llama_creamy_baby"), BabyRender.tex("llama/llama_white_baby"),
        BabyRender.tex("llama/llama_brown_baby"), BabyRender.tex("llama/llama_gray_baby")};
    private static final ResourceLocation[] CAT = {BabyRender.tex("cat/ocelot_baby"), BabyRender.tex("cat/cat_black_baby"),
        BabyRender.tex("cat/cat_red_baby"), BabyRender.tex("cat/cat_siamese_baby")};

    /** Tricky Trials' renderers for its farm animal variants (no compile dependency: named, and asked by reflection). */
    static final String TT_COW = "com.exiledradio.trickytrialsbackport.animals.client.FarmRenderers$Cow";
    static final String TT_PIG = "com.exiledradio.trickytrialsbackport.animals.client.FarmRenderers$Pig";
    static final String TT_CHICKEN = "com.exiledradio.trickytrialsbackport.animals.client.FarmRenderers$Chicken";
    private static final String TT_FARM_ANIMALS = "com.exiledradio.trickytrialsbackport.animals.FarmAnimals";
    /** 26.3's farm animal variants, in Tricky Trials' {@code FarmVariant} order. */
    static final String[] FARM_VARIANTS = {"temperate", "warm", "cold"};

    private static Method ttVariant;

    private BabyRenderers() {
    }

    /**
     * A calf's, piglet's or chick's texture: its Tricky Trials variant's baby texture when Tricky Trials draws that
     * animal's variants, else the temperate one (1.12.2's one look, as the adult then has).
     */
    static <T extends Entity> BabyRender.Texture<T> farm(Map<Class<? extends Entity>, Render<? extends Entity>> map,
            Class<? extends Entity> cls, String ttRenderer, final String mob) {
        final ResourceLocation[] textures = new ResourceLocation[FARM_VARIANTS.length];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = BabyRender.tex(mob + "/" + mob + "_" + FARM_VARIANTS[i] + "_baby");
        }
        Render<? extends Entity> current = map.get(cls);
        if (current == null || !current.getClass().getName().equals(ttRenderer) || ttVariantMethod() == null) {
            return fixed(textures[0]);
        }
        return new BabyRender.Texture<T>() {
            @Override
            public ResourceLocation get(T entity) {
                return textures[ttVariantIndex(entity)];
            }
        };
    }

    private static Method ttVariantMethod() {
        if (ttVariant == null) {
            try {
                ttVariant = Class.forName(TT_FARM_ANIMALS).getMethod("variant", Entity.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                GameDropsBackport.LOGGER.warn("Baby models: Tricky Trials' FarmAnimals.variant not found; temperate calves, piglets and chicks", e);
                return null;
            }
        }
        return ttVariant;
    }

    /** Tricky Trials' variant of the animal as an index into {@link #FARM_VARIANTS}; temperate if unknown. */
    static int ttVariantIndex(Entity entity) {
        try {
            Object v = ttVariantMethod() == null ? null : ttVariant.invoke(null, entity);
            if (v instanceof Enum) {
                int i = ((Enum<?>) v).ordinal();
                if (i >= 0 && i < FARM_VARIANTS.length) {
                    return i;
                }
            }
        } catch (ReflectiveOperationException e) {
            // fall through: temperate
        }
        return 0;
    }

    private static ResourceLocation[] horse(String... names) {
        ResourceLocation[] out = new ResourceLocation[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = names[i] == null ? null : BabyRender.tex("horse/" + names[i] + "_baby");
        }
        return out;
    }

    private static <T> BabyRender.Texture<T> fixed(final ResourceLocation texture) {
        return new BabyRender.Texture<T>() {
            @Override
            public ResourceLocation get(T entity) {
                return texture;
            }
        };
    }

    /** From client post-init (by reflection from the mod class): wraps vanilla's renderers. */
    public static void install() {
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        Map<Class<? extends Entity>, Render<? extends Entity>> map = rm.entityRenderMap;

        // With Tricky Trials' farm animal variants (1.21.5's, its renderers replace these three), the 26.1 baby
        // model draws every variant: one baby model for all of them, as 26.3's renderers pair it, and the variant's
        // own baby texture.
        wrap(map, EntityChicken.class, RenderChicken.class, TT_CHICKEN,
                new BabyRender<EntityChicken>(rm, new BabyChickenModel(), 0.3F,
                        BabyRenderers.<EntityChicken>farm(map, EntityChicken.class, TT_CHICKEN, "chicken")));
        wrap(map, EntityCow.class, RenderCow.class, TT_COW,
                new BabyRender<EntityCow>(rm, new BabyQuadrupedModel<EntityCow>(BabyQuadrupedModel.cow()), 0.7F,
                        BabyRenderers.<EntityCow>farm(map, EntityCow.class, TT_COW, "cow")));
        // 26.3's MushroomCowMushroomLayer skips babies: no mushrooms.
        wrap(map, EntityMooshroom.class, RenderMooshroom.class,
                new BabyRender<EntityMooshroom>(rm, new BabyQuadrupedModel<EntityMooshroom>(BabyQuadrupedModel.cow()), 0.7F,
                        BabyRenderers.<EntityMooshroom>fixed(BabyRender.tex("cow/mooshroom_red_baby"))));
        // 26.1: saddles no longer render on babies.
        wrap(map, EntityPig.class, RenderPig.class, TT_PIG,
                new BabyRender<EntityPig>(rm, new BabyQuadrupedModel<EntityPig>(BabyQuadrupedModel.pig()), 0.7F,
                        BabyRenderers.<EntityPig>farm(map, EntityPig.class, TT_PIG, "pig")));
        wrap(map, EntitySheep.class, RenderSheep.class, sheep(rm));
        wrap(map, EntityWolf.class, RenderWolf.class, wolf(rm));
        wrap(map, EntityOcelot.class, RenderOcelot.class,
                new BabyRender<EntityOcelot>(rm, new BabyFelineModel(), 0.4F, new BabyRender.Texture<EntityOcelot>() {
                    @Override
                    public ResourceLocation get(EntityOcelot e) {
                        int skin = e.getTameSkin();
                        return CAT[skin >= 0 && skin < CAT.length ? skin : 0];
                    }
                }));
        wrap(map, EntityRabbit.class, RenderRabbit.class,
                new BabyRender<EntityRabbit>(rm, new BabyRabbitModel(), 0.3F, new BabyRender.Texture<EntityRabbit>() {
                    @Override
                    public ResourceLocation get(EntityRabbit e) {
                        return BabyRender.tex("rabbit/rabbit_" + rabbitType(e) + "_baby");
                    }
                }));
        horses(rm, map);
        wrap(map, EntityLlama.class, RenderLlama.class,
                new BabyRender<EntityLlama>(rm, new BabyLlamaModel(), 0.7F, new BabyRender.Texture<EntityLlama>() {
                    @Override
                    public ResourceLocation get(EntityLlama e) {
                        int v = e.getVariant();
                        return LLAMA[v >= 0 && v < LLAMA.length ? v : 0];
                    }
                }));
        wrap(map, EntityPolarBear.class, RenderPolarBear.class,
                new BabyRender<EntityPolarBear>(rm, new BabyPolarBearModel(), 0.9F,
                        BabyRenderers.<EntityPolarBear>fixed(BabyRender.tex("bear/polarbear_baby"))));
        if (Loader.isModLoaded("villagerbackport")) {
            GameDropsBackport.LOGGER.info("Village & Pillage is installed: it owns villagers' rendering, baby villagers left to it");
        } else {
            wrap(map, EntityVillager.class, RenderVillager.class, villager(rm));
        }
        zombies(rm, map);
    }

    private static String rabbitType(EntityRabbit e) {
        if ("Toast".equals(TextFormatting.getTextWithoutFormattingCodes(e.getName()))) {
            return "toast";
        }
        switch (e.getRabbitType()) {
            case 1:
                return "white";
            case 2:
                return "black";
            case 3:
                return "white_splotched";
            case 4:
                return "gold";
            case 5:
                return "salt";
            case 99:
                return "caerbannog";
            default:
                return "brown";
        }
    }

    private static BabyRender<EntitySheep> sheep(RenderManager rm) {
        BabySheepModel model = new BabySheepModel();
        BabyRender<EntitySheep> render = new BabyRender<EntitySheep>(rm, model, 0.7F,
                BabyRenderers.<EntitySheep>fixed(BabyRender.tex("sheep/sheep_baby")));
        final ResourceLocation wool = BabyRender.tex("sheep/sheep_wool_baby");
        // SheepWoolLayer: the baby wool is the body layer again, tinted as 1.12's LayerSheepWool tints it.
        render.addLayer(new OverlayLayer<EntitySheep>(render, model, new OverlayLayer.Overlay<EntitySheep>() {
            @Override
            public ResourceLocation texture(EntitySheep e) {
                return e.getSheared() ? null : wool;
            }

            @Override
            public float[] color(EntitySheep e, float partialTicks) {
                if (e.hasCustomName() && "jeb_".equals(e.getCustomNameTag())) {
                    int i = e.ticksExisted / 25 + e.getEntityId();
                    int n = EnumDyeColor.values().length;
                    float f = ((float) (e.ticksExisted % 25) + partialTicks) / 25.0F;
                    float[] a = EntitySheep.getDyeRgb(EnumDyeColor.byMetadata(i % n));
                    float[] b = EntitySheep.getDyeRgb(EnumDyeColor.byMetadata((i + 1) % n));
                    return new float[]{a[0] * (1.0F - f) + b[0] * f, a[1] * (1.0F - f) + b[1] * f, a[2] * (1.0F - f) + b[2] * f};
                }
                return EntitySheep.getDyeRgb(e.getFleeceColor());
            }
        }, false));
        return render;
    }

    private static BabyRender<EntityWolf> wolf(RenderManager rm) {
        final ResourceLocation wild = BabyRender.tex("wolf/wolf_baby");
        final ResourceLocation tame = BabyRender.tex("wolf/wolf_tame_baby");
        final ResourceLocation angry = BabyRender.tex("wolf/wolf_angry_baby");
        final ResourceLocation collar = BabyRender.tex("wolf/wolf_collar_baby");
        BabyWolfModel model = new BabyWolfModel();
        BabyRender<EntityWolf> render = new BabyRender<EntityWolf>(rm, model, 0.5F, new BabyRender.Texture<EntityWolf>() {
            @Override
            public ResourceLocation get(EntityWolf e) {
                return e.isTamed() ? tame : (e.isAngry() ? angry : wild);
            }
        }) {
            @Override
            public void doRender(EntityWolf e, double x, double y, double z, float yaw, float partialTicks) {
                // RenderWolf: darker while wet.
                if (e.isWolfWet()) {
                    float f = e.getBrightness() * e.getShadingWhileWet(partialTicks);
                    GlStateManager.color(f, f, f);
                }
                super.doRender(e, x, y, z, yaw, partialTicks);
            }
        };
        render.addLayer(new OverlayLayer<EntityWolf>(render, model, new OverlayLayer.Overlay<EntityWolf>() {
            @Override
            public ResourceLocation texture(EntityWolf e) {
                return e.isTamed() ? collar : null;
            }

            @Override
            public float[] color(EntityWolf e, float partialTicks) {
                return e.getCollarColor().getColorComponentValues();
            }
        }, false));
        return render;
    }

    private static void horses(RenderManager rm, Map<Class<? extends Entity>, Render<? extends Entity>> map) {
        // No saddle, armor or chests on babies.
        BabyEquineModel horseModel = BabyEquineModel.horse();
        BabyRender<EntityHorse> horse = new BabyRender<EntityHorse>(rm, horseModel, 0.75F, new BabyRender.Texture<EntityHorse>() {
            @Override
            public ResourceLocation get(EntityHorse e) {
                int color = e.getHorseVariant() & 255;
                return HORSE[color >= 0 && color < HORSE.length ? color : 0];
            }
        });
        // HorseMarkingLayer, translucent.
        horse.addLayer(new OverlayLayer<EntityHorse>(horse, horseModel, new OverlayLayer.Overlay<EntityHorse>() {
            @Override
            public ResourceLocation texture(EntityHorse e) {
                int markings = (e.getHorseVariant() & 65280) >> 8;
                return markings > 0 && markings < MARKINGS.length ? MARKINGS[markings] : null;
            }

            @Override
            public float[] color(EntityHorse e, float partialTicks) {
                return new float[]{1.0F, 1.0F, 1.0F};
            }
        }, true));
        wrap(map, EntityHorse.class, RenderHorse.class, horse);
        wrap(map, EntitySkeletonHorse.class, RenderAbstractHorse.class, new BabyRender<EntitySkeletonHorse>(rm, BabyEquineModel.horse(), 0.75F,
                BabyRenderers.<EntitySkeletonHorse>fixed(BabyRender.tex("horse/horse_skeleton_baby"))));
        wrap(map, EntityZombieHorse.class, RenderAbstractHorse.class, new BabyRender<EntityZombieHorse>(rm, BabyEquineModel.horse(), 0.75F,
                BabyRenderers.<EntityZombieHorse>fixed(BabyRender.tex("horse/horse_zombie_baby"))));
        // DonkeyRenderer: donkey and mule both use BabyDonkeyModel.
        wrap(map, EntityDonkey.class, RenderAbstractHorse.class, new BabyRender<EntityDonkey>(rm, BabyEquineModel.donkey(), 0.75F,
                BabyRenderers.<EntityDonkey>fixed(BabyRender.tex("horse/donkey_baby"))));
        wrap(map, EntityMule.class, RenderAbstractHorse.class, new BabyRender<EntityMule>(rm, BabyEquineModel.donkey(), 0.75F,
                BabyRenderers.<EntityMule>fixed(BabyRender.tex("horse/mule_baby"))));
    }

    private static BabyRender<EntityVillager> villager(RenderManager rm) {
        BabyVillagerModel model = new BabyVillagerModel();
        // VillagerRenderer halves the baby shadow again: 0.5 * 0.5, then 1.12's child half.
        BabyRender<EntityVillager> render = new BabyRender<EntityVillager>(rm, model, 0.25F,
                BabyRenderers.<EntityVillager>fixed(BabyRender.tex("villager/villager_baby")));
        render.addLayer(new BabyHeadLayer(model, -0.1171875F, -0.07421875F, 1.0F, 0.85F));
        // VillagerProfessionLayer: babies show only their type (1.12 has only plains) with the hat, no profession.
        render.addLayer(new OverlayLayer<EntityVillager>(render, model,
                OverlayLayer.<EntityVillager>plain(BabyRender.tex("villager/baby/plains")), false));
        return render;
    }

    private static void zombies(RenderManager rm, Map<Class<? extends Entity>, Render<? extends Entity>> map) {
        wrap(map, EntityZombie.class, RenderZombie.class, zombie(rm, BabyHumanoidModel.zombie(), false,
                BabyRender.tex("zombie/zombie_baby"), 0.0F, 0.0F, 1.0F, 0.75F));
        wrap(map, EntityHusk.class, RenderHusk.class, zombie(rm, BabyHumanoidModel.zombie(), false,
                BabyRender.tex("zombie/husk_baby"), 0.0F, 0.0F, 1.0F, 0.75F));
        // 26.4's baby frostbite: the baby zombie's model (BabyFrostbiteModel adds nothing), when the frostbite stream is on.
        wrap(map, com.exiledradio.gamedropsbackport.frostbite.EntityFrostbite.class,
                com.exiledradio.gamedropsbackport.frostbite.client.RenderFrostbite.class, zombie(rm, BabyHumanoidModel.zombie(), false,
                        BabyRender.tex("zombie/frostbite_baby"), 0.0F, 0.0F, 1.0F, 0.75F));
        BabyHumanoidModel zvModel = BabyHumanoidModel.zombieVillager();
        BabyRender<EntityZombie> zombieVillager = zombie(rm, zvModel, false,
                BabyRender.tex("zombie_villager/zombie_villager_baby"), -0.1171875F, -0.07421875F, 1.0F, 0.85F);
        zombieVillager.addLayer(new OverlayLayer<EntityZombie>(zombieVillager, zvModel,
                OverlayLayer.<EntityZombie>plain(BabyRender.tex("zombie_villager/baby/plains")), false));
        wrap(map, EntityZombieVillager.class, RenderZombieVillager.class, zombieVillager);
        if (Loader.isModLoaded("netherupdatebackport")) {
            GameDropsBackport.LOGGER.info("Nether Update is installed: baby zombie pigmen left to it");
        } else {
            wrap(map, EntityPigZombie.class, RenderPigZombie.class, zombie(rm, BabyHumanoidModel.zombifiedPiglin(), true,
                    BabyRender.tex("piglin/zombified_piglin_baby"), 0.0F, 0.0F, 1.0019531F, 0.9F));
        }
    }

    /** AbstractZombieRenderer / HumanoidMobRenderer: armor, head item, held items; 0.5 shadow. */
    private static BabyRender<EntityZombie> zombie(RenderManager rm, BabyHumanoidModel model, boolean piglin,
            ResourceLocation texture, float headY, float skullY, float headScale, float layerScale) {
        BabyRender<EntityZombie> render = new BabyRender<EntityZombie>(rm, model, 0.5F, BabyRenderers.<EntityZombie>fixed(texture)) {
            @Override
            public boolean handles(EntityZombie e) {
                return BabyArmorLayer.isSupported(e);
            }

            @Override
            protected void applyRotations(EntityZombie e, float ageInTicks, float rotationYaw, float partialTicks) {
                // LivingEntityRenderer.isShaking: a converting zombie villager.
                if (e instanceof EntityZombieVillager && ((EntityZombieVillager) e).isConverting()) {
                    rotationYaw += (float) (Math.cos((float) Math.floor(ageInTicks) * 3.25F) * Math.PI * 0.4F);
                }
                super.applyRotations(e, ageInTicks, rotationYaw, partialTicks);
            }
        };
        render.addLayer(new BabyArmorLayer(render, piglin));
        render.addLayer(new BabyHeadLayer(model, headY, skullY, headScale, layerScale));
        render.addLayer(new BabyHeldItemLayer(model));
        return render;
    }

    /** Wraps a class's renderer if it is still vanilla's own. */
    private static <T extends EntityLiving> void wrap(Map<Class<? extends Entity>, Render<? extends Entity>> map, Class<? extends T> cls,
            Class<?> vanillaClass, BabyRender<? extends T> baby) {
        wrap(map, cls, vanillaClass, null, baby);
    }

    /** Wraps a class's renderer if it is vanilla's own, or the named family renderer this baby renderer knows. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends EntityLiving> void wrap(Map<Class<? extends Entity>, Render<? extends Entity>> map, Class<? extends T> cls,
            Class<?> vanillaClass, String familyClass, BabyRender<? extends T> baby) {
        Render<? extends Entity> current = map.get(cls);
        if (current == null || current.getClass() != vanillaClass
                && (familyClass == null || !current.getClass().getName().equals(familyClass))) {
            GameDropsBackport.LOGGER.info("Baby models: {} has renderer {}, not vanilla's; left as it is", cls.getSimpleName(),
                    current == null ? "none" : current.getClass().getName());
            return;
        }
        map.put(cls, new BabyWrapper((Render) current, (BabyRender) baby));
    }
}
