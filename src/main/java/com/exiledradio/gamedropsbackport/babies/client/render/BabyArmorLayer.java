package com.exiledradio.gamedropsbackport.babies.client.render;

import com.exiledradio.gamedropsbackport.babies.client.model.BabyHumanoidModel;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * 26.3's {@code HumanoidArmorLayer} for babies: the baby armor meshes with the {@code humanoid_baby} equipment
 * textures (leather dyed, with its overlay), and 1.12's enchantment glint. Only vanilla armor: babies wearing any
 * other armor are drawn by vanilla's renderer instead (see {@link #isSupported}).
 */
public final class BabyArmorLayer implements LayerRenderer<EntityLivingBase> {

    private static final EntityEquipmentSlot[] ORDER = {
        EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET, EntityEquipmentSlot.HEAD};
    private static final ResourceLocation LEATHER_OVERLAY = BabyRender.tex("equipment/humanoid_baby/leather_overlay");

    private final RenderLivingBase<?> renderer;
    private final Map<EntityEquipmentSlot, BabyHumanoidModel> models = new EnumMap<EntityEquipmentSlot, BabyHumanoidModel>(EntityEquipmentSlot.class);

    public BabyArmorLayer(RenderLivingBase<?> renderer, boolean piglin) {
        this.renderer = renderer;
        for (EntityEquipmentSlot slot : ORDER) {
            models.put(slot, BabyHumanoidModel.armor(slot, piglin));
        }
    }

    /** Vanilla armor in vanilla materials, whose baby textures 26.3 has. */
    private static boolean isVanilla(ItemArmor armor) {
        Item item = armor;
        if (item.getRegistryName() == null || !"minecraft".equals(item.getRegistryName().getNamespace())) {
            return false;
        }
        ItemArmor.ArmorMaterial material = armor.getArmorMaterial();
        return material == ItemArmor.ArmorMaterial.LEATHER || material == ItemArmor.ArmorMaterial.CHAIN
                || material == ItemArmor.ArmorMaterial.IRON || material == ItemArmor.ArmorMaterial.GOLD
                || material == ItemArmor.ArmorMaterial.DIAMOND;
    }

    /** False when the entity wears armor this layer can't draw as 26.3 does (modded armor). */
    public static boolean isSupported(EntityLivingBase entity) {
        for (EntityEquipmentSlot slot : ORDER) {
            ItemStack stack = entity.getItemStackFromSlot(slot);
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                if (armor.getEquipmentSlot() == slot && !isVanilla(armor)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        for (EntityEquipmentSlot slot : ORDER) {
            ItemStack stack = entity.getItemStackFromSlot(slot);
            if (!(stack.getItem() instanceof ItemArmor)) {
                continue;
            }
            ItemArmor armor = (ItemArmor) stack.getItem();
            if (armor.getEquipmentSlot() != slot || !isVanilla(armor)) {
                continue;
            }
            BabyHumanoidModel model = models.get(slot);
            model.setModelAttributes(renderer.getMainModel());
            model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            String name = armor.getArmorMaterial().getName();
            renderer.bindTexture(BabyRender.tex("equipment/humanoid_baby/" + name));
            if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER) {
                int color = armor.getColor(stack);
                GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
                model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                renderer.bindTexture(LEATHER_OVERLAY);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if (stack.hasEffect()) {
                LayerArmorBase.renderEnchantedGlint(renderer, entity, model, limbSwing, limbSwingAmount, partialTicks,
                        ageInTicks, netHeadYaw, headPitch, scale);
            }
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
