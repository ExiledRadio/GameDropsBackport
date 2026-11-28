package com.exiledradio.gamedropsbackport.selftest;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes a stream's blocks and items into selftest-states.txt and selftest-items.txt for tools/check_assets.py, from
 * the lists the stream's {@code Reg} keeps (the 26.4 streams each have their own {@code Reg}).
 */
final class RegDump {

    private RegDump() {
    }

    static void dump(Collection<Block> blocks, Collection<Block> multipart, Map<Block, IProperty<?>[]> ignored,
                     Map<Item, String> itemModels) {
        PrintWriter out = SelfTestRunner.statesOut();
        for (Block block : blocks) {
            String kind = multipart.contains(block) ? "multipart" : "variants";
            for (IBlockState state : block.getBlockState().getValidStates()) {
                out.println(block.getRegistryName() + "\t" + variant(state, ignored) + "\t" + kind);
            }
        }
        out.close();
        PrintWriter items = SelfTestRunner.itemsOut();
        for (Map.Entry<Item, String> e : itemModels.entrySet()) {
            ItemStack stack = new ItemStack(e.getKey());
            items.println(e.getKey().getRegistryName() + "\t0\t" + e.getValue() + "\t" + e.getKey().getTranslationKey(stack) + ".name");
        }
        items.close();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static String variant(IBlockState state, Map<Block, IProperty<?>[]> ignoredMap) {
        Map<String, String> sorted = new TreeMap<String, String>();
        IProperty<?>[] ignored = ignoredMap.get(state.getBlock());
        List<IProperty<?>> skip = ignored == null ? Collections.<IProperty<?>>emptyList() : Arrays.asList(ignored);
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            if (!skip.contains(e.getKey())) {
                IProperty p = e.getKey();
                sorted.put(p.getName(), p.getName((Comparable) e.getValue()));
            }
        }
        if (sorted.isEmpty()) {
            return "normal";
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }
}
