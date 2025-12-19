package dev.sixdev.unrrifts.core;

public enum RunMode {
    PVE("PvE"),
    PVP("PvP");

    private final String display;
    RunMode(String display){ this.display = display; }
    public String display(){ return display; }
}
