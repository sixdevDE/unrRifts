package dev.sixdev.unrrifts.core;

public enum WorldType {
    CAVE_RIFT("CAVE RIFT"),
    DUNGEON_ROOMS("DUNGEON ROOMS"),
    RAVINE_WORLD("RAVINE WORLD"),
    WOODS_WORLD("WOODS WORLD");

    private final String display;
    WorldType(String display){ this.display = display; }
    public String display(){ return display; }
}
