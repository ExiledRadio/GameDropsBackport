package com.exiledradio.gamedropsbackport.sulfurcube.client;

/**
 * Where the sulfur cube's held block sits. 26.3's {@code SulfurCubeInnerLayer} translates by (-0.5, -0.518, -0.5) and
 * draws the block, so it is centered in the gel. 1.12.2's {@code BlockModelRenderer.renderModelBrightness} first
 * rotates 90 degrees about Y around the current origin (and never rotates back), which moves a block drawn from that
 * corner one whole block sideways. Like 1.12.2's {@code RenderTNTPrimed}, rotate -90 first and translate to the
 * matching corner (z +0.5), so the two rotations cancel and the block lands where 26.3 puts it.
 *
 * <p>No client classes here, so the self-test can check the math on a dedicated server.
 */
public final class HeldBlockPose {

    /** The yaw applied before the block is drawn. */
    public static final float YAW = -90.0F;
    /** The translation after {@link #YAW}: 26.3's (-0.5, -0.518, -0.5) seen from the rotated frame. */
    public static final float X = -0.5F;
    public static final float Y = -0.518F;
    public static final float Z = 0.5F;
    /** The yaw 1.12.2's {@code renderModelBrightness} applies before drawing the block's quads. */
    public static final float LEGACY_DRAW_YAW = 90.0F;

    private HeldBlockPose() {
    }

    /** Where block-space point (x, y, z) ends up after {@link #YAW}, the translation and the legacy draw yaw. */
    public static double[] place(double x, double y, double z) {
        double[] p = rotateY(LEGACY_DRAW_YAW, x, y, z);
        p = new double[] {p[0] + X, p[1] + Y, p[2] + Z};
        return rotateY(YAW, p[0], p[1], p[2]);
    }

    /** {@code glRotatef(deg, 0, 1, 0)} applied to a point. */
    private static double[] rotateY(float deg, double x, double y, double z) {
        double r = Math.toRadians(deg);
        double c = Math.cos(r);
        double s = Math.sin(r);
        return new double[] {x * c + z * s, y, -x * s + z * c};
    }
}
