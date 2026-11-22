package com.exiledradio.gamedropsbackport.sulfurcube;

/**
 * 26.3's twelve sulfur cube archetypes ({@code data/minecraft/sulfur_cube_archetype/*.json}): what a cube becomes while it
 * holds a block. The values are the attribute modifiers' results on a cube's base values, already clamped as 26.3's
 * attribute ranges clamp them: knockback resistance to -2..1 (26.x lowered its minimum, so -1 doubles knockback and -2
 * triples it), explosion knockback resistance and bounciness to 0-1.
 *
 * <p>Friction and air drag work as 26.3's {@code LivingEntity.computeModifiedFriction}: a block's (or the air's) loss
 * of speed per tick is multiplied by the modifier, so 0 keeps all speed and 2 doubles the loss.
 */
public enum Archetype {
    //            kbRes  expRes bounce friction        airDrag          buoyant  hPower  vPower  pushCool pushThreshold
    BOUNCY("bouncy", -2, 0, 0.9, 1 - 0.7, 1 - 0.99, true, 0.4125F, 0.105F, 0.7F, 0.3F),
    EXPLOSIVE("explosive", -1, 0, 0.5, 1 - 0.7, 1 - 0.7, true, 0.4125F, 0.09F, 0.7F, 0.1F),
    FAST_FLAT("fast_flat", -1, 0, 0.5, 1 - 0.8, 1 - 0.99, false, 0.9125F, 0.09F, 0.9F, 0.03F),
    FAST_SLIDING("fast_sliding", 0.5, 0.5, 0.1, 1 - 0.95, 1 - 0.99, false, 0.6625F, 0.09F, 1.0F, 0.05F),
    HIGH_RESISTANCE("high_resistance", 0.7, 0.7, 0.2, 1.0, 1 - 0.99, false, 0.4125F, 0.09F, 0.7F, 0.03F),
    HOT("hot", -1, 0, 0.5, 1 - 0.7, 1 - 0.9, true, 0.4125F, 0.09F, 0.7F, 0.2F),
    LIGHT("light", -1, 0, 1.0, 1 - 0.7, 1 + 0.8, true, 0.4125F, 0.18F, 0.7F, 0.2F),
    REGULAR("regular", -1, 0, 0.5, 1 - 0.7, 1 - 0.9, true, 0.4125F, 0.09F, 0.5F, 0.2F),
    SLOW_BOUNCY("slow_bouncy", 0.4, 0.4, 0.6, 1 - 0.7, 1 - 0.95, false, 0.4125F, 0.24F, 0.5F, 0.05F),
    SLOW_FLAT("slow_flat", 0.5, 0.5, 0.4, 1 - 0.6, 1 - 0.9, false, 0.4125F, 0.105F, 0.9F, 0.03F),
    SLOW_SLIDING("slow_sliding", 0.8, 0.8, 0.1, 1 - 0.95, 1 - 0.99, false, 0.4125F, 0.09F, 1.0F, 0.02F),
    STICKY("sticky", -2, 0, 0.0, 2.0, 1 - 0.99, false, 0.4125F, 0.09F, 0.5F, 0.05F),
    /**
     * Something no archetype names (26.3's tags list no skull, so a skull is only ever in a cube by command): the base
     * attributes ({@code createSulfurCubeAttributes}, Attributes' defaults), not buoyant, the default knockback (0.33,
     * 0.06) and sound settings (regular's sounds, threshold 0.2, cooldown 0.5).
     */
    NONE("none", 0, 0, 0.0, 1.0, 1.0, false, 0.33F, 0.06F, 0.5F, 0.2F);

    /** 26.3's {@code SulfurCubeArchetype.DEFAULT_KNOCKBACK_MODIFIERS} ({@link #NONE}'s). */
    public static final float DEFAULT_H_POWER = 0.33F;
    public static final float DEFAULT_V_POWER = 0.06F;

    /** The explosive archetype's explosion: power 3, no fire, a 120-tick (6 s) fuse. */
    public static final float EXPLOSION_POWER = 3.0F;
    public static final int FUSE = 120;

    public final String id;
    public final double knockbackResistance;
    public final double explosionKnockbackResistance;
    public final double bounciness;
    public final float frictionModifier;
    public final float airDragModifier;
    public final boolean buoyant;
    public final float horizontalPower;
    public final float verticalPower;
    /** seconds */
    public final float pushSoundCooldown;
    public final float pushSoundThreshold;

    Archetype(String id, double kb, double exp, double bounce, double friction, double airDrag, boolean buoyant,
              float hPower, float vPower, float pushCooldown, float pushThreshold) {
        this.id = id;
        this.knockbackResistance = kb;
        this.explosionKnockbackResistance = exp;
        this.bounciness = bounce;
        this.frictionModifier = (float) friction;
        this.airDragModifier = (float) airDrag;
        this.buoyant = buoyant;
        this.horizontalPower = hPower;
        this.verticalPower = vPower;
        this.pushSoundCooldown = pushCooldown;
        this.pushSoundThreshold = pushThreshold;
    }

    public boolean explodes() {
        return this == EXPLOSIVE;
    }

    /** The hot archetype burns what it touches: 1 damage of type {@code sulfurCubeHot}. */
    public boolean hot() {
        return this == HOT;
    }

    /** 26.3's {@code computeModifiedFriction}: {@code 1 - (1 - friction) * modifier}, clamped to 0-1. */
    public static float modifiedFriction(float friction, float modifier) {
        return Math.max(0.0F, Math.min(1.0F, 1.0F - (1.0F - friction) * modifier));
    }
}
