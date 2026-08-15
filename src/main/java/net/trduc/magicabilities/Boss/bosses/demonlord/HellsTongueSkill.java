package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.skill.SkillPlugins;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class HellsTongueSkill extends AbstractSkill {
    private static final double RANGE = 8.0;
    private static final double SLASH_DAMAGE = 6.0;
    private static final double EXECUTE_THRESHOLD = 0.3;
    private static final double EXECUTE_BONUS_DAMAGE = 6.0;
    private static final double ARC_WIDTH_DEGREES = 50.0;

    public HellsTongueSkill() {
        super(new Builder("hells_tongue")
                .cost(1.0)
                .cooldownTicks(60)
                .precondition(Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, RANGE))
                .effect(Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.15))
                .targetKey(WorldStateKeys.NEAREST_THREAT)
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        if (target == null || target.isDead()) {
            return;
        }

        Plugin plugin = SkillPlugins.get(boss);
        if (plugin == null) {
            return;
        }

        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.7f);

        double targetHealthPercent = context.getWorldState().getValue(WorldStateKeys.TARGET_HEALTH_PERCENT, 1.0);
        double rootedTicksRemaining = context.getWorldState().getValue(WorldStateKeys.TARGET_ROOTED_TICKS, 0.0);
        boolean executeWindow = targetHealthPercent <= EXECUTE_THRESHOLD || rootedTicksRemaining > 0.0;
        double perSlashDamage = SLASH_DAMAGE + (executeWindow ? EXECUTE_BONUS_DAMAGE / 3.0 : 0.0);

        int[] slashDelays = {0, 3, 6};
        int currentPhase = context.getCurrentPhase();
        for (int slashIndex = 0; slashIndex < slashDelays.length; slashIndex++) {
            final int idx = slashIndex;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (boss.isDead()) {
                        return;
                    }
                    fireBlade(boss, target, idx, perSlashDamage, currentPhase);
                }
            }.runTaskLater(plugin, slashDelays[idx]);
        }
    }

    private void fireBlade(Mob boss, LivingEntity target, int idx, double damage, int currentPhase) {
        Vector toTarget = target.getLocation().toVector().subtract(boss.getLocation().toVector());
        if (toTarget.lengthSquared() < 0.01) {
            return;
        }
        toTarget.normalize();

        Set<LivingEntity> hit = new HashSet<>();
        double halfArcRad = Math.toRadians(ARC_WIDTH_DEGREES / 2.0);
        for (Entity entity : boss.getWorld().getNearbyEntities(boss.getLocation(), RANGE, RANGE / 2, RANGE)) {
            if (!(entity instanceof LivingEntity) || entity.equals(boss) || hit.contains(entity)) {
                continue;
            }
            Vector toEntity = entity.getLocation().toVector().subtract(boss.getLocation().toVector());
            if (toEntity.lengthSquared() < 0.01) {
                continue;
            }
            toEntity.normalize();
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, toTarget.dot(toEntity))));
            if (angle <= halfArcRad) {
                LivingEntity le = (LivingEntity) entity;
                hit.add(le);
                DamageAPI.dealDamage(boss, le, damage, currentPhase);
                le.setFireTicks(60);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, true));
            }
        }

        boss.getWorld().spawnParticle(Particle.DUST, boss.getLocation().add(toTarget.clone().multiply(2)).add(0, 1, 0),
                20, 0.6, 0.4, 0.6, 0, new Particle.DustOptions(Color.fromRGB(255, 70, 0), 1.6f));
        boss.getWorld().spawnParticle(Particle.FLAME, boss.getLocation().add(toTarget.clone().multiply(2)).add(0, 1, 0),
                10, 0.4, 0.3, 0.4, 0.03);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 1.3f);
    }
}
