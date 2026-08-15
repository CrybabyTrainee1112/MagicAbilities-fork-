package net.trduc.magicabilitiesfork.powers.custom;

import net.trduc.magicabilitiesfork.powers.IdlePower;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.Removeable;
import net.trduc.magicabilitiesfork.powers.executions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;
import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.particleApi;
import static net.trduc.magicabilitiesfork.misc.PowerUtils.*;
import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;
import static net.trduc.magicabilitiesfork.cooldowns.CooldownApi.isOnCooldown;

public class VenomPalettePower extends Power implements IdlePower, Removeable {

    private static final String vp_trigger  = "venompalette.trigger";
    private static final String vp_canvas   = "venompalette.canvas";
    private static final String vp_inject   = "venompalette.inject";
    private static final String vp_antidote = "venompalette.antidote";
    private static final String vp_ult      = "venompalette.masterpiece";

    private static final int    MAX_MARKS         = 4;
    private static final double CASCADE_RADIUS    = 3.0;
    private static final double ULT_RADIUS        = 10.0;
    private static final double AURA_RADIUS       = 5.0;
    private static final double ANTIDOTE_RADIUS   = 6.0;
    private static final double CANVAS_RADIUS     = 2.2;
    private static final long   CANVAS_LIFE_TICKS = 160L;
    private static final long   AURA_LIFE_TICKS   = 600L;

    private static final Color C_MARK     = Color.fromRGB(60,  200, 90);
    private static final Color C_WARN     = Color.fromRGB(220, 60,  40);
    private static final Color C_BURST    = Color.fromRGB(130, 235, 140);
    private static final Color C_CANVAS   = Color.fromRGB(80,  190, 100);
    private static final Color C_INJECT   = Color.fromRGB(25,  120, 55);
    private static final Color C_ANTIDOTE = Color.fromRGB(165, 230, 255);
    private static final Color C_AURA     = Color.fromRGB(95,  210, 115);

    private static final NamespacedKey MARK_KEY = new NamespacedKey("magicabilitiesfork", "vp_marks");
    private static final Map<UUID, Integer> activeMarks = new ConcurrentHashMap<>();
    private static volatile boolean tickerStarted = false;

    private final int XP_ULT;
    private boolean wasOnGround;
    private boolean internalDamage = false;
    private BukkitRunnable auraTask = null;

    public VenomPalettePower(Player owner) {
        super(owner);
        XP_ULT = cfgInt("venompalette.xp.masterpiece", 24);
        wasOnGround = owner.isOnGround();
        ensureTicker();
    }

    @Override
    public void executePower(Execute ex) {
        if (ex instanceof DealDamageExecute) { onDealDamage((DealDamageExecute) ex); return; }
        if (ex instanceof MoveExecute)       { onMove((MoveExecute) ex); return; }
        if (!isEnabled()) return;
        if (ex instanceof LeftClickExecute)  { onLeft((LeftClickExecute) ex);   return; }
        if (ex instanceof RightClickExecute) { onRight((RightClickExecute) ex); }
    }

