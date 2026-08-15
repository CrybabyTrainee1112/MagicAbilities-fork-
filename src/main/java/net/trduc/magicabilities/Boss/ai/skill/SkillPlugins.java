package net.trduc.magicabilitiesfork.Boss.ai.skill;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class SkillPlugins {
    private static final String PLUGIN_NAME = "MagicAbilitiesfork";

    private SkillPlugins() {
    }

    public static Plugin get(Entity anyEntity) {
        return anyEntity.getServer().getPluginManager().getPlugin(PLUGIN_NAME);
    }
}
