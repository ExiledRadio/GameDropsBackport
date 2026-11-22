package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.command.AdvancementCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /advancement grant|revoke <targets> only|from|through|until|everything ...} as 26.3 answers it (26.2 and 26.3,
 * MC-123068). {@code targets} is any number of players, not one. The reply counts as 26.3's {@code AdvancementCommands}
 * with its {@code CommandResponseTracker}: one advancement or several ({@code one}/{@code many}), to the one player it
 * changed or to how many players it changed ({@code to.one}/{@code to.many}); "many" counts the advancements asked for.
 * Nothing changed is the error, worded by the players asked for. 26.3's keys and English ("Granted the advancement %s to
 * %s", "Revoked %s advancements from %s players", ...). {@code test} (1.12.2's, gone from 26.x) and everything while the
 * setting is off are 1.12.2's.
 */
public final class AdvancementCountsCommand extends AdvancementCommand {

    enum Mode {
        ONLY("only", false, false),
        THROUGH("through", true, true),
        FROM("from", false, true),
        UNTIL("until", true, false),
        EVERYTHING("everything", true, true);

        final String name;
        final boolean parents;
        final boolean children;

        Mode(String name, boolean parents, boolean children) {
            this.name = name;
            this.parents = parents;
            this.children = children;
        }

        static Mode byName(String name) {
            for (Mode m : values()) {
                if (m.name.equals(name)) {
                    return m;
                }
            }
            return null;
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        boolean grant = args.length > 0 && "grant".equals(args[0]);
        boolean revoke = args.length > 0 && "revoke".equals(args[0]);
        if (!Changes.on(ModConfig.changes.advancementCounts) || !grant && !revoke) {
            super.execute(server, sender, args);
            return;
        }
        String key = "commands.advancement." + args[0];
        if (args.length < 3) {
            throw new CommandException(key + ".usage");
        }
        List<EntityPlayerMP> players = getPlayers(server, sender, args[1]);
        Mode mode = Mode.byName(args[2]);
        if (mode == null) {
            throw new CommandException(key + ".usage");
        }
        if (mode == Mode.EVERYTHING) {
            if (args.length != 3) {
                throw new CommandException(key + ".everything.usage");
            }
            List<Advancement> all = new ArrayList<Advancement>();
            for (Advancement a : server.getAdvancementManager().getAdvancements()) {
                all.add(a);
            }
            perform(sender, players, grant, all);
            return;
        }
        if (args.length < 4) {
            throw new CommandException(key + "." + mode.name + ".usage");
        }
        Advancement advancement = findAdvancement(server, args[3]);
        if (mode == Mode.ONLY && args.length == 5) {
            performCriterion(sender, players, grant, advancement, args[4]);
            return;
        }
        if (args.length != 4) {
            throw new CommandException(key + "." + mode.name + ".usage");
        }
        perform(sender, players, grant, advancements(advancement, mode));
    }

    /** 26.3's {@code getAdvancements}: the parents nearest first, the advancement, then its children depth first. */
    static List<Advancement> advancements(Advancement target, Mode mode) {
        List<Advancement> list = new ArrayList<Advancement>();
        if (mode.parents) {
            for (Advancement parent = target.getParent(); parent != null; parent = parent.getParent()) {
                list.add(parent);
            }
        }
        list.add(target);
        if (mode.children) {
            addChildren(target, list);
        }
        return list;
    }

    private static void addChildren(Advancement parent, List<Advancement> out) {
        for (Advancement child : parent.getChildren()) {
            out.add(child);
            addChildren(child, out);
        }
    }

    /** 26.3's {@code Advancement.name}: the bracketed, colored title with its description on hover, or the id. */
    static ITextComponent name(Advancement advancement) {
        return advancement.getDisplayText().createCopy();
    }

