package net.trduc.magicabilitiesfork.Boss.ai.worldstate;

public final class WorldStateKeys {


    public static final WorldKey BOSS_HEALTH_PERCENT = new WorldKey("boss_health_percent");
    public static final WorldKey BOSS_CURRENT_PHASE = new WorldKey("boss_current_phase");

    public static final WorldKey NEAREST_THREAT_DISTANCE = new WorldKey("nearest_threat_distance");
    public static final WorldKey HIGHEST_THREAT_AGGRO = new WorldKey("highest_threat_aggro");
    public static final WorldKey NEARBY_PLAYER_COUNT = new WorldKey("nearby_player_count");

    public static final WorldKey BOSS_POSITION_X = new WorldKey("boss_position_x");
    public static final WorldKey BOSS_POSITION_Y = new WorldKey("boss_position_y");
    public static final WorldKey BOSS_POSITION_Z = new WorldKey("boss_position_z");

    public static final WorldKey TIME_OF_DAY = new WorldKey("time_of_day");
    public static final WorldKey IS_RAINING = new WorldKey("is_raining");

    public static final WorldKey PLAYERS_CLUSTERED = new WorldKey("players_clustered");
    public static final WorldKey ARENA_HAS_HAZARDS = new WorldKey("arena_has_hazards");

    public static final WorldKey TARGET_HEALTH_PERCENT = new WorldKey("target_health_percent");
    public static final WorldKey TARGET_ROOTED_TICKS = new WorldKey("target_rooted_ticks");
    public static final WorldKey REINFORCEMENTS_COOLDOWN_TICKS = new WorldKey("reinforcements_cooldown_ticks");

    public static final WorldKey DEMON_BLOOD_CHARGE = new WorldKey("demon_blood_charge");


    public static final TargetKey NEAREST_THREAT = new TargetKey("nearest_threat");
    public static final TargetKey HIGHEST_THREAT = new TargetKey("highest_threat");
    public static final TargetKey ARENA_CENTER = new TargetKey("arena_center");

    public static final TargetKey CLUSTER_CENTER = new TargetKey("cluster_center");
    public static final TargetKey ESCAPE_POINT = new TargetKey("escape_point");

    private WorldStateKeys() {
    }
}
