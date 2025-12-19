package dev.sixdev.unrrifts.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

public final class Util {
    private Util(){}

    public static String locToString(Location l){
        return l.getWorld().getName()+","+l.getX()+","+l.getY()+","+l.getZ()+","+l.getYaw()+","+l.getPitch();
    }

    public static Location stringToLoc(String s){
        if (s == null || s.isBlank()) return null;
        String[] p = s.split(",");
        if (p.length < 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        double x = Double.parseDouble(p[1]);
        double y = Double.parseDouble(p[2]);
        double z = Double.parseDouble(p[3]);
        float yaw = p.length >= 5 ? Float.parseFloat(p[4]) : 0f;
        float pitch = p.length >= 6 ? Float.parseFloat(p[5]) : 0f;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public static String upper(String s){
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
