package net.trduc.magicabilities.powers.custom;

import net.trduc.magicabilities.cooldowns.CooldownApi;
import net.trduc.magicabilities.powers.IdlePower;
import net.trduc.magicabilities.powers.Power;
import net.trduc.magicabilities.powers.Removeable;
import net.trduc.magicabilities.powers.executions.*;
import net.trduc.magicabilities.powers.executions.DealDamageExecute;
import net.trduc.magicabilities.powers.executions.Execute;
import net.trduc.magicabilities.powers.executions.IdleExecute;
import net.trduc.magicabilities.powers.executions.LeftClickExecute;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static net.trduc.magicabilities.MagicAbilities.*;
import static net.trduc.magicabilities.cooldowns.Cooldowns.cooldowns;
import static net.trduc.magicabilities.data.PlayerData.getPlayerData;
import static net.trduc.magicabilities.players.PowerPlayer.players;
public class SnowpartingBladePower extends Power implements IdlePower, Removeable {

    private static final String sb_slash   = "snowblade.slash";
    private static final String sb_step    = "snowblade.step";
    private static final String sb_drive   = "snowblade.drive";
    private static final String sb_arctic  = "snowblade.arctic";

    private static final int XP_SLASH_DEFAULT  = 3;
    private static final int XP_STEP_DEFAULT   = 6;
    private static final int XP_DRIVE_DEFAULT  = 9;
    private static final int XP_ARCTIC_DEFAULT = 12;
    private final int XP_SLASH;
    private final int XP_STEP;
    private final int XP_DRIVE;
    private final int XP_ARCTIC;
    private static final int XP_PER_HIT = 1;
    private static final Color C_ICE_WHITE  = Color.fromRGB(240, 248, 255);
    private static final Color C_ICE_BLUE   = Color.fromRGB(140, 210, 255);
    private static final Color C_ICE_SHARP  = Color.fromRGB(180, 230, 255);
    private static final Color C_SILVER     = Color.fromRGB(200, 215, 230);
    private static final Color C_FROST_EDGE = Color.fromRGB(100, 180, 255);

    private static final Color[] BLADE_COLS = {
            C_ICE_WHITE, C_ICE_BLUE, C_ICE_SHARP, C_SILVER, C_FROST_EDGE
    };

