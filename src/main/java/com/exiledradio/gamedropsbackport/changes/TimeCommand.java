package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.CommandTime;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 26.1's {@code /time} on world clocks ({@link WorldClocks}), in place of 1.12.2's (same name). Its forms:
 * <pre>
 * time [of &lt;clock&gt;] set &lt;time&gt;|&lt;time marker&gt;
 * time [of &lt;clock&gt;] add &lt;time&gt;
 * time [of &lt;clock&gt;] pause|resume
 * time [of &lt;clock&gt;] rate &lt;rate&gt;
 * time [of &lt;clock&gt;] query time|&lt;timeline&gt; [repetition]
 * time query gametime
 * </pre>
 * {@code set day}, {@code noon}, {@code night} and {@code midnight} are time markers now: the clock goes forward to the
 * next one (an error if it is already there), where 1.12.2 set the time back to that tick of day 0. A time takes
 * 26.x's units ({@code 1d}, {@code 20s}, {@code 5t}). Without {@code of}, the clock is the sender's dimension's
 * ({@code minecraft:overworld}, {@code minecraft:the_end} in the End; the Nether has none). Messages are 26.1's.
 *
 * <p>1.12.2's {@code query daytime} and {@code query day}, gone from 26.1, still answer (as {@code query minecraft:day}
 * and {@code query minecraft:day repetition}) so old command blocks keep working; they are not suggested. While the
 * setting is off, this is 1.12.2's own command.
 */
public final class TimeCommand extends CommandBase {

    private final CommandTime vanilla = new CommandTime();

