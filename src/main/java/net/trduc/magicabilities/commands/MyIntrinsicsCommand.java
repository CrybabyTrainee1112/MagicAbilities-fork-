package net.trduc.magicabilitiesfork.commands;

import net.trduc.magicabilitiesfork.intrinsics.Intrinsic;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicManager;
import net.trduc.magicabilitiesfork.intrinsics.gui.IntrinsicGui;
import net.trduc.magicabilitiesfork.intrinsics.item.IntrinsicBook;
import net.trduc.magicabilitiesfork.intrinsics.player.PlayerIntrinsicStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MyIntrinsicsCommand implements CommandExecutor, TabCompleter {

    private final IntrinsicManager intrinsicManager;
    private final PlayerIntrinsicStorage storage;
    private final IntrinsicGui vaultGui;

    public MyIntrinsicsCommand(IntrinsicManager intrinsicManager, PlayerIntrinsicStorage storage, IntrinsicGui vaultGui) {
        this.intrinsicManager = intrinsicManager;
        this.storage = storage;
        this.vaultGui = vaultGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players have intrinsics to manage.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            vaultGui.open(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list":
                return handleList(player);
            case "activate":
                return handleActivate(player, args);
            case "deactivate":
                return handleDeactivate(player, args);
            default:
                player.sendMessage(ChatColor.YELLOW + "Usage: /myintrinsics [list|activate|deactivate <id>]");
                return true;
        }
    }

    private boolean handleList(Player player) {
        Set<IntrinsicId> learned = storage.getLearned(player.getUniqueId());
        if (learned.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "You haven't learned any Intrinsics yet. Defeat bosses for a chance at an Intrinsic Book.");
            return true;
        }
        Set<IntrinsicId> active = storage.getActive(player.getUniqueId());
        player.sendMessage(ChatColor.GOLD + "Your Intrinsics:");
        for (IntrinsicId id : learned) {
            String marker = active.contains(id) ? ChatColor.GREEN + "[active]" : ChatColor.DARK_GRAY + "[inactive]";
            player.sendMessage("  " + marker + ChatColor.RESET + " " + IntrinsicBook.displayName(id) + ChatColor.GRAY + " (" + id.name() + ")");
        }
        return true;
    }

    private boolean handleActivate(Player player, String[] args) {
        IntrinsicId id = parseId(player, args);
        if (id == null) return true;
        if (!storage.hasLearned(player.getUniqueId(), id)) {
            player.sendMessage(ChatColor.RED + "You haven't learned " + IntrinsicBook.displayName(id) + " yet.");
            return true;
        }
        Intrinsic result = intrinsicManager.equip(player, id);
        for (IntrinsicId lineId : IntrinsicId.values()) {
            if (lineId.line().equals(id.line())) {
                storage.setActive(player.getUniqueId(), lineId, result.getId() == lineId);
            }
        }
        if (result.getId() != id) {
            player.sendMessage(ChatColor.YELLOW + "You already have a higher tier of that line active: "
                    + IntrinsicBook.displayName(result.getId()) + ".");
        } else {
            player.sendMessage(ChatColor.GREEN + "Activated " + IntrinsicBook.displayName(id) + ".");
        }
        return true;
    }

    private boolean handleDeactivate(Player player, String[] args) {
        IntrinsicId id = parseId(player, args);
        if (id == null) return true;
        intrinsicManager.unequip(player, id);
        storage.setActive(player.getUniqueId(), id, false);
        player.sendMessage(ChatColor.YELLOW + "Deactivated " + IntrinsicBook.displayName(id) + ".");
        return true;
    }

    private IntrinsicId parseId(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /myintrinsics " + args[0] + " <id>");
            return null;
        }
        try {
            return IntrinsicId.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Unknown intrinsic id: " + args[1]);
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return new ArrayList<>();
        Player player = (Player) sender;
        if (args.length == 1) {
            return filter(Arrays.asList("activate", "deactivate"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("activate")) {
            return storage.getLearned(player.getUniqueId()).stream()
                    .map(Enum::name)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("deactivate")) {
            return storage.getActive(player.getUniqueId()).stream()
                    .map(Enum::name)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
