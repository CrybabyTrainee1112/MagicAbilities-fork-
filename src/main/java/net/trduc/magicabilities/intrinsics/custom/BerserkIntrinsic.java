package net.trduc.magicabilitiesfork.intrinsics.custom;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trduc.magicabilitiesfork.intrinsics.Intrinsic;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BerserkIntrinsic extends Intrinsic {

    private final IntrinsicId id;
    private final int tier;
    private final double bonusPerStack;
    private final long durationTicks;

    private int stacks = 0;
    private long expiryTick = Long.MIN_VALUE;

    private BerserkIntrinsic(LivingEntity owner, IntrinsicId id, int tier, double bonusPerStack, long durationSeconds) {
        super(owner);
        this.id = id;
        this.tier = tier;
        this.bonusPerStack = bonusPerStack;
        this.durationTicks = durationSeconds * 20L;
    }

    public static BerserkIntrinsic tier1(LivingEntity owner) {
        return new BerserkIntrinsic(owner, IntrinsicId.BERSERK_1, 1, 1.0, 10);
    }

    public static BerserkIntrinsic tier2(LivingEntity owner) {
        return new BerserkIntrinsic(owner, IntrinsicId.BERSERK_2, 2, 2.0, 11);
    }

    public static BerserkIntrinsic tier3(LivingEntity owner) {
        return new BerserkIntrinsic(owner, IntrinsicId.BERSERK_3, 3, 3.0, 12);
    }

    @Override
    public IntrinsicId getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        String roman = tier == 1 ? "I" : tier == 2 ? "II" : "III";
        return "Cuồng sát " + roman;
    }

    public int getStacks() {
        pruneIfExpired();
        return stacks;
    }

    @Override
    public void onKill(LivingEntity victim) {
        if (!isBelowHalfHealth(getOwner())) {
            return;
        }
        pruneIfExpired();
        stacks++;
        expiryTick = currentTick() + durationTicks;
        notifyOwner();
    }

    @Override
    public double onDamageDealt(LivingEntity victim, double baseDamage) {
        pruneIfExpired();
        if (stacks <= 0) {
            return baseDamage;
        }
        return baseDamage + stacks * bonusPerStack;
    }

    @Override
    public void onTick() {
        pruneIfExpired();
    }

    private void pruneIfExpired() {
        if (stacks > 0 && currentTick() > expiryTick) {
            stacks = 0;
        }
    }

    private boolean isBelowHalfHealth(LivingEntity entity) {
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealthAttr != null ? maxHealthAttr.getValue() : entity.getMaxHealth();
        return entity.getHealth() < max * 0.5;
    }

    private long currentTick() {
        return getOwner().getWorld().getFullTime();
    }

    private void notifyOwner() {
        if (!(getOwner() instanceof Player)) {
            return;
        }
        Player p = (Player) getOwner();
        int bonus = (int) Math.round(stacks * bonusPerStack);
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                ChatColor.RED + getDisplayName() + ChatColor.GRAY + " x" + stacks +
                        ChatColor.RED + " (+" + bonus + " sát thương hiệu quả)"));
    }
}
