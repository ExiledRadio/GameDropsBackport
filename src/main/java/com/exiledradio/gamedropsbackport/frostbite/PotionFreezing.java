package com.exiledradio.gamedropsbackport.frostbite;

import com.exiledradio.backportcore.freeze.FreezeTicks;

import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 26.4's Freezing ({@code FreezingMobEffect}, harmful, color 0x9EEEEE): every tick it adds one to Backport Core's
 * freeze ticks ({@code freezeForTicks(1)}); while it lasts the counter does not thaw and full freezing hurts 1.5
 * ({@code DAMAGE_VALUE}; registered with {@link FreezeTicks#registerEffect}). Everything else (the damage every 40
 * ticks, the slowness, the frost overlay, leather armor, fire) is Core's freeze ticks. Its icon is 26.4's 18x18
 * {@code mob_effect/freezing}, not a cell of 1.12.2's sheet.
 */
public class PotionFreezing extends Potion {

    public static final int COLOR = 0x9EEEEE;
    public static final float DAMAGE_VALUE = 1.5F;

    public PotionFreezing() {
        super(true, COLOR);
        setPotionName("effect.gamedropsbackport.freezing");
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
        if (!entity.world.isRemote) {
            FreezeTicks.freezeFor(entity, 1);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        com.exiledradio.gamedropsbackport.frostbite.client.FrostbiteClient.drawIcon(x + 6, y + 7, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        com.exiledradio.gamedropsbackport.frostbite.client.FrostbiteClient.drawIcon(x + 3, y + 3, alpha);
    }
}
