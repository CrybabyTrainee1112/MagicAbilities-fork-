package net.trduc.magicabilitiesfork.powers;

import net.trduc.magicabilitiesfork.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RandomPowerAssigner {

    private static List<PowerType> pool = Collections.emptyList();
    private static final Random RNG = new Random();

    private RandomPowerAssigner() {}

    public static void loadPool(FileConfiguration config) {
        List<PowerType> excluded = new ArrayList<>();
        for (String s : config.getStringList("exclude")) {
            try {
                excluded.add(PowerType.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        List<PowerType> loaded = new ArrayList<>();
        for (String s : config.getStringList("pool")) {
            try {
                PowerType pt = PowerType.valueOf(s.trim().toUpperCase());
                if (!excluded.contains(pt) && !loaded.contains(pt)) {
                    loaded.add(pt);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        pool = Collections.unmodifiableList(loaded);
    }

    public static PowerType randomPower(PlayerData pd) {
        List<PowerType> bag = parseBag(pd.getRandomBag());

        if (bag.isEmpty()) {
            bag = new ArrayList<>(pool);
        }

        if (bag.isEmpty()) {
            pd.setRandomBag("");
            return PowerType.NONE;
        }

        PowerType chosen = bag.remove(RNG.nextInt(bag.size()));
        pd.setRandomBag(serializeBag(bag));
        return chosen;
    }

    private static List<PowerType> parseBag(String raw) {
        List<PowerType> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return list;
        for (String s : raw.split(",")) {
            if (s.trim().isEmpty()) continue;
            try {
                list.add(PowerType.valueOf(s.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    private static String serializeBag(List<PowerType> bag) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bag.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(bag.get(i).name());
        }
        return sb.toString();
    }

    public static List<PowerType> getPool() {
        return pool;
    }
}
