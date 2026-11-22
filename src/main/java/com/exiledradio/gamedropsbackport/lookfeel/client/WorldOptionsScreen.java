package com.exiledradio.gamedropsbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.client.MenuBackground;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.lookfeel.WorldOptionsRules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLockIconButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

/**
 * 26.3's {@code WorldOptionsScreen}, opened from the pause menu in place of Open to LAN: a header (World Options), a
 * scrolling content of two two-column grids, and a footer with Apply Changes and Cancel. Changes apply only with
 * Apply Changes; locking the difficulty asks first.
 * <ul>
 *   <li>General (a host): Default Game Mode, Personal Game Mode (308 wide), Allow Commands, Difficulty and its lock.
 *       Not a host: Difficulty alone, inactive (1.12.2 has no packet to change a server's difficulty). 26.3's Game
 *       Rules... and Restrictions... are left out: 1.12.2 has neither screen.</li>
 *   <li>Multiplayer (a host): LAN, Port, Command Access, Force Game Mode. Turning LAN off closes the world again
 *       ({@link Lan#unpublish}); a new port while open closes it and opens it on that port, as 26.3's
 *       {@code changeMultiplayerScope}.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class WorldOptionsScreen extends GuiScreen {

    static final int HEADER = 33;
    static final int FOOTER = 33;
    static final int HEADING = 17;
    static final GameType[] MODES = {GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE, GameType.SPECTATOR};

    /** The port this mod last opened the world on (1.12.2 keeps none). */
    static int publishedPort = -1;
    /** For the client self-test. */
    public static volatile int applied;

    private final GuiScreen lastScreen;
    @Nullable
    private final IntegratedServer server;
    private final boolean host;
    private final boolean hardcore;

    private EnumDifficulty initialDifficulty;
    private EnumDifficulty wantedDifficulty;
    private boolean initialLocked;
    private boolean wantedLocked;
    private GameType initialDefault;
    private GameType wantedDefault;
    private GameType initialPersonal;
    private GameType wantedPersonal;
    private boolean initialAllow;
    private boolean wantedAllow;
    private boolean initialLan;
    private boolean wantedLan;
    private boolean initialAccess;
    private boolean wantedAccess;
    private boolean initialForce;
    private boolean wantedForce;
    private int initialPort;

    private final List<GuiButton> content = new ArrayList<GuiButton>();
    private final List<int[]> headings = new ArrayList<int[]>();
    private final List<String> headingText = new ArrayList<String>();
    private Cycle defaultMode;
    private Cycle personalMode;
    private Cycle allow;
    private Cycle difficulty;
    private GuiLockIconButton lock;
    private Cycle lan;
    private Cycle access;
    private Cycle force;
    private GuiTextField port;
    private int portY;
    private String portErrorKey;
    private boolean portEnabled;
    private GuiButton apply;
    private int scroll;
    private int contentHeight;

    public WorldOptionsScreen(GuiScreen lastScreen) {
        this.lastScreen = lastScreen;
        Minecraft mc = Minecraft.getMinecraft();
        this.server = mc.getIntegratedServer();
        this.host = this.server != null && mc.isIntegratedServerRunning();
        WorldInfo info = mc.world.getWorldInfo();
        this.hardcore = info.isHardcoreModeEnabled();
        this.initialDifficulty = this.wantedDifficulty = mc.world.getDifficulty();
        this.initialLocked = this.wantedLocked = info.isDifficultyLocked();
        if (this.host) {
            WorldInfo serverInfo = this.server.worlds[0].getWorldInfo();
            this.initialDefault = this.wantedDefault = serverInfo.getGameType();
            this.initialPersonal = this.wantedPersonal = mc.playerController.getCurrentGameType();
            this.initialAllow = this.wantedAllow = serverInfo.areCommandsAllowed();
            this.initialLan = this.wantedLan = this.server.getPublic();
            PlayerList players = this.server.getPlayerList();
            boolean everyone = ReflectionHelper.getPrivateValue(PlayerList.class, players, "commandsAllowedForAll", "field_72407_n");
            GameType joiners = ReflectionHelper.getPrivateValue(PlayerList.class, players, "gameType", "field_72410_m");
            this.initialAccess = this.wantedAccess = this.initialLan && everyone;
            this.initialForce = this.wantedForce = !this.initialLan || joiners != null;
            this.initialPort = this.initialLan ? Lan.port(this.server) : -1;
        }
    }

    // ---- layout -------------------------------------------------------------------------------------------------

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.content.clear();
        this.headings.clear();
        this.headingText.clear();
        int left = this.width / 2 - 154;
        int right = this.width / 2 + 4;
        int y = HEADER + 8;
        heading("options.worldOptions.general.title", y);
        y += HEADING + 4;
        if (this.host) {
            this.defaultMode = add(new Cycle(left, y, 308, "options.worldOptions.game_mode", "options.worldOptions.game_mode.tooltip"));
            y += 24;
            this.personalMode = add(new Cycle(left, y, 308, "options.worldOptions.personal_game_mode",
                    "options.worldOptions.personal_game_mode.tooltip"));
            y += 24;
            this.allow = add(new Cycle(left, y, 150, "gamedropsbackport.worldOptions.allowCommands", "options.worldOptions.allow_commands.tooltip"));
            this.difficulty = add(new Cycle(right, y, 130, "options.difficulty", null));
            this.lock = add(new GuiLockIconButton(nextId(), right + 130, y));
        } else {
            this.difficulty = add(new Cycle(this.width / 2 - 75, y, 130, "options.difficulty", null));
            this.lock = add(new GuiLockIconButton(nextId(), this.width / 2 + 55, y));
        }
        y += 20;
        if (this.host) {
            y += 8 + 8 + 8;
            heading("options.worldOptions.multiplayer.title", y);
            y += HEADING + 4;
            this.lan = add(new Cycle(left, y, 150, "menu.multiplayerOptions.lan", null));
            String text = this.port != null ? this.port.getText() : this.initialPort > 0 ? String.valueOf(this.initialPort) : "";
            this.port = new GuiTextField(nextId(), this.fontRenderer, right + 1, y + 1, 148, 18);
            this.port.setMaxStringLength(5);
            this.port.setText(text);
            this.portY = y + 1;
            y += 24;
            this.access = add(new Cycle(left, y, 150, "options.worldOptions.guest.command_access", null));
            this.force = add(new Cycle(right, y, 150, "options.worldOptions.guest.force_game_mode", null));
            y += 20;
        } else {
            this.lan = this.access = this.force = null;
            this.port = null;
        }
        this.contentHeight = y + 8 - HEADER;
        this.apply = new GuiButton(nextId(), left, this.height - FOOTER + 6, 150, 20, I18n.format("menu.multiplayerOptions.applyChanges"));
        this.buttonList.add(this.apply);
        this.buttonList.add(new GuiButton(nextId(), right, this.height - FOOTER + 6, 150, 20, I18n.format("gui.cancel")));
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll()));
        refresh();
    }

    private int idCounter = 0x6D0000;

    private int nextId() {
        return this.idCounter++;
    }

    private <T extends GuiButton> T add(T button) {
        this.content.add(button);
        this.buttonList.add(button);
        return button;
    }

    private void heading(String key, int y) {
        this.headings.add(new int[]{y});
        this.headingText.add(key);
    }

    private int contentBottom() {
        return this.height - FOOTER;
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - (contentBottom() - HEADER));
    }

    /** Values, labels, tooltips and states, as 26.3's update* methods leave them. */
    void refresh() {
        String on = I18n.format("options.on");
        String off = I18n.format("options.off");
        if (this.host) {
            boolean modes = WorldOptionsRules.gameModeActive(this.hardcore, this.wantedAllow);
            this.defaultMode.set(modeName(this.wantedDefault), modes,
                    WorldOptionsRules.gameModeTooltip(this.hardcore, this.wantedAllow, "options.worldOptions.game_mode.tooltip"));
            this.personalMode.set(modeName(this.wantedPersonal), modes,
                    WorldOptionsRules.gameModeTooltip(this.hardcore, this.wantedAllow, "options.worldOptions.personal_game_mode.tooltip"));
            this.allow.set(this.wantedAllow ? on : off, !this.hardcore,
                    this.hardcore ? "options.worldOptions.allow_commands.disabled.tooltip" : "options.worldOptions.allow_commands.tooltip");
            this.lan.set(this.wantedLan ? on : off, true,
                    this.wantedLan ? "menu.multiplayerOptions.network.lan.tooltip" : "menu.multiplayerOptions.network.off.tooltip");
            this.wantedAccess = WorldOptionsRules.commandAccessValue(this.wantedLan, this.wantedAllow, this.wantedAccess);
            this.access.set(this.wantedAccess ? on : off, WorldOptionsRules.commandAccessActive(this.wantedLan, this.wantedAllow),
                    WorldOptionsRules.commandAccessTooltip(this.wantedLan, this.wantedAllow));
            this.wantedForce = WorldOptionsRules.forceValue(this.hardcore, this.wantedLan, this.wantedAccess, this.wantedForce);
            this.force.set(this.wantedForce ? on : off, WorldOptionsRules.forceActive(this.hardcore, this.wantedLan, this.wantedAccess),
                    WorldOptionsRules.forceTooltip(this.hardcore, this.wantedLan, this.wantedAccess, this.wantedForce));
            boolean portOn = this.wantedLan;
            this.port.setEnabled(portOn);
            this.portEnabled = portOn;
            if (!this.wantedLan) {
                this.port.setText("");
                this.port.setFocused(false);
            }
            this.portErrorKey = portOn ? WorldOptionsRules.portError(this.port.getText(), this.initialPort, WorldOptionsScreen::portFree) : null;
            this.port.setTextColor(this.portErrorKey == null ? 0xE0E0E0 : 0xDF5050);
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean permitted = this.host;
        String blocked = WorldOptionsRules.difficultyBlocked(permitted, this.initialLocked, this.hardcore);
        this.difficulty.set(I18n.format(this.wantedDifficulty.getTranslationKey()), blocked == null, blocked);
        this.lock.setLocked(this.wantedLocked);
        this.lock.enabled = blocked == null;
        this.apply.enabled = hasChanges();
        for (GuiButton b : this.content) {
            if (b.getClass() == Cycle.class) {
                ((Cycle) b).place(this.scroll);
            } else if (b == this.lock) {
                b.y = this.difficulty.y;
            }
            b.visible = b.y >= HEADER && b.y + 20 <= contentBottom();
        }
        if (this.port != null) {
            this.port.y = this.portY - this.scroll;
            this.port.setVisible(this.port.y - 1 >= HEADER && this.port.y + 19 <= contentBottom());
        }
        if (mc.world == null) {
            this.mc.displayGuiScreen(this.lastScreen);
        }
    }

    private static String modeName(GameType mode) {
        return I18n.format("selectWorld.gameMode." + mode.getName());
    }

    static boolean portFree(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    boolean hasChanges() {
        boolean changed = this.wantedDifficulty != this.initialDifficulty || this.wantedLocked != this.initialLocked;
        if (this.host) {
            changed |= this.wantedDefault != this.initialDefault || this.wantedPersonal != this.initialPersonal
                    || this.wantedAllow != this.initialAllow || this.wantedLan != this.initialLan
                    || this.wantedAccess != this.initialAccess || this.wantedForce != this.initialForce
                    || lanPortChanged();
        }
        boolean portOk = !this.host || !this.wantedLan || this.portErrorKey == null;
        return changed && portOk;
    }

    /** 26.3's {@code lanPortChanged}: open, kept open, and a port other than the one it is open on (blank: a new one). */
    boolean lanPortChanged() {
        if (!this.host || !this.initialLan || !this.wantedLan || this.port == null) {
            return false;
        }
        String text = this.port.getText().trim();
        if (text.isEmpty()) {
            // 26.3's tryParsePort: blank picks a free port, so a known port changes; an unknown one stays.
            return this.initialPort > 0;
        }
        try {
            return Integer.parseInt(text) != this.initialPort;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ---- input --------------------------------------------------------------------------------------------------

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button == this.apply) {
            if (this.wantedLocked != this.initialLocked) {
                this.mc.displayGuiScreen(new GuiYesNo((result, id) -> {
                    if (result) {
                        applyChanges();
                        this.mc.displayGuiScreen(this.lastScreen);
                    } else {
                        this.mc.displayGuiScreen(this);
                    }
                }, I18n.format("difficulty.lock.title"), new TextComponentTranslation("difficulty.lock.question",
                        new TextComponentTranslation(this.wantedDifficulty.getTranslationKey())).getFormattedText(), 0));
            } else {
                applyChanges();
                this.mc.displayGuiScreen(this.lastScreen);
            }
            return;
        }
        if (!this.content.contains(button)) {
            this.mc.displayGuiScreen(this.lastScreen);
            return;
        }
        boolean back = isShiftKeyDown();
        if (button == this.defaultMode) {
            this.wantedDefault = cycle(this.wantedDefault, back);
        } else if (button == this.personalMode) {
            this.wantedPersonal = cycle(this.wantedPersonal, back);
        } else if (button == this.allow) {
            this.wantedAllow = !this.wantedAllow;
            if (!this.wantedAllow) {
                // updatePermissionDependentButtons: the modes fall back to what they were.
                this.wantedDefault = this.initialDefault;
                this.wantedPersonal = this.initialDefault;
            }
            this.wantedAccess = this.initialAccess;
        } else if (button == this.difficulty) {
            EnumDifficulty[] values = EnumDifficulty.values();
            this.wantedDifficulty = values[(this.wantedDifficulty.ordinal() + (back ? values.length - 1 : 1)) % values.length];
        } else if (button == this.lock) {
            this.wantedLocked = !this.wantedLocked;
        } else if (button == this.lan) {
            this.wantedLan = !this.wantedLan;
            this.wantedAccess = this.initialAccess;
            this.wantedForce = this.initialForce;
            if (this.wantedLan && this.initialLan && this.port != null) {
                // updatePortControlsState: back on, the port it is open on.
                this.port.setText(this.initialPort > 0 ? String.valueOf(this.initialPort) : "");
            }
        } else if (button == this.access) {
            this.wantedAccess = !this.wantedAccess;
        } else if (button == this.force) {
            this.wantedForce = !this.wantedForce;
        }
        refresh();
    }

    static GameType cycle(GameType mode, boolean back) {
        int i = Arrays.asList(MODES).indexOf(mode);
        int n = MODES.length;
        return MODES[((i < 0 ? 0 : i) + (back ? n - 1 : 1)) % n];
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.port != null && this.port.textboxKeyTyped(typedChar, keyCode)) {
            refresh();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.port != null && this.port.getVisible()) {
            this.port.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && maxScroll() > 0) {
            this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - Integer.signum(wheel) * 10));
            refresh();
        }
    }

    @Override
    public void updateScreen() {
        if (this.port != null) {
            this.port.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    // ---- apply --------------------------------------------------------------------------------------------------

    /** 26.3's {@code applyChanges}, with 1.12.2's own calls: the client's world info for difficulty (as GuiOptions). */
    void applyChanges() {
        applied++;
        Minecraft mc = this.mc;
        WorldInfo clientInfo = mc.world.getWorldInfo();
        if (this.wantedDifficulty != this.initialDifficulty) {
            clientInfo.setDifficulty(this.wantedDifficulty);
        }
        if (this.wantedLocked && !this.initialLocked) {
            clientInfo.setDifficultyLocked(true);
        }
        if (!this.host) {
            return;
        }
        IntegratedServer server = this.server;
        final GameType defaultMode = this.wantedDefault;
        final GameType personal = this.wantedPersonal;
        final boolean allowCommands = this.wantedAllow;
        final boolean portChanged = lanPortChanged();
        // changeMultiplayerScope: unpublish when open and turned off or moved, then publish when wanted.
        final boolean unpublish = this.initialLan && (!this.wantedLan || portChanged);
        final boolean publish = this.wantedLan && (!this.initialLan || portChanged);
        final boolean lanOpen = this.wantedLan;
        final boolean commandAccess = this.wantedAccess;
        final boolean forceMode = this.wantedForce;
        final boolean defaultChanged = defaultMode != this.initialDefault;
        final boolean personalChanged = personal != this.initialPersonal;
        final boolean allowChanged = allowCommands != this.initialAllow;
        String text = this.port.getText().trim();
        final int port = text.isEmpty() ? freePort() : Integer.parseInt(text);
        final java.util.UUID self = mc.player.getUniqueID();
        final EnumDifficulty newDifficulty = this.wantedDifficulty != this.initialDifficulty ? this.wantedDifficulty : null;
        final boolean lockNow = this.wantedLocked && !this.initialLocked;
        server.addScheduledTask(() -> {
            PlayerList players = server.getPlayerList();
            // The server's own calls (as /difficulty), not only the client's world info: a paused game does not sync it.
            if (newDifficulty != null && !server.worlds[0].getWorldInfo().isDifficultyLocked()) {
                server.setDifficultyForAllWorlds(newDifficulty);
            }
            if (lockNow) {
                for (net.minecraft.world.WorldServer world : server.worlds) {
                    world.getWorldInfo().setDifficultyLocked(true);
                }
            }
            if (allowChanged) {
                for (net.minecraft.world.WorldServer world : server.worlds) {
                    world.getWorldInfo().setAllowCommands(allowCommands);
                }
            }
            if (defaultChanged) {
                // /defaultgamemode's own call; it also sets the joiners' mode, which Force Game Mode decides below.
                server.setGameType(defaultMode);
            }
            if (personalChanged) {
                EntityPlayerMP owner = players.getPlayerByUUID(self);
                if (owner != null) {
                    owner.setGameType(personal);
                }
            }
            final boolean stopped = unpublish && Lan.unpublish(server);
            boolean published = false;
            if (publish) {
                published = Lan.publish(server, port);
            }
            if (lanOpen) {
                players.setCommandsAllowedForAll(commandAccess);
                players.setGameType(forceMode ? server.worlds[0].getWorldInfo().getGameType() : null);
            } else {
                players.setGameType(null);
            }
            for (EntityPlayerMP player : players.getPlayers()) {
                players.updatePermissionLevel(player);
            }
            if (stopped) {
                mc.addScheduledTask(() -> mc.ingameGUI.getChatGUI().printChatMessage(
                        new TextComponentTranslation("menu.multiplayerOptions.publish.stopped")));
            }
            if (publish) {
                final boolean ok = published;
                mc.addScheduledTask(() -> mc.ingameGUI.getChatGUI().printChatMessage(ok
                        ? new TextComponentTranslation("menu.multiplayerOptions.publish.started.lan", port)
                        : new TextComponentTranslation("commands.publish.failed")));
            }
        });
    }

    static int freePort() {
        try {
            return net.minecraft.util.HttpUtil.getSuitableLanPort();
        } catch (IOException e) {
            return 25564;
        }
    }

    // ---- drawing ------------------------------------------------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        MenuBackground.contentFrame(0, HEADER, this.width, contentBottom(), this.scroll);
        drawCenteredString(this.fontRenderer, I18n.format("options.worldOptions.title"), this.width / 2, (HEADER - 9) / 2, 0xFFFFFF);
        for (int i = 0; i < this.headings.size(); i++) {
            int y = this.headings.get(i)[0] - this.scroll;
            if (y >= HEADER && y + HEADING <= contentBottom()) {
                drawCenteredString(this.fontRenderer, "§n§l" + I18n.format(this.headingText.get(i)), this.width / 2, y + 4, 0xFFFFFF);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.port != null && this.port.getVisible()) {
            this.port.drawTextBox();
            if (this.port.getText().isEmpty() && !this.port.isFocused()) {
                String hint = this.portEnabled ? String.valueOf(freePortHint()) : I18n.format("lanServer.port");
                this.fontRenderer.drawStringWithShadow(hint, this.port.x + 4, this.port.y + 5, 0x808080);
            }
        }
        String tip = tooltipAt(mouseX, mouseY);
        if (tip != null) {
            List<String> lines = new ArrayList<String>();
            for (String part : I18n.format(tip).split("\\\\n")) {
                lines.addAll(this.fontRenderer.listFormattedStringToWidth(part, 170));
            }
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    private int hintPort = -1;

    private int freePortHint() {
        if (this.hintPort < 0) {
            this.hintPort = freePort();
        }
        return this.hintPort;
    }

    @Nullable
    String tooltipAt(int mouseX, int mouseY) {
        for (GuiButton b : this.content) {
            if (b instanceof Cycle && b.visible && mouseX >= b.x && mouseY >= b.y && mouseX < b.x + b.width && mouseY < b.y + b.height) {
                return ((Cycle) b).tooltip;
            }
        }
        if (this.port != null && this.port.getVisible() && mouseX >= this.port.x && mouseX < this.port.x + this.port.width
                && mouseY >= this.port.y && mouseY < this.port.y + this.port.height) {
            return this.portErrorKey != null ? this.portErrorKey : "lanServer.port";
        }
        return null;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    // ---- for the client self-test -------------------------------------------------------------------------------

    public List<String> labels() {
        List<String> out = new ArrayList<String>();
        for (GuiButton b : this.buttonList) {
            out.add(b.displayString + (b.enabled ? "" : " (off)"));
        }
        return out;
    }

    public void press(String labelStart) {
        for (GuiButton b : new ArrayList<GuiButton>(this.buttonList)) {
            if (b.displayString.startsWith(labelStart) && b.enabled) {
                actionPerformed(b);
                return;
            }
        }
        GameDropsBackport.LOGGER.warn("World Options: no button {}", labelStart);
    }

    public void scrollToEnd() {
        this.scroll = maxScroll();
        refresh();
    }

    public void typePort(String text) {
        if (this.port != null) {
            this.port.setText(text);
            refresh();
        }
    }

    // ---- widgets ------------------------------------------------------------------------------------------------

    /** 26.3's {@code CycleButton}: "Name: Value", its tooltip a lang key. */
    final class Cycle extends GuiButton {
        final String nameKey;
        final int baseY;
        @Nullable
        String tooltip;

        Cycle(int x, int y, int width, String nameKey, @Nullable String tooltip) {
            super(nextId(), x, y, width, 20, "");
            this.nameKey = nameKey;
            this.baseY = y;
            this.tooltip = tooltip;
        }

        void set(String value, boolean active, @Nullable String tooltip) {
            this.displayString = I18n.format(this.nameKey).replaceAll(":$", "") + ": " + value;
            this.enabled = active;
            this.tooltip = tooltip;
        }

        void place(int scroll) {
            this.y = this.baseY - scroll;
        }
    }
}
