package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.cooldowns.CooldownApi;
import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.*;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class KagemushaPower extends Power implements IdlePower, Removeable {

    private static final String K_AMBUSH   = "kagemusha.ambush";
    private static final String K_DANCE    = "kagemusha.dance";
    private static final String K_SWAP     = "kagemusha.swap";
    private static final String K_CONVERGE = "kagemusha.converge";
    private static final String K_DIVERGE  = "kagemusha.diverge";
    private static final String K_FRENZY   = "kagemusha.frenzy";

    private static final Color C_INK   = Color.fromRGB( 18,  10,  28);
    private static final Color C_UMBRA = Color.fromRGB( 82,  32, 122);
    private static final Color C_WISP  = Color.fromRGB(205, 190, 235);
    private static final Color C_VOID  = Color.fromRGB(132,   0,  96);

    private int    XP_FRENZY;

    private int    MAX_DOUBLES;
    private int    DOUBLE_DURATION_TICKS;
    private int    HISTORY_MAX_SAMPLES;
    private int    SPAWN_INTERVAL_TICKS;
    private double SPAWN_MIN_DISTANCE;
    private double MISS_CHANCE;

    private double AMBUSH_RANGE;
    private double AMBUSH_DMG_PER_DOUBLE;

    private double DANCE_RANGE;
    private double DANCE_RADIUS;
    private int    DANCE_DURATION_TICKS;
    private int    DANCE_DIZZY_TICKS;

    private int    SWAP_SAFEFALL_TICKS;

    private double CONVERGE_HEAL_FRACTION;

    private double DIVERGE_RADIUS;
    private double DIVERGE_AGGRO_RADIUS;

    private int    FRENZY_BONUS_DOUBLES;
    private int    FRENZY_TOTAL_DOUBLES;
    private int    FRENZY_DURATION_TICKS;
    private double FRENZY_HIT_DMG;
    private int    FRENZY_HIT_INTERVAL_TICKS;
    private double FRENZY_STRIKE_RADIUS;

    private final LinkedList<Snapshot> history = new LinkedList<>();

    private final LinkedHashMap<UUID, ShadowDouble> doubles = new LinkedHashMap<>();

    private int     idleTick      = 0;
    private int     lastSpawnTick = -99999;
    private boolean wasOnGround   = true;

    private boolean frenzyActive  = false;
    private int     frenzyEndTick = -1;

    private int  dizzyUntilTick = -1;
    private UUID dizzyTargetId  = null;

    private final Random rng = new Random();

    private static final class Snapshot {
        final Location location;
        final boolean  sneaking;
        Snapshot(Location location, boolean sneaking) {
            this.location = location;
            this.sneaking = sneaking;
        }
    }

    private static final class ShadowDouble {
        ArmorStand      stand;
        List<Snapshot>  path;
        int             pathIndex;
        int             spawnTick;
    }

    public KagemushaPower(Player owner) {
        super(owner);
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration cfg = magicPlugin.getConfig();

        XP_FRENZY = cfg.getInt("kagemusha.xp.frenzy", 30);

        MAX_DOUBLES           = cfg.getInt("kagemusha.doubles.max", 3);
        DOUBLE_DURATION_TICKS = cfg.getInt("kagemusha.doubles.duration-ticks", 100);
        HISTORY_MAX_SAMPLES   = cfg.getInt("kagemusha.doubles.delay-window-ticks", 20);
        SPAWN_INTERVAL_TICKS  = cfg.getInt("kagemusha.doubles.spawn-interval-ticks", 14);
        SPAWN_MIN_DISTANCE    = cfg.getDouble("kagemusha.doubles.spawn-min-distance", 0.3);
        MISS_CHANCE           = cfg.getDouble("kagemusha.doubles.miss-chance", 0.5);

        AMBUSH_RANGE          = cfg.getDouble("kagemusha.dmg.ambush-range", 14.0);
        AMBUSH_DMG_PER_DOUBLE = cfg.getDouble("kagemusha.dmg.ambush-dmg-per-double", 3.0);

        DANCE_RANGE           = cfg.getDouble("kagemusha.dmg.dance-range", 10.0);
        DANCE_RADIUS          = cfg.getDouble("kagemusha.dmg.dance-radius", 3.0);
        DANCE_DURATION_TICKS  = cfg.getInt("kagemusha.dmg.dance-duration-ticks", 60);
        DANCE_DIZZY_TICKS     = cfg.getInt("kagemusha.dmg.dance-dizzy-ticks", 60);

        SWAP_SAFEFALL_TICKS   = cfg.getInt("kagemusha.dmg.swap-safefall-ticks", 20);

        CONVERGE_HEAL_FRACTION = cfg.getDouble("kagemusha.dmg.converge-heal-fraction", 0.20);

        DIVERGE_RADIUS        = cfg.getDouble("kagemusha.dmg.diverge-radius", 6.0);
        DIVERGE_AGGRO_RADIUS  = cfg.getDouble("kagemusha.dmg.diverge-aggro-radius", 10.0);

        FRENZY_BONUS_DOUBLES     = cfg.getInt("kagemusha.dmg.frenzy-bonus-doubles", 6);
        FRENZY_TOTAL_DOUBLES     = MAX_DOUBLES + FRENZY_BONUS_DOUBLES;
        FRENZY_DURATION_TICKS    = cfg.getInt("kagemusha.dmg.frenzy-duration-ticks", 160);
        FRENZY_HIT_DMG           = cfg.getDouble("kagemusha.dmg.frenzy-hit-dmg", 2.5);
        FRENZY_HIT_INTERVAL_TICKS= cfg.getInt("kagemusha.dmg.frenzy-hit-interval-ticks", 8);
        FRENZY_STRIKE_RADIUS     = cfg.getDouble("kagemusha.dmg.frenzy-strike-radius", 8.0);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) {
            passiveConfusion((DamagedByExecute) ex);
            return;
        }
        if (ex instanceof MoveExecute) {
            onMove((MoveExecute) ex);
            return;
        }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); }
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 0:
                if (onCd(K_AMBUSH, p, this)) return;
                cloneAmbush(p);
                addCd(K_AMBUSH, p);
                return;
            case 2:
                if (!p.isSneaking()) { sendActionBar(p, "§7Hold sneak to Umbral Swap."); return; }
                if (onCd(K_SWAP, p, this)) return;
                umbralSwap(p);
                addCd(K_SWAP, p);
                return;
            case 5:
                if (onCd(K_FRENZY, p, this)) return;
                if (!checkXp(p, XP_FRENZY, this)) return;
                spendXp(p, XP_FRENZY);
                mirrorImageFrenzy(p);
                addCd(K_FRENZY, p);
                return;
            default:
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        switch (slot) {
            case 1:
                if (onCd(K_DANCE, p, this)) return;
                danceAway(p);
                addCd(K_DANCE, p);
                return;
            case 4:
                if (!p.isSneaking()) { sendActionBar(p, "§7Hold sneak to Diverge."); return; }
                if (onCd(K_DIVERGE, p, this)) return;
                diverge(p);
                addCd(K_DIVERGE, p);
                return;
            default:
        }
    }


    private void onMove(MoveExecute ex) {
        Player p = ex.getPlayer();
        maybeSpawnDouble(p, ex);

        if (!isEnabled()) { wasOnGround = p.isOnGround(); return; }

        boolean onGroundNow = p.isOnGround();
        double  dy          = ex.getTo().getY() - ex.getFrom().getY();

        if (wasOnGround && !onGroundNow && dy > 0.05 && !p.isFlying() && !p.isGliding()) {
            int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
            if (slot == 3 && !doubles.isEmpty() && !CooldownApi.isOnCooldown(K_CONVERGE, p)) {
                converge(p);
                addCd(K_CONVERGE, p);
            }
        }
        wasOnGround = onGroundNow;
    }

    private void maybeSpawnDouble(Player p, MoveExecute ex) {
        double distSq = ex.getFrom().distanceSquared(ex.getTo());
        if (distSq < SPAWN_MIN_DISTANCE * SPAWN_MIN_DISTANCE) return;
        if (idleTick - lastSpawnTick < SPAWN_INTERVAL_TICKS) return;
        lastSpawnTick = idleTick;
        spawnNewDouble(p);
    }

    private void spawnNewDouble(Player p) {
        List<Snapshot> path = new ArrayList<>();
        for (Snapshot s : history) path.add(new Snapshot(s.location.clone(), s.sneaking));
        if (path.isEmpty()) path.add(new Snapshot(p.getLocation().clone(), p.isSneaking()));

        if (doubles.size() >= MAX_DOUBLES) {
            UUID oldest = doubles.keySet().iterator().next();
            ShadowDouble old = doubles.remove(oldest);
            if (old != null) despawnDoubleVisual(old.stand);
        }

        Location spawnLoc = path.get(0).location.clone();
        ArmorStand stand = spawnDoubleStand(spawnLoc, p);

        ShadowDouble sd = new ShadowDouble();
        sd.stand     = stand;
        sd.path      = path;
        sd.pathIndex = -1;
        sd.spawnTick = idleTick;
        doubles.put(stand.getUniqueId(), sd);

        particleApi.spawnColoredParticles(spawnLoc.clone().add(0, 1, 0), C_UMBRA, 1.1f, 14, 0.25, 0.35, 0.25);
        particleApi.spawnParticles(spawnLoc.clone().add(0, 0.2, 0), Particle.SOUL, 6, 0.2, 0.1, 0.2, 0.02);
        p.getWorld().playSound(spawnLoc, Sound.ENTITY_PHANTOM_FLAP, 0.35f, 1.6f);
    }


    private void cloneAmbush(Player p) {
        if (doubles.isEmpty()) { sendActionBar(p, "§cNo shadow doubles active!"); return; }
        LivingEntity target = getInSight(p, AMBUSH_RANGE, 0.85);
        if (target == null) { sendActionBar(p, "§cNo target in sight!"); return; }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_BITE, 0.7f, 1.4f);
        double totalDmg = 0;
        for (ShadowDouble sd : doubles.values()) {
            if (sd.stand == null || !sd.stand.isValid()) continue;
            Location lunge = target.getLocation().clone().add(
                    (rng.nextDouble() - 0.5) * 1.6, 0, (rng.nextDouble() - 0.5) * 1.6);
            adjustToGround(lunge);
            sd.stand.teleport(lunge);
            particleLine(lunge.clone().add(0, 1, 0), target.getLocation().clone().add(0, 1, 0), 0.35, C_VOID, 0.9f);
            target.damage(AMBUSH_DMG_PER_DOUBLE, p);
            totalDmg += AMBUSH_DMG_PER_DOUBLE;
        }
        target.setVelocity(knockbackVector(p.getLocation(), target, 0.6, 0.25));
        particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0), C_VOID, 1.6f, 24, 0.3, 0.4, 0.3);
        sendActionBar(p, "§5Clone Ambush §ffor §c" + String.format("%.1f", totalDmg) + "§f damage.");
    }


    private void danceAway(Player p) {
        if (doubles.isEmpty()) { sendActionBar(p, "§cNo shadow doubles to dance with!"); return; }
        LivingEntity target = getInSight(p, DANCE_RANGE, 0.8);
        if (target == null) { sendActionBar(p, "§cNo target in sight!"); return; }

        applyPotion(target, PotionEffectType.SLOWNESS, DANCE_DIZZY_TICKS, 1);
        applyPotion(target, PotionEffectType.NAUSEA, DANCE_DIZZY_TICKS, 0);
        dizzyTargetId  = target.getUniqueId();
        dizzyUntilTick = idleTick + DANCE_DIZZY_TICKS;

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 1.5f);
        String name = target instanceof Player ? ((Player) target).getName() : target.getName();
        sendActionBar(p, "§dDancing circles around " + name + "...");

        final List<ArmorStand> stands = new ArrayList<>();
        final List<Double> baseAngles = new ArrayList<>();
        int i = 0;
        for (ShadowDouble sd : doubles.values()) {
            if (sd.stand == null || !sd.stand.isValid()) continue;
            stands.add(sd.stand);
            baseAngles.add(i * (360.0 / Math.max(1, doubles.size())));
            i++;
        }

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= DANCE_DURATION_TICKS || !target.isValid() || !p.isOnline()) { cancel(); return; }
                Location center = target.getLocation();
                for (int j = 0; j < stands.size(); j++) {
                    ArmorStand as = stands.get(j);
                    if (!as.isValid()) continue;
                    double angle = baseAngles.get(j) + t * 9;
                    Location orbit = orbitPoint(center, DANCE_RADIUS, angle, 0.05);
                    orbit.setDirection(center.toVector().subtract(orbit.toVector()));
                    as.teleport(orbit);
                }
                if (t % 3 == 0)
                    particleCircle(center.clone().add(0, 0.1, 0), DANCE_RADIUS, C_WISP, 0.7f, 18, t * 6);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }


    private void umbralSwap(Player p) {
        if (doubles.isEmpty()) { sendActionBar(p, "§cNo doubles to swap with!"); return; }
        UUID targetId = nearestDoubleId(p);
        ShadowDouble sd = targetId == null ? null : doubles.get(targetId);
        if (sd == null || sd.stand == null || !sd.stand.isValid()) {
            sendActionBar(p, "§cNo doubles to swap with!");
            return;
        }

        Location pLoc = p.getLocation().clone();
        Location dLoc = sd.stand.getLocation().clone();

        particleApi.spawnColoredParticles(pLoc.clone().add(0, 1, 0), C_UMBRA, 1.4f, 26, 0.3, 0.4, 0.3);
        particleApi.spawnColoredParticles(dLoc.clone().add(0, 1, 0), C_WISP, 1.4f, 26, 0.3, 0.4, 0.3);
        p.getWorld().playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);

        p.teleport(dLoc);
        sd.stand.teleport(pLoc);
        List<Snapshot> single = new ArrayList<>();
        single.add(new Snapshot(pLoc.clone(), false));
        sd.path      = single;
        sd.pathIndex = -1;

        applyPotion(p, PotionEffectType.SLOW_FALLING, SWAP_SAFEFALL_TICKS, 0);
        p.getWorld().playSound(dLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
        sendActionBar(p, "§5Swapped with your double.");
    }

    private UUID nearestDoubleId(Player p) {
        UUID best = null;
        double bestD = Double.MAX_VALUE;
        for (Map.Entry<UUID, ShadowDouble> e : doubles.entrySet()) {
            if (e.getValue().stand == null || !e.getValue().stand.isValid()) continue;
            double d = e.getValue().stand.getLocation().distance(p.getLocation());
            if (d < bestD) { bestD = d; best = e.getKey(); }
        }
        return best;
    }


    private void converge(Player p) {
        List<UUID> ids = new ArrayList<>(doubles.keySet());
        int count = ids.size();
        if (count == 0) return;

        Location center = p.getLocation().clone().add(0, 1, 0);
        for (UUID id : ids) {
            ShadowDouble sd = doubles.remove(id);
            if (sd == null || sd.stand == null) continue;
            if (sd.stand.isValid()) {
                Location from = sd.stand.getLocation().clone().add(0, 1, 0);
                particleLine(from, center, 0.4, C_UMBRA, 1.0f);
                particleApi.spawnParticles(from, Particle.SOUL, 8, 0.2, 0.3, 0.2, 0.03);
            }
            despawnDoubleVisual(sd.stand);
        }
        double healAmount = count * CONVERGE_HEAL_FRACTION * getMaxHp(p);
        safeHeal(p, healAmount);

        particleApi.spawnColoredParticles(center, C_WISP, 1.6f, 30, 0.35, 0.4, 0.35);
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.3f);
        sendActionBar(p, "§aConverged §f" + count + " §adouble(s) — healed §f"
                + String.format("%.1f", healAmount) + "§a HP.");
    }


    private void diverge(Player p) {
        if (doubles.isEmpty()) { sendActionBar(p, "§cNo shadow doubles to scatter!"); return; }

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 0.8f);
        int i = 0;
        List<ArmorStand> spread = new ArrayList<>();
        for (ShadowDouble sd : doubles.values()) {
            if (sd.stand == null || !sd.stand.isValid()) continue;
            double angle = i * (360.0 / doubles.size());
            Location dest = orbitPoint(p.getLocation(), DIVERGE_RADIUS, angle, 0);
            adjustToGround(dest);
            dest.setDirection(p.getLocation().toVector().subtract(dest.toVector()));
            sd.stand.teleport(dest);
            List<Snapshot> single = new ArrayList<>();
            single.add(new Snapshot(dest.clone(), false));
            sd.path      = single;
            sd.pathIndex = -1;
            particleApi.spawnColoredParticles(dest.clone().add(0, 1, 0), C_UMBRA, 1.2f, 16, 0.2, 0.3, 0.2);
            spread.add(sd.stand);
            i++;
        }

        int assigned = 0;
        for (LivingEntity e : getNearbyTargets(p, DIVERGE_AGGRO_RADIUS)) {
            if (assigned >= spread.size()) break;
            if (!(e instanceof Mob)) continue;
            Mob mob = (Mob) e;
            if (mob.getTarget() == null || !mob.getTarget().equals(p)) continue;
            mob.setTarget(spread.get(assigned));
            assigned++;
        }
        sendActionBar(p, "§bDoubles scattered.");
    }


    private void mirrorImageFrenzy(Player p) {
        p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ MIRROR IMAGE FRENZY ✦");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.6f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.8f);

        int toSpawn = Math.max(0, FRENZY_TOTAL_DOUBLES - doubles.size());
        for (int i = 0; i < toSpawn; i++) {
            double angle = i * (360.0 / Math.max(1, toSpawn));
            Location loc = orbitPoint(p.getLocation(), 2.0, angle, 0);
            adjustToGround(loc);
            ArmorStand stand = spawnDoubleStand(loc, p);

            ShadowDouble sd = new ShadowDouble();
            sd.stand = stand;
            List<Snapshot> single = new ArrayList<>();
            single.add(new Snapshot(loc.clone(), false));
            sd.path      = single;
            sd.pathIndex = -1;
            sd.spawnTick = idleTick;
            doubles.put(stand.getUniqueId(), sd);
        }

        frenzyActive  = true;
        frenzyEndTick = idleTick + FRENZY_DURATION_TICKS;

        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0), C_VOID, 2.2f, 60, 0.6, 0.7, 0.6);
        particleCircle(p.getLocation().clone().add(0, 0.1, 0), 3.0, C_WISP, 1.4f, 30, 0);
    }

    private void frenzyStrike(Player p, ShadowDouble sd) {
        if (sd.stand == null || !sd.stand.isValid()) return;
        LivingEntity target = findFrenzyTarget(p, sd.stand.getLocation(), FRENZY_STRIKE_RADIUS);
        if (target == null) return;

        Location dest = target.getLocation().clone().add(
                (rng.nextDouble() - 0.5) * 1.4, 0, (rng.nextDouble() - 0.5) * 1.4);
        adjustToGround(dest);
        sd.stand.teleport(dest);

        target.damage(FRENZY_HIT_DMG, p);
        target.setVelocity(knockbackVector(dest, target, 0.4, 0.2));
        particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0), C_VOID, 1.0f, 10, 0.2, 0.3, 0.2);
        p.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_BITE, 0.3f, 1.7f);
    }

    private LivingEntity findFrenzyTarget(Player owner, Location from, double radius) {
        LivingEntity best = null;
        double bestD = radius;
        for (Entity e : from.getWorld().getNearbyEntities(from, radius, radius, radius)) {
            if (e.equals(owner) || e instanceof ArmorStand || !(e instanceof LivingEntity)) continue;
            double d = e.getLocation().distance(from);
            if (d < bestD) { bestD = d; best = (LivingEntity) e; }
        }
        return best;
    }

    private void endFrenzy(Player p) {
        frenzyActive = false;
        Location center = p.getLocation().clone().add(0, 1, 0);
        for (ShadowDouble sd : doubles.values()) {
            if (sd.stand != null && sd.stand.isValid()) {
                particleApi.spawnColoredParticles(sd.stand.getLocation().clone().add(0, 1, 0), C_VOID, 1.8f, 20, 0.25, 0.35, 0.25);
            }
            despawnDoubleVisual(sd.stand);
        }
        doubles.clear();
        p.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.35f, 1.8f);
        particleApi.spawnColoredParticles(center, C_WISP, 1.6f, 30, 0.4, 0.5, 0.4);
        sendActionBar(p, "§5The mirror shatters.");
    }


    private void passiveConfusion(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        Entity damager = ex.getDamager();

        boolean doublesPresent = !doubles.isEmpty();
        boolean dizzyAttacker  = damager != null && dizzyTargetId != null
                && damager.getUniqueId().equals(dizzyTargetId) && idleTick <= dizzyUntilTick;
        if (!doublesPresent && !dizzyAttacker) return;

        if (rng.nextDouble() < MISS_CHANCE) {
            ex.getDamageEvent().setCancelled(true);
            particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0), C_WISP, 0.9f, 10, 0.25, 0.3, 0.25);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.4f, 1.8f);
            sendActionBar(p, "§5Your doubles confused the attack!");
        }
    }


    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                history.addLast(new Snapshot(p.getLocation().clone(), p.isSneaking()));
                while (history.size() > HISTORY_MAX_SAMPLES) history.removeFirst();

                List<UUID> expired = new ArrayList<>();
                for (Map.Entry<UUID, ShadowDouble> entry : doubles.entrySet()) {
                    ShadowDouble sd = entry.getValue();
                    if (sd.stand == null || sd.stand.isDead() || !sd.stand.isValid()) {
                        expired.add(entry.getKey());
                        continue;
                    }

                    if (!frenzyActive && (idleTick - sd.spawnTick) >= DOUBLE_DURATION_TICKS) {
                        despawnDoubleVisual(sd.stand);
                        expired.add(entry.getKey());
                        continue;
                    }

                    if (!sd.path.isEmpty()) {
                        sd.pathIndex = (sd.pathIndex + 1) % sd.path.size();
                        Snapshot snap = sd.path.get(sd.pathIndex);
                        Location target = snap.location.clone();
                        if (snap.sneaking) target.subtract(0, 0.15, 0);
                        sd.stand.teleport(target);
                    }

                    if (idleTick % 4 == 0) {
                        particleApi.spawnColoredParticles(sd.stand.getLocation().clone().add(0, 1.1, 0), C_UMBRA, 0.9f, 3, 0.15, 0.25, 0.15);
                        particleApi.spawnParticles(sd.stand.getLocation().clone().add(0, 0.1, 0), Particle.SCULK_SOUL, 1, 0.15, 0.05, 0.15, 0.01);
                    }

                    if (frenzyActive && idleTick % FRENZY_HIT_INTERVAL_TICKS == 0) {
                        frenzyStrike(p, sd);
                    }
                }
                for (UUID id : expired) doubles.remove(id);

                if (frenzyActive && idleTick >= frenzyEndTick) {
                    endFrenzy(p);
                }

                if (isAuraEnabled(p) && doubles.isEmpty() && idleTick % 12 == 0) {
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.6, 0), C_WISP, 0.5f, 1, 0.1, 0.05, 0.1);
                }

                idleTick++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 1);
        return r;
    }

    @Override
    public void remove() {
        for (ShadowDouble sd : doubles.values()) safeRemove(sd.stand);
        doubles.clear();
        history.clear();
        frenzyActive  = false;
        dizzyTargetId = null;
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§5Clone Ambush";
            case 1: return "§dDance Away";
            case 2: return "§5Umbral Swap";
            case 3: return "§aConverge";
            case 4: return "§bDiverge";
            case 5: return "§5§l✦ MIRROR IMAGE FRENZY §d[ULT]";
            default: return "§7none";
        }
    }


    private ArmorStand spawnDoubleStand(Location loc, Player p) {
        ArmorStand as = loc.getWorld().spawn(loc, ArmorStand.class, en -> {
            en.setVisible(true);
            en.setArms(true);
            en.setBasePlate(false);
            en.setSmall(false);
            en.setGravity(false);
            en.setMarker(false);
            en.setCollidable(false);
            en.setInvulnerable(true);
            en.setSilent(true);
            en.setPersistent(false);
            en.setCustomNameVisible(false);
            en.setRightArmPose(new EulerAngle(Math.toRadians(-15), 0, 0));
        });
        as.getEquipment().setHelmet(playerHeadSkull(p));
        as.getEquipment().setChestplate(darkLeather(Material.LEATHER_CHESTPLATE));
        as.getEquipment().setLeggings(darkLeather(Material.LEATHER_LEGGINGS));
        as.getEquipment().setBoots(darkLeather(Material.LEATHER_BOOTS));
        ItemStack hand = p.getInventory().getItemInMainHand();
        as.getEquipment().setItemInMainHand(
                hand != null && hand.getType() != Material.AIR ? hand.clone() : new ItemStack(Material.STICK));
        return as;
    }

    private ItemStack playerHeadSkull(Player p) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack darkLeather(Material mat) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(C_INK);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void despawnDoubleVisual(ArmorStand stand) {
        if (stand == null) return;
        if (stand.isValid()) {
            particleApi.spawnParticles(stand.getLocation().clone().add(0, 1, 0), Particle.SOUL, 6, 0.15, 0.3, 0.15, 0.02);
            stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.3f, 1.7f);
        }
        safeRemove(stand);
    }
}
