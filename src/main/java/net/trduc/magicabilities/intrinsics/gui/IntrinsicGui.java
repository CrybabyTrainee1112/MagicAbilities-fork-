package net.trduc.magicabilitiesfork.intrinsics.gui;

import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import net.trduc.magicabilitiesfork.intrinsics.IntrinsicManager;
import net.trduc.magicabilitiesfork.intrinsics.item.IntrinsicBook;
import net.trduc.magicabilitiesfork.intrinsics.player.PlayerIntrinsicStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class IntrinsicGui implements Listener {
    private static final int VAULT_SIZE = 9;
    private static final int VAULT_ROW = 1;
    private static final int VAULT_OFFSET = VAULT_ROW * 9;
    private static final int TOTAL_SIZE = 27;
    private static final int INFO_SLOT = 4;
    private static final int CLOSE_SLOT = 18 + 4;

    private final Plugin plugin;
    private final IntrinsicManager intrinsicManager;
    private final PlayerIntrinsicStorage storage;
    private final Map<UUID, Inventory> openVaults = new HashMap<>();

    public IntrinsicGui(Plugin plugin, IntrinsicManager intrinsicManager, PlayerIntrinsicStorage storage) {
        this.plugin = plugin;
        this.intrinsicManager = intrinsicManager;
        this.storage = storage;
    }

    public void open(Player player) {
        List<IntrinsicId> active = new ArrayList<>(storage.getActive(player.getUniqueId()));
        Inventory inv = Bukkit.createInventory(null, TOTAL_SIZE,
                ChatColor.DARK_PURPLE + "Intrinsic Vault" + ChatColor.GRAY + " (" + active.size() + "/" + VAULT_SIZE + ")");

        decorateFrame(inv, active.size());

        for (int i = 0; i < active.size() && i < VAULT_SIZE; i++) {
            inv.setItem(VAULT_OFFSET + i, IntrinsicBook.create(plugin, active.get(i)));
        }
        openVaults.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private void decorateFrame(Inventory inv, int activeCount) {
        for (int c = 0; c < 9; c++) {
            boolean corner = (c == 0 || c == 8);
            Material mat = corner ? Material.BLACK_STAINED_GLASS_PANE : Material.PURPLE_STAINED_GLASS_PANE;
            inv.setItem(c, filler(mat, " "));
            inv.setItem(18 + c, filler(mat, " "));
        }

        ItemStack info = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Intrinsic Vault");
        infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Drag a learned Intrinsic Book",
                ChatColor.GRAY + "into a slot below to activate it.",
                ChatColor.GRAY + "Drag it back out to deactivate.",
                " ",
                ChatColor.GRAY + "Active: " + ChatColor.WHITE + activeCount + ChatColor.GRAY + "/" + VAULT_SIZE,
                ChatColor.DARK_GRAY + "Only the highest tier of each",
                ChatColor.DARK_GRAY + "ability line can be active."
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(INFO_SLOT, info);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Close");
        close.setItemMeta(closeMeta);
        inv.setItem(CLOSE_SLOT, close);
    }

    private ItemStack filler(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isVaultSlot(int rawSlot) {
        return rawSlot >= VAULT_OFFSET && rawSlot < VAULT_OFFSET + VAULT_SIZE;
    }

    private boolean vaultHasRoom(Inventory top) {
        for (int slot = VAULT_OFFSET; slot < VAULT_OFFSET + VAULT_SIZE; slot++) {
            ItemStack it = top.getItem(slot);
            if (it == null || it.getType() == Material.AIR) return true;
        }
        return false;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!isVault(top)) return;

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= TOTAL_SIZE) continue;
            if (!isVaultSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
            ItemStack placed = event.getNewItems().get(rawSlot);
            if (placed != null && IntrinsicBook.getIntrinsicId(plugin, placed) == null) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(ChatColor.RED + "Only Intrinsic Books can be placed in the vault.");
                return;
            }
        }
        Player player = (Player) event.getWhoClicked();
        Bukkit.getScheduler().runTask(plugin, () -> syncVaultState(player, top));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!isVault(top)) return;
        Player player = (Player) event.getWhoClicked();

        int rawSlot = event.getRawSlot();
        boolean withinTop = rawSlot >= 0 && rawSlot < TOTAL_SIZE;
        boolean vaultSlotClicked = withinTop && isVaultSlot(rawSlot);

        if (withinTop && !vaultSlotClicked) {
            event.setCancelled(true);
            if (rawSlot == CLOSE_SLOT) {
                player.closeInventory();
            }
            return;
        }

        if (vaultSlotClicked) {
            ItemStack incoming = event.getCursor();
            if (event.getHotbarButton() >= 0) {
                incoming = player.getInventory().getItem(event.getHotbarButton());
            }
            if (incoming != null && incoming.getType() != Material.AIR
                    && IntrinsicBook.getIntrinsicId(plugin, incoming) == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Only Intrinsic Books can be placed in the vault.");
                return;
            }
        }

        if (event.isShiftClick() && !withinTop) {
            ItemStack shifting = event.getCurrentItem();
            if (shifting != null && IntrinsicBook.getIntrinsicId(plugin, shifting) != null
                    && !vaultHasRoom(top)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Vault is full (max " + VAULT_SIZE + ").");
                return;
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncVaultState(player, top));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory top = event.getView().getTopInventory();
        if (!isVault(top)) return;
        syncVaultState(player, top);
        openVaults.remove(player.getUniqueId());
    }

    private boolean isVault(Inventory inv) {
        return openVaults.containsValue(inv);
    }

    private void syncVaultState(Player player, Inventory top) {
        Set<IntrinsicId> nowInVault = EnumSet.noneOf(IntrinsicId.class);
        for (int slot = VAULT_OFFSET; slot < VAULT_OFFSET + VAULT_SIZE; slot++) {
            ItemStack item = top.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            IntrinsicId id = IntrinsicBook.getIntrinsicId(plugin, item);
            if (id == null) {
                top.setItem(slot, null);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                continue;
            }
            if (!nowInVault.add(id)) {
                top.setItem(slot, null);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
        }

        Map<String, IntrinsicId> bestPerLine = new HashMap<>();
        for (IntrinsicId id : nowInVault) {
            bestPerLine.merge(id.line(), id, (a, b) -> a.tier() >= b.tier() ? a : b);
        }
        Set<IntrinsicId> toEvict = new HashSet<>(nowInVault);
        toEvict.removeAll(bestPerLine.values());
        if (!toEvict.isEmpty()) {
            for (int slot = VAULT_OFFSET; slot < VAULT_OFFSET + VAULT_SIZE; slot++) {
                ItemStack item = top.getItem(slot);
                if (item == null) continue;
                IntrinsicId id = IntrinsicBook.getIntrinsicId(plugin, item);
                if (id != null && toEvict.contains(id)) {
                    top.setItem(slot, null);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                }
            }
            nowInVault.removeAll(toEvict);
            player.sendMessage(ChatColor.YELLOW + "Only the highest tier of each Intrinsic line can be kept - the lower tier(s) were returned.");
        }

        Set<IntrinsicId> wasActive = storage.getActive(player.getUniqueId());

        for (IntrinsicId id : nowInVault) {
            if (!wasActive.contains(id)) {
                storage.learn(player.getUniqueId(), id);
                storage.setActive(player.getUniqueId(), id, true);
                intrinsicManager.equip(player, id);
                player.sendMessage(ChatColor.GREEN + "Activated " + IntrinsicBook.displayName(id) + ".");
            }
        }
        for (IntrinsicId id : wasActive) {
            if (!nowInVault.contains(id)) {
                storage.setActive(player.getUniqueId(), id, false);
                intrinsicManager.unequip(player, id);
                player.sendMessage(ChatColor.YELLOW + "Deactivated " + IntrinsicBook.displayName(id) + ".");
            }
        }
    }
}
