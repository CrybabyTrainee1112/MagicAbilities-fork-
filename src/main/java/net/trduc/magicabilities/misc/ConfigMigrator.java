package net.trduc.magicabilitiesfork.misc;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class ConfigMigrator {

    private static final int LATEST_CONFIG_VERSION   = 2;
    private static final int LATEST_COOLDOWN_VERSION = 1;
    private static final int LATEST_RANDOM_POWERS_VERSION = 1;

    private ConfigMigrator() {}

    public static void migrate(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        migrateMainConfig(plugin, log);
        migrateCooldownsConfig(plugin, log);
        migrateRandomPowersConfig(plugin, log);
    }

    private static void migrateMainConfig(JavaPlugin plugin, Logger log) {
        File cfgFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration current = YamlConfiguration.loadConfiguration(cfgFile);
        int version = current.getInt("config-version", 0);

        if (version >= LATEST_CONFIG_VERSION) return;

        log.info("[ConfigMigrator] config.yml is at version " + version
                + ", upgrading to version " + LATEST_CONFIG_VERSION + "...");

        FileConfiguration defaults = loadDefaultResource(plugin, "config.yml");
        if (defaults == null) {
            log.warning("[ConfigMigrator] Could not read default config.yml from jar!");
            return;
        }

        List<String> added = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (key.equals("config-version")) continue;
            ConfigurationSection sec = defaults.getConfigurationSection(key);
            if (sec != null) continue;
            if (!current.contains(key)) {
                current.set(key, defaults.get(key));
                added.add(key);
            }
        }

        current.set("config-version", LATEST_CONFIG_VERSION);

        try {
            current.save(cfgFile);
        } catch (Exception e) {
            log.warning("[ConfigMigrator] Could not save config.yml: " + e.getMessage());
            return;
        }
        plugin.reloadConfig();

        if (added.isEmpty()) {
            log.info("[ConfigMigrator] config.yml: no new keys to add.");
        } else {
            log.info("[ConfigMigrator] config.yml: added " + added.size() + " missing key(s):");
            for (String k : added) log.info("  + " + k + " = " + current.get(k));
        }
    }

    private static void migrateCooldownsConfig(JavaPlugin plugin, Logger log) {
        File cdFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        if (!cdFile.exists()) {
            plugin.saveResource("cooldowns.yml", false);
            log.info("[ConfigMigrator] cooldowns.yml not found, created from default.");
            return;
        }

        FileConfiguration current  = YamlConfiguration.loadConfiguration(cdFile);
        FileConfiguration defaults = loadDefaultResource(plugin, "cooldowns.yml");

        if (defaults == null) {
            log.warning("[ConfigMigrator] Could not read default cooldowns.yml from jar!");
            return;
        }

        int version = current.getInt("config-version", 0);
        if (version >= LATEST_COOLDOWN_VERSION) return;

        log.info("[ConfigMigrator] cooldowns.yml is at version " + version
                + ", upgrading to version " + LATEST_COOLDOWN_VERSION + "...");

        List<String> added = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (key.equals("config-version")) continue;
            ConfigurationSection sec = defaults.getConfigurationSection(key);
            if (sec != null) continue;
            if (!current.contains(key)) {
                current.set(key, defaults.get(key));
                added.add(key);
            }
        }

        current.set("config-version", LATEST_COOLDOWN_VERSION);

        try {
            current.save(cdFile);
        } catch (Exception e) {
            log.warning("[ConfigMigrator] Could not save cooldowns.yml: " + e.getMessage());
            return;
        }

        if (added.isEmpty()) {
            log.info("[ConfigMigrator] cooldowns.yml: no new keys to add.");
        } else {
            log.info("[ConfigMigrator] cooldowns.yml: added " + added.size() + " missing key(s):");
            for (String k : added) log.info("  + " + k + " = " + current.get(k));
        }
    }

    private static void migrateRandomPowersConfig(JavaPlugin plugin, Logger log) {
        File rpFile = new File(plugin.getDataFolder(), "random-powers.yml");
        if (!rpFile.exists()) {
            plugin.saveResource("random-powers.yml", false);
            log.info("[ConfigMigrator] random-powers.yml not found, created from default.");
            return;
        }

        FileConfiguration current  = YamlConfiguration.loadConfiguration(rpFile);
        FileConfiguration defaults = loadDefaultResource(plugin, "random-powers.yml");

        if (defaults == null) {
            log.warning("[ConfigMigrator] Could not read default random-powers.yml from jar!");
            return;
        }

        int version = current.getInt("config-version", 0);
        if (version >= LATEST_RANDOM_POWERS_VERSION) return;

        log.info("[ConfigMigrator] random-powers.yml is at version " + version
                + ", upgrading to version " + LATEST_RANDOM_POWERS_VERSION + "...");

        List<String> added = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (key.equals("config-version")) continue;
            ConfigurationSection sec = defaults.getConfigurationSection(key);
            if (sec != null) continue;
            if (!current.contains(key)) {
                current.set(key, defaults.get(key));
                added.add(key);
            }
        }

        current.set("config-version", LATEST_RANDOM_POWERS_VERSION);

        try {
            current.save(rpFile);
        } catch (Exception e) {
            log.warning("[ConfigMigrator] Could not save random-powers.yml: " + e.getMessage());
            return;
        }

        if (added.isEmpty()) {
            log.info("[ConfigMigrator] random-powers.yml: no new keys to add.");
        } else {
            log.info("[ConfigMigrator] random-powers.yml: added " + added.size() + " missing key(s):");
            for (String k : added) log.info("  + " + k + " = " + current.get(k));
        }
    }

    private static FileConfiguration loadDefaultResource(JavaPlugin plugin, String name) {
        try {
            InputStreamReader reader = new InputStreamReader(
                    plugin.getResource(name), StandardCharsets.UTF_8);
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            return null;
        }
    }
}

