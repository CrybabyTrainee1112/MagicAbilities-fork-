package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.Execute;
import net.trduc.magicabilitiesfork.powers.executions.IdleExecute;
import net.trduc.magicabilitiesfork.powers.executions.LeftClickExecute;
import net.trduc.magicabilitiesfork.powers.executions.SneakExecute;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class CosmicPower extends Power implements IdlePower, Removeable {

    private static final String c_comet     = "cosmic.comet";
    private static final String c_step      = "cosmic.step";
    private static final String c_nebula    = "cosmic.nebula";
    private static final String c_pulsar    = "cosmic.pulsar";
    private static final String c_guard     = "cosmic.guard";
    private static final String c_supernova = "cosmic.supernova";

    private static final Color C_NEBULA_VIOLET = Color.fromRGB(130, 60, 210);
    private static final Color C_NEBULA_PINK   = Color.fromRGB(230, 90, 190);
    private static final Color C_COMET_CYAN    = Color.fromRGB(110, 220, 255);
    private static final Color C_STAR_GOLD     = Color.fromRGB(255, 205, 110);
    private static final Color C_STARLIGHT     = Color.fromRGB(255, 248, 230);

    private static final Color[] NEBULA_COLORS    = {C_NEBULA_VIOLET, C_NEBULA_PINK, C_COMET_CYAN};
    private static final Color[] NOVA_RING_COLORS = {C_NEBULA_VIOLET, C_NEBULA_PINK, C_STAR_GOLD, C_COMET_CYAN};

    private static final int GUARD_TICKS = 200;

    private boolean channeling  = false;
    private boolean guardActive = false;
    private BukkitRunnable activeTask = null;
    private BukkitRunnable guardTask  = null;
    private BukkitRunnable beamTask   = null;
    private BukkitRunnable nebulaTask = null;

    public CosmicPower(Player owner) {
        super(owner);
    }

    @Override
    public void executePower(Execute ex) {
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute) {
            onLeft((LeftClickExecute) ex);
            return;
        }
        if (ex instanceof SneakExecute) {
            onSneak((SneakExecute) ex);
        }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        if (channeling) return;
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0:
                if (onCd(c_comet, p, this)) return;
                cometStrike(p);
                addCd(c_comet, p);
                return;
            case 2:
                if (onCd(c_nebula, p, this)) return;
                nebulaVeil(p);
                addCd(c_nebula, p);
                return;
            case 3:
                if (onCd(c_pulsar, p, this)) return;
                pulsarBeam(p);
                addCd(c_pulsar, p);
                return;
            case 4:
                if (guardActive) {
                    sendActionBar(p, "§f✦ Constellation Guard is already active!");
                    return;
                }
                if (onCd(c_guard, p, this)) return;
                constellationGuard(p);
                addCd(c_guard, p);
                return;
            case 5:
                if (onCd(c_supernova, p, this)) return;
                addCd(c_supernova, p);
                beginSupernova(p);
        }
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        if (channeling) return;
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 1) return;
        if (onCd(c_step, p, this)) return;
        starfallStep(p);
        addCd(c_step, p);
    }


    private void cometStrike(Player p) {
        final Location origin = p.getEyeLocation().clone();
        final Vector dir = origin.getDirection().clone().normalize();

        p.getWorld().playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.3f);
        p.getWorld().playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.6f);

        final double SPEED = 1.15;
        final double MAX_RANGE = 28.0;
        final Set<Entity> ignore = new HashSet<>();
        ignore.add(p);

        new BukkitRunnable() {
            final Location loc = origin.clone();
            final Vector curDir = dir.clone();
            double traveled = 0;

            @Override
            public void run() {
                if (!p.isOnline() || traveled >= MAX_RANGE) {
                    cancel();
                    return;
                }

                LivingEntity target = findHomingTarget(loc, curDir, 6.0, 35.0, ignore);
                if (target != null) {
                    Vector toTarget = target.getEyeLocation().toVector().subtract(loc.toVector());
                    if (toTarget.lengthSquared() > 0.0001) {
                        toTarget.normalize();
                        Vector blended = curDir.clone().multiply(0.9).add(toTarget.multiply(0.1));
                        if (isVecFinite(blended) && blended.lengthSquared() > 0.001) {
                            blended.normalize();
                            curDir.setX(blended.getX()).setY(blended.getY()).setZ(blended.getZ());
                        }
                    }
                }

                loc.add(curDir.clone().multiply(SPEED));
                traveled += SPEED;

                particleApi.spawnColoredParticles(loc, C_STARLIGHT, 1.5f, 2, 0.05, 0.05, 0.05);
                particleApi.spawnColoredParticles(loc, C_STAR_GOLD, 1.1f, 1, 0.08, 0.08, 0.08);
                Location tail1 = loc.clone().add(curDir.clone().multiply(-0.6));
                particleApi.spawnColoredParticles(tail1, C_COMET_CYAN, 0.9f, 1, 0.12, 0.12, 0.12);
                Location tail2 = tail1.clone().add(curDir.clone().multiply(-0.6));
                particleApi.spawnColoredParticles(tail2, C_NEBULA_VIOLET, 0.8f, 1, 0.15, 0.15, 0.15);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (ignore.contains(e) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                    cometImpact(p, loc.clone());
                    cancel();
                    return;
                }

                if (!loc.getBlock().isPassable() || loc.getBlock().isLiquid()) {
                    cometImpact(p, loc.clone());
                    cancel();
                }
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void cometImpact(Player p, Location center) {
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.6f);
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.9f, 1.2f);
        particleApi.spawnColoredParticles(center, C_STARLIGHT, 2.0f, 40, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(center, C_STAR_GOLD, 1.6f, 25, 0.8, 0.8, 0.8);
        particleApi.spawnColoredParticles(center, C_COMET_CYAN, 1.4f, 20, 0.7, 0.7, 0.7);

        double radius = 2.6;
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) e;
            le.damage(9.0, p);
            applyPotion(le, PotionEffectType.GLOWING, 40, 0);
            Vector kb = knockbackVector(center, e, 1.4, 0.4);
            e.setVelocity(kb);
        }
    }

    private LivingEntity findHomingTarget(Location loc, Vector dir, double range, double coneDeg, Set<Entity> ignore) {
        LivingEntity best = null;
        double bestDist = range;
        double cosLimit = Math.cos(Math.toRadians(coneDeg));
        for (Entity e : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (ignore.contains(e) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            Vector to = e.getLocation().clone().add(0, 1, 0).toVector().subtract(loc.toVector());
            double dist = to.length();
            if (dist < 0.01 || dist > bestDist) continue;
            double dot = to.normalize().dot(dir);
            if (dot < cosLimit) continue;
            bestDist = dist;
            best = (LivingEntity) e;
        }
        return best;
    }


    private void starfallStep(Player p) {
        Location origin = p.getLocation().clone();
        Location target = getRaycastTarget(p, 8);
        target.add(p.getEyeLocation().getDirection().clone().normalize().multiply(-0.4));

        spawnStarBurst(origin.clone().add(0, 1, 0));
        p.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
        p.getWorld().playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);

        particleLine(origin.clone().add(0, 1, 0), target.clone().add(0, 1, 0), 0.5, C_COMET_CYAN, 1.0f);

        p.teleport(target);

        spawnStarBurst(target.clone().add(0, 1, 0));
        p.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.7f);
        applyPotion(p, PotionEffectType.SPEED, 40, 1);
    }

    private void spawnStarBurst(Location loc) {
        particleApi.spawnColoredParticles(loc, C_STARLIGHT, 1.6f, 18, 0.35, 0.35, 0.35);
        particleApi.spawnColoredParticles(loc, C_COMET_CYAN, 1.2f, 12, 0.3, 0.3, 0.3);
        particleApi.spawnColoredParticles(loc, C_NEBULA_VIOLET, 1.0f, 10, 0.25, 0.25, 0.25);
    }


    private void nebulaVeil(Player p) {
        final Location center = getRaycastTarget(p, 12);

        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.6f, 0.6f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.6f, 0.6f);
        sendActionBar(p, "§5§l✦ Nebula Veil ✦");

        final double RADIUS = 4.0;
        final int DURATION = 90;
        final Random rng = new Random();

        nebulaTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= DURATION || !p.isOnline()) {
                    cancel();
                    nebulaTask = null;
                    return;
                }

                int pts = 26;
                double spin = t * 4;
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(i * (360.0 / pts) + spin);
                    double r = RADIUS * (0.5 + 0.5 * Math.sin(t * 0.05 + i));
                    double y = Math.sin(t * 0.08 + i * 0.7) * 1.2;
                    Location lp = center.clone().add(Math.cos(a) * r, 1.0 + y, Math.sin(a) * r);
                    Color c = NEBULA_COLORS[i % NEBULA_COLORS.length];
                    particleApi.spawnColoredParticles(lp, c, 1.0f, 1, 0.1, 0.1, 0.1);
                    if (i % 4 == 0) particleApi.spawnParticles(lp, Particle.CLOUD, 1, 0.15, 0.15, 0.15, 0.0);
                }
                if (t % 3 == 0) {
                    Location star = center.clone().add(
                            (rng.nextDouble() - 0.5) * RADIUS * 2,
                            0.3 + rng.nextDouble() * 2.2,
                            (rng.nextDouble() - 0.5) * RADIUS * 2);
                    particleApi.spawnColoredParticles(star, C_STARLIGHT, 0.7f, 1, 0.02, 0.02, 0.02);
                }

                if (t % 10 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, RADIUS, 2.5, RADIUS)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;
                        applyPotion(le, PotionEffectType.BLINDNESS, 30, 0);
                        applyPotion(le, PotionEffectType.SLOWNESS, 30, 1);
                    }
                }
                t++;
            }
        };
        nebulaTask.runTaskTimer(magicPlugin, 0, 1);
    }


    private void pulsarBeam(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 1.2f);
        sendActionBar(p, "§e§l≡ Pulsar Beam ≡");

        final int CHANNEL_TICKS = 32;
        final double MAX_RANGE = 22.0;

        beamTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= CHANNEL_TICKS || !p.isOnline()) {
                    cancel();
                    beamTask = null;
                    if (p.isOnline()) {
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 1.8f);
                    }
                    return;
                }

                Location eye = p.getEyeLocation();
                Vector dir = eye.getDirection().clone().normalize();
                Location end = eye.clone();
                double traveled = 0;
                List<LivingEntity> struck = new ArrayList<>();

                while (traveled < MAX_RANGE) {
                    end.add(dir.clone().multiply(0.5));
                    traveled += 0.5;
                    if (!end.getBlock().isPassable() || end.getBlock().isLiquid()) break;

                    Color c = t % 2 == 0 ? C_STAR_GOLD : C_STARLIGHT;
                    particleApi.spawnColoredParticles(end, c, 1.0f, 1, 0.03, 0.03, 0.03);

                    for (Entity e : end.getWorld().getNearbyEntities(end, 0.6, 0.6, 0.6)) {
                        if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
                        if (!struck.contains(e)) struck.add((LivingEntity) e);
                    }
                }

                if (t % 4 == 0 && !struck.isEmpty()) {
                    for (LivingEntity le : struck) {
                        le.damage(2.5, p);
                        applyPotionSilent(le, PotionEffectType.SLOWNESS, 10, 0);
                    }
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f);
                }
                if (t % 6 == 0) {
                    p.getWorld().playSound(eye, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 1.6f);
                }
                t++;
            }
        };
        beamTask.runTaskTimer(magicPlugin, 0, 1);
    }


    private void constellationGuard(Player p) {
        guardActive = true;
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.3f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.8f, 1.2f);
        sendActionBar(p, "§f§l✦ Constellation Guard ✦");

        applyPotion(p, PotionEffectType.ABSORPTION, GUARD_TICKS, 1);
        applyPotion(p, PotionEffectType.RESISTANCE, GUARD_TICKS, 0);

        guardTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || t >= GUARD_TICKS) {
                    guardActive = false;
                    cancel();
                    guardTask = null;
                    if (p.isOnline()) {
                        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0), C_STARLIGHT, 1.2f, 14, 0.3, 0.3, 0.3);
                    }
                    return;
                }
                Location base = p.getLocation().clone().add(0, 1.1, 0);
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * 8 + i * 120);
                    Location shard = base.clone().add(Math.cos(a) * 1.3, Math.sin(t * 0.1 + i) * 0.25, Math.sin(a) * 1.3);
                    Color c = NEBULA_COLORS[i % NEBULA_COLORS.length];
                    particleApi.spawnColoredParticles(shard, c, 1.0f, 1, 0.02, 0.02, 0.02);
                    particleApi.spawnColoredParticles(shard, C_STARLIGHT, 0.7f, 1, 0.02, 0.02, 0.02);
                }
                t++;
            }
        };
        guardTask.runTaskTimer(magicPlugin, 0, 2);
    }


    private void beginSupernova(Player p) {
        channeling = true;
        final Location anchor = p.getLocation().clone();
        final int CHANNEL_TICKS = 60;

        p.setWalkSpeed(0.0f);
        p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ SUPERNOVA " +
                ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "— a star is collapsing above you...");
        warnNearby(p, anchor, 30);

        p.getWorld().playSound(anchor, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
        p.getWorld().playSound(anchor, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 0.6f);
        p.getWorld().playSound(anchor, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 0.7f);

        activeTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || t >= CHANNEL_TICKS) {
                    cancel();
                    activeTask = null;
                    channeling = false;
                    if (p.isOnline()) p.setWalkSpeed(0.2f);
                    detonateSupernova(p, anchor.clone());
                    return;
                }

                Location cur = p.getLocation();
                if (Math.abs(cur.getX() - anchor.getX()) > 0.1 || Math.abs(cur.getZ() - anchor.getZ()) > 0.1) {
                    p.teleport(new Location(anchor.getWorld(), anchor.getX(), cur.getY(), anchor.getZ(), cur.getYaw(), cur.getPitch()));
                }

                double progress = (double) t / CHANNEL_TICKS;
                double height = 8.0 * progress;
                int rings = 4 + (int) (progress * 6);

                for (int r = 0; r < rings; r++) {
                    double ringY = height * (r / (double) Math.max(1, rings));
                    double ringRadius = 0.6 + 2.0 * Math.sin(Math.PI * (r / (double) Math.max(1, rings)));
                    double spin = t * (8 - r * 0.4) + r * 40;
                    Color c = NOVA_RING_COLORS[r % NOVA_RING_COLORS.length];
                    int pts = 12;
                    for (int i = 0; i < pts; i++) {
                        double a = Math.toRadians(i * (360.0 / pts) + spin);
                        Location lp = anchor.clone().add(Math.cos(a) * ringRadius, ringY, Math.sin(a) * ringRadius);
                        particleApi.spawnColoredParticles(lp, c, 1.1f, 1, 0.02, 0.02, 0.02);
                    }
                }

                drawGalaxySwirl(anchor.clone().add(0, height, 0), 1.5 + progress * 2.0, t);

                if (t % 8 == 0) {
                    float pitch = 0.5f + (float) progress;
                    p.getWorld().playSound(anchor, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, pitch);
                    p.getWorld().playSound(anchor, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f + (float) progress * 0.6f);
                }
                t++;
            }
        };
        activeTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private void drawGalaxySwirl(Location center, double radius, int tick) {
        int arms = 3;
        int pts = 8;
        double spin = tick * 6;
        for (int a = 0; a < arms; a++) {
            double armOffset = a * (360.0 / arms);
            Color c = NEBULA_COLORS[a % NEBULA_COLORS.length];
            for (int i = 0; i < pts; i++) {
                double t2 = (double) i / pts;
                double r = radius * (0.2 + t2 * 0.9);
                double ang = Math.toRadians(armOffset + spin + t2 * 120);
                Location lp = center.clone().add(Math.cos(ang) * r, Math.sin(t2 * Math.PI) * 0.3, Math.sin(ang) * r);
                particleApi.spawnColoredParticles(lp, c, 0.9f, 1, 0.02, 0.02, 0.02);
            }
        }
    }

    private void warnNearby(Player caster, Location center, double radius) {
        for (Player other : center.getWorld().getPlayers()) {
            if (other.equals(caster)) continue;
            if (other.getLocation().distance(center) > radius) continue;
            other.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "⚠ " + ChatColor.LIGHT_PURPLE +
                    caster.getName() + " is channeling a supernova nearby...");
        }
    }

    private void detonateSupernova(Player p, Location center) {
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        p.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.7f);
        p.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 0.8f);

        Location blastCenter = center.clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(blastCenter, C_STARLIGHT, 2.6f, 260, 2.0, 2.0, 2.0);
        particleApi.spawnColoredParticles(blastCenter, C_STAR_GOLD, 2.2f, 180, 1.6, 1.6, 1.6);
        particleApi.spawnColoredParticles(blastCenter, C_NEBULA_PINK, 2.0f, 140, 1.4, 1.4, 1.4);
        particleApi.spawnParticles(blastCenter, Particle.SONIC_BOOM, 1, 0, 0, 0, 0);

        final double MAX_RADIUS = 11.0;
        new BukkitRunnable() {
            double radius = 1;
            int wave = 0;

            @Override
            public void run() {
                if (radius > MAX_RADIUS) {
                    cancel();
                    return;
                }
                int pts = (int) (radius * 8);
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(i * (360.0 / pts) + wave * 12);
                    Location lp = center.clone().add(Math.cos(a) * radius, 0.3, Math.sin(a) * radius);
                    particleApi.spawnColoredParticles(lp, wave % 2 == 0 ? C_STARLIGHT : C_NEBULA_VIOLET, 1.6f, 1, 0.05, 0.1, 0.05);
                }
                if (wave % 2 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, Math.max(0.5f, 1.3f - wave * 0.05f));
                }
                radius += 1.8;
                wave++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);

        for (Entity e : center.getWorld().getNearbyEntities(center, MAX_RADIUS, MAX_RADIUS * 0.7, MAX_RADIUS)) {
            if (e.equals(p) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) e;
            double dist = Math.max(1.0, e.getLocation().distance(center));
            double dmg = 32.0 * Math.max(0.25, 1.0 - dist / (MAX_RADIUS + 2));
            le.damage(dmg, p);
            Vector kb = knockbackVector(center, e, 2.4, 1.1);
            e.setVelocity(kb);
            applyPotion(le, PotionEffectType.GLOWING, 60, 0);
        }

        sendActionBar(p, "§d§l✦ Supernova!");
    }


    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        final Player p = ex.getPlayer();
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancel();
                    return;
                }
                if (!isAuraEnabled(p) || channeling) return;
                double a = Math.toRadians(t * 20);
                Location lp = p.getLocation().clone().add(Math.cos(a) * 0.55, 1.15 + Math.sin(t * 0.2) * 0.15, Math.sin(a) * 0.55);
                particleApi.spawnColoredParticles(lp, NEBULA_COLORS[t % NEBULA_COLORS.length], 0.7f, 1, 0.02, 0.02, 0.02);
                if (t % 4 == 0) {
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 0.1, 0), C_STARLIGHT, 0.6f, 1, 0.3, 0.05, 0.3);
                }
                t++;
            }
        };
        task.runTaskTimer(magicPlugin, 0, 5);
        return task;
    }

    @Override
    public void remove() {
        channeling = false;
        guardActive = false;
        if (activeTask != null) {
            try {
                activeTask.cancel();
            } catch (Exception ignored) {
            }
            activeTask = null;
        }
        if (guardTask != null) {
            try {
                guardTask.cancel();
            } catch (Exception ignored) {
            }
            guardTask = null;
        }
        if (beamTask != null) {
            try {
                beamTask.cancel();
            } catch (Exception ignored) {
            }
            beamTask = null;
        }
        if (nebulaTask != null) {
            try {
                nebulaTask.cancel();
            } catch (Exception ignored) {
            }
            nebulaTask = null;
        }
        if (getOwner() != null && getOwner().isOnline()) {
            getOwner().setWalkSpeed(0.2f);
        }
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0:
                return "&d&lComet Strike";
            case 1:
                return "&b&lStarfall Step";
            case 2:
                return "&5&lNebula Veil";
            case 3:
                return "&e&lPulsar Beam";
            case 4:
                return "&f&lConstellation Guard";
            case 5:
                return "&d&l✦ SUPERNOVA ✦";
            default:
                return "&7none";
        }
    }
}
