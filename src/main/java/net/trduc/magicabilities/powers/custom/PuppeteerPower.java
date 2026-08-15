package net.trduc.magicabilitiesfork.powers.custom;

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

public class PuppeteerPower extends Power implements IdlePower, Removeable {

    private static final String p_string = "puppeteer.string";
    private static final String p_yank   = "puppeteer.yank";
    private static final String p_swap   = "puppeteer.swap";
    private static final String p_march  = "puppeteer.march";
    private static final String p_cut    = "puppeteer.cut";
    private static final String p_web    = "puppeteer.web";
    private static final String p_loose  = "puppeteer.loose";
    private static final String p_danse  = "puppeteer.danse";

    private static final Color C_VIOLET = Color.fromRGB(150, 60,  210);
    private static final Color C_INK    = Color.fromRGB(70,  20,  110);
    private static final Color C_PALE   = Color.fromRGB(230, 210, 245);
    private static final Color C_BLOOD  = Color.fromRGB(180, 20,  40);

    private int    XP_DANSE;
    private int    MAX_STRINGS;
    private double LEASH_RANGE;
    private double STRING_RANGE;
    private double STRING_MITIGATION;

    private double STRING_PRICK_DMG;
    private double YANK_DMG;
    private double YANK_STRENGTH;
    private double YANK_PULL_CAP;
    private int    SWAP_SAFEFALL_TICKS;
    private int    MARCH_RECORD_TICKS;
    private int    MARCH_STAGGER_TICKS;
    private double MARCH_HIT_RADIUS;
    private double MARCH_COLLISION_DMG;
    private double CUT_BASE_DMG;
    private double CUT_DMG_PER_DISTANCE;
    private double CUT_DMG_MAX;
    private double WEB_RANGE;
    private double WEB_RADIUS;
    private int    WEB_DURATION_TICKS;
    private int    LOOSE_DISORIENT_TICKS;
    private int    LOOSE_SPEED_TICKS;
    private int    DANSE_DURATION_TICKS;
    private double DANSE_SOLO_RADIUS;
    private double DANSE_FINISHER_DMG;
    private double DANSE_PULL_STRENGTH;

    private final LinkedHashMap<UUID, StringLink> strings = new LinkedHashMap<>();

    private static final class StringLink {
        final UUID targetId;
        double pulledDistance = 0;
        boolean aiDisabled = false;
        StringLink(UUID targetId) { this.targetId = targetId; }
    }

