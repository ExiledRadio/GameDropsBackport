package com.exiledradio.gamedropsbackport.poplar;

import com.exiledradio.backportcore.trade.TradeRegistry;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;

import java.util.Random;

/**
 * 26.3's wandering trader trades for poplar, through Backport Core's {@link TradeRegistry} (the trader is Village &amp;
 * Pillage's; without it nothing reads the registry):
 * <ul>
 *   <li>common ({@link TradeRegistry.TraderPool#ORDINARY}): a poplar sapling for 5 emeralds, 8 uses; 3 shelf
 *       mushrooms for 1 emerald, 12 uses;</li>
 *   <li>uncommon ({@link TradeRegistry.TraderPool#SPECIAL}): 8 poplar logs for 1 emerald, 4 uses.</li>
 * </ul>
 * Each gives 1 experience, as {@code villager_trade}'s defaults.
 */
final class PoplarTrades {

    private PoplarTrades() {
    }

    static void init() {
        TradeRegistry.addTraderTrade(TradeRegistry.TraderPool.ORDINARY, sell(PoplarContent.sapling, 1, 5, 8));
        TradeRegistry.addTraderTrade(TradeRegistry.TraderPool.ORDINARY, sell(PoplarContent.shelfMushroom, 3, 1, 12));
        TradeRegistry.addTraderTrade(TradeRegistry.TraderPool.SPECIAL, sell(PoplarContent.set.log, 8, 1, 4));
    }

    private static TradeRegistry.TraderTrade sell(final Block block, final int count, final int emeralds, final int uses) {
        return new TradeRegistry.TraderTrade() {
            @Override
            public boolean available() {
                return block != null;
            }

            @Override
            public MerchantRecipe offer(Random random) {
                return new MerchantRecipe(new ItemStack(Items.EMERALD, emeralds), ItemStack.EMPTY,
                        new ItemStack(block, count), 0, uses);
            }
        };
    }
}
