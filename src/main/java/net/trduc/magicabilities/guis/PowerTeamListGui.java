package net.trduc.magicabilitiesfork.guis;

import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.PowerTeam;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerTeamListGui implements Listener {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int[] CARD_COLS = {0, 2, 4, 6, 8};
    private static final int[] DIVIDER_COLS = {1, 3, 5, 7};
    private static final int NAME_ROW = 1;
    private static final int MEMBER_ROW_START = 2;
    private static final int MEMBER_ROWS = 3;
    private static final int NAV_ROW = 5;
    private static final int PREV_SLOT = NAV_ROW * 9 + 7;
    private static final int NEXT_SLOT = NAV_ROW * 9 + 8;
    private static final int CLOSE_SLOT = NAV_ROW * 9 + 0;
    private static final int PAGE_CAPACITY = CARD_COLS.length;

    private final DbManager db;
    private final Map<Inventory, Integer> invPage = new HashMap<>();
    private final Map<Inventory, String> invViewer = new HashMap<>();

    public PowerTeamListGui(DbManager db) {
        this.db = db;
    }

    public void openTeamListGui(Player viewer) {
        openTeamListGui(viewer, 0);
    }

    public void openTeamListGui(Player viewer, int page) {
        List<String> teamNames = db.listPowerTeams();
        int totalPages = Math.max(1, (int) Math.ceil(teamNames.size() / (double) PAGE_CAPACITY));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        Inventory inv = Bukkit.createInventory(null, SIZE,
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Team List" + ChatColor.RESET + ChatColor.GRAY + " (" + (page + 1) + "/" + totalPages + ")");

        decorateFrame(inv, teamNames.size());

        int startIndex = page * PAGE_CAPACITY;
        for (int i = 0; i < CARD_COLS.length; i++) {
            int col = CARD_COLS[i];
            int teamIndex = startIndex + i;

            if (teamIndex >= teamNames.size()) {
                for (int r = NAME_ROW; r < MEMBER_ROW_START + MEMBER_ROWS; r++) {
                    inv.setItem(r * 9 + col, filler(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                }
                continue;
            }

            String teamName = teamNames.get(teamIndex);
            PowerTeam pt = db.getPowerTeam(teamName);
            ChatColor teamColor = parseColor(pt == null ? null : pt.getColor());
            List<String> members = pt == null ? new ArrayList<>() : pt.getMembers();

            ItemStack plaque = new ItemStack(teamColorToMaterial(teamColor));
            ItemMeta plaqueMeta = plaque.getItemMeta();
            plaqueMeta.setDisplayName(teamColor + "" + ChatColor.BOLD + teamName);
            plaqueMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Owner: " + ChatColor.WHITE + (pt == null ? "?" : pt.getOwner()),
                    ChatColor.GRAY + "Members: " + ChatColor.WHITE + members.size(),
                    " ",
                    ChatColor.DARK_GRAY + "Team color: " + teamColor + prettyColorName(teamColor)
            ));
            plaque.setItemMeta(plaqueMeta);
            inv.setItem(NAME_ROW * 9 + col, plaque);

            for (int m = 0; m < MEMBER_ROWS; m++) {
                int slot = (MEMBER_ROW_START + m) * 9 + col;
                if (m == MEMBER_ROWS - 1 && members.size() > MEMBER_ROWS) {
                    int remaining = members.size() - (MEMBER_ROWS - 1);
                    ItemStack more = new ItemStack(Material.PAPER);
                    ItemMeta moreMeta = more.getItemMeta();
                    moreMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "+" + remaining + " more");
                    moreMeta.setLore(java.util.Collections.singletonList(
                            ChatColor.GRAY + "...and " + remaining + " more member" + (remaining == 1 ? "" : "s") + "."));
                    more.setItemMeta(moreMeta);
                    inv.setItem(slot, more);
                } else if (m < members.size()) {
                    String memberName = members.get(m);
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta headMeta = (SkullMeta) head.getItemMeta();
                    try { headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(memberName)); } catch (Throwable ignored) {}
                    boolean isOwner = pt != null && memberName.equals(pt.getOwner());
                    String tag = isOwner ? ChatColor.GOLD + " (Owner)" : "";
                    headMeta.setDisplayName(ChatColor.AQUA + memberName + tag);
                    head.setItemMeta(headMeta);
                    inv.setItem(slot, head);
                } else {
                    inv.setItem(slot, filler(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                }
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

        if (teamNames.isEmpty()) {
            ItemStack info = new ItemStack(Material.BARRIER);
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "No teams yet");
            infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "No Power Teams exist on this server yet.",
                    ChatColor.GRAY + "Use " + ChatColor.WHITE + "/powerteam create <name>" + ChatColor.GRAY + " to start one!"
            ));
            info.setItemMeta(infoMeta);
            inv.setItem(NAV_ROW * 9 + 4, info);
        }

        String viewerName = viewer.getName();
        List<Inventory> toRemove = new ArrayList<>();
        for (Map.Entry<Inventory, String> e : invViewer.entrySet()) {
            if (e.getValue().equals(viewerName)) toRemove.add(e.getKey());
        }
        for (Inventory iRem : toRemove) {
            invViewer.remove(iRem);
            invPage.remove(iRem);
        }

        invViewer.put(inv, viewerName);
        invPage.put(inv, page);
        viewer.openInventory(inv);
    }

    private void decorateFrame(Inventory inv, int totalTeams) {
        for (int c = 0; c < 9; c++) {
            boolean corner = (c == 0 || c == 8);
            Material topMat = corner ? Material.BLACK_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE;
            inv.setItem(c, filler(topMat, " "));
            inv.setItem(NAV_ROW * 9 + c, filler(topMat, " "));
        }
        for (int r = NAME_ROW; r < MEMBER_ROW_START + MEMBER_ROWS; r++) {
            for (int c : DIVIDER_COLS) {
                inv.setItem(r * 9 + c, filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
            }
        }

        ItemStack title = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Team List");
        titleMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "All Power Teams currently on the server.",
                " ",
                ChatColor.GRAY + "Total teams: " + ChatColor.WHITE + totalTeams
        ));
        title.setItemMeta(titleMeta);
        inv.setItem(4, title);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Close");
        close.setItemMeta(closeMeta);
        inv.setItem(CLOSE_SLOT, close);
    }

    private ChatColor parseColor(String color) {
        if (color == null) return ChatColor.WHITE;
        try {
            return ChatColor.valueOf(color.toUpperCase());
        } catch (Exception e) {
            ChatColor byCode = ChatColor.getByChar(color.replace("&", "").replace("§", ""));
            return byCode != null ? byCode : ChatColor.WHITE;
        }
    }

    private Material teamColorToMaterial(ChatColor color) {
        if (color == null) return Material.WHITE_CONCRETE;
        switch (color) {
            case BLACK: return Material.BLACK_CONCRETE;
            case DARK_GRAY: return Material.GRAY_CONCRETE;
            case GRAY: return Material.LIGHT_GRAY_CONCRETE;
            case DARK_RED: return Material.RED_CONCRETE;
            case RED: return Material.PINK_CONCRETE;
            case GOLD: return Material.ORANGE_CONCRETE;
            case YELLOW: return Material.YELLOW_CONCRETE;
            case DARK_GREEN: return Material.GREEN_CONCRETE;
            case GREEN: return Material.LIME_CONCRETE;
            case DARK_AQUA: return Material.CYAN_CONCRETE;
            case AQUA: return Material.LIGHT_BLUE_CONCRETE;
            case DARK_BLUE: return Material.BLUE_CONCRETE;
            case BLUE: return Material.LIGHT_BLUE_CONCRETE;
            case DARK_PURPLE: return Material.PURPLE_CONCRETE;
            case LIGHT_PURPLE: return Material.MAGENTA_CONCRETE;
            case WHITE:
            default: return Material.WHITE_CONCRETE;
        }
    }

    private String prettyColorName(ChatColor color) {
        String[] parts = color.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private ItemStack filler(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        invPage.remove(inv);
        invViewer.remove(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (!invViewer.containsKey(inv)) return;
        e.setCancelled(true);

        Player clicker = (Player) e.getWhoClicked();
        int page = invPage.getOrDefault(inv, 0);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        if (slot == CLOSE_SLOT) {
            clicker.closeInventory();
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        if (slot == PREV_SLOT && clicked.getType() == Material.RED_CONCRETE) {
            openTeamListGui(clicker, page - 1);
            return;
        }
        if (slot == NEXT_SLOT && clicked.getType() == Material.LIME_CONCRETE) {
            openTeamListGui(clicker, page + 1);
        }
    }
}
