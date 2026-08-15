package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.skill.SkillPlugins;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class JudgmentSkill extends AbstractSkill {
    private static final int CHARGE_TICKS = 60;
    private static final double RADIUS = 15.0;
    private static final double BASE_DAMAGE = 20.0;
    private static final double MAX_HEALTH_DAMAGE_FRACTION = 0.30;
    private static final double CHARGE_COST = 1.0;

    public JudgmentSkill() {
        super(new Builder("judgment")
                .cost(0.5)
                .cooldownTicks(900)
                .precondition(Condition.greaterOrEqual(WorldStateKeys.DEMON_BLOOD_CHARGE, CHARGE_COST))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -1.0),
                        Effect.setValue(WorldStateKeys.DEMON_BLOOD_CHARGE, 0.0)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        World world = boss.getWorld();
        Plugin plugin = SkillPlugins.get(boss);
        if (plugin == null) {
            return;
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, CHARGE_TICKS + 10, 3, false, false));
        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
        world.playSound(boss.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.2f);

        new BukkitRunnable() {
            int ct = CHARGE_TICKS;

            @Override
            public void run() {
                if (boss.isDead()) {
                    cancel();
                    return;
                }
                if (ct <= 0) {
                    cancel();
                    release(boss, world, plugin, context.getCurrentPhase());
                    return;
                }

                Location loc = boss.getLocation().clone().add(0, 1, 0);
                double r = 3.0 - (CHARGE_TICKS - ct) * 0.045;
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(i * 36 + (CHARGE_TICKS - ct) * 14);
                    Location lp = loc.clone().add(Math.cos(a) * Math.max(0.2, r), Math.random() * 2.5, Math.sin(a) * Math.max(0.2, r));
                    world.spawnParticle(Particle.DUST, lp, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(i % 2 == 0 ? Color.fromRGB(255, 70, 0) : Color.fromRGB(200, 5, 5), 1.4f));
                }
                if (ct % 10 == 0) {
                    world.playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.3f + (CHARGE_TICKS - ct) * 0.015f);
                    for (Entity entity : world.getNearbyEntities(loc, RADIUS, 8, RADIUS)) {
                        if (entity.equals(boss) || !(entity instanceof LivingEntity)) {
                            continue;
                        }
                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15, 0, false, false));
                    }
                }
                ct--;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void release(Mob boss, World world, Plugin plugin, int currentPhase) {
        world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.4f);
        world.playSound(boss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
        Location epicenter = boss.getLocation().clone().add(0, 1, 0);

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(epicenter, RADIUS, 8, RADIUS)) {
            if (entity.equals(boss) || !(entity instanceof LivingEntity)) {
                continue;
            }
            targets.add((LivingEntity) entity);
        }

        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!target.isValid() || target.isDead()) {
                        return;
                    }
                    Location targetLoc = target.getLocation().clone();

                    AttributeInstance maxHealthAttr = target.getAttribute(Attribute.MAX_HEALTH);
                    double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
                    double damage = BASE_DAMAGE + maxHealth * MAX_HEALTH_DAMAGE_FRACTION;

                    DamageAPI.dealDamage(boss, target, damage, currentPhase);
                    target.setFireTicks(120);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 2, false, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3, false, true));

                    targetLoc.getWorld().strikeLightningEffect(targetLoc);
                    world.spawnParticle(Particle.DUST, targetLoc.clone().add(0, 0.5, 0), 25,
                            0.6, 0.5, 0.6, 0, new Particle.DustOptions(Color.fromRGB(255, 160, 30), 2.2f));
                    world.spawnParticle(Particle.LAVA, targetLoc, 8, 0.5, 0.3, 0.5, 0.1);
                    world.playSound(targetLoc, Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);
                    world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.7f);
                }
            }.runTaskLater(plugin, i * 5L);
        }

        world.spawnParticle(Particle.DUST, epicenter, 80, 3.0, 3.0, 3.0, 0,
                new Particle.DustOptions(Color.fromRGB(200, 5, 5), 2.0f));
        world.spawnParticle(Particle.LAVA, epicenter, 30, 3.0, 1.5, 3.0, 0.15);
    }
}
