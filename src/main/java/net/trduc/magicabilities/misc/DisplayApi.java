package net.trduc.magicabilitiesfork.misc;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class DisplayApi {

    private final JavaPlugin plugin;

    public DisplayApi(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemDisplay spawnBladePlane(Location origin, Vector direction, float length, float width,
                                        Material material, int lifeTicks) {
        Location loc = origin.clone();
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
        display.setItemStack(new ItemStack(material));
        display.setBillboard(Display.Billboard.FIXED);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setPersistent(false);

        Vector dir = direction.clone().normalize();
        Quaternionf rot = new Quaternionf().rotationTo(
                new Vector3f(0, 0, 1),
                new Vector3f((float) dir.getX(), (float) dir.getY(), (float) dir.getZ()));

        final Vector3f baseScale = new Vector3f(width, width, length);
        Transformation base = new Transformation(new Vector3f(0, 0, 0), rot, baseScale, new Quaternionf());
        display.setInterpolationDuration(2);
        display.setInterpolationDelay(0);
        display.setTransformation(base);

        BukkitRunnable r = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                if (!display.isValid() || tick >= lifeTicks) {
                    display.remove();
                    cancel();
                    return;
                }
                float progress = (float) tick / (float) lifeTicks;
                float shrink = Math.max(0f, 1f - progress);
                Transformation t = new Transformation(
                        new Vector3f(0, 0, 0), rot,
                        new Vector3f(baseScale.x * shrink, baseScale.y * shrink, baseScale.z),
                        new Quaternionf());
                display.setInterpolationDuration(2);
                display.setInterpolationDelay(0);
                display.setTransformation(t);
            }
        };
        r.runTaskTimer(plugin, 1, 1);
        return display;
    }

    public void spawnGhostAfterimage(Player p, org.bukkit.ChatColor glowColor, int lifeTicks) {
        Location loc = p.getLocation();
        ArmorStand ghost = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        ghost.setBasePlate(false);
        ghost.setArms(true);
        ghost.setMarker(false);
        ghost.setGravity(false);
        ghost.setCollidable(false);
        ghost.setInvulnerable(true);
        ghost.setSilent(true);
        ghost.setSmall(false);
        ghost.setPersistent(false);
        ghost.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            skull.setItemMeta(meta);
        }

        ghost.getEquipment().setHelmet(skull);
        ghost.getEquipment().setChestplate(p.getInventory().getChestplate());
        ghost.getEquipment().setLeggings(p.getInventory().getLeggings());
        ghost.getEquipment().setBoots(p.getInventory().getBoots());
        ghost.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand());

        ghost.setGlowing(true);

        String teamName = "ghost_" + UUID.randomUUID().toString().substring(0, 8);
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        team.setColor(glowColor);
        team.addEntry(ghost.getUniqueId().toString());

        org.bukkit.attribute.AttributeInstance scaleAttr = ghost.getAttribute(org.bukkit.attribute.Attribute.SCALE);

        final Team finalTeam = team;
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                if (!ghost.isValid() || tick >= lifeTicks) {
                    if (ghost.isValid()) ghost.remove();
                    finalTeam.unregister();
                    cancel();
                    return;
                }
                if (scaleAttr != null) {
                    double progress = (double) tick / (double) lifeTicks;
                    double shrink = Math.max(0.02, 1.0 - progress);
                    scaleAttr.setBaseValue(shrink);
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }
}
