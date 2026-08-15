package net.trduc.magicabilitiesfork.Boss.core;

import java.util.*;
import java.util.stream.Collectors;

public class BossRegistry {
    private static final Map<String, BossType> REGISTERED_TYPES = new HashMap<>();

    public static void register(BossType bossType) {
        Objects.requireNonNull(bossType, "BossType cannot be null");
        REGISTERED_TYPES.put(bossType.getId(), bossType);
    }

    public static Optional<BossType> get(String bossTypeId) {
        Objects.requireNonNull(bossTypeId, "Boss type ID cannot be null");
        return Optional.ofNullable(REGISTERED_TYPES.get(bossTypeId));
    }

    public static boolean isRegistered(String bossTypeId) {
        Objects.requireNonNull(bossTypeId, "Boss type ID cannot be null");
        return REGISTERED_TYPES.containsKey(bossTypeId);
    }

    public static Collection<BossType> getAll() {
        return Collections.unmodifiableCollection(REGISTERED_TYPES.values());
    }

    public static Set<String> getAllIds() {
        return Collections.unmodifiableSet(REGISTERED_TYPES.keySet());
    }

    public static int getRegisteredCount() {
        return REGISTERED_TYPES.size();
    }

    public static void clear() {
        REGISTERED_TYPES.clear();
    }

    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Registered boss types: ").append(REGISTERED_TYPES.size()).append("\n");
        for (BossType type : REGISTERED_TYPES.values()) {
            sb.append("  - ").append(type).append("\n");
        }
        return sb.toString();
    }
}
