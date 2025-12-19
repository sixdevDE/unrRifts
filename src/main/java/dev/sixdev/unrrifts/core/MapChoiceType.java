package dev.sixdev.unrrifts.core;

public enum MapChoiceType {
    GENERATED("Generated"),
    CUSTOM("Custom Map");

    private final String display;
    MapChoiceType(String display){ this.display = display; }
    public String display(){ return display; }
}
