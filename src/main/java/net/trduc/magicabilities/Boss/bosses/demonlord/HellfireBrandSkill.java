package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class HellfireBrandSkill extends AbstractSkill {
    private static final double RANGE = 10.0;
    private static final double BASE_DAMAGE = 7.0;
    private static final double EXECUTE_THRESHOLD = 0.3;
    private static final double EXECUTE_BONUS_DAMAGE = 9.0;

    public HellfireBrandSkill() {
        super(new Builder("hellfire_brand")
                .cooldownTicks(80)
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
        if (boss.getLocation().distance(target.getLocation()) > RANGE) {
            return;
        }

        double targetHealthPercent = context.getWorldState().getValue(WorldStateKeys.TARGET_HEALTH_PERCENT, 1.0);
        double damage = BASE_DAMAGE + (targetHealthPercent <= EXECUTE_THRESHOLD ? EXECUTE_BONUS_DAMAGE : 0.0);
        DamageAPI.dealDamage(boss, target, damage, context.getCurrentPhase());

        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
    }
}
