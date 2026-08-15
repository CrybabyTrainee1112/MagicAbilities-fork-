package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class ChronarchPower extends Power implements IdlePower, Removeable {

    private static final String c_bolt     = "chronarch.bolt";
    private static final String c_rewind   = "chronarch.rewind";
    private static final String c_haste    = "chronarch.haste";
    private static final String c_stasis   = "chronarch.stasis";
    private static final String c_field    = "chronarch.field";
    private static final String c_echo     = "chronarch.echo";
    private static final String c_precog   = "chronarch.precog";
    private static final String c_flicker  = "chronarch.flicker";
    private static final String c_zerohour = "chronarch.zerohour";

    private static final Color C_GOLD  = Color.fromRGB(255, 196, 60);
    private static final Color C_AMBER = Color.fromRGB(224, 138, 14);
    private static final Color C_CYAN  = Color.fromRGB(94, 224, 232);
    private static final Color C_PALE  = Color.fromRGB(240, 236, 214);
    private static final Color C_VOID  = Color.fromRGB(94, 34, 130);
    private static final Color[] TIME_COLORS = { C_GOLD, C_AMBER, C_CYAN, C_PALE };

    private int    XP_ZEROHOUR;
    private double WINDOW_SECONDS;
    private int    SAMPLE_INTERVAL_TICKS;
    private int    maxSamples;

    private double BOLT_DMG;
    private int    BOLT_SLOW_TICKS;
    private double REWIND_HEAL_FRACTION;
    private int    HASTE_DURATION_TICKS;
    private double HASTE_TICK_DMG;
    private int    STASIS_DURATION_TICKS;
    private double STASIS_RELEASE_DMG;
    private double FIELD_RADIUS;
    private int    FIELD_DURATION_TICKS;
    private double ECHO_HIT_DMG;
    private double ECHO_BURST_DMG;
    private double PRECOG_DISTANCE;
    private int    PRECOG_RESISTANCE_TICKS;
    private double ZEROHOUR_RADIUS;
    private int    ZEROHOUR_FREEZE_TICKS;
    private double ZEROHOUR_IMPACT_DMG;
    private int    ZEROHOUR_CHARGE_TICKS;

    private final LinkedList<TimeSnapshot> history = new LinkedList<>();

    private boolean hasteActive = false;
    private BukkitRunnable hasteTask = null;
    private long lastMomentumMs = 0L;

    private static final class TimeSnapshot {
        final Location location;
        final double health;
        TimeSnapshot(Location location, double health) {
            this.location = location;
            this.health = health;
        }
    }

    public ChronarchPower(Player owner) {
        super(owner);
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration cfg = magicPlugin.getConfig();

        XP_ZEROHOUR             = cfg.getInt("chronarch.xp.zerohour", 25);
        WINDOW_SECONDS          = cfg.getDouble("chronarch.history.window-seconds", 3.0);
        SAMPLE_INTERVAL_TICKS   = cfg.getInt("chronarch.history.sample-interval-ticks", 2);
        maxSamples              = Math.max(4, (int) Math.round((WINDOW_SECONDS * 20.0) / SAMPLE_INTERVAL_TICKS));

        BOLT_DMG                = cfg.getDouble("chronarch.dmg.bolt", 7.0);
        BOLT_SLOW_TICKS         = cfg.getInt("chronarch.dmg.bolt-slow-ticks", 20);
        REWIND_HEAL_FRACTION    = cfg.getDouble("chronarch.dmg.rewind-heal-fraction", 0.4);
        HASTE_DURATION_TICKS    = cfg.getInt("chronarch.dmg.haste-duration-ticks", 80);
        HASTE_TICK_DMG          = cfg.getDouble("chronarch.dmg.haste-tick-dmg", 3.0);
        STASIS_DURATION_TICKS   = cfg.getInt("chronarch.dmg.stasis-duration-ticks", 50);
        STASIS_RELEASE_DMG      = cfg.getDouble("chronarch.dmg.stasis-release-dmg", 10.0);
        FIELD_RADIUS            = cfg.getDouble("chronarch.dmg.field-radius", 4.0);
        FIELD_DURATION_TICKS    = cfg.getInt("chronarch.dmg.field-duration-ticks", 120);
        ECHO_HIT_DMG            = cfg.getDouble("chronarch.dmg.echo-hit-dmg", 6.0);
        ECHO_BURST_DMG          = cfg.getDouble("chronarch.dmg.echo-burst-dmg", 12.0);
        PRECOG_DISTANCE         = cfg.getDouble("chronarch.dmg.precog-distance", 5.0);
        PRECOG_RESISTANCE_TICKS = cfg.getInt("chronarch.dmg.precog-resistance-ticks", 20);
        ZEROHOUR_RADIUS         = cfg.getDouble("chronarch.dmg.zerohour-radius", 6.0);
        ZEROHOUR_FREEZE_TICKS   = cfg.getInt("chronarch.dmg.zerohour-freeze-ticks", 60);
        ZEROHOUR_IMPACT_DMG     = cfg.getDouble("chronarch.dmg.zerohour-impact-dmg", 8.0);
        ZEROHOUR_CHARGE_TICKS   = cfg.getInt("chronarch.dmg.zerohour-charge-ticks", 24);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DealDamageExecute) {
            passiveMomentum((DealDamageExecute) ex);
            return;
        }
        if (ex instanceof DamagedByExecute) {
            passiveFlicker((DamagedByExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); return; }
        if (ex instanceof SneakExecute)      { onSneak((SneakExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0: if (onCd(c_bolt, p, this)) return; chronoBolt(p); addCd(c_bolt, p); return;
            case 1: if (onCd(c_rewind, p, this)) return; temporalRewind(p); addCd(c_rewind, p); return;
            case 2: if (onCd(c_haste, p, this)) return; hasteSurge(p); addCd(c_haste, p); return;
            case 3: if (onCd(c_stasis, p, this)) return; stasisLock(p); addCd(c_stasis, p); return;
            case 4: if (onCd(c_field, p, this)) return; slowField(p); addCd(c_field, p); return;
            case 7:
                if (onCd(c_zerohour, p, this)) return;
                if (!checkXp(p, XP_ZEROHOUR, this)) return;
                spendXp(p, XP_ZEROHOUR);
                zeroHour(p);
                addCd(c_zerohour, p);
                return;
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (onCd(c_echo, p, this)) return;
        temporalEcho(p);
        addCd(c_echo, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(c_precog, p, this)) return;
        precognitionStep(p);
        addCd(c_precog, p);
    }


    private void chronoBolt(Player p) {
        ArmorStand bolt = spawnProjectile(p);
        final Vector dir = p.getEyeLocation().getDirection().normalize();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 1.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (bolt.isDead() || t > 60) { safeRemove(bolt); cancel(); return; }
                bolt.teleport(bolt.getLocation().add(dir.clone().multiply(1.5)));
                Location loc = bolt.getLocation();

                particleApi.spawnColoredParticles(loc, C_GOLD, 1.1f, 2, 0.05, 0.05, 0.05);
                if (t % 2 == 0)
                    particleApi.spawnColoredParticles(loc, C_CYAN, 0.9f, 1, 0.06, 0.06, 0.06);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.9, 0.9, 0.9)) {
                    if (e.equals(p) || e instanceof ArmorStand) continue;
                    if (e instanceof LivingEntity) {
                        ((LivingEntity) e).damage(BOLT_DMG, p);
                        applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, BOLT_SLOW_TICKS, 1);
                        particleApi.spawnColoredParticles(loc, C_PALE, 1.3f, 18, 0.2, 0.2, 0.2);
                        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 1.2f);
                        safeRemove(bolt); cancel(); return;
                    }
                }
                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    safeRemove(bolt); cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }


    private void temporalRewind(Player p) {
        TimeSnapshot snap = oldestUsableSnapshot();
        if (snap == null) {
            sendActionBar(p, "§eNot enough time recorded yet!");
            return;
        }

        double before    = snap.health;
        double now       = p.getHealth();
        double missing   = Math.max(0, before - now);
        double healBack  = missing * REWIND_HEAL_FRACTION;

        Location origin = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(origin, C_VOID, 1.4f, 40, 0.3, 0.5, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.6f);

        Location dest = snap.location.clone();
        p.teleport(dest);
        safeHeal(p, healBack);

        Location arrive = dest.clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(arrive, C_GOLD, 1.4f, 40, 0.3, 0.5, 0.3);
        particleApi.spawnColoredParticles(arrive, C_CYAN, 1.1f, 20, 0.3, 0.5, 0.3);
        p.getWorld().playSound(dest, Sound.ITEM_TOTEM_USE, 0.5f, 1.6f);
        sendActionBar(p, "§6§l⏳ Rewound " + String.format("%.1f", WINDOW_SECONDS) + "s through time.");
    }


    private void hasteSurge(Player p) {
        hasteActive = true;
        applyPotion(p, PotionEffectType.SPEED, HASTE_DURATION_TICKS, 2);
        applyPotion(p, PotionEffectType.STRENGTH, HASTE_DURATION_TICKS, 0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.7f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 1.8f);

        hasteTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= HASTE_DURATION_TICKS || !p.isOnline()) {
                    hasteActive = false;
                    cancel();
                    return;
                }
                Location loc = p.getLocation().clone().add(0, 1, 0);
                particleApi.spawnColoredParticles(loc, C_GOLD, 1.0f, 3, 0.35, 0.2, 0.35);
                if (t % 2 == 0)
                    particleApi.spawnColoredParticles(loc, C_CYAN, 0.8f, 2, 0.3, 0.2, 0.3);

                if (t % 4 == 0) {
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.3, 1.3, 1.3)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(HASTE_TICK_DMG, p);
                        e.setVelocity(knockbackVector(loc, e, 0.6, 0.15));
                    }
                }
                t++;
            }
        };
        hasteTask.runTaskTimer(magicPlugin, 0, 1);
    }


    private void stasisLock(Player p) {
        LivingEntity target = getInSight(p, 12, 0.85);
        if (target == null) target = getNearestTarget(p, 6);
        if (target == null) {
            sendActionBar(p, "§cNo target nearby!");
            return;
        }
        final LivingEntity frozen = target;

        applyPotion(frozen, PotionEffectType.SLOWNESS,  STASIS_DURATION_TICKS + 5, 10);
        applyPotion(frozen, PotionEffectType.JUMP_BOOST, STASIS_DURATION_TICKS + 5, -1);
        applyPotion(frozen, PotionEffectType.GLOWING,    STASIS_DURATION_TICKS + 5, 0);
        p.getWorld().playSound(frozen.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!frozen.isValid() || t >= STASIS_DURATION_TICKS) {
                    if (frozen.isValid()) {
                        frozen.damage(STASIS_RELEASE_DMG, p);
                        applyPotion(frozen, PotionEffectType.SLOWNESS, 40, 1);
                        Location loc = frozen.getLocation().clone().add(0, 1, 0);
                        particleApi.spawnColoredParticles(loc, C_GOLD, 2.0f, 40, 0.4, 0.6, 0.4);
                        particleApi.spawnColoredParticles(loc, C_VOID, 1.6f, 20, 0.4, 0.6, 0.4);
                        frozen.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 0.6f);
                    }
                    cancel();
                    return;
                }
                frozen.setVelocity(new Vector(0, 0, 0));
                Location loc = frozen.getLocation().clone().add(0, 1, 0);
                particleCircle(loc, 0.9, t % 6 < 3 ? C_CYAN : C_GOLD, 1.6f, 10, t * 12);
                if (t % 10 == 0)
                    frozen.getWorld().playSound(loc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 2.0f);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }


    private void slowField(Player p) {
        final Location center = p.getLocation().clone();
        p.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.6f);
        particleCircle(center, FIELD_RADIUS, C_CYAN, 1.5f, 28, 0);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= FIELD_DURATION_TICKS) { cancel(); return; }

                if (t % 5 == 0) {
                    particleCircle(center, FIELD_RADIUS, C_GOLD, 1.2f, 24, t * 6);
                    particleCircle(center, FIELD_RADIUS * 0.6, C_PALE, 1.0f, 16, -t * 8);
                }
                if (t % 10 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, FIELD_RADIUS, 2.5, FIELD_RADIUS)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        if (e.getLocation().distance(center) > FIELD_RADIUS) continue;
                        applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 30, 1);
                        applyPotion((LivingEntity) e, PotionEffectType.WEAKNESS, 30, 0);
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }


    private void temporalEcho(Player p) {
        if (history.size() < 4) {
            sendActionBar(p, "§eNot enough time recorded yet!");
            return;
        }
        final List<Location> path = new ArrayList<>();
        for (TimeSnapshot s : history) path.add(s.location.clone());
        path.add(p.getLocation().clone());

        ArmorStand echo = spawnProjectileAt(path.get(0));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.7f);
        particleApi.spawnColoredParticles(path.get(0).clone().add(0, 1, 0), C_VOID, 1.6f, 30, 0.3, 0.5, 0.3);

        final Set<Entity> grazed = new HashSet<>();

        new BukkitRunnable() {
            int idx = 0;
            @Override public void run() {
                if (idx >= path.size()) {
                    Location end = echo.getLocation().clone();
                    particleApi.spawnColoredParticles(end.clone().add(0, 1, 0), C_GOLD, 2.0f, 50, 0.5, 0.7, 0.5);
                    particleApi.spawnColoredParticles(end.clone().add(0, 1, 0), C_CYAN, 1.6f, 25, 0.5, 0.7, 0.5);
                    end.getWorld().playSound(end, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.5f);
                    for (Entity e : end.getWorld().getNearbyEntities(end, 2.2, 2.2, 2.2)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        ((LivingEntity) e).damage(ECHO_BURST_DMG, p);
                        e.setVelocity(knockbackVector(end, e, 1.4, 0.4));
                    }
                    safeRemove(echo);
                    cancel();
                    return;
                }
                echo.teleport(path.get(idx).clone().add(0, 0.05, 0));
                Location loc = echo.getLocation();
                particleApi.spawnColoredParticles(loc.clone().add(0, 1, 0), C_CYAN, 1.1f, 3, 0.15, 0.2, 0.15);
                particleApi.spawnColoredParticles(loc.clone().add(0, 1, 0), C_PALE, 0.9f, 1, 0.1, 0.15, 0.1);

                for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0, 1, 0), 1.1, 1.1, 1.1)) {
                    if (e.equals(p) || e instanceof ArmorStand || grazed.contains(e) || !(e instanceof LivingEntity)) continue;
                    grazed.add(e);
                    ((LivingEntity) e).damage(ECHO_HIT_DMG, p);
                    applyPotion((LivingEntity) e, PotionEffectType.SLOWNESS, 20, 1);
                }
                idx++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }


    private void precognitionStep(Player p) {
        Vector dir = p.getEyeLocation().getDirection().clone().setY(0);
        if (dir.lengthSquared() < 0.0001) dir = new Vector(1, 0, 0);
        dir.normalize();

        Location from = p.getLocation().clone();
        Location dest = safeDashDestination(from, dir, PRECOG_DISTANCE);
        adjustToGround(dest);
        dest.setDirection(from.getDirection());

        particleApi.spawnColoredParticles(from.clone().add(0, 1, 0), C_CYAN, 1.3f, 25, 0.25, 0.4, 0.25);
        p.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.3f);

        p.teleport(dest);
        applyPotion(p, PotionEffectType.RESISTANCE, PRECOG_RESISTANCE_TICKS, 3);

        particleApi.spawnColoredParticles(dest.clone().add(0, 1, 0), C_GOLD, 1.3f, 25, 0.25, 0.4, 0.25);
        p.getWorld().playSound(dest, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.6f);
        sendActionBar(p, "§b§lYou were already gone.");
    }


    private void zeroHour(Player p) {
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "⏳ ZERO HOUR ⏳");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= ZEROHOUR_CHARGE_TICKS || !p.isOnline()) {
                    if (p.isOnline()) releaseZeroHour(p);
                    cancel();
                    return;
                }
                Location loc = p.getLocation().clone().add(0, 1, 0);
                double r = 2.2 - (2.0 * t / ZEROHOUR_CHARGE_TICKS);
                particleCircle(loc, Math.max(0.2, r), C_GOLD, 2.0f, 16, -t * 20);
                particleCircle(loc, Math.max(0.2, r) * 0.6, C_CYAN, 1.6f, 10, t * 26);
                if (t % 4 == 0)
                    particleApi.spawnParticles(loc, Particle.ENCHANT, 6, 0.6, 0.6, 0.6, 0.6);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void releaseZeroHour(Player p) {
        Location center = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.7f);
        p.getWorld().playSound(center, Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);

        particleApi.spawnColoredParticles(center, C_GOLD, 2.5f, 120, ZEROHOUR_RADIUS, 1.5, ZEROHOUR_RADIUS);
        particleApi.spawnColoredParticles(center, C_CYAN, 2.0f, 80, ZEROHOUR_RADIUS, 1.5, ZEROHOUR_RADIUS);
        particleCircle(center, ZEROHOUR_RADIUS, C_PALE, 2.2f, 40, 0);

        applyPotion(p, PotionEffectType.SPEED, 80, 1);
        applyPotion(p, PotionEffectType.STRENGTH, 80, 1);

        for (Entity e : center.getWorld().getNearbyEntities(center, ZEROHOUR_RADIUS, ZEROHOUR_RADIUS, ZEROHOUR_RADIUS)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            if (e.getLocation().distance(center) > ZEROHOUR_RADIUS) continue;
            LivingEntity victim = (LivingEntity) e;
            victim.damage(ZEROHOUR_IMPACT_DMG, p);
            applyPotion(victim, PotionEffectType.SLOWNESS,  ZEROHOUR_FREEZE_TICKS, 10);
            applyPotion(victim, PotionEffectType.JUMP_BOOST, ZEROHOUR_FREEZE_TICKS, -1);
            applyPotion(victim, PotionEffectType.GLOWING,    ZEROHOUR_FREEZE_TICKS, 0);
            freezeInPlace(victim, ZEROHOUR_FREEZE_TICKS);
        }
    }


    private void passiveMomentum(DealDamageExecute ex) {
        Player p = ex.getPlayer();
        long now = System.currentTimeMillis();
        if (now - lastMomentumMs < 400) return;
        lastMomentumMs = now;

        reduceCooldown(c_bolt, p, 0.5);
        reduceCooldown(c_haste, p, 0.5);

        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.2, 0), C_GOLD, 0.7f, 2, 0.15, 0.1, 0.15);
    }

    private void passiveFlicker(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        if (p.getHealth() > 7) return;
        if (CooldownApi.isOnCooldown(c_flicker, p)) return;

        applyPotion(p, PotionEffectType.RESISTANCE, 40, 1);
        applyPotion(p, PotionEffectType.SPEED, 40, 1);
        Location loc = p.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(loc, C_CYAN, 1.4f, 30, 0.3, 0.5, 0.3);
        particleApi.spawnColoredParticles(loc, C_PALE, 1.1f, 15, 0.3, 0.5, 0.3);
        p.getWorld().playSound(loc, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 0.8f, 1.4f);
        sendActionBar(p, "§b§lTime stutters around you...");
        addCdFixed(c_flicker, p, 10.0);
    }


    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (t % SAMPLE_INTERVAL_TICKS == 0) {
                    history.addLast(new TimeSnapshot(p.getLocation().clone(), p.getHealth()));
                    while (history.size() > maxSamples) history.removeFirst();
                }

                if (isAuraEnabled(p) && t % 10 == 0) {
                    Location loc = p.getLocation().clone().add(0, 0.1, 0);
                    Color c = TIME_COLORS[(t / 10) % TIME_COLORS.length];
                    particleCircle(loc, 0.55, c, 0.9f, 6, t * 9);
                    particleApi.spawnColoredParticles(loc.clone().add(0, 1.7, 0), C_PALE, 0.7f, 1, 0.15, 0.05, 0.15);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 1);
        return r;
    }

    @Override
    public void remove() {
        if (hasteTask != null) { hasteTask.cancel(); hasteTask = null; }
        hasteActive = false;
        history.clear();
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§6Chrono Bolt";
            case 1: return "§eTemporal Rewind";
            case 2: return "§6Haste Surge";
            case 3: return "§bStasis Lock";
            case 4: return "§3Slow Field";
            case 5: return "§bTemporal Echo";
            case 6: return "§3Precognition Step";
            case 7: return "§6§l⏳ ZERO HOUR §e[ULT]";
            default: return "§7none";
        }
    }


    private TimeSnapshot oldestUsableSnapshot() {
        if (history.size() < 4) return null;
        return history.peekFirst();
    }

    private void freezeInPlace(LivingEntity target, int ticks) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!target.isValid() || t >= ticks) { cancel(); return; }
                target.setVelocity(new Vector(0, 0, 0));
                if (t % 8 == 0)
                    particleCircle(target.getLocation().clone().add(0, 1, 0), 0.8, C_GOLD, 1.3f, 8, t * 10);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void reduceCooldown(String key, Player p, double amountSeconds) {
        if (!CooldownApi.isOnCooldown(key, p)) return;
        double remainingSec = CooldownApi.getCooldownForPlayerLong(key, p) / 1000.0;
        double next = remainingSec - amountSeconds;
        if (next <= 0) {
            CooldownApi.removeCooldown(key, p);
        } else {
            CooldownApi.addCooldown(key, p, next);
        }
    }

    private Location safeDashDestination(Location from, Vector dir, double maxDist) {
        Location probe = from.clone().add(0, 1.0, 0);
        Location last = from.clone();
        int steps = (int) (maxDist * 4);
        for (int i = 0; i < steps; i++) {
            probe.add(dir.clone().multiply(0.25));
            if (!probe.getBlock().isPassable()) break;
            Location candidate = probe.clone().subtract(0, 1.0, 0);
            if (!candidate.getBlock().isPassable()) break;
            last = candidate;
        }
        return last;
    }
}
