package net.trduc.magicabilitiesfork.intrinsics;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.EnumSet;
import java.util.Set;

public final class DamageCategory {

    private static final Set<DamageCause> PHYSICAL_CAUSES = EnumSet.of(
            DamageCause.ENTITY_ATTACK,
            DamageCause.ENTITY_SWEEP_ATTACK,
            DamageCause.PROJECTILE,
            DamageCause.THORNS,
            DamageCause.FALLING_BLOCK,
            DamageCause.FALL,
            DamageCause.BLOCK_EXPLOSION,
            DamageCause.ENTITY_EXPLOSION,
            DamageCause.CONTACT,
            DamageCause.CRAMMING,
            DamageCause.FLY_INTO_WALL
    );

    private DamageCategory() {
    }

    public static boolean isPhysical(DamageCause cause) {
        return cause != null && PHYSICAL_CAUSES.contains(cause);
    }
}