    @Override
    public String getName() {
        return "time";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return Changes.on(ModConfig.changes.timeCommand) ? "commands.gamedropsbackport.time.usage" : this.vanilla.getUsage(sender);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!Changes.on(ModConfig.changes.timeCommand)) {
            this.vanilla.execute(server, sender, args);
            return;
        }
        WorldClocks clocks = WorldClocks.get(server);
        int i = 0;
        String clock;
        if (args.length >= 2 && "of".equals(args[0])) {
            clock = fullId(args[1]);
            if (clocks.clock(clock) == null) {
                throw new CommandException("argument.resource.not_found", args[1], "minecraft:world_clock");
            }
            i = 2;
        } else {
            if (args.length == 2 && "query".equals(args[0]) && "gametime".equals(args[1])) {
                long t = sender.getEntityWorld().getTotalWorldTime();
                sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, wrap(t));
                sender.sendMessage(new TextComponentTranslation("commands.time.query.gametime", t));
                return;
            }
            clock = defaultClock(sender.getEntityWorld());
        }
        if (args.length <= i) {
            throw new WrongUsageException(getUsage(sender));
        }
        String sub = args[i];
        String[] rest = Arrays.copyOfRange(args, i + 1, args.length);
        WorldClocks.Clock c = clocks.clock(clock);
        long total = clocks.totalTicks(server, clock);
        if ("set".equals(sub) && rest.length == 1) {
            Integer ticks = parseTime(rest[0], 0, true);
            if (ticks != null) {
                if (total == ticks) {
                    throw new CommandException("commands.time.set.already_at_time", clock, ticks);
                }
                clocks.setTotalTicks(server, clock, ticks);
                result(sender, ticks);
                notifyCommandListener(sender, this, "commands.time.set.absolute", clock, ticks);
                return;
            }
            String marker = fullId(rest[0]);
            Integer at = markerTicks(clock, marker);
            if (at == null) {
                throw new CommandException("commands.time.no_time_marker_found", clock, marker);
            }
            if (total % WorldClocks.DAY_PERIOD == at) {
                throw new CommandException("commands.time.set.already_at_time_marker", clock, marker);
            }
            long to = total + durationToNext(total % WorldClocks.DAY_PERIOD, at);
            clocks.setTotalTicks(server, clock, to);
            result(sender, wrap(to));
            notifyCommandListener(sender, this, "commands.time.set.time_marker", clock, marker);
            return;
        }
        if ("add".equals(sub) && rest.length == 1) {
            Integer ticks = parseTime(rest[0], Integer.MIN_VALUE, false);
            long to = Math.max(total + ticks, 0L);
            clocks.setTotalTicks(server, clock, to);
            result(sender, wrap(to));
            notifyCommandListener(sender, this, "commands.time.set.absolute", clock, to);
            return;
        }
        if (("pause".equals(sub) || "resume".equals(sub)) && rest.length == 0) {
            boolean pause = "pause".equals(sub);
            if (c.paused == pause) {
                throw new CommandException(pause ? "commands.time.pause.already_paused" : "commands.time.pause.already_running", clock);
            }
            c.paused = pause;
            clocks.markDirty();
            WorldClocks.sync(server);
            result(sender, 1);
            notifyCommandListener(sender, this, pause ? "commands.time.pause" : "commands.time.resume", clock);
            return;
        }
        if ("rate".equals(sub) && rest.length == 1) {
            float rate = parseFloat(rest[0], 1.0E-5F, 1000.0F);
            if (c.rate == rate) {
                throw new CommandException("commands.time.rate.already_same", clock, rate);
            }
            c.rate = rate;
            clocks.markDirty();
            WorldClocks.sync(server);
            result(sender, 1);
            notifyCommandListener(sender, this, "commands.time.rate", clock, rate);
            return;
        }
        if ("query".equals(sub) && (rest.length == 1 || rest.length == 2)) {
            query(sender, clock, total, rest);
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }

    private void query(ICommandSender sender, String clock, long total, String[] rest) throws CommandException {
        if (rest.length == 1 && "time".equals(rest[0])) {
            result(sender, wrap(total));
            sender.sendMessage(new TextComponentTranslation("commands.time.query.absolute", clock, total));
            return;
        }
        // 1.12.2's forms, as the day timeline.
        boolean repetition = rest.length == 2;
        String timeline;
        if (rest.length == 1 && "daytime".equals(rest[0])) {
            timeline = "minecraft:day";
        } else if (rest.length == 1 && "day".equals(rest[0])) {
            timeline = "minecraft:day";
            repetition = true;
        } else {
            timeline = fullId(rest[0]);
            if (repetition && !"repetition".equals(rest[1])) {
                throw new WrongUsageException(getUsage(sender));
            }
        }
        Integer period = WorldClocks.TIMELINES.get(timeline);
        if (period == null) {
            throw new CommandException("argument.resource.not_found", timeline, "minecraft:timeline");
        }
        if (!WorldClocks.OVERWORLD.equals(clock)) {
            throw new CommandException("commands.time.wrong_timeline_for_clock", timeline, clock);
        }
        if (repetition) {
            long n = period == 0 ? 0 : total / period;
            result(sender, wrap(n));
            sender.sendMessage(new TextComponentTranslation("commands.time.query.timeline.repetitions", timeline, n));
        } else {
            long n = period == 0 ? total : total % period;
            result(sender, wrap(n));
            sender.sendMessage(new TextComponentTranslation("commands.time.query.timeline", timeline, n));
        }
    }

    // ------------------------------------------------------------------------------------------

    /** 26.1's {@code DimensionType.defaultClock}: the overworld's clock, the End's own, none for the Nether. */
    static String defaultClock(World world) throws CommandException {
        int dim = world.provider.getDimension();
        if (dim == 1) {
            return WorldClocks.THE_END;
        }
        if (dim == 0 || (dim != -1 && world.provider.isSurfaceWorld())) {
            return WorldClocks.OVERWORLD;
        }
        String name = world.provider.getDimensionType().getName();
        throw new CommandException("commands.time.no_default_clock", name.indexOf(':') < 0 ? "minecraft:" + name : name);
    }

    @Nullable
    static Integer markerTicks(String clock, String marker) {
        if (!WorldClocks.OVERWORLD.equals(clock)) {
            return null;
        }
        for (Object[] m : WorldClocks.DAY_MARKERS) {
            if (m[0].equals(marker)) {
                return (Integer) m[1];
            }
        }
        return null;
    }

    /** {@code ClockTimeMarker.durationToNext}. */
    static long durationToNext(long from, long to) {
        long d = to - from;
        return d > 0 ? d : WorldClocks.DAY_PERIOD + d;
    }

    /**
     * 26.x's {@code TimeArgument}: a number with an optional unit, {@code d} (24000), {@code s} (20) or {@code t}.
     *
     * @param lenient true: a word that is no time gives null (then it is a marker); false: an error
     */
    @Nullable
    static Integer parseTime(String s, int min, boolean lenient) throws CommandException {
        int unit = 1;
        String num = s;
        if (!s.isEmpty()) {
            char last = s.charAt(s.length() - 1);
            if (last == 'd' || last == 's' || last == 't') {
                unit = last == 'd' ? 24000 : last == 's' ? 20 : 1;
                num = s.substring(0, s.length() - 1);
            } else if (Character.isLetter(last) && !lenient) {
                throw new CommandException("argument.time.invalid_unit");
            }
        }
        float f;
        try {
            f = Float.parseFloat(num);
        } catch (NumberFormatException e) {
            if (lenient) {
                return null;
            }
            throw new CommandException("parsing.float.invalid", num);
        }
        int ticks = Math.round(f * unit);
        if (ticks < min) {
            throw new CommandException("argument.time.tick_count_too_low", min, ticks);
        }
        return ticks;
    }

    static float parseFloat(String s, float min, float max) throws CommandException {
        float f;
        try {
            f = Float.parseFloat(s);
        } catch (NumberFormatException e) {
            throw new CommandException("parsing.float.invalid", s);
        }
        if (f < min) {
            throw new CommandException("argument.float.low", min, f);
        }
        if (f > max) {
            throw new CommandException("argument.float.big", max, f);
        }
        return f;
    }

    static String fullId(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        return lower.indexOf(':') < 0 ? "minecraft:" + lower : lower;
    }

    static int wrap(long ticks) {
        return (int) (ticks % 2147483647L);
    }

    private static void result(ICommandSender sender, int value) {
        sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, value);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!Changes.on(ModConfig.changes.timeCommand)) {
            return this.vanilla.getTabCompletions(server, sender, args, targetPos);
        }
        int i = 0;
        String clock = null;
        if (args.length >= 1 && "of".equals(args[0])) {
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, WorldClocks.OVERWORLD, WorldClocks.THE_END);
            }
            clock = fullId(args[1]);
            i = 2;
        }
        int at = args.length - 1 - i;
        if (at == 0) {
            List<String> subs = new ArrayList<String>(Arrays.asList("set", "add", "pause", "resume", "rate", "query"));
            if (i == 0) {
                subs.add("of");
            }
            return getListOfStringsMatchingLastWord(args, subs);
        }
        if (at == 1 && "set".equals(args[i])) {
            if (clock == null) {
                try {
                    clock = defaultClock(sender.getEntityWorld());
                } catch (CommandException e) {
                    return Collections.emptyList();
                }
            }
            return WorldClocks.OVERWORLD.equals(clock)
                    ? getListOfStringsMatchingLastWord(args, "day", "noon", "night", "midnight")
                    : Collections.<String>emptyList();
        }
        if (at == 1 && "query".equals(args[i])) {
            List<String> q = new ArrayList<String>(Arrays.asList("time"));
            if (i == 0) {
                q.add("gametime");
            }
            q.addAll(WorldClocks.TIMELINES.keySet());
            return getListOfStringsMatchingLastWord(args, q);
        }
        if (at == 2 && "query".equals(args[i]) && WorldClocks.TIMELINES.containsKey(fullId(args[i + 1]))) {
            return getListOfStringsMatchingLastWord(args, "repetition");
        }
        return Collections.emptyList();
    }
}
