package com.exiledradio.gamedropsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * The changes stream's patches (docs/PATCHES.md, P1-P5). Each one calls {@code changes.ChangeHooks}, which keeps 1.12.2's
 * behavior while its setting is off. Inserted code loads method parameters only at a method's head (the ProGuard
 * frames rule); the rest replace one instruction with a static call of the same stack shape.
 *
 * <ul>
 *   <li><b>P1</b> {@code BlockBed.onLanded}: the bounce constant 0.66 becomes {@code ChangeHooks.bedBounce()}.</li>
 *   <li><b>P2</b> {@code EntityEnderman.attackEntityFrom}: at the head, {@code if (endermanTakesProjectile(this, source))
 *       return super.attackEntityFrom(source, amount);}.</li>
 *   <li><b>P3</b> {@code EntityLiving.despawnEntity}: at the head, {@code if (persistentDespawn(this)) return;}.</li>
 *   <li><b>P4</b> {@code EntityWitch.attackEntityWithRangedAttack}: its {@code EntityPotion.shoot} becomes
 *       {@code ChangeHooks.witchShoot}.</li>
 *   <li><b>P5</b> {@code EntityZombie.onLivingUpdate}, {@code AbstractSkeleton.onLivingUpdate}: the helmet's
 *       {@code setItemDamage} becomes {@code ChangeHooks.sunDamage}.</li>
 *   <li><b>P6</b> {@code NetHandlerPlayServer.processChatMessage}: at the head, {@code SpamCounters.chatPacket(this,
 *       packet)} (never returns); the spam limit 200 becomes {@code SpamCounters.spamLimit(this)}.</li>
 * </ul>
 */
public final class ChangesTransformer implements IClassTransformer {

    static final String HOOKS = "com/exiledradio/gamedropsbackport/changes/ChangeHooks";

    static final String BED = "net.minecraft.block.BlockBed";
    static final String ENDERMAN = "net.minecraft.entity.monster.EntityEnderman";
    static final String LIVING = "net.minecraft.entity.EntityLiving";
    static final String WITCH = "net.minecraft.entity.monster.EntityWitch";
    static final String ZOMBIE = "net.minecraft.entity.monster.EntityZombie";
    static final String SKELETON = "net.minecraft.entity.monster.AbstractSkeleton";
    static final String NET_PLAY = "net.minecraft.network.NetHandlerPlayServer";
    static final String SPAM = "com/exiledradio/gamedropsbackport/changes/SpamCounters";

    /** For PatchCheck: every class patched here (all on both sides). */
    static final String[] TARGETS = {BED, ENDERMAN, LIVING, WITCH, ZOMBIE, SKELETON, NET_PLAY};

    /** Set once each patch is applied; the self-test reads them. */
    public static volatile boolean appliedP1;
    public static volatile boolean appliedP2;
    public static volatile boolean appliedP3;
    public static volatile boolean appliedP4;
    public static volatile int appliedP5;
    public static volatile boolean appliedP6;

