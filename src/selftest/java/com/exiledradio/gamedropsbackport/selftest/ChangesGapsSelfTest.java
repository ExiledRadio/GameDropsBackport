package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.asm.ChangesTransformer;
import com.exiledradio.gamedropsbackport.changes.AdvancementCountsCommand;
import com.exiledradio.gamedropsbackport.changes.HostGameModeCommand;
import com.exiledradio.gamedropsbackport.changes.PeacefulSpawning;
import com.exiledradio.gamedropsbackport.changes.ServerProperties;
import com.exiledradio.gamedropsbackport.changes.SpamCounters;
import com.exiledradio.gamedropsbackport.changes.UnpublishCommand;

import com.mojang.authlib.GameProfile;
import net.minecraft.command.ICommand;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.UUID;

/**
 * The changes stream's later settings: {@code ocelotsPeaceful}, {@code spamThresholds} (P6), {@code hostGameModeDefault},
 * {@code advancementCounts}, {@code teamJoinCounts} and {@code unpublish} on a dedicated server. The host and LAN parts
 * need an integrated server: the client self-test ({@code ClientSelfTest}) has them.
 */
final class ChangesGapsSelfTest {

    private ChangesGapsSelfTest() {
    }

    static void run(Check check, MinecraftServer server, WorldServer world) {
        ocelots(check, world);
        spam(check, server, world);
        host(check, server);
        EntityPlayerMP a = player(world, "GDTestA");
        EntityPlayerMP b = player(world, "GDTestB");
        server.getPlayerList().getPlayers().add(a);
        server.getPlayerList().getPlayers().add(b);
        world.playerEntities.add(a);
        world.playerEntities.add(b);
        try {
            advancements(check, server, world);
            teams(check, server, world);
        } finally {
            server.getPlayerList().getPlayers().remove(a);
            server.getPlayerList().getPlayers().remove(b);
            world.playerEntities.remove(a);
            world.playerEntities.remove(b);
        }
        unpublish(check, server, world);
    }

