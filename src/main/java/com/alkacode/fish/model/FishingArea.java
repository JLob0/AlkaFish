package com.alkacode.fish.model;

import org.bukkit.Location;

/** Área de pesca pública única do servidor. */
public class FishingArea {
    private final String id;
    private final String displayName;
    private FishingRegion region;
    private FishingRegion lobbyRegion;
    private Location spawn;
    private Location exit;

    public FishingArea(String id, String displayName, FishingRegion region) {
        this.id = id;
        this.displayName = displayName;
        this.region = region;
        this.spawn = region.getCenter();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public FishingRegion getRegion() { return region; }
    public void setRegion(FishingRegion region) { this.region = region; }
    public FishingRegion getLobbyRegion() { return lobbyRegion; }
    public void setLobbyRegion(FishingRegion lobbyRegion) { this.lobbyRegion = lobbyRegion; }
    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
    public Location getExit() { return exit; }
    public void setExit(Location exit) { this.exit = exit; }

    /** Único check de "está na área" - sempre ignora Y (ver FishingRegion#containsIgnoreY),
     * cai pra região se não houver lobby configurado. */
    public boolean containsLobby(Location location) {
        return lobbyRegion != null ? lobbyRegion.containsIgnoreY(location) : region.containsIgnoreY(location);
    }
}
