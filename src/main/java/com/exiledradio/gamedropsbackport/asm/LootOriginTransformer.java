package com.exiledradio.gamedropsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P6 (docs/PATCHES.md): container loot rolls at the container. 26.3's {@code RandomizableContainer.unpackLootTable}
 * gives the loot context the container's position ({@code ORIGIN}); 1.12.2's {@code TileEntityLockableLoot.fillWithLoot}
 * gives none. The method now starts with {@code LootOrigin.enter(this)} (only {@code this} is read, at the head) and
 * calls {@code LootOrigin.exit()} before each return, so a camp table rolled inside it knows the container's block.
 * Elsewhere {@code LootOrigin} is empty and the camp loot falls back to the opening player's block.
 */
public final class LootOriginTransformer implements IClassTransformer {

    static final String TARGET = "net.minecraft.tileentity.TileEntityLockableLoot";
    static final String[] TARGETS = {TARGET};
    private static final String HOOK = "com/exiledradio/gamedropsbackport/camps/LootOrigin";

    /** Set once the patch is applied; the self-test reads it. */
    public static volatile boolean appliedP6;

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        try {
            ClassNode node = Asm.read(bytes);
            MethodNode m = Asm.method(node, "fillWithLoot", "func_184281_d", "(Lnet/minecraft/entity/player/EntityPlayer;)V");
            if (m == null) {
                Asm.LOG.warn("P6 (loot origin): TileEntityLockableLoot.fillWithLoot not found; camp loot rolls at the player.");
                return bytes;
            }
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.RETURN) {
                    m.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "exit", "()V", false));
                }
            }
            InsnList head = new InsnList();
            head.add(new VarInsnNode(Opcodes.ALOAD, 0));
            head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "enter", "(Lnet/minecraft/tileentity/TileEntity;)V", false));
            m.instructions.insert(head);
            appliedP6 = true;
            Asm.LOG.info("P6 (loot origin): TileEntityLockableLoot.fillWithLoot patched.");
            return Asm.write(node);
        } catch (RuntimeException e) {
            Asm.LOG.error("P6 (loot origin) failed; the class is left as it was.", e);
            return bytes;
        }
    }
}
