package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class AssassinPower extends Power implements IdlePower, Removeable {

    private static final String AS_SHADOWSTEP = "assassin.shadowstep";
    private static final String AS_KNIVES     = "assassin.knives";
    private static final String AS_SMOKEVEIL  = "assassin.smokeveil";
    private static final String AS_BACKSTAB   = "assassin.backstab";
    private static final String AS_CLONE      = "assassin.clone";
    private static final String AS_VANISH     = "assassin.vanish";
    private static final String AS_ULTIMATE   = "assassin.nightofcuts";

    private static final Color C_SHADOW = Color.fromRGB(40, 15, 65);
    private static final Color C_DUSK   = Color.fromRGB(95, 40, 135);
    private static final Color C_MOON   = Color.fromRGB(180, 210, 255);
    private static final Color C_EDGE   = Color.fromRGB(225, 225, 235);
    private static final Color C_BLEED  = Color.fromRGB(150, 20, 40);

    private double SHADOWSTEP_DISTANCE;
    private int    SHADOWSTEP_SPEED_TICKS;

    private int    KNIVES_COUNT;
    private double KNIVES_DMG;
    private int    KNIVES_BLEED_MAX_STACKS;
    private double KNIVES_BLEED_DURATION;
    private double KNIVES_BLEED_DMG_PER_STACK;

    private double SMOKEVEIL_RADIUS;
    private int    SMOKEVEIL_DURATION_TICKS;
    private int    SMOKEVEIL_ENEMY_EFFECT_TICKS;

    private double BACKSTAB_BASE_DMG;
    private double BACKSTAB_BONUS_DMG;
    private double BACKSTAB_DASH_DISTANCE;

    private int    CLONE_DURATION_TICKS;

    private double VANISH_STEALTHED_DMG;
    private double VANISH_UNSTEALTHED_DMG;
    private double VANISH_RANGE;

    private int    ULT_XP_COST;
    private int    ULT_DURATION_TICKS;
    private double ULT_RADIUS;
    private double ULT_HIT_DMG;
    private double ULT_FINISHER_DMG;

    private final Map<UUID, Integer> bleedStacks = new HashMap<>();
    private final Map<UUID, Long>    bleedUntil  = new HashMap<>();
    private long        stealthUntil = 0L;
    private ArmorStand  activeClone  = null;
    private final Random rng = new Random();

    public AssassinPower(Player owner) {
        super(owner);
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration cfg = magicPlugin.getConfig();

        SHADOWSTEP_DISTANCE        = cfg.getDouble("assassin.dmg.shadowstep-distance", 6.0);
        SHADOWSTEP_SPEED_TICKS     = cfg.getInt("assassin.dmg.shadowstep-speed-ticks", 40);

        KNIVES_COUNT                = cfg.getInt("assassin.dmg.knives-count", 3);
        KNIVES_DMG                  = cfg.getDouble("assassin.dmg.knives-dmg", 2.0);
        KNIVES_BLEED_MAX_STACKS     = cfg.getInt("assassin.dmg.knives-bleed-max-stacks", 5);
        KNIVES_BLEED_DURATION       = cfg.getDouble("assassin.dmg.knives-bleed-duration", 6.0);
        KNIVES_BLEED_DMG_PER_STACK  = cfg.getDouble("assassin.dmg.knives-bleed-dmg-per-stack", 0.5);

        SMOKEVEIL_RADIUS             = cfg.getDouble("assassin.dmg.smokeveil-radius", 3.0);
        SMOKEVEIL_DURATION_TICKS     = cfg.getInt("assassin.dmg.smokeveil-duration-ticks", 100);
        SMOKEVEIL_ENEMY_EFFECT_TICKS = cfg.getInt("assassin.dmg.smokeveil-enemy-effect-ticks", 60);

        BACKSTAB_BASE_DMG      = cfg.getDouble("assassin.dmg.backstab-base-dmg", 5.0);
        BACKSTAB_BONUS_DMG     = cfg.getDouble("assassin.dmg.backstab-bonus-dmg", 6.0);
        BACKSTAB_DASH_DISTANCE = cfg.getDouble("assassin.dmg.backstab-dash-distance", 3.0);

        CLONE_DURATION_TICKS = cfg.getInt("assassin.dmg.clone-duration-ticks", 100);

        VANISH_STEALTHED_DMG   = cfg.getDouble("assassin.dmg.vanish-stealthed-dmg", 12.0);
        VANISH_UNSTEALTHED_DMG = cfg.getDouble("assassin.dmg.vanish-unstealthed-dmg", 6.0);
        VANISH_RANGE            = cfg.getDouble("assassin.dmg.vanish-range", 10.0);

        ULT_XP_COST        = cfg.getInt("assassin.xp.nightofcuts", 30);
        ULT_DURATION_TICKS = cfg.getInt("assassin.dmg.ult-duration-ticks", 70);
        ULT_RADIUS          = cfg.getDouble("assassin.dmg.ult-radius", 8.0);
        ULT_HIT_DMG          = cfg.getDouble("assassin.dmg.ult-hit-dmg", 2.5);
        ULT_FINISHER_DMG     = cfg.getDouble("assassin.dmg.ult-finisher-dmg", 7.0);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedExecute) {
            preventSelfDamage((DamagedExecute) ex);
            return;
        }
        if (ex instanceof DamagedByExecute) {
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) { onLeft((LeftClickExecute) ex); return; }
        if (ex instanceof SneakExecute)     { onSneak((SneakExecute) ex); }
    }

    private void preventSelfDamage(DamagedExecute ex) {
        EntityDamageEvent ev = (EntityDamageEvent) ex.getRawEvent();
        if (ev.getCause() == EntityDamageEvent.DamageCause.FALL) {
            ev.setCancelled(true);
        }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0:
                if (onCd(AS_SHADOWSTEP, p, this)) return;
                shadowStep(p);
                addCd(AS_SHADOWSTEP, p);
                return;
            case 1:
                if (onCd(AS_KNIVES, p, this)) return;
                throwingKnives(p);
                addCd(AS_KNIVES, p);
                return;
            case 2:
                if (onCd(AS_SMOKEVEIL, p, this)) return;
                smokeVeil(p);
                addCd(AS_SMOKEVEIL, p);
                return;
            case 3:
                if (onCd(AS_BACKSTAB, p, this)) return;
                backstab(p);
                addCd(AS_BACKSTAB, p);
                return;
            case 4:
                if (onCd(AS_CLONE, p, this)) return;
                shadowClone(p);
                addCd(AS_CLONE, p);
                return;
            case 5:
                if (onCd(AS_VANISH, p, this)) return;
                vanishStrike(p);
                addCd(AS_VANISH, p);
                return;
            default:
        }
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        if (onCd(AS_ULTIMATE, p, this)) return;
        if (!checkXp(p, ULT_XP_COST, this)) return;
        spendXp(p, ULT_XP_COST);
        nightOfCuts(p);
        addCd(AS_ULTIMATE, p);
    }


    private boolean isStealthed(Player p) {
        return stealthUntil > System.currentTimeMillis();
    }

    private void grantStealth(Player p, int ticks) {
        stealthUntil = System.currentTimeMillis() + ticks * 50L;
        applyPotionSilent(p, PotionEffectType.INVISIBILITY, ticks + 5, 0);
    }

    private void breakStealth(Player p) {
        stealthUntil = 0L;
        removePotion(p, PotionEffectType.INVISIBILITY);
    }


    private void shadowStep(Player p) {
        Vector dir = p.getLocation().getDirection().normalize();
        Location from = p.getLocation().clone();
        Location dest = safeStepDestination(from, dir, SHADOWSTEP_DISTANCE);

        smokePuff(from.clone().add(0, 1, 0), 14);
        p.teleport(dest);
        smokePuff(p.getLocation().clone().add(0, 1, 0), 18);
        p.getWorld().playSound(from, Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.4f);

        applyPotion(p, PotionEffectType.SPEED, SHADOWSTEP_SPEED_TICKS, 1);
        shadowTrail(from.clone().add(0, 1, 0), p.getLocation().clone().add(0, 1, 0));
    }


    private void throwingKnives(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.7f);
        Vector base = p.getLocation().getDirection().normalize();
        double spreadDeg = 8.0;
        for (int i = 0; i < KNIVES_COUNT; i++) {
            double offset = (i - (KNIVES_COUNT - 1) / 2.0) * spreadDeg;
            Vector dir = rotateAroundY(base.clone(), offset);
            throwKnife(p, dir);
        }
    }

    private void throwKnife(Player p, Vector dir) {
        new BukkitRunnable() {
            final Location cur = p.getEyeLocation().clone();
            final Set<UUID> hit = new HashSet<>();
            int t = 0;

            @Override
            public void run() {
                if (t >= 24 || !p.isOnline()) { cancel(); return; }
                cur.add(dir.clone().multiply(1.3));

                if (!cur.getBlock().isPassable()) { cancel(); return; }

                particleApi.spawnColoredParticles(cur, C_EDGE, 0.28f, 1, 0.02, 0.02, 0.02);
                particleApi.spawnParticles(cur, Particle.SMOKE, 1, 0.02, 0.02, 0.02, 0.0);

                for (LivingEntity e : nearbyLiving(cur, 1.1)) {
                    if (e.equals(p) || hit.contains(e.getUniqueId())) continue;
                    hit.add(e.getUniqueId());
                    e.damage(KNIVES_DMG, p);
                    addBleed(e);
                    duskSpark(e.getLocation().clone().add(0, e.getHeight() * 0.6, 0), C_BLEED, 0.6f, 6);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void addBleed(LivingEntity e) {
        UUID id = e.getUniqueId();
        int stacks = bleedStacks.getOrDefault(id, 0);
        bleedStacks.put(id, Math.min(KNIVES_BLEED_MAX_STACKS, stacks + 1));
        bleedUntil.put(id, System.currentTimeMillis() + (long) (KNIVES_BLEED_DURATION * 1000));
    }


    private void smokeVeil(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.6f);
        final Location center = p.getLocation().clone();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= SMOKEVEIL_DURATION_TICKS || !p.isOnline()) { cancel(); return; }

                if (t % 4 == 0) veilRing(center, SMOKEVEIL_RADIUS, t * 10);
                if (t % 20 == 0) {
                    particleApi.spawnParticles(center.clone().add(0, 0.2, 0), Particle.CAMPFIRE_COSY_SMOKE, 6,
                            SMOKEVEIL_RADIUS * 0.5, 0.3, SMOKEVEIL_RADIUS * 0.5, 0.02);
                }

                if (t % 10 == 0) {
                    if (p.getLocation().distance(center) <= SMOKEVEIL_RADIUS) {
                        grantStealth(p, 15);
                        applyPotion(p, PotionEffectType.SPEED, 15, 0);
                    }
                    for (LivingEntity e : nearbyLiving(center, SMOKEVEIL_RADIUS)) {
                        if (e.equals(p)) continue;
                        applyPotion(e, PotionEffectType.BLINDNESS, SMOKEVEIL_ENEMY_EFFECT_TICKS, 0);
                        applyPotion(e, PotionEffectType.NAUSEA, SMOKEVEIL_ENEMY_EFFECT_TICKS, 0);
                        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_CAT_HISS, 0.5f, 0.8f);
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }


    private void backstab(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Location dashFrom = p.getLocation().clone();
        Location dashTo = safeStepDestination(dashFrom, dir, BACKSTAB_DASH_DISTANCE);

        smokePuff(dashFrom.clone().add(0, 1, 0), 8);
        p.teleport(dashTo);
        shadowTrail(dashFrom.clone().add(0, 1, 0), p.getLocation().clone().add(0, 1, 0));

        LivingEntity target = nearbyClosest(p.getLocation(), 2.6, p);
        if (target == null) {
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.3f, 1.8f);
            return;
        }

        boolean fromBehind = isBehind(p, target);
        double dmg = BACKSTAB_BASE_DMG + (fromBehind ? BACKSTAB_BONUS_DMG : 0);

        bladeFlash(p.getLocation().clone().add(0, 1.1, 0), target.getLocation().clone().add(0, 1.1, 0),
                fromBehind ? C_MOON : C_EDGE);
        target.damage(dmg, p);
        target.setVelocity(target.getVelocity().add(dir.clone().multiply(0.35)).setY(0.12));

        if (fromBehind) {
            p.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.3f);
            duskSpark(target.getLocation().clone().add(0, target.getHeight() * 0.7, 0), C_MOON, 1.0f, 14);
            sendActionBar(p, "§d✦ Backstab! §f" + String.format("%.1f", dmg) + " dmg");
        } else {
            p.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.0f);
        }
    }

    private boolean isBehind(Player attacker, LivingEntity target) {
        Vector facing = target.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.0001) return false;
        facing.normalize();
        Vector towardAttacker = attacker.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
        if (towardAttacker.lengthSquared() < 0.0001) return false;
        towardAttacker.normalize();
        return facing.dot(towardAttacker) > 0.5;
    }


    private void shadowClone(Player p) {
        destroyClone();

        Location loc = p.getLocation().clone();
        ArmorStand clone = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        clone.setBasePlate(false);
        clone.setArms(true);
        clone.setMarker(false);
        clone.setGravity(false);
        clone.setCollidable(false);
        clone.setInvulnerable(true);
        clone.setSilent(true);
        clone.setSmall(false);
        clone.setPersistent(false);
        clone.setHeadPose(new org.bukkit.util.EulerAngle(0, Math.toRadians(rng.nextInt(360)), 0));

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            skull.setItemMeta(meta);
        }
        clone.getEquipment().setHelmet(skull);
        clone.getEquipment().setChestplate(p.getInventory().getChestplate());
        clone.getEquipment().setLeggings(p.getInventory().getLeggings());
        clone.getEquipment().setBoots(p.getInventory().getBoots());
        clone.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand());

        activeClone = clone;
        grantStealth(p, CLONE_DURATION_TICKS);
        applyPotion(p, PotionEffectType.SPEED, CLONE_DURATION_TICKS, 0);
        smokePuff(loc.clone().add(0, 1, 0), 20);
        p.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 1f, 0.7f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= CLONE_DURATION_TICKS || !clone.isValid()) {
                    if (clone.isValid()) {
                        smokePuff(clone.getLocation().clone().add(0, 1, 0), 16);
                        clone.getWorld().playSound(clone.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.3f);
                    }
                    if (activeClone == clone) destroyClone();
                    cancel();
                    return;
                }
                if (t % 5 == 0) {
                    particleApi.spawnParticles(clone.getLocation().clone().add(0, 1.0, 0), Particle.SMOKE, 2,
                            0.2, 0.4, 0.2, 0.01);
                }
                if (t % 10 == 0) {
                    for (Entity e : clone.getNearbyEntities(6, 4, 6)) {
                        if (e instanceof Mob) {
                            Mob m = (Mob) e;
                            if (m.getTarget() != null && m.getTarget().equals(p)) {
                                m.setTarget(clone);
                            }
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void destroyClone() {
        if (activeClone != null) {
            if (activeClone.isValid()) activeClone.remove();
            activeClone = null;
        }
    }


    private void vanishStrike(Player p) {
        boolean stealthed = isStealthed(p);
        LivingEntity target = stealthed
                ? nearbyClosest(p.getLocation(), VANISH_RANGE, p)
                : rayTraceLiving(p, VANISH_RANGE);

        if (target == null) {
            sendActionBar(p, ChatColor.GRAY + "No target found.");
            return;
        }

        if (stealthed) {
            breakStealth(p);

            Vector approachDir = p.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
            if (approachDir.lengthSquared() < 0.01) approachDir = new Vector(1, 0, 0);
            approachDir.normalize();
            Location behind = target.getLocation().clone().add(approachDir.multiply(1.1));
            behind.setDirection(target.getLocation().toVector().subtract(behind.toVector()));

            Location from = p.getLocation().clone();
            smokePuff(from.clone().add(0, 1, 0), 16);
            if (behind.getBlock().isPassable()) p.teleport(behind);
            smokePuff(p.getLocation().clone().add(0, 1, 0), 20);

            bladeFlash(p.getLocation().clone().add(0, 1.1, 0), target.getLocation().clone().add(0, 1.1, 0), C_MOON);
            target.damage(VANISH_STEALTHED_DMG, p);
            target.setVelocity(new Vector(0, 0.35, 0));
            p.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.35f, 1.9f);
            duskSpark(target.getLocation().clone().add(0, target.getHeight() * 0.6, 0), C_MOON, 1.3f, 22);
            sendActionBar(p, "§d✦ Vanish Strike! §f" + String.format("%.1f", VANISH_STEALTHED_DMG) + " dmg");
        } else {
            Vector dir = target.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (dir.lengthSquared() < 0.0001) dir = new Vector(1, 0, 0);
            dir.normalize();
            bladeFlash(p.getLocation().clone().add(0, 1.1, 0), target.getLocation().clone().add(0, 1.1, 0), C_EDGE);
            target.damage(VANISH_UNSTEALTHED_DMG, p);
            target.setVelocity(dir.multiply(0.3).setY(0.1));
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
        }
    }


    private void nightOfCuts(Player p) {
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity e : nearbyLiving(p.getLocation(), ULT_RADIUS)) {
            if (!e.equals(p)) targets.add(e);
        }
        if (targets.isEmpty()) {
            sendActionBar(p, ChatColor.GRAY + "No enemies nearby.");
            return;
        }

        p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Night of a Thousand Cuts");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.8f);
        grantStealth(p, ULT_DURATION_TICKS + 10);
        applyPotion(p, PotionEffectType.SPEED, ULT_DURATION_TICKS + 10, 2);

        new BukkitRunnable() {
            int t = 0;
            int idx = 0;
            LivingEntity lastHit = null;

            @Override
            public void run() {
                List<LivingEntity> alive = new ArrayList<>();
                for (LivingEntity e : targets) if (e.isValid()) alive.add(e);

                if (t >= ULT_DURATION_TICKS || alive.isEmpty() || !p.isOnline()) {
                    breakStealth(p);
                    LivingEntity finisher = (lastHit != null && lastHit.isValid())
                            ? lastHit
                            : (alive.isEmpty() ? null : alive.get(0));
                    if (finisher != null) finishNightOfCuts(p, finisher);
                    cancel();
                    return;
                }

                if (t % 5 == 0) {
                    LivingEntity tgt = alive.get(idx % alive.size());
                    idx++;
                    lastHit = tgt;
                    blinkStrike(p, tgt);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void blinkStrike(Player p, LivingEntity target) {
        Vector approachDir = target.getLocation().getDirection().multiply(-1).setY(0);
        if (approachDir.lengthSquared() < 0.01) approachDir = new Vector(1, 0, 0);
        approachDir.normalize();
        Location behind = target.getLocation().clone().add(approachDir.multiply(1.0));
        behind.setDirection(target.getLocation().toVector().subtract(behind.toVector()));
        if (behind.getBlock().isPassable()) p.teleport(behind);

        bladeFlash(p.getLocation().clone().add(0, 1.1, 0), target.getLocation().clone().add(0, 1.1, 0), C_DUSK);
        target.damage(ULT_HIT_DMG, p);
        p.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.8f);
    }

    private void finishNightOfCuts(Player p, LivingEntity target) {
        if (!target.isValid()) return;
        target.damage(ULT_FINISHER_DMG, p);
        target.setVelocity(new Vector(0, 0.7, 0));
        Location loc = target.getLocation().clone().add(0, target.getHeight() * 0.6, 0);
        duskSpark(loc, C_MOON, 1.6f, 30);
        duskSpark(loc, C_DUSK, 1.2f, 20);
        target.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.4f, 1.9f);
    }


    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (isAuraEnabled(p) && t % 12 == 0) {
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 0.1, 0), C_SHADOW, 0.3f, 1,
                            0.2, 0.02, 0.2);
                }
                if (t % 20 == 0) tickBleeds();
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 1);
        return r;
    }

    private void tickBleeds() {
        if (bleedUntil.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = bleedUntil.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID id = entry.getKey();

            if (entry.getValue() <= now) {
                it.remove();
                bleedStacks.remove(id);
                continue;
            }
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof LivingEntity) || e.isDead() || !e.isValid()) {
                it.remove();
                bleedStacks.remove(id);
                continue;
            }
            int stacks = bleedStacks.getOrDefault(id, 0);
            if (stacks <= 0) continue;
            LivingEntity le = (LivingEntity) e;
            le.damage(stacks * KNIVES_BLEED_DMG_PER_STACK, getOwner());
            particleApi.spawnColoredParticles(le.getLocation().clone().add(0, le.getHeight() * 0.5, 0),
                    C_BLEED, 0.35f, stacks, 0.15, 0.2, 0.15);
        }
    }


    @Override
    public void remove() {
        destroyClone();
        bleedStacks.clear();
        bleedUntil.clear();
        stealthUntil = 0L;
        removePotion(getOwner(), PotionEffectType.INVISIBILITY);
    }


    private void smokePuff(Location center, int amount) {
        particleApi.spawnParticles(center, Particle.CAMPFIRE_COSY_SMOKE, Math.max(1, amount / 2), 0.3, 0.4, 0.3, 0.02);
        particleApi.spawnParticles(center, Particle.SMOKE, Math.max(1, amount / 2), 0.25, 0.35, 0.25, 0.03);
        particleApi.spawnColoredParticles(center, C_DUSK, 0.6f, Math.max(1, amount / 4), 0.2, 0.3, 0.2);
    }

    private void shadowTrail(Location from, Location to) {
        Vector full = to.toVector().subtract(from.toVector());
        double length = full.length();
        if (length < 0.05) return;
        Vector dir = full.clone().multiply(1.0 / length);
        int steps = Math.max(6, (int) (length * 6));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double fade = 1.0 - t * 0.4;
            Location lp = from.clone().add(dir.clone().multiply(length * t));
            particleApi.spawnColoredParticles(lp, C_SHADOW, (float) (0.55 * fade), 1, 0.03, 0.03, 0.03);
            if (i % 2 == 0) particleApi.spawnParticles(lp, Particle.SMOKE, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void bladeFlash(Location from, Location to, Color accentColor) {
        Vector full = to.toVector().subtract(from.toVector());
        double length = full.length();
        if (length < 0.01) return;
        Vector dir = full.clone().multiply(1.0 / length);
        Vector perp = perpOf(dir);

        int steps = Math.max(6, (int) (length * 7));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Location base = from.clone().add(dir.clone().multiply(length * t));
            particleApi.spawnColoredParticles(base.clone().add(perp.clone().multiply(0.05)), C_SHADOW, 0.4f, 1,
                    0.008, 0.008, 0.008);
            particleApi.spawnColoredParticles(base, C_EDGE, 0.3f, 1, 0.008, 0.008, 0.008);
            particleApi.spawnColoredParticles(base.clone().add(perp.clone().multiply(-0.06)), accentColor, 0.22f, 1,
                    0.006, 0.006, 0.006);
        }
        duskSpark(to, accentColor, 0.9f, 4);
    }

    private void veilRing(Location center, double radius, double rotationDeg) {
        int points = 26;
        for (int i = 0; i < points; i++) {
            double a = Math.toRadians(i * (360.0 / points) + rotationDeg);
            Location lp = center.clone().add(Math.cos(a) * radius, 0.05, Math.sin(a) * radius);
            particleApi.spawnColoredParticles(lp, C_SHADOW, 0.4f, 1, 0.01, 0.02, 0.01);
            if (i % 3 == 0) particleApi.spawnParticles(lp, Particle.CAMPFIRE_COSY_SMOKE, 1, 0.05, 0.1, 0.05, 0.005);
        }
    }

    private void duskSpark(Location loc, Color color, float size, int amount) {
        particleApi.spawnColoredParticles(loc, color, size, amount, 0.12, 0.12, 0.12);
        particleApi.spawnParticles(loc, Particle.WITCH, Math.max(1, amount / 4), 0.15, 0.15, 0.15, 0.02);
    }

    private Vector perpOf(Vector dir) {
        Vector up = Math.abs(dir.getY()) < 0.95 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector perp = dir.clone().crossProduct(up);
        if (perp.lengthSquared() < 0.0001) perp = new Vector(1, 0, 0);
        return perp.normalize();
    }

    private Vector rotateAroundY(Vector v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = -v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z).normalize();
    }


    private LivingEntity rayTraceLiving(Player p, double range) {
        RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation(), p.getEyeLocation().getDirection(), range,
                e -> e instanceof LivingEntity && !e.equals(p));
        if (result == null || result.getHitEntity() == null) return null;
        return (LivingEntity) result.getHitEntity();
    }

    private LivingEntity nearbyClosest(Location loc, double radius, Player exclude) {
        LivingEntity best = null;
        double bestD = radius;
        for (LivingEntity e : nearbyLiving(loc, radius)) {
            if (e.equals(exclude)) continue;
            double d = e.getLocation().distance(loc);
            if (d <= bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private List<LivingEntity> nearbyLiving(Location loc, double radius) {
        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(e instanceof LivingEntity) || e instanceof ArmorStand) continue;
            if (e instanceof Player && ((Player) e).getGameMode() == GameMode.SPECTATOR) continue;
            list.add((LivingEntity) e);
        }
        return list;
    }

    private Location safeStepDestination(Location from, Vector dir, double maxDist) {
        Vector d = dir.clone().normalize();
        Location cur = from.clone().add(0, 1.0, 0);
        Location last = from.clone();
        int steps = (int) (maxDist * 4);
        for (int i = 0; i < steps; i++) {
            cur.add(d.clone().multiply(0.25));
            if (!cur.getBlock().isPassable()) break;
            Location candidate = cur.clone().subtract(0, 1.0, 0);
            if (!candidate.getBlock().isPassable()) break;
            last = candidate;
        }
        last.setDirection(d);
        return last;
    }


    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§5Shadow Step";
            case 1: return "§5Throwing Knives";
            case 2: return "§dSmoke Veil";
            case 3: return "§dBackstab";
            case 4: return "§5Shadow Clone";
            case 5: return "§4Vanish Strike";
            default: return "§7none";
        }
    }
}
