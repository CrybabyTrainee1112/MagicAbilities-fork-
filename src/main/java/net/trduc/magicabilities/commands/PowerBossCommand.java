package net.trduc.magicabilitiesfork.commands;

import net.trduc.magicabilitiesfork.Boss.core.Boss;
import net.trduc.magicabilitiesfork.Boss.core.BossFactory;
import net.trduc.magicabilitiesfork.Boss.core.BossManager;
import net.trduc.magicabilitiesfork.Boss.core.BossRegistry;
import net.trduc.magicabilitiesfork.Boss.core.BossType;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMastery;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMasteryStore;
import net.trduc.magicabilitiesfork.MagicAbilitiesfork;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PowerBossCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("summon", "list", "kill", "info", "mastery");

    private BossManager bossManager() {
        return MagicAbilitiesfork.magicPlugin.getBossManager();
    }

    private BossFactory bossFactory() {
        return MagicAbilitiesfork.magicPlugin.getBossFactory();
    }

    private BossMasteryStore masteryStore() {
        return MagicAbilitiesfork.magicPlugin.getBossMasteryStore();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("powerboss")) return false;

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "summon":
                handleSummon(sender, args);
                return true;
            case "list":
                handleList(sender);
                return true;
            case "kill":
                handleKill(sender, args);
                return true;
            case "info":
                handleInfo(sender);
                return true;
            case "mastery":
                handleMastery(sender, args);
                return true;
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void handleSummon(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerboss summon <bossType> [x y z]");
            sendKnownTypesHint(sender);
            return;
        }

        String bossTypeId = args[1].toLowerCase();
        if (!BossRegistry.isRegistered(bossTypeId)) {
            sender.sendMessage(ChatColor.RED + "Unknown boss type: " + bossTypeId);
            sendKnownTypesHint(sender);
            return;
        }

        Location location = resolveLocation(sender, args);
        if (location == null) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerboss summon <bossType> [x y z] (position required from console)");
            return;
        }

        try {
            Boss boss = bossFactory().spawn(bossTypeId, location);
            sender.sendMessage(ChatColor.GREEN + "Summoned " + boss.getBossType() + " at "
                    + fmt(location.getX()) + ", " + fmt(location.getY()) + ", " + fmt(location.getZ()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Failed to summon boss: " + e.getMessage());
        }
    }

    private void handleList(CommandSender sender) {
        Collection<BossType> types = BossRegistry.getAll();
        if (types.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No boss types are registered.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Registered boss types (" + types.size() + "):");
        types.stream()
                .sorted(Comparator.comparing(BossType::getId))
                .forEach(type -> sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + type.getId()
                        + ChatColor.GRAY + " (" + type.getDisplayName() + ChatColor.GRAY + ", "
                        + type.getMaxHealth() + " HP)"));
    }

    private void handleKill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerboss kill <bossType|all>");
            return;
        }

        String target = args[1].toLowerCase();
        Collection<Boss> toKill = target.equals("all")
                ? new ArrayList<>(bossManager().getAllBosses())
                : new ArrayList<>(bossManager().getBossesByType(target));

        if (toKill.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No active bosses match: " + target);
            return;
        }

        for (Boss boss : toKill) {
            boss.die();
            bossManager().unregisterBoss(boss);
            boss.getEntity().remove();
        }
        sender.sendMessage(ChatColor.GREEN + "Removed " + toKill.size() + " boss(es).");
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Boss Debug Info ===");
        for (String line : bossManager().getDebugInfo().split("\n")) {
            if (!line.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + line);
            }
        }
    }

    private Location resolveLocation(CommandSender sender, String[] args) {
        if (args.length >= 5) {
            try {
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);
                Location base = (sender instanceof Player)
                        ? ((Player) sender).getLocation()
                        : sender.getServer().getWorlds().get(0).getSpawnLocation();
                return new Location(base.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (sender instanceof Player) {
            return ((Player) sender).getLocation();
        }
        return null;
    }

    private void sendKnownTypesHint(CommandSender sender) {
        String ids = BossRegistry.getAllIds().stream().sorted().collect(Collectors.joining(", "));
        if (!ids.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Known types: " + ids);
        }
    }

    private void handleMastery(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerboss mastery <bossType>");
            sendKnownTypesHint(sender);
            return;
        }

        String bossTypeId = args[1].toLowerCase();
        if (!BossRegistry.isRegistered(bossTypeId)) {
            sender.sendMessage(ChatColor.RED + "Unknown boss type: " + bossTypeId);
            sendKnownTypesHint(sender);
            return;
        }

        BossMastery mastery = masteryStore().load(bossTypeId);
        double hpMultPerTier = MagicAbilitiesfork.magicPlugin.getConfig().getDouble("boss-mastery.hp-mult-per-tier", 0.15);
        double dmgMultPerTier = MagicAbilitiesfork.magicPlugin.getConfig().getDouble("boss-mastery.dmg-mult-per-tier", 0.10);
        sender.sendMessage(ChatColor.GOLD + "Mastery for " + bossTypeId + ":");
        sender.sendMessage(ChatColor.GRAY + " Tier: " + ChatColor.WHITE + mastery.getTier());
        sender.sendMessage(ChatColor.GRAY + " Wins (times killed by players): " + ChatColor.WHITE + mastery.getWins());
        sender.sendMessage(ChatColor.GRAY + " Next spawn HP x" + fmt(1.0 + mastery.getTier() * hpMultPerTier)
                + ", DMG x" + fmt(1.0 + mastery.getTier() * dmgMultPerTier));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/powerboss summon <bossType> [x y z]");
        sender.sendMessage(ChatColor.GOLD + "/powerboss list");
        sender.sendMessage(ChatColor.GOLD + "/powerboss kill <bossType|all>");
        sender.sendMessage(ChatColor.GOLD + "/powerboss info");
        sender.sendMessage(ChatColor.GOLD + "/powerboss mastery <bossType>");
    }

    private String fmt(double v) {
        return String.format("%.1f", v);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("powerboss")) return List.of();

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("summon") || args[0].equalsIgnoreCase("kill")
                || args[0].equalsIgnoreCase("mastery"))) {
            List<String> options = new ArrayList<>(BossRegistry.getAllIds());
            if (args[0].equalsIgnoreCase("kill")) {
                options.add("all");
            }
            return options.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
