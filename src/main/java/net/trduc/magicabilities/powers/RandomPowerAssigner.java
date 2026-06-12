package net.trduc.magicabilities.powers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomPowerAssigner {

    private static final List<PowerType> POOL = Collections.unmodifiableList(Arrays.asList(
            PowerType.ICE,
            PowerType.LIGHTNING,
            PowerType.SHOGUN,
            PowerType.FIRE,
            PowerType.WITCHER,
            PowerType.NATURE,
            PowerType.PHOENIX,
            PowerType.TWILIGHT_MIRAGE,
            PowerType.ETERNITY,
            PowerType.CURSEWEAVER,
            PowerType.THUNDER_GOD,
            PowerType.WIND,
            PowerType.DEMON,
            PowerType.WATER,
            PowerType.Warp,
            PowerType.WITHER,
            PowerType.ICE_DRAGON,
            PowerType.WOOD_DRAGON,
            PowerType.SNOWPARTING_BLADE,
            PowerType.METEOR_LORD
    ));

    private static final Random RNG = new Random();

    public static PowerType randomPower() {
        return POOL.get(RNG.nextInt(POOL.size()));
    }

    public static List<PowerType> getPool() {
        return POOL;
    }
}
