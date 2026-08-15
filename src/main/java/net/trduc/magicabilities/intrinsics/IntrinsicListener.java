package net.trduc.magicabilitiesfork.intrinsics;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IntrinsicListener implements Listener {

    private final IntrinsicManager manager;
    private final Map<UUID, UUID> lastDamagerId = new HashMap<>();

    public void pruneStaleDamagers() {
        lastDamagerId.keySet().removeIf(victimId -> Bukkit.getEntity(victimId) == null);
    }

    public IntrinsicListener(IntrinsicManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity)) {
            return;
        }
        LivingEntity damager = (LivingEntity) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();

        lastDamagerId.put(victim.getUniqueId(), damager.getUniqueId());

        if (!manager.hasAny(damager)) {
            return;
        }
        double damage = event.getDamage();
        for (Intrinsic intrinsic : manager.getActive(damager)) {
            damage = intrinsic.onDamageDealt(victim, damage);
        }
        event.setDamage(damage);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        LivingEntity victim = (LivingEntity) event.getEntity();
        if (!manager.hasAny(victim)) {
            return;
        }
        for (Intrinsic intrinsic : manager.getActive(victim)) {
            intrinsic.onDamaged(event);
            if (event.isCancelled()) {
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getAllowFlight()) {
            return;
        }
        if (!manager.hasAny(player)) {
            return;
        }
        for (Intrinsic intrinsic : manager.getActive(player)) {
            if (intrinsic.onAirJump(player)) {
                event.setCancelled(true);
                player.setFlying(false);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();

        if (manager.hasAny(victim)) {
            for (Intrinsic intrinsic : manager.getActive(victim)) {
                intrinsic.onOwnerDeath(event);
            }
        }

        UUID damagerId = lastDamagerId.remove(victim.getUniqueId());
        if (damagerId == null) {
            return;
        }
        Entity damagerEntity = Bukkit.getEntity(damagerId);
        if (!(damagerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity killer = (LivingEntity) damagerEntity;
        if (!manager.hasAny(killer)) {
            return;
        }
        for (Intrinsic intrinsic : manager.getActive(killer)) {
            intrinsic.onKill(victim);
        }
    }
}