    private boolean driving = false;
    private BukkitRunnable hudRunnable = null;
    public SnowpartingBladePower(Player owner) {
        super(owner);
        org.bukkit.configuration.file.FileConfiguration cfg = magicPlugin.getConfig();
        XP_SLASH  = cfg.getInt("snowblade.xp.slash",  XP_SLASH_DEFAULT);
        XP_STEP   = cfg.getInt("snowblade.xp.step",   XP_STEP_DEFAULT);
        XP_DRIVE  = cfg.getInt("snowblade.xp.drive",  XP_DRIVE_DEFAULT);
        XP_ARCTIC = cfg.getInt("snowblade.xp.arctic", XP_ARCTIC_DEFAULT);
    }
    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DealDamageExecute) {
            gainXp(((DealDamageExecute) ex).getPlayer(), XP_PER_HIT);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) onLeftClick((LeftClickExecute) ex);
    }

    private void onLeftClick(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0:
                if (onCd(sb_slash, p)) return;
                if (!checkXp(p, XP_SLASH)) return;
                frostSlash(p);
                spendXp(p, XP_SLASH);
                CooldownApi.addCooldown(sb_slash, p, cooldowns.get(sb_slash));
                return;
            case 1:
                if (onCd(sb_step, p)) return;
                if (!checkXp(p, XP_STEP)) return;
                blizzardStep(p);
                spendXp(p, XP_STEP);
                CooldownApi.addCooldown(sb_step, p, cooldowns.get(sb_step));
                return;
            case 2:
                if (driving) return;
                if (onCd(sb_drive, p)) return;
                if (!checkXp(p, XP_DRIVE)) return;
                shatterDrive(p);
                spendXp(p, XP_DRIVE);
                CooldownApi.addCooldown(sb_drive, p, cooldowns.get(sb_drive));
                return;
            case 3:
                if (onCd(sb_arctic, p)) return;
                if (!checkXp(p, XP_ARCTIC)) return;
                arcticSeverance(p);
                spendXp(p, XP_ARCTIC);
                CooldownApi.addCooldown(sb_arctic, p, cooldowns.get(sb_arctic));
        }
    }

    private void frostSlash(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);

        int[] damages = {8, 10, 13};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            final int dmg = damages[i];
            new BukkitRunnable() {
                @Override public void run() {
                    if (!p.isOnline()) { cancel(); return; }
                    shootFrostBlade(p, idx * 8.0, dmg);
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.4f + idx * 0.15f);
                }
            }.runTaskLater(magicPlugin, idx * 4L);
        }
    }

    private void shootFrostBlade(Player p, double yawOffset, int damage) {
        Vector base = p.getEyeLocation().getDirection().clone().normalize();
        Vector dir  = yawRotate(base, yawOffset);
        Vector right = yawRotate(dir.clone().setY(0).normalize(), 90).normalize();

        ArmorStand blade = spawnAs(p.getEyeLocation().clone().add(dir.clone().multiply(0.5)));
        Random r = new Random();
        Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            double spin = 0;
            @Override public void run() {
                if (blade.isDead() || t > 28) { safeRemove(blade); cancel(); return; }
                blade.teleport(blade.getLocation().add(dir.clone().multiply(1.6)));
                Location loc = blade.getLocation();
                spin += 35;
                for (int i = -2; i <= 2; i++) {
                    double a    = Math.toRadians(spin + i * 18);
                    double offX = Math.cos(a) * 0.55;
                    double offY = Math.sin(a) * 0.25;
                    Location lp = loc.clone().add(right.clone().multiply(offX)).add(0, offY, 0);
                    Color c = BLADE_COLS[Math.abs(i) % BLADE_COLS.length];
                    particleApi.spawnColoredParticles(lp, c, 1.2f, 2, 0.03, 0.03, 0.03);
                }
                particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.4f, 2, 0.04, 0.04, 0.04);
                if (t % 3 == 0)
                    particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 1, 0.06, 0.06, 0.06, 0.02);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 0.9, 1.0)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(damage, p);
                    applyFrostSlow(e);
                    slashHitBurst(loc);
                    safeRemove(blade); cancel(); return;
                }
                if (!loc.getBlock().isPassable()) { safeRemove(blade); cancel(); return; }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void slashHitBurst(Location loc) {
        particleApi.spawnColoredParticles(loc, C_ICE_WHITE,  1.5f, 15, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_ICE_BLUE,   1.2f, 10, 0.4, 0.4, 0.4);
        particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 8, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.6f);
    }

    private void blizzardStep(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,          0.6f, 1.8f);

        Vector dir  = p.getEyeLocation().getDirection().clone().setY(0.1).normalize();
        Location from = p.getLocation().clone();
        Location to   = from.clone();
        for (int i = 0; i < 20; i++) {
            to.add(dir.clone().multiply(0.5));
            if (!to.getBlock().isPassable()) { to.subtract(dir.clone().multiply(0.5)); break; }
        }
        new BukkitRunnable() {
            @Override public void run() {
                double dist = from.distance(to);
                if (dist < 0.1) return;
                int steps = (int)(dist * 5);
                Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
                Location cur = from.clone().add(0, 1, 0);
                Random r = new Random();
                for (int i = 0; i < steps; i++) {
                    particleApi.spawnColoredParticles(cur, BLADE_COLS[i % BLADE_COLS.length],
                            1.1f, 2, 0.06, 0.06, 0.06);
                    if (i % 4 == 0)
                        particleApi.spawnParticles(cur, Particle.SNOWFLAKE, 1, 0.05, 0.05, 0.05, 0.02);
                    cur.add(step);
                }
            }
        }.runTask(magicPlugin);
        p.teleport(to.clone().add(0, 0.5, 0));
        p.setFallDistance(0);
        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_ICE_WHITE,  1.6f, 25, 0.6, 0.6, 0.6);
        particleApi.spawnColoredParticles(to.clone().add(0,1,0), C_ICE_BLUE,   1.3f, 20, 0.8, 0.8, 0.8);
        particleApi.spawnParticles(to.clone().add(0,1,0), Particle.SNOWFLAKE, 15, 0.7, 0.7, 0.7, 0.1);
        p.getWorld().playSound(to, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f);
        p.getWorld().playSound(to, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f);
        int numCrystals = 4;
        for (int i = 1; i <= numCrystals; i++) {
            double frac = (double) i / (numCrystals + 1);
            Location cl = from.clone().add(to.toVector().subtract(from.toVector()).multiply(frac));
            spawnFrostCrystal(cl, p);
        }
    }

    private void spawnFrostCrystal(Location loc, Player owner) {
        Random r = new Random();
        Location ground = loc.clone();
        while (ground.getBlock().isPassable() && ground.getY() > 0) ground.add(0,-1,0);
        ground.add(0, 1.2, 0);
        final Location finalGround = ground.clone();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60) { cancel(); return; }
                if (t % 5 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.toRadians(i * 60 + t * 8);
                        Location lp = finalGround.clone().add(Math.cos(a)*0.35, 0, Math.sin(a)*0.35);
                        particleApi.spawnColoredParticles(lp, BLADE_COLS[i % BLADE_COLS.length],
                                0.95f, 1, 0.03, 0.03, 0.03);
                    }
                    particleApi.spawnColoredParticles(finalGround, C_ICE_WHITE, 1.1f, 1, 0.03, 0.03, 0.03);
                    particleApi.spawnParticles(finalGround, Particle.SNOWFLAKE, 1, 0.1, 0.1, 0.1, 0.02);
                }
                for (Entity e : finalGround.getWorld().getNearbyEntities(finalGround, 1.0, 1.3, 1.0)) {
                    if (e.equals(owner) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    ((LivingEntity) e).damage(3, owner);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
                    particleApi.spawnColoredParticles(finalGround, C_ICE_WHITE, 1.4f, 20, 0.4, 0.4, 0.4);
                    particleApi.spawnParticles(finalGround, Particle.SNOWFLAKE, 10, 0.3, 0.3, 0.3, 0.1);
                    finalGround.getWorld().playSound(finalGround, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);
                    cancel(); return;
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void shatterDrive(Player p) {
        driving = true;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,          1f, 0.5f);

        Vector dir = p.getEyeLocation().getDirection().clone().setY(0.08).normalize();
        p.setVelocity(dir.clone().multiply(2.6));

        Set<UUID> hit = new HashSet<>();
        Random r = new Random();
        int[] ticks = {0};

        new BukkitRunnable() {
            @Override public void run() {
                int t = ticks[0];
                if (t > 15 || (p.isOnGround() && t > 2)) {
                    driveImpact(p.getLocation(), p, r);
                    driving = false;
                    cancel(); return;
                }

                Location loc = p.getLocation().clone().add(0, 0.8, 0);
                for (int side = -1; side <= 1; side += 2) {
                    Vector right = yawRotate(dir.clone().setY(0).normalize(), 90 * side).normalize();
                    for (int j = 0; j < 4; j++) {
                        double s = j * 0.3;
                        Location lp = loc.clone().add(right.clone().multiply(s * side));
                        Color c = BLADE_COLS[j % BLADE_COLS.length];
                        particleApi.spawnColoredParticles(lp, c, 1.2f, 2, 0.04, 0.06, 0.04);
                    }
                }
                particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.5f, 4, 0.1, 0.1, 0.1);
                if (t % 2 == 0)
                    particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 2, 0.2, 0.2, 0.2, 0.05);

                for (Entity e : loc.getWorld().getNearbyEntities(p.getLocation(), 1.4, 1.4, 1.4)) {
                    if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                    if (!(e instanceof LivingEntity)) continue;
                    hit.add(e.getUniqueId());
                    ((LivingEntity) e).damage(14, p);
                    applyFrostSlow(e);
                    e.setVelocity(dir.clone().multiply(1.5).setY(0.5));
                }
                ticks[0]++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void driveImpact(Location loc, Player p, Random r) {
        p.setFallDistance(0);
        Location center = loc.clone().add(0, 0.5, 0);
        particleApi.spawnColoredParticles(center, C_ICE_WHITE, 2f,   80, 1.5, 1.5, 1.5);
        particleApi.spawnColoredParticles(center, C_ICE_BLUE,  1.7f, 30, 2.0, 2.0, 2.0);
        particleApi.spawnColoredParticles(center, C_ICE_SHARP, 1.5f, 20, 2.5, 2.5, 2.5);
        particleApi.spawnParticles(center, Particle.SNOWFLAKE, 25, 2.5, 1.5, 2.5, 0.2);
        p.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK,          1f, 0.4f);
        p.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);
        new BukkitRunnable() {
            double rad = 0.3; int t = 0;
            @Override public void run() {
                if (rad > 4) { cancel(); return; }
                for (int i = 0; i < 24; i++) {
                    double a = Math.toRadians(i * 15 + t * 10);
                    Location lp = center.clone().add(Math.cos(a)*rad, 0.1, Math.sin(a)*rad);
                    particleApi.spawnColoredParticles(lp,
                            t%3==0 ? C_ICE_WHITE : t%3==1 ? C_ICE_BLUE : C_ICE_SHARP,
                            1.1f, 2, 0.04, 0.04, 0.04);
                }
                rad += 0.5; t++;
            }
        }.runTaskTimer(magicPlugin, 0, 2);
        for (Entity e : center.getWorld().getNearbyEntities(center, 3.5, 3.5, 3.5)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(center);
            double dmg  = Math.max(6, 18 - dist * 3.0);
            ((LivingEntity) e).damage(dmg, p);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   40, 9, false, false));
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128, false, false));
            e.setVelocity(e.getLocation().subtract(center).toVector().normalize().multiply(1.2).setY(0.4));
        }
    }

    private void arcticSeverance(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,          0.8f, 0.4f);
        p.sendMessage(ChatColor.AQUA + "✦ " + ChatColor.BOLD + "ARCTIC SEVERANCE!");
        List<ArmorStand> orbs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72);
            Location orbPos = p.getLocation().clone().add(0, 1.3, 0)
                    .add(Math.cos(angle)*2.0, 0, Math.sin(angle)*2.0);
            ArmorStand orb = spawnAs(orbPos);
            orbs.add(orb);
        }

        Random r = new Random();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 40) {
                    cancel();
                    launchSeverance(p, orbs, r);
                    return;
                }

                Location center = p.getLocation().clone().add(0, 1.3, 0);
                for (int i = 0; i < orbs.size(); i++) {
                    ArmorStand orb = orbs.get(i);
                    if (orb.isDead()) continue;
                    double angle = Math.toRadians(i * 72 + t * 9);
                    double radius = 2.0;
                    Location target = center.clone().add(Math.cos(angle)*radius, 0, Math.sin(angle)*radius);
                    orb.teleport(target);

                    Color c = BLADE_COLS[i % BLADE_COLS.length];
                    for (int wing = -1; wing <= 1; wing++) {
                        double wa = Math.toRadians(wing * 25);
                        Location wl = target.clone().add(
                                Math.cos(angle+wa)*0.4, Math.sin(t*0.15+i)*0.2, Math.sin(angle+wa)*0.4);
                        particleApi.spawnColoredParticles(wl, c, 1.2f, 2, 0.04, 0.04, 0.04);
                    }
                    particleApi.spawnColoredParticles(target, C_ICE_WHITE, 1.4f, 1, 0.03, 0.03, 0.03);
                    if (t % 4 == 0)
                        particleApi.spawnParticles(target, Particle.SNOWFLAKE, 1, 0.05, 0.05, 0.05, 0.02);
                }

                if (t == 20) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 0.8f);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void launchSeverance(Player p, List<ArmorStand> orbs, Random r) {
        Location target = p.getEyeLocation().clone();
        Vector dir = p.getEyeLocation().getDirection().clone().normalize();
        for (int i = 0; i < 50; i++) {
            target.add(dir.clone().multiply(0.5));
            if (!target.getBlock().isPassable()) { target.subtract(dir.clone().multiply(0.5)); break; }
        }
        final Location finalTarget = target.clone();

        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.3f);

        Set<UUID> hit = new HashSet<>();
        for (int i = 0; i < orbs.size(); i++) {
            final int idx = i;
            final ArmorStand orb = orbs.get(i);
            new BukkitRunnable() {
                @Override public void run() {
                    if (orb.isDead()) { cancel(); return; }
                    Vector toTarget = finalTarget.toVector().subtract(orb.getLocation().toVector());
                    double dist = toTarget.length();
                    if (dist < 1.2) {
                        arcticSeveranceHit(finalTarget, p, orb, hit, idx == orbs.size()-1);
                        cancel(); return;
                    }
                    Vector step = toTarget.normalize().multiply(Math.min(2.2, dist));
                    orb.teleport(orb.getLocation().add(step));
                    Location loc = orb.getLocation();
                    Color c = BLADE_COLS[idx % BLADE_COLS.length];
                    for (int w = -2; w <= 2; w++) {
                        double wa = Math.toRadians(w * 20 + idx * 72);
                        Location wl = loc.clone().add(Math.cos(wa)*0.4, Math.sin(wa*0.5)*0.2, Math.sin(wa)*0.4);
                        particleApi.spawnColoredParticles(wl, c, 1.2f, 2, 0.04, 0.04, 0.04);
                    }
                    particleApi.spawnColoredParticles(loc, C_ICE_WHITE, 1.5f, 2, 0.03, 0.03, 0.03);
                    particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 1, 0.04, 0.04, 0.04, 0.02);
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                        if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
                        if (!(e instanceof LivingEntity)) continue;
                        hit.add(e.getUniqueId());
                        ((LivingEntity) e).damage(20, p);
                        applyFrostSlow(e);
                        slashHitBurst(loc);
                    }
                }
            }.runTaskTimer(magicPlugin, idx * 2L, 1);
        }
    }

    private void arcticSeveranceHit(Location loc, Player p, ArmorStand orb, Set<UUID> hit, boolean isLast) {
        safeRemove(orb);
        slashHitBurst(loc);

        if (!isLast) return;
        p.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.2f);
        p.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK,          1f, 0.3f);

        particleApi.spawnColoredParticles(loc, C_ICE_WHITE,  2.5f, 150, 2.5, 2.5, 2.5);
        particleApi.spawnColoredParticles(loc, C_ICE_BLUE,   2f,   120, 3.0, 3.0, 3.0);
        particleApi.spawnColoredParticles(loc, C_ICE_SHARP,  1.8f, 20,  3.5, 3.5, 3.5);
        particleApi.spawnParticles(loc, Particle.SNOWFLAKE, 40, 3.5, 3.0, 3.5, 0.3);
        Random r = new Random();
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(i * 72 + r.nextInt(20));
            Location tip = loc.clone().add(Math.cos(a)*3.5, 0.1, Math.sin(a)*3.5);
            particleApi.drawColoredLine(loc, tip, 1.5, BLADE_COLS[i % BLADE_COLS.length], 1.2f, 0);
        }
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (e.equals(p) || e instanceof ArmorStand || hit.contains(e.getUniqueId())) continue;
            if (!(e instanceof LivingEntity)) continue;
            double dist = e.getLocation().distance(loc);
            double dmg  = Math.max(8, 35 - dist * 4.5);
            ((LivingEntity) e).damage(dmg, p);
            applyFrostSlow(e);
            if (dist < 2.5) {
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   60, 9, false, false));
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 128, false, false));
            }
            e.setVelocity(e.getLocation().subtract(loc).toVector().normalize().multiply(1.8).setY(0.5));
        }
    }

    private boolean checkXp(Player p, int cost) {
        if (cost <= 0) return true;
        if (p.getTotalExperience() >= cost) return true;
        p.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(
                        ChatColor.RED + "xp Need " + cost + " XP — Present: " + p.getTotalExperience()));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
        return false;
    }
    private void spendXp(Player p, int cost) {
        if (cost <= 0) return;
        int remaining = Math.max(0, p.getTotalExperience() - cost);
        p.setLevel(0);
        p.setExp(0f);
        p.setTotalExperience(0);
        if (remaining > 0) p.giveExp(remaining);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.8f);
    }

    private void gainXp(Player p, int amount) {
        p.giveExp(amount);
    }

    private void showHud(Player p) {
        int xp = p.getTotalExperience();
        String bar = ChatColor.AQUA + "XP: " + ChatColor.WHITE + xp + "  "
                + ChatColor.GRAY + "│ "
                + xpSlot("S0", XP_SLASH,  xp) + " "
                + xpSlot("S1", XP_STEP,   xp) + " "
                + xpSlot("S2", XP_DRIVE,  xp) + " "
                + xpSlot("S3", XP_ARCTIC, xp);
        p.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(bar));
    }

    private String xpSlot(String name, int cost, int xp) {
        if (cost == 0) return ChatColor.GREEN + name + "(free)";
        return (xp >= cost ? ChatColor.GREEN : ChatColor.RED) + name + "(" + cost + "xp)";
    }


    private void applyFrostSlow(Entity e) {
        if (!(e instanceof LivingEntity)) return;
        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, true));
    }
    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        final Random r = new Random();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                Location center = p.getLocation().clone().add(0, 1.1, 0);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 + t * 45);
                    double yOsc = Math.sin(t * 0.8 + i) * 0.2;
                    Location lp = center.clone().add(Math.cos(a)*1.05, yOsc, Math.sin(a)*1.05);
                    particleApi.spawnParticles(lp, Particle.SNOWFLAKE, 1, 0.03, 0.03, 0.03, 0.01);
                    particleApi.spawnColoredParticles(lp, BLADE_COLS[i % BLADE_COLS.length], 0.85f, 1, 0.03, 0.03, 0.03);
                }
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 - t * 60);
                    Location lp = center.clone().add(Math.cos(a)*0.65, 0.5+Math.sin(a*0.5)*0.12, Math.sin(a)*0.65);
                    particleApi.spawnColoredParticles(lp, C_ICE_BLUE, 0.8f, 1, 0.03, 0.03, 0.03);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 20);
        hudRunnable = new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                showHud(p);
            }
        };
        hudRunnable.runTaskTimer(magicPlugin, 0, 4);

        return new BukkitRunnable() {
            @Override public void run() {}
        };
    }

    @Override
    public void remove() {
        driving = false;
        if (hudRunnable != null) { hudRunnable.cancel(); hudRunnable = null; }
    }


    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "&bFrost Slash &7(" + XP_SLASH + "xp)";
            case 1: return "&bBlizzard Step &7(" + XP_STEP + "xp)";
            case 2: return "&bShatter Drive &7(" + XP_DRIVE + "xp)";
            case 3: return "&b&lArctic Severance &7(" + XP_ARCTIC + "xp)";
            default: return "&7none";
        }
    }


    private boolean onCd(String key, Player p) {
        if (CooldownApi.isOnCooldown(key, p)) {
            onCooldownInfo(CooldownApi.getCooldownForPlayerLong(key, p));
            return true;
        }
        return false;
    }

    private ArmorStand spawnAs(Location loc) {
        return loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(false); en.setGravity(false); en.setSmall(true); en.setMarker(true);
        });
    }

    private void safeRemove(ArmorStand as) { if (!as.isDead()) as.remove(); }

    private Vector yawRotate(Vector v, double deg) {
        double rad = Math.toRadians(deg);
        return new Vector(v.getX()*Math.cos(rad)+v.getZ()*Math.sin(rad),
                v.getY(), -v.getX()*Math.sin(rad)+v.getZ()*Math.cos(rad));
    }
}
