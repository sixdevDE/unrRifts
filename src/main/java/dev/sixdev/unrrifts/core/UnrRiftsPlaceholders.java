package dev.sixdev.unrrifts.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class UnrRiftsPlaceholders extends PlaceholderExpansion {

    private final LeaderboardService lb;

    public UnrRiftsPlaceholders(LeaderboardService lb) {
        this.lb = lb;
    }

    @Override public String getIdentifier() { return "unrrifts"; } // %unrrifts_...%
    @Override public String getAuthor() { return "sixdev"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return null;
        params = params.toLowerCase(Locale.ROOT);

        // Global
        if (params.equals("fastest_name")) return safe(lb.getGlobalFastestName(), "-");
        if (params.equals("fastest_time_ms")) return String.valueOf(lb.getGlobalFastestTimeMs());
        if (params.equals("fastest_time")) return formatMs(lb.getGlobalFastestTimeMs());

        // Player
        if (params.equals("wins")) {
            if (player == null) return "0";
            return String.valueOf(lb.getWins(player.getUniqueId(), player.getName()));
        }
        if (params.equals("best_time_ms")) {
            if (player == null) return "0";
            return String.valueOf(lb.getBestTimeMs(player.getUniqueId(), player.getName()));
        }
        if (params.equals("best_time")) {
            if (player == null) return "-";
            long ms = lb.getBestTimeMs(player.getUniqueId(), player.getName());
            return ms <= 0 ? "-" : formatMs(ms);
        }

        // Top wins: topwins_1_name / topwins_1_wins (bis 10)
        if (params.startsWith("topwins_")) {
            String[] p = params.split("_");
            if (p.length < 3) return null;
            int rank = parseRank(p[1]);
            if (rank < 1) return null;

            UUID id = lb.getTopWinsUuid(rank);
            if (id == null) return "-";

            if (p[2].equals("name")) return safe(lb.getName(id), "-");
            if (p[2].equals("wins")) return String.valueOf(lb.getWins(id, lb.getName(id)));
            return null;
        }

        // Top fastest: topfastest_1_name / topfastest_1_time / topfastest_1_time_ms
        if (params.startsWith("topfastest_")) {
            String[] p = params.split("_");
            if (p.length < 3) return null;
            int rank = parseRank(p[1]);
            if (rank < 1) return null;

            UUID id = lb.getTopFastestUuid(rank);
            if (id == null) return "-";

            long ms = lb.getBestTimeMs(id, lb.getName(id));
            if (p[2].equals("name")) return safe(lb.getName(id), "-");
            if (p[2].equals("time_ms")) return String.valueOf(ms);
            if (p[2].equals("time")) return ms <= 0 ? "-" : formatMs(ms);
            return null;
        }

        return null;
    }

    private int parseRank(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }

    private String safe(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private String formatMs(long ms) {
        if (ms <= 0) return "-";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - minutes * 60;
        long millis = ms - (minutes * 60_000L) - (seconds * 1000L);
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
