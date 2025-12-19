package dev.sixdev.unrrifts;

import dev.sixdev.unrrifts.cmd.RiftCommand;
import dev.sixdev.unrrifts.cmd.UnrRiftsCommand;
import dev.sixdev.unrrifts.cmd.MapBuildCommands;
import dev.sixdev.unrrifts.core.*;
import dev.sixdev.unrrifts.gui.GuiListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class UnrRiftsPlugin extends JavaPlugin {

    private static UnrRiftsPlugin instance;

    private ConfigService configService;
    private LeaderboardService leaderboardService;
    private LobbyGroupManager lobbyGroupManager;
    private RunManager runManager;
    private GuiListener guiListener;

    private MapBuildManager mapBuildManager;

    public static UnrRiftsPlugin get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // services
        this.configService = new ConfigService(this);
        this.leaderboardService = new LeaderboardService(this, configService);
        this.runManager = new RunManager(this, configService, leaderboardService);
        this.lobbyGroupManager = new LobbyGroupManager(this, configService, runManager);
        this.guiListener = new GuiListener(this, configService, lobbyGroupManager);

        this.mapBuildManager = new MapBuildManager();

        // PlaceholderAPI hook (softdepend)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new dev.sixdev.unrrifts.core.UnrRiftsPlaceholders(leaderboardService).register();
            getLogger().info("PlaceholderAPI found: registered %unrrifts_*% placeholders.");
        } else {
            getLogger().info("PlaceholderAPI not found: placeholders disabled.");
        }

        // commands
        var rift = getCommand("rift");
        if (rift != null) rift.setExecutor(new RiftCommand(this, guiListener));

        var unrrifts = getCommand("unrrifts");
        if (unrrifts != null) {
            var cmd = new UnrRiftsCommand(this, configService, lobbyGroupManager, runManager);
            unrrifts.setExecutor(cmd);
            unrrifts.setTabCompleter(cmd);
        }

        

// manual map build commands
String[] buildCmds = new String[]{"unrmapstartbuild","unrmapfinalize","unrmapsetbreak","unrpspawn","unrmspawn","unrlspawn","unrbspawn","unrexfil","unrevent"};
MapBuildCommands mbc = new MapBuildCommands(this, configService, mapBuildManager);
for (String c : buildCmds){
    var pc = getCommand(c);
    if (pc != null) pc.setExecutor(mbc);
}


        // listeners
        Bukkit.getPluginManager().registerEvents(guiListener, this);
        Bukkit.getPluginManager().registerEvents(new MapBuildListener(mapBuildManager), this);
        Bukkit.getPluginManager().registerEvents(new RuntimeListener(this, configService, lobbyGroupManager, runManager), this);

        getLogger().info("unrRifts enabled.");
    }

    @Override
    public void onDisable() {
        try {
            if (lobbyGroupManager != null) lobbyGroupManager.shutdown();
            if (runManager != null) runManager.shutdown();
            if (leaderboardService != null) leaderboardService.save();
        } catch (Exception ignored) {}

        getLogger().info("unrRifts disabled.");
    }

    public ConfigService cfg() { return configService; }
    public LeaderboardService leaderboard() { return leaderboardService; }
    public LobbyGroupManager groups() { return lobbyGroupManager; }
    public RunManager runs() { return runManager; }
}
