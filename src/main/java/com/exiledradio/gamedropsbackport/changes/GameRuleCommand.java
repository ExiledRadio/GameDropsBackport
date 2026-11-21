package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandGameRule;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameRules;

/**
 * 1.12.2's {@code /gamerule} with 26.3's messages: "Game rule %s is currently set to %s" for a query, "Game rule %s is
 * now set to %s" for a change (sent to operators, as 26.3's {@code sendSuccess(..., true)}), and the error "Game rule
 * %s is already set to %s" when the value does not change. 1.12.2's list (no arguments) and its errors stay. While the
 * setting is off, 1.12.2's own messages.
 */
public final class GameRuleCommand extends CommandGameRule {

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!Changes.on(ModConfig.changes.gameRuleMessages) || args.length == 0) {
            super.execute(server, sender, args);
            return;
        }
        GameRules rules = server.getWorld(0).getGameRules();
        String rule = args[0];
        if (args.length == 1) {
            if (!rules.hasRule(rule)) {
                throw new CommandException("commands.gamerule.norule", rule);
            }
            String value = rules.getString(rule);
            sender.sendMessage(new TextComponentTranslation("commands.gamerule.query", rule, value));
            sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, rules.getInt(rule));
            return;
        }
        String value = buildString(args, 1);
        if (rules.areSameType(rule, GameRules.ValueType.BOOLEAN_VALUE) && !"true".equals(value) && !"false".equals(value)) {
            throw new CommandException("commands.generic.boolean.invalid", value);
        }
        if (rules.hasRule(rule) && rules.getString(rule).equals(value)) {
            throw new CommandException("commands.gamerule.not_set", rule, value);
        }
        rules.setOrCreateGameRule(rule, value);
        notifyGameRuleChange(rules, rule, server);
        sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, rules.getInt(rule));
        notifyCommandListener(sender, this, "commands.gamerule.set", rule, value);
    }
}
