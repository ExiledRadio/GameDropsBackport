package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.gamerules.client.GameRulesScreen;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.MenuScreens;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.lookfeel.client.Lan;
import com.exiledradio.gamedropsbackport.lookfeel.client.WorldOptionsScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * {@code runClient -Pselftest=client}: the look and feel on a real client. The title screen (26.3's panorama), then
 * a superflat creative world: the pause menu (26.2's icon row, World Options...), the in-world Options screen
 * (no Difficulty), and World Options itself (allow commands, a personal game mode, difficulty, open to LAN on a
 * chosen port). Photos go to {@code eclipse/gdclient/screenshots}; the result is logged as
 * {@code CLIENT SELFTEST DONE pass= fail=}.
 *
 * <p>Unattended, as Tricky Trials' is: {@code -Dfml.noGrab=true} and {@code inGameHasFocus} kept false, so the user's
 * mouse never turns the player.
 */
public final class ClientSelfTest {

    private static final String WORLD = "gamedrops-client-test";
    private static final String FLAT = "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;";

    private final List<IntSupplier> steps = new ArrayList<IntSupplier>();
    private int step;
    private int wait;
    private int ticks;
    private int pass;
    private int fail;
    private int phase;
    private File shots;
    private int lanPort;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientSelfTest());
        GameDropsBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            GameDropsBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            GameDropsBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    private static void unfocus() {
        Minecraft.getMinecraft().inGameHasFocus = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() != null) {
            unfocus();
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        unfocus();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        unfocus();
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            if (this.phase == 0 && mc.currentScreen instanceof GuiMainMenu && ++this.ticks >= 100) {
                this.shots = new File(mc.gameDir, "gdclient");
                FileUtils.deleteQuietly(new File(this.shots, "screenshots"));
                this.shots.mkdirs();
                ok(Presentation.panoramas().get(Presentation.panoramas().size() - 1).faces[0].getNamespace().equals(GameDropsBackport.MOD_ID),
                        "title screen: our panorama is the newest");
                shot("title.png");
                gameMusic(mc);
                this.phase = 1;
                this.ticks = 0;
                FileUtils.deleteQuietly(new File(new File(mc.gameDir, "saves"), WORLD));
                mc.gameSettings.pauseOnLostFocus = false;
                System.clearProperty(GameDropsBackport.MOD_ID + ".selftest");
                WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.setGeneratorOptions(FLAT);
                mc.launchIntegratedServer(WORLD, WORLD, settings);
            } else if (this.phase == 1 && ++this.ticks > 2400) {
                ok(false, "the test world never opened");
                finish(mc);
            } else if (this.phase == 1 && mc.world != null && mc.player != null && mc.getIntegratedServer() != null && this.ticks >= 60) {
                this.phase = 2;
                scenes(mc);
            } else if (this.phase == 2) {
                if (this.wait > 0) {
                    this.wait--;
                } else if (this.step < this.steps.size()) {
                    this.wait = this.steps.get(this.step++).getAsInt();
                } else {
                    finish(mc);
                }
            }
        } catch (Throwable t) {
            GameDropsBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    /** 26.2's game music: the sound handler's music.game (and so the creative music) holds fingerspit's five. */
    private void gameMusic(Minecraft mc) throws Exception {
        ok(com.exiledradio.gamedropsbackport.music.client.GameMusicPack.installed, "game music: the built-in pack is installed");
        net.minecraft.client.audio.SoundEventAccessor game = mc.getSoundHandler().getAccessor(new net.minecraft.util.ResourceLocation("music.game"));
        java.util.Map<String, net.minecraft.client.audio.Sound> sounds = new java.util.TreeMap<String, net.minecraft.client.audio.Sound>();
        int count = 0;
        if (game != null) {
            for (java.lang.reflect.Field f : net.minecraft.client.audio.SoundEventAccessor.class.getDeclaredFields()) {
                if (java.util.List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    for (Object o : (java.util.List<?>) f.get(game)) {
                        count++;
                        if (o instanceof net.minecraft.client.audio.Sound) {
                            net.minecraft.client.audio.Sound s = (net.minecraft.client.audio.Sound) o;
                            sounds.put(s.getSoundLocation().toString(), s);
                        }
                    }
                }
            }
        }
        boolean all = true;
        for (String track : com.exiledradio.gamedropsbackport.lookfeel.LookAndFeel.MENU_26_2) {
            net.minecraft.client.audio.Sound s = sounds.get(GameDropsBackport.MOD_ID + ":music/game/" + track);
            all &= s != null && s.getWeight() == 2 && s.getVolume() == 0.4F && s.isStreaming();
        }
        ok(all, "game music: music.game holds fingerspit's five at weight 2, volume 0.4, streamed");
        if (mc.getSoundHandler().getAccessor(new net.minecraft.util.ResourceLocation("ambient.cave")) != null) {
            ok(sounds.containsKey("minecraft:music/game/calm1"), "game music: 1.12.2's own tracks stay (" + count + " entries)");
        } else {
            // An offline dev run has no 1.12.2 sounds.json (the asset download failed): nothing of vanilla's to keep.
            GameDropsBackport.LOGGER.info("CLIENT SELFTEST note: no 1.12.2 sounds in this run; music.game has {} entries", count);
        }
        // The biome lists are their owners'; with them installed, they hold 26.2's tracks.
        if (net.minecraftforge.fml.common.Loader.isModLoaded("trailsandtalesbackport")) {
            java.util.Set<String> forest = soundNames(mc, "trailsandtalesbackport:music.overworld.forest");
            ok(forest.contains(GameDropsBackport.MOD_ID + ":music/game/memories") && forest.contains(GameDropsBackport.MOD_ID + ":music/game/shores"),
                    "biome music: Trails & Tales' forest list has Memories and Shores: " + forest.size() + " sounds");
            java.util.Set<String> cherry = soundNames(mc, "trailsandtalesbackport:music.overworld.cherry_grove");
            ok(cherry.contains(GameDropsBackport.MOD_ID + ":music/game/home") && cherry.contains(GameDropsBackport.MOD_ID + ":music/game/nightly"),
                    "biome music: the cherry grove's has Home and Nightly");
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("cavesandcliffsbackport")) {
            java.util.Set<String> lush = soundNames(mc, "cavesandcliffsbackport:music.overworld.lush_caves");
            ok(lush.contains(GameDropsBackport.MOD_ID + ":music/game/shores") && lush.contains("cavesandcliffsbackport:music/game/left_to_bloom"),
                    "biome music: Caves & Cliffs' lush caves list keeps 1.18's and has Shores: " + lush);
            java.util.Set<String> frozen = soundNames(mc, "cavesandcliffsbackport:music.overworld.frozen_peaks");
            ok(frozen.contains(GameDropsBackport.MOD_ID + ":music/game/ebb") && frozen.contains(GameDropsBackport.MOD_ID + ":music/game/home"),
                    "biome music: the frozen peaks' has Ebb and Home");
        }
    }

    /** @return the sound files an event's accessor lists (one level: event references are not followed) */
    private static java.util.Set<String> soundNames(Minecraft mc, String event) throws Exception {
        java.util.Set<String> out = new java.util.TreeSet<String>();
        net.minecraft.client.audio.SoundEventAccessor accessor = mc.getSoundHandler().getAccessor(new net.minecraft.util.ResourceLocation(event));
        if (accessor == null) {
            return out;
        }
        for (java.lang.reflect.Field f : net.minecraft.client.audio.SoundEventAccessor.class.getDeclaredFields()) {
            if (java.util.List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                for (Object o : (java.util.List<?>) f.get(accessor)) {
                    if (o instanceof net.minecraft.client.audio.Sound) {
                        out.add(((net.minecraft.client.audio.Sound) o).getSoundLocation().toString());
                    }
                }
            }
        }
        return out;
    }

    private void scenes(Minecraft mc) {
        // Blocks, items, mobs and their screens first: World Options below switches the player to adventure.
        new ClientScenes(this, mc).add(this.steps);
        // The other backports' babies (Trails & Tales, Wild Update), when their dev jars are on the classpath.
        new FamilyBabyScene(this, mc).add(this.steps);
        // 26.3's off-map player marker (Core's P43): a map 2000 blocks east, held, shows the pointed marker facing west.
        this.steps.add(() -> {
            net.minecraft.entity.player.EntityPlayerMP sp = mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
            mc.getIntegratedServer().addScheduledTask(() -> {
                net.minecraft.item.ItemStack map = net.minecraft.item.ItemMap.setupNewMap(sp.world, sp.posX + 2000, sp.posZ, (byte) 0, true, true);
                sp.inventory.setInventorySlotContents(sp.inventory.currentItem, map);
                sp.inventory.setInventorySlotContents(40, net.minecraft.item.ItemStack.EMPTY);
            });
            mc.player.rotationYaw = 90.0F;
            mc.player.rotationPitch = 70.0F;
            return 60;
        });
        // A loaded server can be seconds behind: wait (up to 30 s) for the map and its marker to reach the client.
        this.steps.add(() -> {
            if (!offLimitsShown(mc)) {
                this.step--; // this step again next tick
                return 1;
            }
            return 20;
        });
        this.steps.add(() -> {
            mc.player.rotationYaw = 90.0F;
            mc.player.rotationPitch = 70.0F;
            net.minecraft.item.ItemStack held = mc.player.getHeldItemMainhand();
            net.minecraft.world.storage.MapData data = held.getItem() instanceof net.minecraft.item.ItemMap
                    ? ((net.minecraft.item.ItemMap) held.getItem()).getMapData(held, mc.world) : null;
            net.minecraft.world.storage.MapDecoration me = null;
            if (data != null) {
                for (net.minecraft.world.storage.MapDecoration d : data.mapDecorations.values()) {
                    me = d.getType() == net.minecraft.world.storage.MapDecoration.Type.PLAYER_OFF_LIMITS ? d : me;
                }
            }
            // Yaw 90 (west): (90 + 8) * 16 / 360 = 4; 1.12.2 gives 0.
            ok(me != null && (me.getRotation() & 15) == 4, "map: the off-limits marker faces west on the client, " + me
                    + (me == null ? "" : " rotation " + me.getRotation()));
            ok(com.exiledradio.backportcore.map.MapFacing.spriteOf(net.minecraft.world.storage.MapDecoration.Type.PLAYER_OFF_LIMITS) != null,
                    "map: 26.3's pointed off-limits sprite");
            shot("map_off_limits.png");
            ok(mc.getTextureManager().getTexture(new net.minecraft.util.ResourceLocation("backportcore",
                    "textures/map/decorations/player_off_limits.png")) != null, "map: the pointed sprite was drawn (texture loaded)");
            return 5;
        });
        // 1.21.5's spawn eggs (Core's SpawnEggModels): at 26.3 each egg is its own texture, ours (the sulfur cube) too.
        this.steps.add(() -> {
            net.minecraft.entity.player.EntityPlayerMP sp = mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
            String[] mobs = {GameDropsBackport.MOD_ID + ":sulfur_cube", "minecraft:zombie", "minecraft:creeper", "minecraft:pig",
                    "minecraft:mooshroom", "minecraft:zombie_pigman", "minecraft:ghast", "minecraft:chicken", "minecraft:villager"};
            mc.getIntegratedServer().addScheduledTask(() -> {
                for (int i = 0; i < mobs.length; i++) {
                    net.minecraft.item.ItemStack egg = new net.minecraft.item.ItemStack(net.minecraft.init.Items.SPAWN_EGG);
                    net.minecraft.item.ItemMonsterPlacer.applyEntityIdToItemStack(egg, new net.minecraft.util.ResourceLocation(mobs[i]));
                    sp.inventory.setInventorySlotContents(i, egg);
                }
                sp.inventory.currentItem = 0;
            });
            mc.player.inventory.currentItem = 0;
            mc.player.rotationPitch = 20.0F;
            return 20;
        });
        this.steps.add(() -> {
            String[][] want = {{GameDropsBackport.MOD_ID + ":sulfur_cube", "minecraft:item/sulfur_cube_spawn_egg"},
                    {"minecraft:zombie", "minecraft:item/zombie_spawn_egg"}, {"minecraft:zombie_pigman", "minecraft:item/zombified_piglin_spawn_egg"}};
            for (String[] w : want) {
                net.minecraft.item.ItemStack egg = new net.minecraft.item.ItemStack(net.minecraft.init.Items.SPAWN_EGG);
                net.minecraft.item.ItemMonsterPlacer.applyEntityIdToItemStack(egg, new net.minecraft.util.ResourceLocation(w[0]));
                net.minecraft.client.renderer.block.model.IBakedModel m = mc.getRenderItem().getItemModelWithOverrides(egg, mc.world, mc.player);
                boolean untinted = true;
                for (net.minecraft.client.renderer.block.model.BakedQuad q : m.getQuads(null, null, 0L)) {
                    untinted &= !q.hasTintIndex();
                }
                ok(w[1].equals(m.getParticleTexture().getIconName()) && untinted, "spawn eggs: " + w[0] + "'s egg is 26.3's " + w[1]
                        + " (" + m.getParticleTexture().getIconName() + ", untinted " + untinted + ")");
            }
            shot("spawn_eggs.png");
            return 5;
        });
        this.steps.add(() -> {
            mc.player.rotationPitch = 0.0F;
            net.minecraft.entity.player.EntityPlayerMP sp = mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
            mc.getIntegratedServer().addScheduledTask(() -> sp.inventory.setInventorySlotContents(sp.inventory.currentItem, net.minecraft.item.ItemStack.EMPTY));
            return 5;
        });
        // The pause menu: World Options... in Open to LAN's place, the 26.2 icon row under Advancements and Statistics.
        this.steps.add(() -> {
            mc.displayGuiScreen(new GuiIngameMenu());
            return 20;
        });
        this.steps.add(() -> {
            GuiButton lan = button(mc.currentScreen, 7);
            GuiButton back = button(mc.currentScreen, 4);
            ok(lan != null && "World Options...".equals(lan.displayString) && lan.enabled, "pause: World Options... in Open to LAN's place");
            ok(MenuScreens.ICONS.size() == 2, "pause: two icon buttons, " + MenuScreens.ICONS.size());
            ok(!MenuScreens.ICONS.isEmpty() && back != null && MenuScreens.ICONS.get(0).y == back.y + 48
                    && MenuScreens.ICONS.get(0).width == 20, "pause: the icon row is the third row, 20 wide");
            GuiButton options = button(mc.currentScreen, 0);
            ok(options != null && lan != null && options.y == lan.y && lan.x == mc.currentScreen.width / 2 + 4, "pause: Options... and World Options... share a row");
            shot("pause.png");
            mc.displayGuiScreen(new GuiOptions(mc.currentScreen, mc.gameSettings));
            return 20;
        });
        this.steps.add(() -> {
            ok(button(mc.currentScreen, 108) == null && button(mc.currentScreen, 109) == null, "options: no Difficulty or lock in a world");
            shot("options.png");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 20;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            List<String> labels = s.labels();
            GameDropsBackport.LOGGER.info("CLIENT SELFTEST world options: {}", labels);
            ok(labels.contains("Default Game Mode: Creative (off)") && labels.contains("Personal Game Mode: Creative (off)"),
                    "world options: game modes off without commands");
            ok(labels.contains("Allow Cheats: OFF") && labels.contains("LAN: OFF"), "world options: commands and LAN off");
            ok(labels.contains("Command Access: OFF (off)") && labels.contains("Force Game Mode: ON (off)"), "world options: guests' rows off without LAN");
            ok(labels.contains("Apply Changes (off)"), "world options: nothing to apply yet");
            ok(labels.contains("Edit Game Rules... (off)"), "world options: Game Rules... off without commands");
            shot("world_options.png");
            s.press("Allow Cheats");
            s.press("Personal Game Mode");
            s.press("Difficulty");
            return 3;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            List<String> labels = s.labels();
            ok(labels.contains("Personal Game Mode: Adventure") && labels.contains("Allow Cheats: ON"), "world options: game modes on with commands");
            ok(labels.contains("Apply Changes"), "world options: changes to apply");
            shot("world_options_changed.png");
            s.press("Apply Changes");
            return 40;
        });
        this.steps.add(() -> {
            ok(WorldOptionsScreen.applied == 1, "world options: applied once");
            ok(mc.playerController.getCurrentGameType() == GameType.ADVENTURE, "world options: personal game mode now adventure, "
                    + mc.playerController.getCurrentGameType());
            ok(mc.getIntegratedServer().worlds[0].getWorldInfo().areCommandsAllowed(), "world options: commands allowed");
            ok(mc.getIntegratedServer().worlds[0].getWorldInfo().getDifficulty() == EnumDifficulty.HARD, "world options: difficulty hard, "
                    + mc.getIntegratedServer().worlds[0].getWorldInfo().getDifficulty());
            ok(mc.getIntegratedServer().worlds[0].getWorldInfo().getGameType() == GameType.CREATIVE, "world options: default game mode kept");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 5;
        });
        // 26.3's Game Rules... (Backport Core's screen, 26.1's look: the search box in a 36-high header).
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            ok(s.labels().contains("Edit Game Rules..."), "world options: Game Rules... on with commands");
            s.press("Edit Game Rules");
            return 20;
        });
        this.steps.add(() -> {
            ok(mc.currentScreen instanceof GameRulesScreen && ((GameRulesScreen) mc.currentScreen).received(), "game rules: the server's rules arrived");
            GameRulesScreen r = (GameRulesScreen) mc.currentScreen;
            ok(r.hasSearch() && r.top() == 36, "game rules: 26.1's look, the search box in the header");
            ok(r.rows().contains("# Chat") && r.rows().contains("Keep inventory after death = false"), "game rules: 26.3's categories and names");
            shot("game_rules.png");
            r.setFilter("keep");
            ok(r.rows().equals(java.util.Arrays.asList("# Player", "Keep inventory after death = false")), "game rules: search, " + r.rows());
            shot("game_rules_search.png");
            r.setFilter("");
            r.set("keepInventory", "true");
            r.pressDone();
            return 20;
        });
        this.steps.add(() -> {
            ok(mc.getIntegratedServer().worlds[0].getGameRules().getBoolean("keepInventory"), "game rules: Done set keepInventory on the server");
            ok(mc.currentScreen instanceof WorldOptionsScreen, "game rules: back to World Options");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 5;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            s.press("LAN");
            s.typePort("80");
            List<String> labels = s.labels();
            ok(labels.contains("Apply Changes (off)"), "world options: port 80 is refused");
            this.lanPort = freePort();
            s.typePort(String.valueOf(this.lanPort));
            labels = s.labels();
            ok(labels.contains("Command Access: OFF") && labels.contains("Apply Changes"), "world options: LAN on, guests' rows on");
            s.press("Command Access");
            s.scrollToEnd();
            return 3;
        });
        this.steps.add(() -> {
            shot("world_options_lan.png");
            ((WorldOptionsScreen) mc.currentScreen).press("Apply Changes");
            return 40;
        });
        this.steps.add(() -> {
            ok(mc.getIntegratedServer().getPublic(), "world options: open to LAN");
            boolean everyone = ReflectionHelper.getPrivateValue(net.minecraft.server.management.PlayerList.class,
                    mc.getIntegratedServer().getPlayerList(), "commandsAllowedForAll", "field_72407_n");
            ok(everyone, "world options: guests can use commands");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 5;
        });
        this.steps.add(() -> {
            List<String> labels = ((WorldOptionsScreen) mc.currentScreen).labels();
            ok(labels.contains("LAN: ON") && labels.contains("Command Access: ON"), "world options: open, LAN can be switched off (26.3)");
            ok(Lan.tcpListeners(mc.getIntegratedServer()) == 1 && Lan.announcing(mc.getIntegratedServer()), "LAN: one listener, announced");
            ok(listening(this.lanPort), "LAN: the port takes connections");
            ((WorldOptionsScreen) mc.currentScreen).press("LAN");
            labels = ((WorldOptionsScreen) mc.currentScreen).labels();
            ok(labels.contains("LAN: OFF") && labels.contains("Apply Changes"), "world options: LAN off is a change to apply");
            ((WorldOptionsScreen) mc.currentScreen).press("Apply Changes");
            return 40;
        });
        this.steps.add(() -> {
            net.minecraft.server.integrated.IntegratedServer server = mc.getIntegratedServer();
            ok(!server.getPublic(), "world options: LAN off closes the world (26.3's unpublishServer)");
            ok(Lan.tcpListeners(server) == 0, "LAN off: the listener is closed");
            ok(!Lan.announcing(server), "LAN off: no longer announced");
            ok(!listening(this.lanPort), "LAN off: the port refuses connections");
            boolean everyone = ReflectionHelper.getPrivateValue(net.minecraft.server.management.PlayerList.class,
                    server.getPlayerList(), "commandsAllowedForAll", "field_72407_n");
            ok(!everyone, "LAN off: guests' command access cleared");
            ok(mc.world != null && mc.player != null, "LAN off: the host stays in the world");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 5;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            ok(s.labels().contains("LAN: OFF"), "world options: LAN shows off again");
            s.press("LAN");
            this.lanPort = freePort();
            s.typePort(String.valueOf(this.lanPort));
            s.press("Apply Changes");
            return 40;
        });
        this.steps.add(() -> {
            ok(mc.getIntegratedServer().getPublic() && listening(this.lanPort), "world options: opened to LAN again");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 5;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            this.oldPort = this.lanPort;
            this.lanPort = freePort();
            s.typePort(String.valueOf(this.lanPort));
            ok(s.labels().contains("Apply Changes"), "world options: a new port while open is a change (26.3's lanPortChanged)");
            s.press("Apply Changes");
            return 40;
        });
        this.steps.add(() -> {
            ok(mc.getIntegratedServer().getPublic() && listening(this.lanPort) && !listening(this.oldPort),
                    "world options: moved to the new port, the old one closed");
            ok(Lan.tcpListeners(mc.getIntegratedServer()) == 1, "one listener after the move");
            mc.displayGuiScreen(null);
            mc.player.sendChatMessage("/unpublish");
            return 30;
        });
        this.steps.add(() -> {
            ok(!mc.getIntegratedServer().getPublic() && !listening(this.lanPort), "/unpublish closes the world (26.2)");
            mc.player.sendChatMessage("/unpublish");
            return 30;
        });
        this.steps.add(() -> {
            ok(chatHas(mc, "No multiplayer game published"), "/unpublish again: 'No multiplayer game published'");
            mc.player.sendChatMessage("/publish");
            return 30;
        });
        this.steps.add(() -> {
            ok(mc.getIntegratedServer().getPublic(), "1.12.2's /publish still opens it");
            mc.player.sendChatMessage("/unpublish");
            return 30;
        });
        this.steps.add(() -> {
            ok(!mc.getIntegratedServer().getPublic() && Lan.tcpListeners(mc.getIntegratedServer()) == 0,
                    "/unpublish closes a world /publish opened");
            // hostGameModeDefault (26.2), on for this check only.
            com.exiledradio.gamedropsbackport.ModConfig.changes.hostGameModeDefault = true;
            mc.player.sendChatMessage("/gamemode survival");
            return 30;
        });
        this.steps.add(() -> {
            ok(mc.getIntegratedServer().worlds[0].getWorldInfo().getGameType() == GameType.SURVIVAL,
                    "hostGameModeDefault: the host's /gamemode survival makes survival the default (26.2), "
                            + mc.getIntegratedServer().worlds[0].getWorldInfo().getGameType());
            com.exiledradio.gamedropsbackport.ModConfig.changes.hostGameModeDefault = false;
            mc.player.sendChatMessage("/gamemode creative");
            return 30;
        });
        this.steps.add(() -> {
            ok(mc.playerController.getCurrentGameType() == GameType.CREATIVE
                            && mc.getIntegratedServer().worlds[0].getWorldInfo().getGameType() == GameType.SURVIVAL,
                    "hostGameModeDefault off (the default): /gamemode leaves the default alone (26.3)");
            mc.displayGuiScreen(null);
            return 5;
        });
    }

    private int mapWaits;

    /** True once the held map shows an off-limits marker, or after 600 tries. */
    private boolean offLimitsShown(Minecraft mc) {
        mc.player.rotationYaw = 90.0F;
        mc.player.rotationPitch = 70.0F;
        if (++this.mapWaits > 600) {
            return true;
        }
        net.minecraft.item.ItemStack held = mc.player.getHeldItemMainhand();
        net.minecraft.world.storage.MapData data = held.getItem() instanceof net.minecraft.item.ItemMap
                ? ((net.minecraft.item.ItemMap) held.getItem()).getMapData(held, mc.world) : null;
        if (data != null) {
            for (net.minecraft.world.storage.MapDecoration d : data.mapDecorations.values()) {
                if (d.getType() == net.minecraft.world.storage.MapDecoration.Type.PLAYER_OFF_LIMITS) {
                    return true;
                }
            }
        }
        return false;
    }

    private int oldPort;

    private static boolean listening(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static boolean chatHas(Minecraft mc, String text) {
        List<net.minecraft.client.gui.ChatLine> lines = ReflectionHelper.getPrivateValue(net.minecraft.client.gui.GuiNewChat.class,
                mc.ingameGUI.getChatGUI(), "chatLines", "field_146252_h");
        for (net.minecraft.client.gui.ChatLine line : lines) {
            if (line.getChatComponent().getUnformattedText().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            int port = socket.getLocalPort();
            return port >= 1024 ? port : 25929;
        } catch (java.io.IOException e) {
            return 25929;
        }
    }

    private static GuiButton button(GuiScreen screen, int id) {
        List<GuiButton> list = ReflectionHelper.getPrivateValue(GuiScreen.class, screen, "buttonList", "field_146292_n");
        for (GuiButton b : list) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    void shot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(this.shots, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        File f = new File(new File(this.shots, "screenshots"), name);
        ok(f.isFile() && f.length() > 1000, "saved " + name);
    }

    private void finish(Minecraft mc) {
        this.phase = 3;
        GameDropsBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }
}
