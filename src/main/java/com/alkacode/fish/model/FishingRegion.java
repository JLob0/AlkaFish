package com.alkacode.fish.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** Região cubóide da área de pesca (min/max normalizados). */
public class FishingRegion {

    private String world;
    private int x1, y1, z1;
    private int x2, y2, z2;

    public FishingRegion(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.x1 = Math.min(x1, x2);
        this.x2 = Math.max(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.y2 = Math.max(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.z2 = Math.max(z1, z2);
    }

    public Location getCenter() {
        World w = Bukkit.getWorld(world);
        double x = x1 + (x2 - x1) / 2.0 + 0.5;
        double y = y1 + (y2 - y1) / 2.0 + 1.0;
        double z = z1 + (z2 - z1) / 2.0 + 0.5;
        return new Location(w, x, y, z);
    }

    /** Ignora Y de propósito - toda a área de pesca (não só o lobby) usa isso. Diferente
     * do AlkaMines (onde a altura importa pra mineração de verdade), a pesca só precisa
     * saber se o jogador está no footprint X/Z certo; o acesso já é 100% controlado por
     * /pescaria ou NPC, nunca andando até lá, então uma seleção de altura curta na hora
     * de configurar a área nunca vira "erro de área" por o jogador estar um pouco acima
     * ou abaixo do Y selecionado. */
    public boolean containsIgnoreY(Location location) {
        World w = location.getWorld();
        if (w == null || !w.getName().equals(world)) return false;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= x1 && x <= x2 && z >= z1 && z <= z2;
    }

    public String getWorld() { return world; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getZ1() { return z1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getZ2() { return z2; }
}
