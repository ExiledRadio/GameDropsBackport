package com.exiledradio.gamedropsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 26.4's changes to existing blocks (docs/PATCHES.md, P8-P11), each calling {@code changes.DeepFreezeHooks}, which
 * keeps 1.12.2's behavior while its setting is off.
 *
 * <ul>
 *   <li><b>P8</b> {@code BlockSnow.canPlaceBlockAt}: its {@code Blocks.PACKED_ICE} becomes
 *       {@code DeepFreezeHooks.snowRefuses()} (26.4's {@code #cannot_support_snow_layer} lost packed ice).</li>
 *   <li><b>P9</b> {@code BlockMushroom.canBlockStay}: at the head, {@code if (mushroomRule()) return
 *       mushroomStays(world, pos);}; in {@code updateTick} the two {@code canBlockStay} calls (where it spreads to)
 *       become {@code DeepFreezeHooks.mushroomSpreads}, which keeps the light rule.</li>
 *   <li><b>P10</b> {@code BlockBreakable.shouldSideBeRendered} (client): at the head, {@code if (iceFaceHidden(this,
 *       world, pos, side)) return false;} ({@code #face_occluding_ice_blocks}).</li>
 *   <li><b>P11</b> {@code BlockFrostedIce.slightlyMelt}: at the head, {@code frostedIceCracks(world, pos, state)} (the
 *       {@code block_change} game event of 26.4's {@code CrackingIceBlock}).</li>
 * </ul>
 */
public final class DeepFreezeTransformer implements IClassTransformer {

    static final String HOOKS = "com/exiledradio/gamedropsbackport/changes/DeepFreezeHooks";
    static final String SNOW = "net.minecraft.block.BlockSnow";
    static final String MUSHROOM = "net.minecraft.block.BlockMushroom";
    static final String BREAKABLE = "net.minecraft.block.BlockBreakable";
    static final String FROSTED = "net.minecraft.block.BlockFrostedIce";

    static final String[] TARGETS = {SNOW, MUSHROOM, BREAKABLE, FROSTED};

    public static volatile boolean appliedP8;
    public static volatile boolean appliedP9;
    public static volatile boolean appliedP10;
    public static volatile boolean appliedP11;

    private static final String D_WORLD = "Lnet/minecraft/world/World;";
    private static final String D_POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String D_STATE = "Lnet/minecraft/block/state/IBlockState;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            if (SNOW.equals(transformedName)) {
                return snow(bytes);
            }
            if (MUSHROOM.equals(transformedName)) {
                return mushroom(bytes);
            }
            if (BREAKABLE.equals(transformedName)) {
                return breakable(bytes);
            }
            if (FROSTED.equals(transformedName)) {
                return frosted(bytes);
            }
        } catch (RuntimeException e) {
            Asm.LOG.error("26.4 patch of {} failed; the class is left as it was.", transformedName, e);
        }
        return bytes;
    }

    private static byte[] snow(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "canPlaceBlockAt", "func_176196_c", "(" + D_WORLD + D_POS + ")Z");
        if (m == null) {
            Asm.LOG.warn("P8 snow on packed ice: {}.canPlaceBlockAt not found; patch skipped.", SNOW);
            return bytes;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.GETSTATIC && "net/minecraft/init/Blocks".equals(((FieldInsnNode) insn).owner)
                    && ("PACKED_ICE".equals(((FieldInsnNode) insn).name) || "field_150403_cj".equals(((FieldInsnNode) insn).name))) {
                m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "snowRefuses", "()Lnet/minecraft/block/Block;", false));
                n++;
            }
        }
        if (n == 0) {
            Asm.LOG.warn("P8 snow on packed ice: no Blocks.PACKED_ICE in {}.canPlaceBlockAt; patch skipped.", SNOW);
            return bytes;
        }
        appliedP8 = true;
        Asm.LOG.info("P8 snow on packed ice: patched {}.canPlaceBlockAt.", SNOW);
        return Asm.write(node);
    }

    private static byte[] mushroom(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        String stayDesc = "(" + D_WORLD + D_POS + D_STATE + ")Z";
        MethodNode stay = Asm.method(node, "canBlockStay", "func_180671_f", stayDesc);
        MethodNode tick = Asm.method(node, "updateTick", "func_180650_b", "(" + D_WORLD + D_POS + D_STATE + "Ljava/util/Random;)V");
        if (stay == null || tick == null) {
            Asm.LOG.warn("P9 mushrooms: {}.canBlockStay or updateTick not found; patch skipped.", MUSHROOM);
            return bytes;
        }
        InsnList test = new InsnList();
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "mushroomRule", "()Z", false));
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new VarInsnNode(Opcodes.ALOAD, 2));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "mushroomStays", "(" + D_WORLD + D_POS + ")Z", false));
        body.add(new InsnNode(Opcodes.IRETURN));
        int n = 0;
        for (AbstractInsnNode insn : tick.instructions.toArray()) {
            if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKEVIRTUAL && stayDesc.equals(((MethodInsnNode) insn).desc)
                    && ("canBlockStay".equals(((MethodInsnNode) insn).name) || "func_180671_f".equals(((MethodInsnNode) insn).name))) {
                tick.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "mushroomSpreads",
                        "(Lnet/minecraft/block/BlockMushroom;" + D_WORLD + D_POS + D_STATE + ")Z", false));
                n++;
            }
        }
        if (n == 0 || !Asm.ifAtHead(stay, test, body)) {
            Asm.LOG.warn("P9 mushrooms: {} has no canBlockStay call in updateTick or canBlockStay starts on a frame; patch skipped.", MUSHROOM);
            return bytes;
        }
        appliedP9 = true;
        Asm.LOG.info("P9 mushrooms: patched {}.canBlockStay and {} spread checks.", MUSHROOM, n);
        return Asm.write(node);
    }

    private static byte[] breakable(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        String desc = "(" + D_STATE + "Lnet/minecraft/world/IBlockAccess;" + D_POS + "Lnet/minecraft/util/EnumFacing;)Z";
        MethodNode m = Asm.method(node, "shouldSideBeRendered", "func_176225_a", desc);
        if (m == null) {
            // A dedicated server's class has no client method: nothing to do there.
            return bytes;
        }
        InsnList loads = new InsnList();
        loads.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loads.add(new VarInsnNode(Opcodes.ALOAD, 2));
        loads.add(new VarInsnNode(Opcodes.ALOAD, 3));
        loads.add(new VarInsnNode(Opcodes.ALOAD, 4));
        InsnList ret = new InsnList();
        ret.add(new InsnNode(Opcodes.ICONST_0));
        ret.add(new InsnNode(Opcodes.IRETURN));
        Asm.headReturnIf(m, loads, HOOKS, "iceFaceHidden",
                "(Lnet/minecraft/block/Block;Lnet/minecraft/world/IBlockAccess;" + D_POS + "Lnet/minecraft/util/EnumFacing;)Z", ret);
        appliedP10 = true;
        Asm.LOG.info("P10 ice faces: patched {}.shouldSideBeRendered.", BREAKABLE);
        return Asm.write(node);
    }

    private static byte[] frosted(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "slightlyMelt", "func_185681_a", "(" + D_WORLD + D_POS + D_STATE + "Ljava/util/Random;Z)V");
        if (m == null) {
            Asm.LOG.warn("P11 frosted ice: {}.slightlyMelt not found; patch skipped.", FROSTED);
            return bytes;
        }
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 1));
        call.add(new VarInsnNode(Opcodes.ALOAD, 2));
        call.add(new VarInsnNode(Opcodes.ALOAD, 3));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "frostedIceCracks", "(" + D_WORLD + D_POS + D_STATE + ")V", false));
        m.instructions.insert(call);
        appliedP11 = true;
        Asm.LOG.info("P11 frosted ice: patched {}.slightlyMelt.", FROSTED);
        return Asm.write(node);
    }
}
