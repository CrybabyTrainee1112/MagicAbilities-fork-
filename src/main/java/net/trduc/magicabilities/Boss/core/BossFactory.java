package net.trduc.magicabilitiesfork.Boss.core;

import net.trduc.magicabilitiesfork.Boss.ai.executor.Brain;
import net.trduc.magicabilitiesfork.Boss.event.BossSpawnEvent;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMastery;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMasteryStore;
import net.trduc.magicabilitiesfork.Boss.threat.ThreatTable;
import net.trduc.magicabilitiesfork.Boss.ai.skill.SkillExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;

import java.util.Objects;

public class BossFactory {
    private final BossManager bossManager;
    private final BossMasteryStore masteryStore;
    private final FileConfiguration config;

    public BossFactory(BossManager bossManager, BossMasteryStore masteryStore, FileConfiguration config) {
        this.bossManager = Objects.requireNonNull(bossManager, "BossManager cannot be null");
        this.masteryStore = Objects.requireNonNull(masteryStore, "BossMasteryStore cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");
    }

    public Boss spawn(String bossTypeId, Location location) {
        Objects.requireNonNull(bossTypeId, "Boss type ID cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");

        BossType bossType = BossRegistry.get(bossTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown boss type: " + bossTypeId));

        Entity spawnedEntity = location.getWorld().spawnEntity(location, bossType.getEntityType());
        if (!(spawnedEntity instanceof Mob)) {
            spawnedEntity.remove();
            throw new IllegalArgumentException("Boss type entity is not a Mob: " + bossType.getEntityType());
        }

        Mob mob = (Mob) spawnedEntity;

        BossMastery mastery = masteryStore.load(bossTypeId);
        configureMob(mob, bossType, mastery);

        SkillExecutor skillExecutor = new SkillExecutor(mob);
        ThreatTable threatTable = new ThreatTable();
        Brain brain = new Brain(
                mob,
                skillExecutor,
                bossType.getActionGraph(),
                bossType.getSensors(),
                bossType.getPhases(),
                bossType.getGoals(),
                bossType.getStrategies()
        );

        Boss boss = new Boss(mob, bossTypeId, brain, skillExecutor, threatTable, bossType.getPhases());

        double dmgMultPerTier = config.getDouble("boss-mastery.dmg-mult-per-tier", 0.10);
        double damageMultiplier = 1.0 + mastery.getTier() * dmgMultPerTier;
        mob.getPersistentDataContainer().set(DamageAPI.MASTERY_DAMAGE_KEY, PersistentDataType.DOUBLE, damageMultiplier);
        boss.setDamageMultiplier(damageMultiplier);

        BossBar bossBar = Bukkit.createBossBar(bossType.getDisplayName(), BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.setProgress(1.0);
        boss.setBossBar(bossBar);

        bossManager.registerBoss(boss);

        Bukkit.getPluginManager().callEvent(new BossSpawnEvent(boss));

        return boss;
    }

    private void configureMob(Mob mob, BossType bossType, BossMastery mastery) {
        mob.setCustomName(bossType.getDisplayName());
        mob.setCustomNameVisible(true);

        AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double hpMultPerTier = config.getDouble("boss-mastery.hp-mult-per-tier", 0.15);
            double scaledHealth = bossType.getMaxHealth() * (1.0 + mastery.getTier() * hpMultPerTier);
            maxHealthAttr.setBaseValue(scaledHealth);
            mob.setHealth(scaledHealth);
        }

        AttributeInstance knockbackAttr = mob.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(1.0);
        }

        AttributeInstance scaleAttr = mob.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(bossType.getScale());
        }

        mob.setRemoveWhenFarAway(false);

        equipBoss(mob, bossType.getId());
    }

    private void equipBoss(Mob mob, String bossTypeId) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }

        if ("demon_lord".equals(bossTypeId)) {
            equipment.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            equipment.setItemInOffHand(new ItemStack(Material.SHIELD));

            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
            equipment.setItemInMainHandDropChance(0.0f);
            equipment.setItemInOffHandDropChance(0.0f);
        }
    }

    public static void registerBossType(BossType bossType) {
        Objects.requireNonNull(bossType, "BossType cannot be null");
        BossRegistry.register(bossType);
    }
}