    /** A real server player with a connection that goes nowhere (Forge grants a FakePlayer no advancements). */
    private static EntityPlayerMP player(WorldServer world, String name) {
        EntityPlayerMP p = new EntityPlayerMP(world.getMinecraftServer(), world,
                new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name), new PlayerInteractionManager(world));
        new NetHandlerPlayServer(world.getMinecraftServer(), new NetworkManager(EnumPacketDirection.SERVERBOUND), p);
        p.setPosition(64.5D, 130D, 64.5D);
        return p;
    }

    private static Object[] args(ChangesSelfTest.Sender s) {
        if (s.messages.isEmpty()) {
            return new Object[0];
        }
        ITextComponent last = s.messages.get(s.messages.size() - 1);
        return last instanceof TextComponentTranslation ? ((TextComponentTranslation) last).getFormatArgs() : new Object[0];
    }

    private static String text(Object o) {
        String t = o instanceof ITextComponent ? ((ITextComponent) o).getUnformattedText() : String.valueOf(o);
        return net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(t);
    }

    // ocelotsPeaceful ------------------------------------------------------------------------------

    private static void ocelots(Check check, WorldServer world) {
        EnumDifficulty was = world.getDifficulty();
        world.getWorldInfo().setDifficulty(EnumDifficulty.PEACEFUL);
        check.ok(PeacefulSpawning.active(world), "on Peaceful the monster pass runs (26.2)");
        ModConfig.changes.ocelotsPeaceful = false;
        check.ok(!PeacefulSpawning.active(world), "setting off: 1.12.2 skips monsters on Peaceful");
        ModConfig.changes.ocelotsPeaceful = true;
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(64.5D, 130D, 64.5D);
        EntityOcelot ocelot = new EntityOcelot(world);
        ocelot.setPosition(64.5D, 130D, 64.5D);
        PeacefulSpawning.inPass = true;
        try {
            check.eq(Event.Result.DENY, ForgeEventFactory.canEntitySpawn(zombie, world, 64.5F, 130F, 64.5F, false),
                    "in the Peaceful pass a zombie is refused");
            check.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(ocelot, world, 64.5F, 130F, 64.5F, false),
                    "in the Peaceful pass an ocelot is left to its own rules (grass or leaves, 2 in 3)");
            check.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(zombie, world, 64.5F, 130F, 64.5F, true),
                    "a monster spawner's zombie is not this pass's");
        } finally {
            PeacefulSpawning.inPass = false;
        }
        check.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(zombie, world, 64.5F, 130F, 64.5F, false),
                "outside the pass nothing is refused");
        int refused = PeacefulSpawning.refused;
        PeacefulSpawning.runPass(world);
        check.ok(!PeacefulSpawning.inPass, "a pass on a world with no player ends cleanly");
        check.ok(PeacefulSpawning.refused >= refused, "the refused count never goes back");
        world.getWorldInfo().setDifficulty(was);
        check.ok(!PeacefulSpawning.active(world) || was == EnumDifficulty.PEACEFUL, "off Peaceful the extra pass does not run");
    }

    // spamThresholds (P6) --------------------------------------------------------------------------

    private static void spam(Check check, MinecraftServer server, WorldServer world) {
        check.ok(ChangesTransformer.appliedP6, "P6 applied (NetHandlerPlayServer.processChatMessage)");
        int[] t = SpamCounters.thresholds();
        check.eq(200, t[0], "chat-spam-threshold-seconds 10 is 200 ticks");
        check.eq(200, t[1], "command-spam-threshold-seconds 10 is 200 ticks");
        if (server.isDedicatedServer()) {
            check.eq(10, ServerProperties.getInt(server, "chat-spam-threshold-seconds", -1),
                    "server.properties has chat-spam-threshold-seconds=10 (26.2)");
            check.eq(10, ServerProperties.getInt(server, "command-spam-threshold-seconds", -1),
                    "server.properties has command-spam-threshold-seconds=10 (26.2)");
        }
        EntityPlayerMP p = player(world, "GDTestSpam");
        NetHandlerPlayServer h = p.connection;
        int now = 1_000_000;
        boolean kicked = false;
        for (int i = 0; i < 9; i++) {
            kicked |= SpamCounters.count(h, false, now);
        }
        check.ok(!kicked, "nine chat messages at once: not kicked");
        for (int i = 0; i < 9; i++) {
            kicked |= SpamCounters.count(h, true, now);
        }
        check.ok(!kicked, "nine commands too: their own counter, not kicked (26.2, MC-302268)");
        check.ok(SpamCounters.count(h, false, now), "the tenth chat message reaches 200: kicked (26.2's count >= threshold)");
        check.eq(200, SpamCounters.countsNow(h, now)[0], "the chat counter is 200");
        check.eq(180, SpamCounters.countsNow(h, now)[1], "the command counter is 180");
        check.eq(150, SpamCounters.countsNow(h, now + 50)[0], "50 ticks later, 50 less");
        check.eq(0, SpamCounters.countsNow(h, now + 1000)[1], "never below 0");
        check.ok(!SpamCounters.count(h, true, now + 1000), "after the counters run down, a command is fine");
        // P6 end to end: the packet itself, on the server thread.
        int before = SpamCounters.countsNow(h, server.getTickCounter())[0];
        h.processChatMessage(new CPacketChatMessage("selftest hello"));
        check.eq(before + 20, SpamCounters.countsNow(h, server.getTickCounter())[0], "a chat packet counts 20 on the chat counter (P6)");
        int commands = SpamCounters.countsNow(h, server.getTickCounter())[1];
        h.processChatMessage(new CPacketChatMessage("/seed"));
        check.eq(commands + 20, SpamCounters.countsNow(h, server.getTickCounter())[1], "a command packet counts on the command counter (P6)");
        check.eq(Integer.MAX_VALUE, SpamCounters.spamLimit(h), "while on, 1.12.2's own counter never kicks");
        ModConfig.changes.spamThresholds = false;
        check.eq(200, SpamCounters.spamLimit(h), "setting off: 1.12.2's limit of 200");
        ModConfig.changes.spamThresholds = true;
    }

    // hostGameModeDefault --------------------------------------------------------------------------

    private static void host(Check check, MinecraftServer server) {
        ICommand gamemode = server.getCommandManager().getCommands().get("gamemode");
        check.ok(gamemode instanceof HostGameModeCommand, "/gamemode is the changes stream's (1.12.2's while off)");
        check.ok(!ModConfig.changes.hostGameModeDefault, "hostGameModeDefault is off by default (26.3 took it back out)");
        check.ok(!HostGameModeCommand.isHost(server, FakePlayerFactory.getMinecraft(server.getWorld(0))),
                "a dedicated server has no host");
    }

    // advancementCounts ----------------------------------------------------------------------------

    private static void advancements(Check check, MinecraftServer server, WorldServer world) {
        check.ok(server.getCommandManager().getCommands().get("advancement") instanceof AdvancementCountsCommand,
                "/advancement is the changes stream's");
        ChangesSelfTest.Sender s = new ChangesSelfTest.Sender(server, world);
        String stone = "minecraft:story/mine_stone";
        s.run("advancement revoke @a everything");
        check.eq("commands.advancement.grant.one.to.one.success", s.run("advancement grant GDTestA only " + stone),
                "grant one advancement to one player (26.3)");
        check.eq("GDTestA", text(args(s)[1]), "the player's name");
        check.ok(text(args(s)[0]).startsWith("["), "the advancement's bracketed title (26.3's Advancement.name): " + text(args(s)[0]));
        check.eq("commands.advancement.grant.one.to.one.failure", s.run("advancement grant GDTestA only " + stone),
                "granting it again is the error");
        check.eq("commands.advancement.grant.one.to.one.success", s.run("advancement grant @a only " + stone),
                "both players, only B changed: B by name (26.3's NON_ZERO)");
        check.eq("GDTestB", text(args(s)[1]), "B by name");
        check.eq("commands.advancement.revoke.one.to.many.success", s.run("advancement revoke @a only " + stone),
                "revoke one advancement from both players: one to many (26.3)");
        check.eq(2, args(s)[1], "how many players it changed");
        check.eq("commands.advancement.revoke.one.to.many.failure", s.run("advancement revoke @a only " + stone),
                "revoking from players who don't have it: the error, counting the players asked for");
        check.eq(2, args(s)[1], "the players asked for");
        check.eq("commands.advancement.grant.many.to.one.success", s.run("advancement grant GDTestA until minecraft:story/smelt_iron"),
                "several advancements to one player");
        check.eq(4, args(s)[0], "until smelt_iron: the root, mine_stone, upgrade_tools, smelt_iron (26.3 counts those asked for)");
        check.eq("commands.advancement.grant.many.to.one.success", s.run("advancement grant @a until minecraft:story/smelt_iron"),
                "to both, only B changed: many to one");
        check.eq("GDTestB", text(args(s)[1]), "B by name");
        check.eq("commands.advancement.revoke.many.to.many.success", s.run("advancement revoke @a everything"),
                "revoke everything from both: many to many");
        check.eq(2, args(s)[1], "two players changed");
        check.eq("commands.advancement.revoke.many.to.many.failure", s.run("advancement revoke @a everything"),
                "nothing left to revoke: the error");
        check.eq("commands.advancement.grant.criterion.to.one.success", s.run("advancement grant GDTestA only " + stone + " get_stone"),
                "grant a criterion");
        check.eq("get_stone", text(args(s)[0]), "the criterion first (26.3's order)");
        check.eq("commands.advancement.grant.criterion.to.one.failure", s.run("advancement grant GDTestA only " + stone + " get_stone"),
                "granting the criterion again: the error");
        check.eq("commands.advancement.grant.criterion.to.one.success", s.run("advancement grant @a only " + stone + " get_stone"),
                "a criterion to both, only B changed: to one");
        check.eq("commands.advancement.revoke.criterion.to.many.success", s.run("advancement revoke @a only " + stone + " get_stone"),
                "revoke the criterion from both: to many");
        check.eq(2, args(s)[2], "two players");
        check.eq("commands.advancement.criterionNotFound", s.run("advancement grant GDTestA only " + stone + " nope"),
                "an unknown criterion");
        s.run("advancement grant GDTestA only " + stone);
        check.eq("commands.advancement.test.advancement.success", s.run("advancement test GDTestA " + stone),
                "test is 1.12.2's");
        s.run("advancement revoke @a everything");
        ModConfig.changes.advancementCounts = false;
        check.eq("commands.advancement.grant.only.success", s.run("advancement grant GDTestA only " + stone),
                "setting off: 1.12.2's message");
        ModConfig.changes.advancementCounts = true;
        s.run("advancement revoke @a everything");
    }

    // teamJoinCounts -------------------------------------------------------------------------------

    private static void teams(Check check, MinecraftServer server, WorldServer world) {
        ChangesSelfTest.Sender s = new ChangesSelfTest.Sender(server, world);
        s.run("scoreboard teams remove gdgaps");
        s.run("scoreboard teams add gdgaps");
        s.run("scoreboard teams option gdgaps color dark_aqua");
        check.eq("commands.team.join.success.multiple", s.run("scoreboard teams join gdgaps GDTestA GDTestB"),
                "two join: 'Added 2 members to team' (26.3)");
        check.eq(2, args(s)[0], "two members");
        check.eq("[gdgaps]", text(args(s)[1]), "the team's bracketed display name");
        check.eq("commands.team.join.success.single", s.run("scoreboard teams join gdgaps GDTestA"),
                "one joins: by name");
        check.eq("GDTestA", text(args(s)[0]), "the member's name");
        check.eq("commands.team.leave.success.multiple", s.run("scoreboard teams leave GDTestA GDTestB"),
                "two leave");
        check.eq(2, args(s)[0], "two left");
        check.eq("commands.team.leave.success.multiple", s.run("scoreboard teams leave GDTestA"),
                "one not in a team: 'Removed 0 members', no error (26.3)");
        check.eq(0, args(s)[0], "none left");
        check.eq("commands.team.join.success.multiple", s.run("scoreboard teams join gdgaps @a"),
                "a selector for both");
        check.eq(2, args(s)[0], "@a: two");
        check.eq("team.notFound", s.run("scoreboard teams join nosuchteam GDTestA"), "an unknown team (26.3)");
        ModConfig.changes.teamJoinCounts = false;
        check.eq("commands.scoreboard.teams.join.success", s.run("scoreboard teams join gdgaps GDTestA"),
                "setting off: 1.12.2's message");
        ModConfig.changes.teamJoinCounts = true;
        s.run("scoreboard teams remove gdgaps");
    }

    // unpublish ------------------------------------------------------------------------------------

    private static void unpublish(Check check, MinecraftServer server, WorldServer world) {
        check.ok(server.getCommandManager().getCommands().get("unpublish") instanceof UnpublishCommand, "/unpublish is there");
        ChangesSelfTest.Sender s = new ChangesSelfTest.Sender(server, world);
        check.eq("commands.unpublish.notPublished", s.run("unpublish"), "a dedicated server: 'No multiplayer game published'");
    }
}
