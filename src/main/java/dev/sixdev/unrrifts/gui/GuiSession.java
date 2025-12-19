package dev.sixdev.unrrifts.gui;

import dev.sixdev.unrrifts.core.*;

public class GuiSession {
    public MapChoiceType mapChoiceType = MapChoiceType.GENERATED;
    public WorldType worldType = WorldType.CAVE_RIFT;
    public String customMap = "";
    public RunMode mode = RunMode.PVE;
    public String kitId = "";

    public LobbyKey toLobbyKey(){
        if (mapChoiceType == MapChoiceType.CUSTOM){
            return LobbyKey.custom(customMap, mode);
        }
        return LobbyKey.generated(worldType, mode);
    }
}