    static boolean perform(EntityPlayerMP player, Advancement advancement, boolean grant) {
        AdvancementProgress progress = player.getAdvancements().getProgress(advancement);
        if (grant) {
            if (progress.isDone()) {
                return false;
            }
            for (String criterion : progress.getRemaningCriteria()) {
                player.getAdvancements().grantCriterion(advancement, criterion);
            }
            return true;
        }
        if (!progress.hasProgress()) {
            return false;
        }
        for (String criterion : progress.getCompletedCriteria()) {
            player.getAdvancements().revokeCriterion(advancement, criterion);
        }
        return true;
    }

    private void perform(ICommandSender sender, List<EntityPlayerMP> players, boolean grant, List<Advancement> advancements)
            throws CommandException {
        String key = grant ? "commands.advancement.grant" : "commands.advancement.revoke";
        int total = 0;
        List<EntityPlayerMP> changed = new ArrayList<EntityPlayerMP>();
        for (EntityPlayerMP player : players) {
            int n = 0;
            for (Advancement advancement : advancements) {
                if (perform(player, advancement, grant)) {
                    n++;
                }
            }
            total += n;
            if (n != 0) {
                changed.add(player);
            }
        }
        boolean one = advancements.size() == 1;
        Object what = one ? name(advancements.get(0)) : (Object) advancements.size();
        String size = one ? ".one" : ".many";
        if (total == 0) {
            if (players.size() == 1) {
                throw new CommandException(key + size + ".to.one.failure", what, players.get(0).getDisplayName());
            }
            throw new CommandException(key + size + ".to.many.failure", what, players.size());
        }
        sender.setCommandStat(CommandResultStats.Type.AFFECTED_ITEMS, total);
        sender.setCommandStat(CommandResultStats.Type.AFFECTED_ENTITIES, changed.size());
        if (changed.size() == 1) {
            notifyCommandListener(sender, this, key + size + ".to.one.success", what, changed.get(0).getDisplayName());
        } else {
            notifyCommandListener(sender, this, key + size + ".to.many.success", what, changed.size());
        }
    }

    private void performCriterion(ICommandSender sender, List<EntityPlayerMP> players, boolean grant, Advancement advancement,
                                  String criterion) throws CommandException {
        if (!advancement.getCriteria().containsKey(criterion)) {
            throw new CommandException("commands.advancement.criterionNotFound", advancement.getId(), criterion);
        }
        String key = grant ? "commands.advancement.grant" : "commands.advancement.revoke";
        List<EntityPlayerMP> changed = new ArrayList<EntityPlayerMP>();
        for (EntityPlayerMP player : players) {
            boolean done = grant ? player.getAdvancements().grantCriterion(advancement, criterion)
                    : player.getAdvancements().revokeCriterion(advancement, criterion);
            if (done) {
                changed.add(player);
            }
        }
        ITextComponent name = name(advancement);
        if (changed.isEmpty()) {
            if (players.size() == 1) {
                throw new CommandException(key + ".criterion.to.one.failure", criterion, name, players.get(0).getDisplayName());
            }
            throw new CommandException(key + ".criterion.to.many.failure", criterion, name, players.size());
        }
        sender.setCommandStat(CommandResultStats.Type.AFFECTED_ENTITIES, changed.size());
        if (changed.size() == 1) {
            notifyCommandListener(sender, this, key + ".criterion.to.one.success", criterion, name, changed.get(0).getDisplayName());
        } else {
            notifyCommandListener(sender, this, key + ".criterion.to.many.success", criterion, name, changed.size());
        }
    }

    /**
     * While on, grant and revoke read their targets themselves (26.3 answers once for all of them); 1.12.2's handler
     * would otherwise run the command once per player a selector matches. {@code test} keeps 1.12.2's.
     */
    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        if (args.length > 1 && ("grant".equals(args[0]) || "revoke".equals(args[0]))
                && Changes.on(ModConfig.changes.advancementCounts)) {
            return false;
        }
        return super.isUsernameIndex(args, index);
    }
}
