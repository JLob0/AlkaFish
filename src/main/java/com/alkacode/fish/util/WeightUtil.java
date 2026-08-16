package com.alkacode.fish.util;

import com.alkacode.fish.model.Fish;

import java.util.concurrent.ThreadLocalRandom;

/** Cálculo de comprimento e peso dos peixes capturados. */
public final class WeightUtil {

    private WeightUtil() {}

    /** Sorteia um comprimento uniforme entre min e max do peixe. */
    public static double rollLength(Fish fish) {
        return fish.getMinLength()
                + (ThreadLocalRandom.current().nextDouble() * (fish.getMaxLength() - fish.getMinLength()));
    }

    /**
     * Deriva o peso a partir do comprimento sorteado (interpola a posição relativa do
     * comprimento entre min/max e aplica no intervalo de peso do peixe).
     */
    public static double rollWeight(Fish fish, double length) {
        double ratio = (length - fish.getMinLength()) / (fish.getMaxLength() - fish.getMinLength());
        ratio = Math.max(0, Math.min(1, ratio));
        return fish.getMinWeight() + (ratio * (fish.getMaxWeight() - fish.getMinWeight()));
    }
}
