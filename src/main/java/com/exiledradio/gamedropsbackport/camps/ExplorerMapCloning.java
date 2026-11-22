package com.exiledradio.gamedropsbackport.camps;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 26.3's {@code map_cloning} ({@code crafting_transmute}, input {@code #clonable_maps}, material {@code map} 1 to 8,
 * {@code add_material_count_to_result}) for the sixteen explorer map items: one explorer map and 1-8 empty maps,
 * anywhere in the grid, make that many plus one of the same map, with all its data (the map, its name). 1.12.2's own
 * {@code RecipesMapClone} keeps copying filled maps; it takes only {@code filled_map}.
 */
public final class ExplorerMapCloning extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private static final int MAX_MATERIALS = 8;

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return !getCraftingResult(inv).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack input = ItemStack.EMPTY;
        int materials = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.getItem() instanceof ItemExplorerMap) {
                if (!input.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                input = s;
            } else if (s.getItem() == Items.MAP) {
                if (++materials > MAX_MATERIALS) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (input.isEmpty() || materials < 1) {
            return ItemStack.EMPTY;
        }
        ItemStack out = input.copy();
        out.setCount(materials + 1);
        return out;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return ForgeHooks.defaultRecipeGetRemainingItems(inv);
    }

    /** A special recipe: not in the recipe book, as 1.12.2's map cloning. */
    @Override
    public boolean isDynamic() {
        return true;
    }
}
