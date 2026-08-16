package com.alkacode.fish.util;

import net.kyori.adventure.bossbar.BossBar;

/** Helpers de visual da BossBar do mini-game de tensão. */
public final class TensionBarUtil {

    private TensionBarUtil() {}

    /** Cor da BossBar conforme a tensão relativa à zona alvo. */
    public static BossBar.Color colorFor(double tension, double lower, double upper) {
        if (tension >= lower && tension <= upper) return BossBar.Color.GREEN;
        return tension < lower ? BossBar.Color.BLUE : BossBar.Color.RED;
    }

    /** Nome da BossBar conforme a situação da tensão. */
    public static String nameFor(double tension, double lower, double upper) {
        if (tension >= lower && tension <= upper) return "<green>✔ Tensão estável";
        return tension < lower ? "<blue>⬆ Puxe! O peixe está solto!" : "<red>⬇ Solte! A linha vai arrebentar!";
    }
}