    private void onDealDamage(DealDamageExecute ex) {
        if (internalDamage) return;
        Player p = ex.getPlayer();
        if (!(ex.getTarget() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) ex.getTarget();
        if (target.equals(p) || target instanceof ArmorStand) return;

        paintMark(target, p, 1, true);
        particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1.6, 0),
                C_MARK, 0.7f, 4, 0.15, 0.1, 0.15);
    }

    private void onLeft(LeftClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());

        if (slot == 2 && p.isSneaking()) {
            if (onCd(vp_canvas, p, this)) return;
            toxicCanvas(p);
            addCd(vp_canvas, p);
            return;
        }
        if (slot == 5) {
            if (onCd(vp_ult, p, this)) return;
            if (!checkXp(p, XP_ULT, this)) return;
            spendXp(p, XP_ULT);
            masterpiece(p);
            addCd(vp_ult, p);
        }
    }

    private void onRight(RightClickExecute ex) {
        Player p = ex.getPlayer();
        int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());

        if (slot == 1) {
            if (onCd(vp_trigger, p, this)) return;
            LivingEntity target = getInSight(p, 12, 0.85);
            if (target == null) { sendActionBar(p, "§7Khong co muc tieu trong tam nhin."); return; }
            int marks = getMarks(target);
            if (marks <= 0) { sendActionBar(p, "§7Muc tieu chua co pattern."); return; }
            detonate(target, p, marks, true);
            addCd(vp_trigger, p);
            return;
        }
        if (slot == 3 && p.isSneaking()) {
            if (onCd(vp_inject, p, this)) return;
            LivingEntity target = getInSight(p, 10, 0.85);
            if (target == null) { sendActionBar(p, "§7Khong co muc tieu trong tam nhin."); return; }
            venomInjection(p, target);
            addCd(vp_inject, p);
        }
    }

    private void onMove(MoveExecute ex) {
        Player p = ex.getPlayer();
        if (!isEnabled()) { wasOnGround = p.isOnGround(); return; }

        boolean onGroundNow = p.isOnGround();
        double dy = ex.getTo().getY() - ex.getFrom().getY();

        if (wasOnGround && !onGroundNow && dy > 0.05 && !p.isFlying() && !p.isGliding()) {
            int slot = getPlayerData(p).getBinds().get(players.get(p).getActiveSlot());
            if (slot == 4 && !isOnCooldown(vp_antidote, p)) {
                antidoteBurst(p);
                addCd(vp_antidote, p);
            }
        }
        wasOnGround = onGroundNow;
    }

    private void paintMark(LivingEntity target, Player p, int amount, boolean allowCascade) {
        if (target == null || target.isDead() || target.equals(p)) return;
        int next = Math.min(MAX_MARKS, getMarks(target) + amount);
        setMarks(target, next);
        if (next >= MAX_MARKS) {
            detonate(target, p, next, allowCascade);
        }
    }

    private void detonate(LivingEntity target, Player p, int marksIn, boolean allowCascade) {
        if (target == null || target.isDead()) return;
        int marks = Math.max(1, Math.min(MAX_MARKS, marksIn));

        double dmg   = 3 + marks * 3.5;
        int    amp   = Math.min(marks - 1, 3);
        int    ticks = 30 + marks * 25;

        dealInternal(target, dmg, p);
        applyPotion(target, PotionEffectType.POISON, ticks, amp);
        applyPotion(target, PotionEffectType.SLOWNESS, ticks / 2, Math.min(marks - 1, 2));
        spawnDetonationBurst(target.getLocation().clone().add(0, 1, 0), marks);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
        clearMarks(target);

        if (allowCascade) {
            Location origin = target.getLocation();
            for (Entity e : origin.getWorld().getNearbyEntities(origin, CASCADE_RADIUS, CASCADE_RADIUS, CASCADE_RADIUS)) {
                if (e.equals(target) || e.equals(p) || !(e instanceof LivingEntity) || e instanceof ArmorStand) continue;
                paintMark((LivingEntity) e, p, 1, false);
            }
        }
    }

    private void dealInternal(LivingEntity target, double amount, Player source) {
        internalDamage = true;
        try {
            target.damage(amount, source);
        } finally {
            internalDamage = false;
        }
    }

    private void toxicCanvas(Player p) {
        Location ground = getGroundBelow(p.getLocation());
        p.getWorld().playSound(ground, Sound.BLOCK_SLIME_BLOCK_HIT, 0.6f, 0.5f);
        particleCircle(ground.clone().add(0, 0.1, 0), CANVAS_RADIUS, C_CANVAS, 0.8f, 18, 0);
        Set<UUID> tagged = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= CANVAS_LIFE_TICKS || !p.isOnline()) { cancel(); return; }
                if (t % 4 == 0) {
                    particleCircle(ground.clone().add(0, 0.1, 0), CANVAS_RADIUS, C_CANVAS, 0.7f, 14, t * 6);
                }
                for (Entity e : ground.getWorld().getNearbyEntities(ground, CANVAS_RADIUS, 1.3, CANVAS_RADIUS)) {
                    if (e.equals(p) || !(e instanceof LivingEntity) || e instanceof ArmorStand) continue;
                    if (!tagged.add(e.getUniqueId())) continue;
                    paintMark((LivingEntity) e, p, 1, true);
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0, 1);
    }

    private void venomInjection(Player p, LivingEntity target) {
        dealInternal(target, 4, p);
        applyPotion(target, PotionEffectType.POISON, 100, 2);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.8f, 0.5f);
        particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0), C_INJECT, 1.1f, 20, 0.3, 0.4, 0.3);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 6 || target.isDead() || !target.isValid()) { cancel(); return; }
                dealInternal(target, 2, p);
                particleApi.spawnColoredParticles(target.getLocation().clone().add(0, 1, 0),
                        C_INJECT, 0.9f, 6, 0.2, 0.25, 0.2);
                tick++;
            }
        }.runTaskTimer(magicPlugin, 10L, 10L);
    }

    private void antidoteBurst(Player p) {
        Location origin = p.getLocation();
        double healed = 0;
        int purged = 0;

        for (Entity e : origin.getWorld().getNearbyEntities(origin, ANTIDOTE_RADIUS, ANTIDOTE_RADIUS, ANTIDOTE_RADIUS)) {
            if (e.equals(p) || !(e instanceof LivingEntity) || e instanceof ArmorStand) continue;
            LivingEntity le = (LivingEntity) e;
            int marks = getMarks(le);
            if (marks <= 0) continue;
            healed += marks * 2.0;
            purged++;
            clearMarks(le);
            particleApi.spawnColoredParticles(le.getLocation().clone().add(0, 1.5, 0), C_ANTIDOTE, 1.0f, 16, 0.25, 0.25, 0.25);
        }

        particleCircle(origin.clone().add(0, 0.1, 0), 3.0, C_ANTIDOTE, 0.9f, 20, 0);
        p.getWorld().playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);

        if (purged > 0) {
            safeHeal(p, healed);
            p.getWorld().playSound(origin, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.7f);
            sendActionBar(p, "§bAntidote: purged " + purged + ", +" + (int) healed + " HP");
        } else {
            sendActionBar(p, "§7Antidote Burst: khong co pattern nao gan de purge.");
        }
    }

    private void masterpiece(Player p) {
        Location origin = p.getLocation();
        p.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
        p.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.5f);
        sendActionBar(p, "§2§l☠ MASTERPIECE!");

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : origin.getWorld().getNearbyEntities(origin, ULT_RADIUS, ULT_RADIUS, ULT_RADIUS)) {
            if (e.equals(p) || !(e instanceof LivingEntity) || e instanceof ArmorStand) continue;
            if (getMarks((LivingEntity) e) > 0) targets.add((LivingEntity) e);
        }
        for (LivingEntity t : targets) {
            int m = getMarks(t);
            if (m > 0) detonate(t, p, m, true);
        }
        startAura(p);
    }

    private void startAura(Player p) {
        if (auraTask != null) { auraTask.cancel(); auraTask = null; }
        auraTask = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline() || t >= AURA_LIFE_TICKS) { cancel(); auraTask = null; return; }
                if (t % 4 == 0) {
                    particleCircle(p.getLocation().clone().add(0, 0.15, 0), 2.3, C_AURA, 0.75f, 14, t * 5);
                }
                if (t % 30 == 0 && t > 0) {
                    for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), AURA_RADIUS, AURA_RADIUS, AURA_RADIUS)) {
                        if (e.equals(p) || !(e instanceof LivingEntity) || e instanceof ArmorStand) continue;
                        paintMark((LivingEntity) e, p, 1, true);
                    }
                }
                t++;
            }
        };
        auraTask.runTaskTimer(magicPlugin, 0, 1);
    }

    private static int getMarks(LivingEntity e) {
        return e.getPersistentDataContainer().getOrDefault(MARK_KEY, PersistentDataType.INTEGER, 0);
    }

    private static void setMarks(LivingEntity e, int value) {
        if (value <= 0) {
            e.getPersistentDataContainer().remove(MARK_KEY);
            activeMarks.remove(e.getUniqueId());
        } else {
            e.getPersistentDataContainer().set(MARK_KEY, PersistentDataType.INTEGER, value);
            activeMarks.put(e.getUniqueId(), value);
        }
        redrawMark(e, value);
    }

    private static void clearMarks(LivingEntity e) {
        setMarks(e, 0);
    }

    private static void redrawMark(LivingEntity e, int marks) {
        if (marks <= 0 || e.isDead()) return;

        Location anchor = e.getEyeLocation().clone().add(0, 0.45, 0);
        Vector facing = e.getLocation().getDirection().clone().setY(0);
        if (facing.lengthSquared() < 0.0001) facing = new Vector(0, 0, 1);
        else facing.normalize();
        Vector right = rotateY(facing, 90);

        Color col = marks >= 3 ? C_WARN : C_MARK;

        Location vertex   = anchor.clone();
        Location leftTop  = anchor.clone().add(right.clone().multiply(-0.4)).add(0, 0.55, 0);
        Location rightTop = anchor.clone().add(right.clone().multiply(0.4)).add(0, 0.55, 0);
        Location topLine  = anchor.clone().add(0, 0.65, 0);

        particleApi.drawColoredLine(leftTop, vertex, 0.45, col, 0.7f, 0);
        if (marks >= 2) particleApi.drawColoredLine(rightTop, vertex, 0.45, col, 0.7f, 0);
        if (marks >= 3) particleApi.drawColoredLine(vertex, topLine, 0.45, col, 0.7f, 0);
    }

    private static void spawnDetonationBurst(Location loc, int marks) {
        int count = 15 + marks * 10;
        particleApi.spawnColoredParticles(loc, C_BURST, 1.2f, count, 0.35, 0.35, 0.35);
        particleApi.spawnColoredParticles(loc, C_MARK,  1.0f, count / 2, 0.4, 0.4, 0.4);
        if (marks >= MAX_MARKS) {
            particleApi.spawnColoredParticles(loc, C_WARN, 1.3f, 10, 0.5, 0.5, 0.5);
        }
    }

    private static synchronized void ensureTicker() {
        if (tickerStarted) return;
        tickerStarted = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeMarks.isEmpty()) return;
                Iterator<Map.Entry<UUID, Integer>> it = activeMarks.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Integer> entry = it.next();
                    Entity raw = Bukkit.getEntity(entry.getKey());
                    if (raw == null || raw.isDead() || !raw.isValid() || !(raw instanceof LivingEntity)) {
                        it.remove();
                        continue;
                    }
                    redrawMark((LivingEntity) raw, entry.getValue());
                }
            }
        }.runTaskTimer(magicPlugin, 20L, 3L);
    }

    @Override
    public BukkitRunnable executeIdle(IdleExecute ex) {
        Player p = ex.getPlayer();
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (isAuraEnabled(p)) {
                    particleApi.spawnColoredParticles(p.getLocation().clone().add(0, 1.1, 0),
                            C_MARK, 0.5f, 1, 0.2, 0.05, 0.2);
                }
            }
        };
        r.runTaskTimer(magicPlugin, 0, 30);
        return r;
    }

    @Override
    public void remove() {
        if (auraTask != null) { auraTask.cancel(); auraTask = null; }
    }

    @Override
    public String getAbilityName(int ability) {
        switch (ability) {
            case 0: return "§2Poison Strike";
            case 1: return "§2Pattern Trigger";
            case 2: return "§2Toxic Canvas";
            case 3: return "§2Venom Injection";
            case 4: return "§2Antidote Burst";
            case 5: return "§2§l☠ MASTERPIECE §a[ULT]";
            default: return "§7none";
        }
    }
}
