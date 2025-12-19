package dev.sixdev.unrrifts.cmd;

import dev.sixdev.unrrifts.UnrRiftsPlugin;
import dev.sixdev.unrrifts.gui.GuiListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RiftCommand implements CommandExecutor {
    private final UnrRiftsPlugin plugin;
    private final GuiListener gui;

    public RiftCommand(UnrRiftsPlugin plugin, GuiListener gui){
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)){
            sender.sendMessage("Players only.");
            return true;
        }

        // /rift leave → abort current run/queue without recording a win
        if (args.length >= 1 && args[0].equalsIgnoreCase("leave")){
            // leave queue if queued
            plugin.groups().leave(p);
            // leave run if currently inside one
            plugin.runs().forceLeave(p);
            return true;
        }

        gui.openRoot(p);
        return true;
    }
}
