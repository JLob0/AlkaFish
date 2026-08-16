package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;

/** Base para hooks por reflection - nunca importa softdepend diretamente. */
public abstract class HookBase {

    protected final AlkaFishPlugin plugin;

    protected HookBase(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    /** true se o plugin alvo está habilitado e o hook conseguiu resolver. */
    public abstract boolean isAvailable();
}
