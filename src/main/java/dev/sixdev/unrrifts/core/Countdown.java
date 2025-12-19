package dev.sixdev.unrrifts.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class Countdown {
    private final Plugin plugin;
    private final int secondsTotal;
    private int secondsLeft;
    private BukkitTask task;

    private final Runnable onTick;
    private final Runnable onDone;

    public Countdown(Plugin plugin, int seconds, Runnable onTick, Runnable onDone){
        this.plugin = plugin;
        this.secondsTotal = seconds;
        this.secondsLeft = seconds;
        this.onTick = onTick;
        this.onDone = onDone;
    }

    public void start(){
        cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            secondsLeft--;
            if (onTick != null) onTick.run();
            if (secondsLeft <= 0){
                cancel();
                if (onDone != null) onDone.run();
            }
        }, 20L, 20L);
    }

    public void cancel(){
        if (task != null){
            task.cancel();
            task = null;
        }
    }

    public int left(){ return secondsLeft; }
    public int total(){ return secondsTotal; }
}
