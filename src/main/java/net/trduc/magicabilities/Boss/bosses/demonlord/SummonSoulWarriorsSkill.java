package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.skill.SkillPlugins;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SummonSoulWarriorsSkill extends AbstractSkill {
    private static final int SUMMON_COUNT = 2;
    private static final int DURATION_TICKS = 160;
    private static final double CHARGE_GAIN = 0.25;
    private static final int COOLDOWN_TICKS = 500;

    public SummonSoulWarriorsSkill() {
        super(new Builder("summon_soul_warriors")
                .cost(1.8)
                .cooldownTicks(COOLDOWN_TICKS)
                .precondition(Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 1))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.DEMON_BLOOD_CHARGE, CHARGE_GAIN),
                        Effect.setValue(WorldStateKeys.REINFORCEMENTS_COOLDOWN_TICKS, COOLDOWN_TICKS)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        World world = boss.getWorld();
        Plugin plugin = SkillPlugins.get(boss);
        if (plugin == null) {
            return;
        }

        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.9f, 0.6f);
        world.playSound(boss.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.7f, 0.5f);

        List<WitherSkeleton> summoned = new ArrayList<>();
        Random rng = new Random();

        for (int i = 0; i < SUMMON_COUNT; i++) {
            double angle = Math.toRadians(i * (360.0 / SUMMON_COUNT));
            Location spawnLoc = boss.getLocation().clone().add(Math.cos(angle) * 1.8, 0, Math.sin(angle) * 1.8);

            for (int p = 0; p < 10; p++) {
                world.spawnParticle(Particle.DUST,
                        spawnLoc.clone().add(rng.nextDouble() * 0.6 - 0.3, rng.nextDouble() * 2, rng.nextDouble() * 0.6 - 0.3),
                        1, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.fromRGB(0, 130, 140), 1.4f));
            }
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc.clone().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0.04);

            WitherSkeleton skeleton = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
            skeleton.setCustomName("§5Soul Warrior");
            skeleton.setCustomNameVisible(true);
            skeleton.setAI(true);
            if (target != null && !target.isDead()) {
                skeleton.setTarget(target);
            }
            summoned.add(skeleton);
        }

        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.4f);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (WitherSkeleton skeleton : summoned) {
                    if (skeleton != null && !skeleton.isDead() && skeleton.isValid()) {
                        world.spawnParticle(Particle.DUST, skeleton.getLocation().add(0, 1, 0), 15,
                                0.3, 0.5, 0.3, 0, new Particle.DustOptions(Color.fromRGB(0, 130, 140), 1.5f));
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, skeleton.getLocation().add(0, 1, 0), 6, 0.2, 0.3, 0.2, 0.04);
                        world.playSound(skeleton.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 1.6f);
                        skeleton.remove();
                    }
                }
            }
        }.runTaskLater(plugin, DURATION_TICKS);
    }
}
