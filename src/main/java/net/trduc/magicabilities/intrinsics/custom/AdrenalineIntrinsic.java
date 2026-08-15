package net.trduc.magicabilitiesfork.intrinsics.custom;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilitiesfork.intrinsics.DamageCategory;
import net.trduc.magicabilitiesfork.intrinsics.Intrinsic;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class AdrenalineIntrinsic extends Intrinsic {

    private static final long COOLDOWN_SECONDS = 1000L;

    private final IntrinsicId id;
    private final int tier;
    private final double triggerMaxHealth;
    private final long durationTicks;
    private final long cooldownTicks;

    private long expiryTick = Long.MIN_VALUE;
    private long cooldownUntilTick = Long.MIN_VALUE;

    private AdrenalineIntrinsic(LivingEntity owner, IntrinsicId id, int tier, double triggerMaxHealth, long durationSeconds) {
        super(owner);
        this.id = id;
        this.tier = tier;
        this.triggerMaxHealth = triggerMaxHealth;
        this.durationTicks = durationSeconds * 20L;
        this.cooldownTicks = COOLDOWN_SECONDS * 20L;
    }

    public static AdrenalineIntrinsic tier1(LivingEntity owner) {
        return new AdrenalineIntrinsic(owner, IntrinsicId.ADRENALINE_1, 1, 6.0, 5);
    }

    public static AdrenalineIntrinsic tier2(LivingEntity owner) {
        return new AdrenalineIntrinsic(owner, IntrinsicId.ADRENALINE_2, 2, 6.0, 6);
    }

    public static AdrenalineIntrinsic tier3(LivingEntity owner) {
        return new AdrenalineIntrinsic(owner, IntrinsicId.ADRENALINE_3, 3, 6.0, 7);
    }

    public static AdrenalineIntrinsic tier4(LivingEntity owner) {
        return new AdrenalineIntrinsic(owner, IntrinsicId.ADRENALINE_4, 4, 4.0, 8);
    }

    public static AdrenalineIntrinsic tier5(LivingEntity owner) {
        return new AdrenalineIntrinsic(owner, IntrinsicId.ADRENALINE_5, 5, 4.0, 9);
    }

    @Override
    public IntrinsicId getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        String roman;
        switch (tier) {
            case 1: roman = "I"; break;
            case 2: roman = "II"; break;
            case 3: roman = "III"; break;
            case 4: roman = "IV"; break;
            default: roman = "V";
        }
        return "Adrenaline " + roman;
    }

    public boolean isImmuneNow() {
        return currentTick() <= expiryTick;
    }

    public boolean isOnCooldown() {
        return currentTick() < cooldownUntilTick;
    }

    @Override
    public void onDamaged(EntityDamageEvent event) {
        boolean physical = DamageCategory.isPhysical(event.getCause());

        if (isImmuneNow()) {
            if (physical) {
                event.setCancelled(true);
            }
            return;
        }

        if (isOnCooldown()) {
            return;
        }

        double before = getOwner().getHealth();
        double after = before - event.getFinalDamage();
        boolean entersCriticalBand = after > 0 && after <= triggerMaxHealth;
        if (!entersCriticalBand) {
            return;
        }

        long now = currentTick();
        expiryTick = now + durationTicks;
        cooldownUntilTick = now + cooldownTicks;
        notifyOwner();

        if (physical) {
            event.setCancelled(true);
        }
    }

    private long currentTick() {
        return getOwner().getWorld().getFullTime();
    }

    private void notifyOwner() {
        if (!(getOwner() instanceof Player)) {
            return;
        }
        Player p = (Player) getOwner();
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                ChatColor.AQUA + getDisplayName() + ChatColor.GRAY + ": miễn sát thương vật lí!"));
    }
}
