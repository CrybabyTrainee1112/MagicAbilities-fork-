package net.trduc.magicabilitiesfork.commands;

import net.trduc.magicabilitiesfork.intrinsics.Intrinsic;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicManager;
import net.trduc.magicabilitiesfork.intrinsics.player.PlayerIntrinsicStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IntrinsicCommand implements CommandExecutor, TabCompleter {

    private final IntrinsicManager intrinsicManager;
    private final PlayerIntrinsicStorage storage;

    public IntrinsicCommand(IntrinsicManager intrinsicManager, PlayerIntrinsicStorage storage) {
        this.intrinsicManager = intrinsicManager;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("intrinsic")) {
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /intrinsic <give|clear|list> <player> [intrinsic_id]");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "give":
                return handleGive(sender, args);
            case "clear":
                return handleClear(sender, args);
            case "list":
                return handleList(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + sub);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /intrinsic give <player> <intrinsic_id>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        IntrinsicId id;
        try {
            id = IntrinsicId.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown intrinsic id: " + args[2]);
            return true;
        }
        storage.learn(target.getUniqueId(), id);
        Intrinsic intrinsic = intrinsicManager.equip(target, id);
        for (IntrinsicId lineId : IntrinsicId.values()) {
            if (lineId.line().equals(id.line())) {
                storage.setActive(target.getUniqueId(), lineId, intrinsic.getId() == lineId);
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Gave " + intrinsic.getDisplayName() + " to " + target.getName());
        target.sendMessage(ChatColor.GOLD + "Nội tại " + intrinsic.getDisplayName() + " đã được kích hoạt!");
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /intrinsic clear <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        for (Intrinsic intrinsic : intrinsicManager.getActive(target)) {
            storage.setActive(target.getUniqueId(), intrinsic.getId(), false);
        }
        intrinsicManager.unequipAll(target);
        sender.sendMessage(ChatColor.GREEN + "Cleared all intrinsics from " + target.getName());
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /intrinsic list <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        List<Intrinsic> active = intrinsicManager.getActive(target);
        if (active.isEmpty()) {
            sender.sendMessage(target.getName() + " has no active intrinsics.");
            return true;
        }
        sender.sendMessage(target.getName() + "'s intrinsics: " +
                active.stream().map(Intrinsic::getDisplayName).collect(Collectors.joining(", ")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("give", "clear", "list"), args[0]);
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(Arrays.stream(IntrinsicId.values()).map(Enum::name).collect(Collectors.toList()), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