    public PuppeteerPower(Player owner) {
        super(owner);
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration cfg = magicPlugin.getConfig();

        XP_DANSE           = cfg.getInt("puppeteer.xp.danse", 30);
        MAX_STRINGS         = cfg.getInt("puppeteer.strings.max", 3);
        LEASH_RANGE          = cfg.getDouble("puppeteer.strings.leash-range", 20.0);
        STRING_RANGE         = cfg.getDouble("puppeteer.strings.string-range", 18.0);
        STRING_MITIGATION    = cfg.getDouble("puppeteer.strings.mitigation-fraction", 0.25);

        STRING_PRICK_DMG     = cfg.getDouble("puppeteer.dmg.string-prick", 1.5);
        YANK_DMG             = cfg.getDouble("puppeteer.dmg.yank-dmg", 2.5);
        YANK_STRENGTH        = cfg.getDouble("puppeteer.dmg.yank-strength", 1.1);
        YANK_PULL_CAP        = cfg.getDouble("puppeteer.dmg.yank-pull-cap", 6.0);
        SWAP_SAFEFALL_TICKS  = cfg.getInt("puppeteer.dmg.swap-safefall-ticks", 30);
        MARCH_RECORD_TICKS   = cfg.getInt("puppeteer.dmg.march-record-ticks", 60);
        MARCH_STAGGER_TICKS  = cfg.getInt("puppeteer.dmg.march-stagger-ticks", 4);
        MARCH_HIT_RADIUS     = cfg.getDouble("puppeteer.dmg.march-hit-radius", 1.2);
        MARCH_COLLISION_DMG  = cfg.getDouble("puppeteer.dmg.march-collision-dmg", 3.5);
        CUT_BASE_DMG         = cfg.getDouble("puppeteer.dmg.cut-base-dmg", 4.0);
        CUT_DMG_PER_DISTANCE = cfg.getDouble("puppeteer.dmg.cut-dmg-per-distance", 0.5);
        CUT_DMG_MAX          = cfg.getDouble("puppeteer.dmg.cut-dmg-max", 18.0);
        WEB_RANGE            = cfg.getDouble("puppeteer.dmg.web-range", 20.0);
        WEB_RADIUS           = cfg.getDouble("puppeteer.dmg.web-radius", 3.0);
        WEB_DURATION_TICKS   = cfg.getInt("puppeteer.dmg.web-duration-ticks", 200);
        LOOSE_DISORIENT_TICKS= cfg.getInt("puppeteer.dmg.loose-disorient-ticks", 40);
        LOOSE_SPEED_TICKS    = cfg.getInt("puppeteer.dmg.loose-speed-ticks", 60);
        DANSE_DURATION_TICKS = cfg.getInt("puppeteer.dmg.danse-duration-ticks", 100);
        DANSE_SOLO_RADIUS    = cfg.getDouble("puppeteer.dmg.danse-solo-radius", 10.0);
        DANSE_FINISHER_DMG   = cfg.getDouble("puppeteer.dmg.danse-finisher-dmg", 9.0);
        DANSE_PULL_STRENGTH  = cfg.getDouble("puppeteer.dmg.danse-pull-strength", 0.9);
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DamagedByExecute) {
            passiveStringWard((DamagedByExecute) ex);
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
            case 0: if (onCd(p_string, p, this)) return; stringShot(p); addCd(p_string, p); return;
            case 1: if (onCd(p_yank, p, this)) return; yank(p); addCd(p_yank, p); return;
            case 2: if (onCd(p_swap, p, this)) return; marionetteSwap(p); addCd(p_swap, p); return;
            case 3: if (onCd(p_march, p, this)) return; puppetMarch(p); addCd(p_march, p); return;
            case 4: if (onCd(p_cut, p, this)) return; cutString(p); addCd(p_cut, p); return;
            case 7:
                if (onCd(p_danse, p, this)) return;
                if (!checkXp(p, XP_DANSE, this)) return;
                spendXp(p, XP_DANSE);
                danseMacabre(p);
                addCd(p_danse, p);
                return;
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 5) return;
        if (onCd(p_web, p, this)) return;
        stringWeb(p);
        addCd(p_web, p);
    }

    private void onSneak(SneakExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
        if (slot != 6) return;
        if (onCd(p_loose, p, this)) return;
        cutLoose(p);
        addCd(p_loose, p);
    }


    private void stringShot(Player p) {
        LivingEntity target = getInSight(p, STRING_RANGE, 0.9);
        if (target == null) {
            sendActionBar(p, "§cNo target in sight!");
            return;
        }
        attachString(p, target);
    }


    private void yank(Player p) {
        List<LivingEntity> targets = validStrungTargets(true);
        if (targets.isEmpty()) {
            sendActionBar(p, "§cNo strings attached!");
            return;
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.9f, 0.7f);
        Location origin = p.getLocation().clone().add(0, 1, 0);

        for (LivingEntity t : targets) {
            StringLink link = strings.get(t.getUniqueId());
            Vector toPlayer = origin.toVector().subtract(t.getLocation().toVector());
            double dist = toPlayer.length();
            if (!isVecFinite(toPlayer) || dist < 0.15) continue;

            double pulled = Math.min(dist, YANK_PULL_CAP);
            if (link != null) link.pulledDistance += pulled;

            t.setVelocity(toPlayer.normalize().multiply(YANK_STRENGTH).setY(0.25));
            t.damage(YANK_DMG, p);
            particleLine(t.getLocation().clone().add(0, 1, 0), origin, 0.5, C_VIOLET, 0.8f);
        }
    }


    private void marionetteSwap(Player p) {
        LivingEntity target = nearestStrungTarget(p);
        if (target == null) {
            sendActionBar(p, "§cNo strings attached!");
            return;
        }
        Location pLoc = p.getLocation().clone();
        Location tLoc = target.getLocation().clone();

        particleApi.spawnColoredParticles(pLoc.clone().add(0, 1, 0), C_VIOLET, 1.5f, 30, 0.3, 0.4, 0.3);
        particleApi.spawnColoredParticles(tLoc.clone().add(0, 1, 0), C_PALE, 1.5f, 30, 0.3, 0.4, 0.3);
        p.getWorld().playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.1f);

        p.teleport(tLoc);
        target.teleport(pLoc);

        applyPotion(p, PotionEffectType.SLOW_FALLING, SWAP_SAFEFALL_TICKS, 0);
        applyPotion(target, PotionEffectType.SLOW_FALLING, SWAP_SAFEFALL_TICKS, 0);

        p.getWorld().playSound(tLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.6f);
        sendActionBar(p, "§dSwapped places with your puppet.");
    }


    private void puppetMarch(Player p) {
        List<LivingEntity> targets = validStrungTargets(true);
        if (targets.isEmpty()) {
            sendActionBar(p, "§cNo strings attached to march!");
            return;
        }

        for (LivingEntity t : targets) {
            StringLink link = strings.get(t.getUniqueId());
            if (link != null && !link.aiDisabled && t instanceof Mob) {
                ((Mob) t).setAI(false);
                link.aiDisabled = true;
            }
            t.setVelocity(new Vector(0, 0, 0));
            particleApi.spawnColoredParticles(t.getLocation().clone().add(0, 1.2, 0), C_VIOLET, 1.1f, 10, 0.2, 0.3, 0.2);
        }
        sendActionBar(p, "§5Recording movement...");
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 1.6f);

        final List<Location> path = new ArrayList<>();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline() || t >= MARCH_RECORD_TICKS) {
                    startMarchPlayback(p, targets, path);
                    cancel();
                    return;
                }
                path.add(p.getLocation().clone());
                if (t % 5 == 0)
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 2.0, 0), C_PALE, 0.8f, 2, 0.1, 0.1, 0.1);
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void startMarchPlayback(Player p, List<LivingEntity> targets, List<Location> path) {
        if (path.isEmpty()) {
            for (LivingEntity t : targets) releaseMarchAI(t);
            return;
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.2f);

        int i = 0;
        for (LivingEntity t : targets) {
            int delay = i * MARCH_STAGGER_TICKS;
            i++;
            new BukkitRunnable() {
                int idx = 0;
                final Set<UUID> hit = new HashSet<>();
                @Override public void run() {
                    if (!t.isValid() || idx >= path.size()) {
                        releaseMarchAI(t);
                        cancel();
                        return;
                    }
                    Location step = path.get(idx).clone();
                    step.setPitch(t.getLocation().getPitch());
                    step.setYaw(t.getLocation().getYaw());
                    t.teleport(step);
                    particleApi.spawnColoredParticles(step.clone().add(0, 1, 0), C_INK, 0.9f, 3, 0.1, 0.15, 0.1);

                    for (Entity other : step.getWorld().getNearbyEntities(step, MARCH_HIT_RADIUS, MARCH_HIT_RADIUS, MARCH_HIT_RADIUS)) {
                        if (other.equals(t) || other.equals(p) || !(other instanceof LivingEntity)) continue;
                        if (hit.contains(other.getUniqueId())) continue;
                        hit.add(other.getUniqueId());
                        ((LivingEntity) other).damage(MARCH_COLLISION_DMG, p);
                        other.setVelocity(knockbackVector(step, other, 0.5, 0.2));
                    }
                    idx++;
                }
            }.runTaskTimer(magicPlugin, delay, 1);
        }
    }

    private void releaseMarchAI(LivingEntity t) {
        StringLink link = strings.get(t.getUniqueId());
        if (link != null && link.aiDisabled && t instanceof Mob) {
            ((Mob) t).setAI(true);
            link.aiDisabled = false;
        }
    }


    private void cutString(Player p) {
        LivingEntity target = nearestStrungTarget(p);
        if (target == null) {
            sendActionBar(p, "§cNo strings attached!");
            return;
        }
        StringLink link = strings.get(target.getUniqueId());
        double pulled = link != null ? link.pulledDistance : 0;
        double dmg = Math.min(CUT_DMG_MAX, CUT_BASE_DMG + pulled * CUT_DMG_PER_DISTANCE);

        Location loc = target.getLocation().clone().add(0, 1, 0);
        particleApi.spawnColoredParticles(loc, C_VIOLET, 1.8f, 30, 0.3, 0.4, 0.3);
        particleApi.spawnColoredParticles(loc, C_PALE, 1.3f, 15, 0.3, 0.4, 0.3);
        target.getWorld().playSound(loc, Sound.BLOCK_CHAIN_BREAK, 0.9f, 0.8f);

        target.damage(dmg, p);
        target.setVelocity(knockbackVector(p.getLocation(), target, 0.9, 0.3));
        detachString(target.getUniqueId());
        sendActionBar(p, "§4Snapped for §f" + String.format("%.1f", dmg) + "§4 damage.");
    }


    private void stringWeb(Player p) {
        Location target = getRaycastTarget(p, (int) WEB_RANGE);
        adjustToGround(target);
        final Location center = target.clone();

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.6f);
        particleCircle(center, WEB_RADIUS, C_VIOLET, 1.0f, 20, 0);

        new BukkitRunnable() {
            int t = 0;
            final Set<UUID> tagged = new HashSet<>();
            @Override public void run() {
                if (t >= WEB_DURATION_TICKS || !p.isOnline()) { cancel(); return; }

                if (t % 6 == 0)
                    particleCircle(center, WEB_RADIUS, C_PALE, 0.8f, 14, t * 4);

                if (t % 8 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, WEB_RADIUS, 1.5, WEB_RADIUS)) {
                        if (e.equals(p) || e instanceof Player || !(e instanceof LivingEntity)) continue;
                        if (tagged.contains(e.getUniqueId()) || strings.containsKey(e.getUniqueId())) continue;
                        tagged.add(e.getUniqueId());
                        attachString(p, (LivingEntity) e);
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }


    private void cutLoose(Player p) {
        List<LivingEntity> targets = validStrungTargets(true);
        if (targets.isEmpty()) {
            sendActionBar(p, "§cNo strings to cut loose!");
            return;
        }
        for (LivingEntity t : targets) {
            applyPotion(t, PotionEffectType.BLINDNESS, LOOSE_DISORIENT_TICKS, 0);
            applyPotion(t, PotionEffectType.SLOWNESS, LOOSE_DISORIENT_TICKS, 1);
            particleApi.spawnColoredParticles(t.getLocation().clone().add(0, 1, 0), C_PALE, 1.3f, 20, 0.25, 0.35, 0.25);
        }
        detachAll();
        applyPotion(p, PotionEffectType.SPEED, LOOSE_SPEED_TICKS, 1);
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0), C_VIOLET, 1.6f, 35, 0.3, 0.4, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.8f, 1.3f);
        sendActionBar(p, "§fStrings cut loose.");
    }


    private void danseMacabre(Player p) {
        List<LivingEntity> targets = validStrungTargets(true);
        if (targets.isEmpty()) {
            sendActionBar(p, "§cNo puppets to dance!");
            return;
        }

        p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Danse Macabre");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 0.8f);

        for (LivingEntity t : targets) {
            StringLink link = strings.get(t.getUniqueId());
            if (t instanceof Mob && link != null && link.aiDisabled) {
                ((Mob) t).setAI(true);
                link.aiDisabled = false;
            }
            applyPotion(t, PotionEffectType.GLOWING, DANSE_DURATION_TICKS + 20, 0);
        }

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                List<LivingEntity> alive = new ArrayList<>();
                for (LivingEntity e : targets) if (e.isValid()) alive.add(e);

                if (t >= DANSE_DURATION_TICKS || alive.isEmpty() || !p.isOnline()) {
                    finishDanse(p, alive);
                    cancel();
                    return;
                }
                if (t % 15 == 0) {
                    for (int i = 0; i < alive.size(); i++) {
                        LivingEntity self = alive.get(i);
                        if (self instanceof Mob) {
                            LivingEntity other = alive.size() > 1
                                    ? alive.get((i + 1) % alive.size())
                                    : findNearestNonStrungEnemy(self, p);
                            if (other != null) ((Mob) self).setTarget(other);
                        } else if (self instanceof Player) {
                            self.damage(1.0, p);
                            applyPotion(self, PotionEffectType.SLOWNESS, 30, 1);
                            applyPotion(self, PotionEffectType.NAUSEA, 30, 0);
                        }
                    }
                }
                if (t % 6 == 0) {
                    for (LivingEntity e : alive)
                        particleApi.spawnColoredParticles(e.getLocation().clone().add(0, 1.5, 0), C_VIOLET, 1.2f, 4, 0.2, 0.2, 0.2);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private LivingEntity findNearestNonStrungEnemy(LivingEntity from, Player owner) {
        LivingEntity best = null;
        double bestD = DANSE_SOLO_RADIUS;
        for (Entity e : from.getWorld().getNearbyEntities(from.getLocation(), DANSE_SOLO_RADIUS, DANSE_SOLO_RADIUS, DANSE_SOLO_RADIUS)) {
            if (e.equals(from) || e.equals(owner) || !(e instanceof LivingEntity)) continue;
            double d = e.getLocation().distance(from.getLocation());
            if (d < bestD) { bestD = d; best = (LivingEntity) e; }
        }
        return best;
    }

    private void finishDanse(Player p, List<LivingEntity> alive) {
        if (alive.isEmpty()) return;

        double cx = 0, cy = 0, cz = 0;
        for (LivingEntity e : alive) {
            cx += e.getLocation().getX();
            cy += e.getLocation().getY();
            cz += e.getLocation().getZ();
        }
        Location center = new Location(alive.get(0).getWorld(), cx / alive.size(), cy / alive.size(), cz / alive.size());

        particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_VIOLET, 2.2f, 80, 1.0, 1.0, 1.0);
        particleApi.spawnColoredParticles(center.clone().add(0, 1, 0), C_PALE, 1.6f, 40, 1.0, 1.0, 1.0);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.5f, 1.6f);

        for (LivingEntity e : alive) {
            Vector pull = center.toVector().subtract(e.getLocation().toVector());
            if (isVecFinite(pull) && pull.lengthSquared() > 0.01)
                e.setVelocity(pull.normalize().multiply(DANSE_PULL_STRENGTH).setY(0.15));
            e.damage(DANSE_FINISHER_DMG, p);
            detachString(e.getUniqueId());
        }
    }


    private void passiveStringWard(DamagedByExecute ex) {
        Player p = ex.getPlayer();
        Entity damager = ex.getDamager();
        if (damager == null) return;
        if (!strings.containsKey(damager.getUniqueId())) return;

        double reduced = ex.getDamageEvent().getDamage() * (1 - STRING_MITIGATION);
        ex.getDamageEvent().setDamage(Math.max(0, reduced));
        particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1, 0), C_VIOLET, 0.8f, 4, 0.2, 0.2, 0.2);
    }


    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }

                if (!strings.isEmpty() && t % 4 == 0) {
                    for (LivingEntity target : validStrungTargets(true)) {
                        particleLine(p.getEyeLocation(), target.getLocation().clone().add(0, 1, 0), 0.6, C_VIOLET, 0.7f);
                    }
                }
                if (isAuraEnabled(p) && !strings.isEmpty() && t % 10 == 0) {
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.4, 0), C_PALE, 0.6f, 2, 0.15, 0.1, 0.15);
                }
                t++;
            }
        };
        r.runTaskTimer(magicPlugin, 0, 1);
        return r;
    }

    @Override
    public void remove() {
        detachAll();
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§5String Shot";
            case 1: return "§5Yank";
            case 2: return "§dMarionette Swap";
            case 3: return "§dPuppet March";
            case 4: return "§4Cut String";
            case 5: return "§5String Web";
            case 6: return "§fCut Loose";
            case 7: return "§5§l✂ DANSE MACABRE §d[ULT]";
            default: return "§7none";
        }
    }


    private void attachString(Player p, LivingEntity target) {
        if (target == null || target.equals(p)) return;
        UUID id = target.getUniqueId();
        if (strings.containsKey(id)) {
            sendActionBar(p, "§5Already strung.");
            return;
        }
        if (strings.size() >= MAX_STRINGS) {
            UUID oldest = strings.keySet().iterator().next();
            detachString(oldest);
        }
        strings.put(id, new StringLink(id));

        target.damage(STRING_PRICK_DMG, p);
        particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0), C_VIOLET, 1.1f, 14, 0.25, 0.35, 0.25);
        particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0), C_BLOOD, 0.8f, 4, 0.15, 0.2, 0.15);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.7f, 1.7f);
        sendActionBar(p, "§5Strung §f(" + strings.size() + "/" + MAX_STRINGS + ")");
    }

    private void detachString(UUID id) {
        StringLink link = strings.remove(id);
        if (link == null) return;
        Entity e = Bukkit.getEntity(id);
        if (link.aiDisabled && e instanceof Mob) {
            ((Mob) e).setAI(true);
        }
    }

    private void detachAll() {
        for (UUID id : new ArrayList<>(strings.keySet())) detachString(id);
    }

    private List<LivingEntity> validStrungTargets(boolean cleanup) {
        List<LivingEntity> out = new ArrayList<>();
        List<UUID> stale = new ArrayList<>();
        Location ownerLoc = getOwner().getLocation();
        for (UUID id : strings.keySet()) {
            Entity e = Bukkit.getEntity(id);
            if (e == null || e.isDead() || !e.isValid() || !(e instanceof LivingEntity)
                    || e.getWorld() == null || !e.getWorld().equals(ownerLoc.getWorld())
                    || e.getLocation().distance(ownerLoc) > LEASH_RANGE) {
                stale.add(id);
                continue;
            }
            out.add((LivingEntity) e);
        }
        if (cleanup) for (UUID id : stale) detachString(id);
        return out;
    }

    private LivingEntity nearestStrungTarget(Player p) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : validStrungTargets(true)) {
            double d = e.getLocation().distance(p.getLocation());
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }
}
