package net.trduc.magicabilitiesfork.intrinsics;

import net.trduc.magicabilitiesfork.Boss.event.BossDeathEvent;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMasteryStore;
import net.trduc.magicabilitiesfork.intrinsics.gui.IntrinsicGui;
import net.trduc.magicabilitiesfork.intrinsics.item.IntrinsicBook;
import net.trduc.magicabilitiesfork.intrinsics.player.PlayerIntrinsicStorage;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class IntrinsicDropListener implements Listener {

    private static final double BASE_CHANCE = 0.15;
    private static final double CHANCE_PER_MASTERY_TIER = 0.05;
    private static final double MAX_CHANCE = 0.50;

    private final Plugin plugin;
    private final PlayerIntrinsicStorage storage;
    private final BossMasteryStore masteryStore;
    private final IntrinsicGui vaultGui;
    private final Random random = new Random();

    public IntrinsicDropListener(Plugin plugin, PlayerIntrinsicStorage storage, BossMasteryStore masteryStore, IntrinsicGui vaultGui) {
        this.plugin = plugin;
        this.storage = storage;
        this.masteryStore = masteryStore;
        this.vaultGui = vaultGui;
    }

    @EventHandler
    public void onBossDeath(BossDeathEvent event) {
        LivingEntity killer = event.getKiller();
        if (!(killer instanceof Player)) return;
        Player player = (Player) killer;

        int tier = masteryStore.load(event.getBoss().getBossType()).getTier();
        double chance = Math.min(MAX_CHANCE, BASE_CHANCE + CHANCE_PER_MASTERY_TIER * tier);
        if (random.nextDouble() >= chance) return;

        List<IntrinsicId> candidates = nextAvailablePerLine(player.getUniqueId());
        if (candidates.isEmpty()) return;

        IntrinsicId awarded = candidates.get(random.nextInt(candidates.size()));
        ItemStack book = IntrinsicBook.create(plugin, awarded);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(book);
        for (ItemStack item : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "An Intrinsic Book (" + IntrinsicBook.displayName(awarded)
                + ") dropped from " + event.getBoss().getBossType() + "!");
    }

    @EventHandler
    public void onUseBook(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        IntrinsicId id = IntrinsicBook.getIntrinsicId(plugin, item);
        if (id == null) return;

        event.setCancelled(true);
        vaultGui.open(event.getPlayer());
    }

    private List<IntrinsicId> nextAvailablePerLine(UUID playerId) {
        Map<String, List<IntrinsicId>> byLine = new LinkedHashMap<>();
        for (IntrinsicId id : IntrinsicId.values()) {
            byLine.computeIfAbsent(id.line(), k -> new ArrayList<>()).add(id);
        }
        List<IntrinsicId> candidates = new ArrayList<>();
        for (List<IntrinsicId> line : byLine.values()) {
            line.sort(Comparator.comparingInt(IntrinsicId::tier));
            for (IntrinsicId candidate : line) {
                if (!storage.hasLearned(playerId, candidate)) {
                    candidates.add(candidate);
                    break;
                }
            }
        }
        return candidates;
    }

}
