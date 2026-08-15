package net.trduc.magicabilitiesfork.Boss.ai.skill;

import org.bukkit.entity.Mob;
import org.bukkit.entity.LivingEntity;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SkillExecutor {
    private static final long MS_PER_TICK = 50L;

    private static boolean debugEnabled = false;

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    private final Mob boss;
    private final Map<String, Long> skillCooldownExpiry;

    public SkillExecutor(Mob boss) {
        this.boss = Objects.requireNonNull(boss, "Boss cannot be null");
        this.skillCooldownExpiry = new HashMap<>();
    }

    public boolean tryExecute(Skill skill, SkillContext context) {
        Objects.requireNonNull(skill, "Skill cannot be null");
        Objects.requireNonNull(context, "SkillContext cannot be null");

        if (!skill.canExecute(context)) {
            if (debugEnabled) {
                org.bukkit.Bukkit.getLogger().info("[Boss] " + boss.getName() + " skipped '" + skill.getId()
                        + "': precondition not met");
            }
            return false;
        }

        if (!isCooldownExpired(skill.getId())) {
            if (debugEnabled) {
                org.bukkit.Bukkit.getLogger().info("[Boss] " + boss.getName() + " skipped '" + skill.getId()
                        + "': on cooldown (" + getRemainingCooldownMs(skill.getId()) + "ms left)");
            }
            return false;
        }

        try {
            skill.execute(context);
            skill.applyEffect(context);
            recordSkillCast(skill.getId(), skill.getCooldownTicks());
            if (debugEnabled) {
                org.bukkit.Bukkit.getLogger().info("[Boss] " + boss.getName() + " cast '" + skill.getId() + "'");
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error executing skill " + skill.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isCooldownExpired(String skillId) {
        Objects.requireNonNull(skillId, "Skill ID cannot be null");

        Long expireAt = skillCooldownExpiry.get(skillId);
        if (expireAt == null) {
            return true;
        }

        return System.currentTimeMillis() >= expireAt;
    }

    public long getRemainingCooldownMs(String skillId) {
        Objects.requireNonNull(skillId, "Skill ID cannot be null");

        Long expireAt = skillCooldownExpiry.get(skillId);
        if (expireAt == null) {
            return 0;
        }

        long remaining = expireAt - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    private void recordSkillCast(String skillId, int cooldownTicks) {
        long cooldownMs = Math.max(0, cooldownTicks) * MS_PER_TICK;
        skillCooldownExpiry.put(skillId, System.currentTimeMillis() + cooldownMs);
    }

    public void resetCooldown(String skillId) {
        skillCooldownExpiry.remove(skillId);
    }

    public void resetAllCooldowns() {
        skillCooldownExpiry.clear();
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        skillCooldownExpiry.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
