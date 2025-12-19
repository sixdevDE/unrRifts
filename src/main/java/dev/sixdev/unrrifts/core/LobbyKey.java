package dev.sixdev.unrrifts.core;

import java.util.Objects;

public final class LobbyKey {
    public final MapChoiceType mapChoiceType;
    public final WorldType worldType; // for generated
    public final String customMap;     // for custom
    public final RunMode mode;

    private LobbyKey(MapChoiceType mapChoiceType, WorldType worldType, String customMap, RunMode mode){
        this.mapChoiceType = mapChoiceType;
        this.worldType = worldType;
        this.customMap = customMap;
        this.mode = mode;
    }

    public static LobbyKey generated(WorldType wt, RunMode mode){
        return new LobbyKey(MapChoiceType.GENERATED, wt, "", mode);
    }

    public static LobbyKey custom(String map, RunMode mode){
        return new LobbyKey(MapChoiceType.CUSTOM, null, map, mode);
    }

    @Override public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof LobbyKey k)) return false;
        return mapChoiceType == k.mapChoiceType &&
                worldType == k.worldType &&
                Objects.equals(customMap, k.customMap) &&
                mode == k.mode;
    }

    @Override public int hashCode(){
        return Objects.hash(mapChoiceType, worldType, customMap, mode);
    }

    @Override public String toString(){
        if (mapChoiceType == MapChoiceType.CUSTOM) return "CUSTOM:"+customMap+":"+mode;
        return "GEN:"+worldType+":"+mode;
    }
}