    private static final String D_DAMAGE = "Lnet/minecraft/util/DamageSource;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            if (BED.equals(transformedName)) {
                return bed(bytes);
            }
            if (ENDERMAN.equals(transformedName)) {
                return enderman(bytes);
            }
            if (LIVING.equals(transformedName)) {
                return living(bytes);
            }
            if (WITCH.equals(transformedName)) {
                return witch(bytes);
            }
            if (ZOMBIE.equals(transformedName) || SKELETON.equals(transformedName)) {
                return sunHelmet(bytes, transformedName);
            }
            if (NET_PLAY.equals(transformedName)) {
                return chat(bytes);
            }
        } catch (RuntimeException e) {
            Asm.LOG.error("Changes patch of {} failed; the class is left as it was.", transformedName, e);
        }
        return bytes;
    }

    // P1 -------------------------------------------------------------------------------------------

    private static byte[] bed(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "onLanded", "func_176216_a", "(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)V");
        if (m == null) {
            Asm.LOG.warn("P1 bed bounce: {}.onLanded not found; patch skipped.", BED);
            return bytes;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn instanceof LdcInsnNode && Double.valueOf(0.6600000262260437D).equals(((LdcInsnNode) insn).cst)) {
                m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "bedBounce", "()D", false));
                n++;
            }
        }
        if (n == 0) {
            Asm.LOG.warn("P1 bed bounce: no 0.66 in {}.onLanded; patch skipped.", BED);
            return bytes;
        }
        appliedP1 = true;
        Asm.LOG.info("P1 bed bounce: patched {}.onLanded.", BED);
        return Asm.write(node);
    }

    // P2 -------------------------------------------------------------------------------------------

    private static byte[] enderman(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "attackEntityFrom", "func_70097_a", "(" + D_DAMAGE + "F)Z");
        if (m == null) {
            Asm.LOG.warn("P2 riding endermen: {}.attackEntityFrom not found; patch skipped.", ENDERMAN);
            return bytes;
        }
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 0));
        test.add(new VarInsnNode(Opcodes.ALOAD, 1));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "endermanTakesProjectile",
                "(Lnet/minecraft/entity/monster/EntityEnderman;" + D_DAMAGE + ")Z", false));
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new VarInsnNode(Opcodes.FLOAD, 2));
        // The method's own name (MCP in dev, SRG in a real game) is the super method's.
        body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, m.name, m.desc, false));
        body.add(new InsnNode(Opcodes.IRETURN));
        if (!Asm.ifAtHead(m, test, body)) {
            Asm.LOG.warn("P2 riding endermen: {}.attackEntityFrom starts on a frame; patch skipped.", ENDERMAN);
            return bytes;
        }
        appliedP2 = true;
        Asm.LOG.info("P2 riding endermen: patched {}.attackEntityFrom.", ENDERMAN);
        return Asm.write(node);
    }

    // P3 -------------------------------------------------------------------------------------------

    private static byte[] living(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "despawnEntity", "func_70623_bb", "()V");
        if (m == null) {
            Asm.LOG.warn("P3 persistent mobs idle: {}.despawnEntity not found; patch skipped.", LIVING);
            return bytes;
        }
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 0));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "persistentDespawn",
                "(Lnet/minecraft/entity/EntityLiving;)Z", false));
        InsnList body = new InsnList();
        body.add(new InsnNode(Opcodes.RETURN));
        if (!Asm.ifAtHead(m, test, body)) {
            Asm.LOG.warn("P3 persistent mobs idle: {}.despawnEntity starts on a frame; patch skipped.", LIVING);
            return bytes;
        }
        appliedP3 = true;
        Asm.LOG.info("P3 persistent mobs idle: patched {}.despawnEntity.", LIVING);
        return Asm.write(node);
    }

    // P4 -------------------------------------------------------------------------------------------

    private static byte[] witch(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "attackEntityWithRangedAttack", "func_82196_d", "(Lnet/minecraft/entity/EntityLivingBase;F)V");
        if (m == null) {
            Asm.LOG.warn("P4 witch throws: {}.attackEntityWithRangedAttack not found; patch skipped.", WITCH);
            return bytes;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (("shoot".equals(call.name) || "func_70186_c".equals(call.name)) && "(DDDFF)V".equals(call.desc)) {
                    m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "witchShoot",
                            "(Lnet/minecraft/entity/projectile/EntityThrowable;DDDFF)V", false));
                    n++;
                }
            }
        }
        if (n != 1) {
            Asm.LOG.warn("P4 witch throws: {} shoot calls in {}; patch skipped.", n, WITCH);
            return bytes;
        }
        appliedP4 = true;
        Asm.LOG.info("P4 witch throws: patched {}.attackEntityWithRangedAttack.", WITCH);
        return Asm.write(node);
    }

    // P5 -------------------------------------------------------------------------------------------

    private static byte[] sunHelmet(byte[] bytes, String target) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "onLivingUpdate", "func_70636_d", "()V");
        if (m == null) {
            Asm.LOG.warn("P5 sun helmet: {}.onLivingUpdate not found; patch skipped.", target);
            return bytes;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("net/minecraft/item/ItemStack".equals(call.owner)
                        && ("setItemDamage".equals(call.name) || "func_77964_b".equals(call.name)) && "(I)V".equals(call.desc)) {
                    m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "sunDamage",
                            "(Lnet/minecraft/item/ItemStack;I)V", false));
                    n++;
                }
            }
        }
        if (n != 1) {
            Asm.LOG.warn("P5 sun helmet: {} setItemDamage calls in {}.onLivingUpdate; patch skipped.", n, target);
            return bytes;
        }
        appliedP5++;
        Asm.LOG.info("P5 sun helmet: patched {}.onLivingUpdate.", target);
        return Asm.write(node);
    }

    // P6 -------------------------------------------------------------------------------------------

    private static byte[] chat(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        String packet = "Lnet/minecraft/network/play/client/CPacketChatMessage;";
        MethodNode m = Asm.method(node, "processChatMessage", "func_147354_a", "(" + packet + ")V");
        if (m == null) {
            Asm.LOG.warn("P6 spam counters: {}.processChatMessage not found; patch skipped.", NET_PLAY);
            return bytes;
        }
        AbstractInsnNode limit = null;
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.SIPUSH && ((org.objectweb.asm.tree.IntInsnNode) insn).operand == 200) {
                limit = insn;
                n++;
            }
        }
        if (n != 1) {
            Asm.LOG.warn("P6 spam counters: {} spam limits in {}.processChatMessage; patch skipped.", n, NET_PLAY);
            return bytes;
        }
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 0));
        test.add(new VarInsnNode(Opcodes.ALOAD, 1));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SPAM, "chatPacket",
                "(Lnet/minecraft/network/NetHandlerPlayServer;" + packet + ")Z", false));
        InsnList body = new InsnList();
        body.add(new InsnNode(Opcodes.RETURN));
        if (!Asm.ifAtHead(m, test, body)) {
            Asm.LOG.warn("P6 spam counters: {}.processChatMessage starts on a frame; patch skipped.", NET_PLAY);
            return bytes;
        }
        // The limit: `this` (slot 0, read again right after) and the hook, in place of the constant; same stack.
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SPAM, "spamLimit", "(Lnet/minecraft/network/NetHandlerPlayServer;)I", false));
        m.instructions.insertBefore(limit, call);
        m.instructions.remove(limit);
        appliedP6 = true;
        Asm.LOG.info("P6 spam counters: patched {}.processChatMessage.", NET_PLAY);
        return Asm.write(node);
    }
}
