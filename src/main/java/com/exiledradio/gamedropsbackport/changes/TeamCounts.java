package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code /scoreboard teams join <team> [<members>...]} and {@code /scoreboard teams leave [<members>...]} answering as
 * 26.3's {@code /team join} and {@code /team leave} (26.3: "the result reflects the exact count of entities that have
 * joined or left"). 26.3's {@code CommandResponseTracker}: the one member that changed by name, otherwise how many
 * ("Added %s members to team %s", "Removed %s members from any team"); members already out of any team are not an
 * error, as 1.12.2's "Could not remove" was. An unknown team is 26.3's "Unknown team". Members are read as 1.12.2 reads
 * them (names, UUIDs, selectors; the sender when none is given).
 */
final class TeamCounts {

    private TeamCounts() {
    }

    /** @return true when the command was ours to answer (the original is then canceled) */
    static boolean handle(ICommand command, ICommandSender sender, String[] args) {
        if (args.length < 2 || !"teams".equalsIgnoreCase(args[0]) || !Changes.on(ModConfig.changes.teamJoinCounts)) {
            return false;
        }
        boolean join = "join".equalsIgnoreCase(args[1]);
        boolean leave = "leave".equalsIgnoreCase(args[1]);
        if (!join && !leave || join && args.length < 3) {
            return false; // 1.12.2's usage errors
        }
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return false;
        }
        Scoreboard scoreboard = server.getWorld(0).getScoreboard();
        try {
            ScorePlayerTeam team = null;
            int start = 2;
            if (join) {
                team = scoreboard.getTeam(args[2]);
                if (team == null) {
                    throw new CommandException("team.notFound", args[2]);
                }
                start = 3;
            }
            List<String> members = members(server, sender, args, start);
            List<String> changed = new ArrayList<String>();
            for (String member : members) {
                boolean done;
                if (join) {
                    done = scoreboard.addPlayerToTeam(member, team.getName());
                } else {
                    done = scoreboard.removePlayerFromTeams(member);
                }
                if (done) {
                    changed.add(member);
                }
            }
            sender.setCommandStat(CommandResultStats.Type.AFFECTED_ENTITIES, changed.size());
            if (join) {
                if (changed.size() == 1) {
                    CommandBase.notifyCommandListener(sender, command, "commands.team.join.success.single",
                            feedbackName(server, changed.get(0)), formattedName(team));
                } else {
                    CommandBase.notifyCommandListener(sender, command, "commands.team.join.success.multiple",
                            changed.size(), formattedName(team));
                }
            } else if (changed.size() == 1) {
                CommandBase.notifyCommandListener(sender, command, "commands.team.leave.success.single",
                        feedbackName(server, changed.get(0)));
            } else {
                CommandBase.notifyCommandListener(sender, command, "commands.team.leave.success.multiple", changed.size());
            }
        } catch (CommandException e) {
            TextComponentTranslation error = new TextComponentTranslation(e.getMessage(), e.getErrorObjects());
            error.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(error);
        }
        return true;
    }

    /** 1.12.2's {@code joinTeam} and {@code leaveTeam} reading of the members, in order, each once. */
    static List<String> members(MinecraftServer server, ICommandSender sender, String[] args, int start) throws CommandException {
        Set<String> names = new LinkedHashSet<String>();
        if (sender instanceof EntityPlayer && start == args.length) {
            names.add(CommandBase.getCommandSenderAsPlayer(sender).getName());
            return new ArrayList<String>(names);
        }
        for (int i = start; i < args.length; i++) {
            String word = args[i];
            if (EntitySelector.isSelector(word)) {
                for (Entity entity : CommandBase.getEntityList(server, sender, word)) {
                    names.add(CommandBase.getEntityName(server, sender, entity.getCachedUniqueIdString()));
                }
            } else {
                names.add(CommandBase.getEntityName(server, sender, word));
            }
        }
        return new ArrayList<String>(names);
    }

    /** 26.3's {@code ScoreHolder.getFeedbackDisplayName}: the entity's or player's name, its score name on hover. */
    static ITextComponent feedbackName(MinecraftServer server, String member) {
        ITextComponent name = null;
        EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(member);
        if (player != null) {
            name = player.getDisplayName();
        } else {
            try {
                Entity entity = server.getEntityFromUuid(java.util.UUID.fromString(member));
                if (entity != null) {
                    name = entity.getDisplayName();
                }
            } catch (IllegalArgumentException notUuid) {
                // a plain score holder name
            }
        }
        if (name == null) {
            return new TextComponentString(member);
        }
        name = name.createCopy();
        name.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(member)));
        return name;
    }

    /**
     * 26.3's {@code PlayerTeam.getFormattedDisplayName}: the display name in square brackets in the team's color, the
     * team's name on hover and as its shift-click insertion.
     */
    static ITextComponent formattedName(ScorePlayerTeam team) {
        ITextComponent inner = new TextComponentString(team.getDisplayName());
        inner.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(team.getName())));
        inner.getStyle().setInsertion(team.getName());
        ITextComponent out = new TextComponentString("[").appendSibling(inner).appendText("]");
        if (team.getColor() != TextFormatting.RESET) {
            out.getStyle().setColor(team.getColor());
        }
        return out;
    }
}
