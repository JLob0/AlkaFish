package com.alkacode.fish.model;

/** Tipos de torneio de pesca. */
public enum TournamentType {
    BIGGEST_FISH("Maior Peixe", "Quem pegar o peixe mais longo vence!"),
    MOST_FISH("Mais Peixes", "Quem pegar mais peixes vence!"),
    TOTAL_WEIGHT("Maior Peso Total", "Quem acumular mais peso vence!"),
    FIRST_LEGENDARY("Primeiro Lendário", "O primeiro a pegar um peixe Lendário vence!"),
    RANDOM_RARITY_FIRST("Primeiro da Raridade", "O primeiro a pegar um peixe da raridade alvo vence!");

    private final String displayName;
    private final String description;

    TournamentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
