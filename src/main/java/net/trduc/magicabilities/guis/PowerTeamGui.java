package net.trduc.magicabilitiesfork.guis;

import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.PowerTeam;
import net.trduc.magicabilitiesfork.data.PowerteamRequest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerTeamGui implements Listener {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int[] ENTRY_ROWS = {1, 3};
    private static final int[] CLUSTER_START_COLS = {0, 5};
    private static final int[] ENTRY_ROW_GAP_COLS = {3, 4, 8};
    private static final int PAGE_CAPACITY = ENTRY_ROWS.length * CLUSTER_START_COLS.length;
    private static final int NAV_ROW = 5;
    private static final int PREV_SLOT = NAV_ROW * 9 + 7;
    private static final int NEXT_SLOT = NAV_ROW * 9 + 8;
    private static final int CLOSE_SLOT = NAV_ROW * 9 + 0;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy HH:mm");

    private final DbManager db;
    private final Map<Inventory, String> invTeam = new HashMap<>();
    private final Map<Inventory, String> invOwner = new HashMap<>();
    private final Map<Inventory, Integer> invPage = new HashMap<>();
    private final Map<Inventory, Map<Integer, PowerteamRequest>> invClusterRequest = new HashMap<>();

    public PowerTeamGui(DbManager db) {
        this.db = db;
    }

    public void openRequestsGui(Player viewer, String team) {
        openRequestsGui(viewer, team, 0);
    }

    public void openRequestsGui(Player viewer, String team, int page) {
        List<PowerteamRequest> reqs = db.listRequestsForTeam(team);
        int totalPages = Math.max(1, (int) Math.ceil(reqs.size() / (double) PAGE_CAPACITY));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        Inventory inv = Bukkit.createInventory(null, SIZE,
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Team Requests: " + ChatColor.RESET + ChatColor.AQUA + team +
                        ChatColor.GRAY + " (" + (page + 1) + "/" + totalPages + ")");

        decorateFrame(inv, team, reqs.size());

        Map<Integer, PowerteamRequest> clusterMap = new HashMap<>();
        int startIndex = page * PAGE_CAPACITY;

        int slotIndex = 0;
        outer:
        for (int rowIdx : ENTRY_ROWS) {
            for (int colStart : CLUSTER_START_COLS) {
                int reqIndex = startIndex + slotIndex;
                int headSlot = rowIdx * 9 + colStart;
                int denySlot = headSlot + 1;
                int acceptSlot = headSlot + 2;

                if (reqIndex < reqs.size()) {
                    PowerteamRequest r = reqs.get(reqIndex);
                    inv.setItem(headSlot, buildHeadItem(r));
                    inv.setItem(denySlot, buildButton(Material.RED_CONCRETE, ChatColor.RED + "" + ChatColor.BOLD + "Deny", "Click to deny this request.", r));
                    inv.setItem(acceptSlot, buildButton(Material.LIME_CONCRETE, ChatColor.GREEN + "" + ChatColor.BOLD + "Accept", "Click to accept this request.", r));
                    clusterMap.put(headSlot, r);
                } else {
                    inv.setItem(headSlot, filler(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                    inv.setItem(denySlot, filler(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                    inv.setItem(acceptSlot, filler(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                }
                slotIndex++;
                if (slotIndex >= PAGE_CAPACITY) break outer;
            }
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.RED_CONCRETE);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "<< Previous Page");
            prev.setItemMeta(prevMeta);
            inv.setItem(PREV_SLOT, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Next Page >>");
            next.setItemMeta(nextMeta);
            inv.setItem(NEXT_SLOT, next);
        }

        if (reqs.isEmpty()) {
            ItemStack info = new ItemStack(Material.BARRIER);
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "No pending requests");
            infoMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Join requests for " + team + " will appear here."));
            info.setItemMeta(infoMeta);
            inv.setItem(NAV_ROW * 9 + 4, info);
        }

        String viewerName = viewer.getName();
        List<Inventory> toRemove = new ArrayList<>();
        for (Map.Entry<Inventory, String> e : invOwner.entrySet()) {
            if (e.getValue().equals(viewerName)) toRemove.add(e.getKey());
        }
        for (Inventory iRem : toRemove) {
            invOwner.remove(iRem);
            invTeam.remove(iRem);
            invPage.remove(iRem);
            invClusterRequest.remove(iRem);
        }

        invTeam.put(inv, team);
        invOwner.put(inv, viewerName);
        invPage.put(inv, page);
        invClusterRequest.put(inv, clusterMap);
        viewer.openInventory(inv);
    }

    private void decorateFrame(Inventory inv, String team, int pendingCount) {
        for (int c = 0; c < 9; c++) {
            boolean corner = (c == 0 || c == 8);
            Material mat = corner ? Material.BLACK_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE;
            inv.setItem(c, filler(mat, " "));
            inv.setItem(NAV_ROW * 9 + c, filler(mat, " "));
        }
        for (int c = 0; c < 9; c++) {
            inv.setItem(2 * 9 + c, filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
            inv.setItem(4 * 9 + c, filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }
        for (int rowIdx : ENTRY_ROWS) {
            for (int c : ENTRY_ROW_GAP_COLS) {
                inv.setItem(rowIdx * 9 + c, filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
            }
        }

        ItemStack title = new ItemStack(Material.NAME_TAG);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + team);
        titleMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Pending join requests for this team.",
                " ",
                ChatColor.GRAY + "Pending: " + ChatColor.WHITE + pendingCount
        ));
        title.setItemMeta(titleMeta);
        inv.setItem(4, title);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Close");
        close.setItemMeta(closeMeta);
        inv.setItem(CLOSE_SLOT, close);
    }

    private ItemStack buildHeadItem(PowerteamRequest r) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(r.getTarget())); } catch (Throwable ignored) {}
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + r.getTarget());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Requested by: " + ChatColor.WHITE + r.getRequester(),
                ChatColor.GRAY + "Requested: " + ChatColor.WHITE + DATE_FORMAT.format(new java.util.Date(r.getTs()))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildButton(Material mat, String name, String actionHint, PowerteamRequest r) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Target: " + ChatColor.WHITE + r.getTarget(),
                " ",
                ChatColor.DARK_GRAY + actionHint
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filler(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private boolean canManage(String team, Player p) {
        if (p.hasPermission("magic.admin")) return true;
        PowerTeam pt = db.getPowerTeam(team);
        if (pt == null) return false;
        if (pt.getOwner().equalsIgnoreCase(p.getName())) return true;
        return db.isCoowner(team, p.getName());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        invTeam.remove(inv);
        invOwner.remove(inv);
        invPage.remove(inv);
        invClusterRequest.remove(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (!invTeam.containsKey(inv)) return;
        e.setCancelled(true);

        Player clicker = (Player) e.getWhoClicked();
        String team = invTeam.get(inv);
        int page = invPage.getOrDefault(inv, 0);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        if (slot == CLOSE_SLOT) {
            clicker.closeInventory();
            return;
        }

        if (!canManage(team, clicker)) {
            clicker.sendMessage(ChatColor.RED + "Only the team owner, co-owner, or admins can manage requests.");
            return;
        }

        if (slot == PREV_SLOT && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.RED_CONCRETE) {
            openRequestsGui(clicker, team, page - 1);
            return;
        }
        if (slot == NEXT_SLOT && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.LIME_CONCRETE) {
            openRequestsGui(clicker, team, page + 1);
            return;
        }

        int row = slot / 9;
        int col = slot % 9;
        int clusterStartCol = -1;
        for (int c : CLUSTER_START_COLS) {
            if (col >= c && col <= c + 2) { clusterStartCol = c; break; }
        }
        boolean isEntryRow = false;
        for (int r : ENTRY_ROWS) if (r == row) isEntryRow = true;
        if (!isEntryRow || clusterStartCol == -1) return;

        int headSlot = row * 9 + clusterStartCol;
        Map<Integer, PowerteamRequest> clusterMap = invClusterRequest.get(inv);
        PowerteamRequest req = clusterMap == null ? null : clusterMap.get(headSlot);
        if (req == null) return;

        int role = col - clusterStartCol;
        String targetName = req.getTarget();

        if (role == 2) {
            boolean ok = db.approveRequest(team, targetName, clicker.getName());
            if (ok) {
                clicker.sendMessage(ChatColor.GREEN + "Approved " + targetName + " into " + team);
                Player tp = Bukkit.getPlayer(targetName);
                if (tp != null) {
                    tp.sendMessage(ChatColor.GREEN + "You have been added to team " + team + ".");
                    net.trduc.magicabilitiesfork.misc.TeamColorSync.syncPlayer(db, tp);
                }
                openRequestsGui(clicker, team, page);
            } else {
                clicker.sendMessage(ChatColor.RED + "Approve failed.");
            }
        } else if (role == 1) {
            boolean ok = db.denyRequest(team, targetName, clicker.getName());
            if (ok) {
                clicker.sendMessage(ChatColor.YELLOW + "Denied the request from " + targetName);
                Player tp = Bukkit.getPlayer(targetName);
                if (tp != null) tp.sendMessage(ChatColor.RED + "Your request to join team " + team + " was denied.");
                openRequestsGui(clicker, team, page);
            } else {
                clicker.sendMessage(ChatColor.RED + "Deny failed or request no longer exists.");
            }
        }
    }
}
