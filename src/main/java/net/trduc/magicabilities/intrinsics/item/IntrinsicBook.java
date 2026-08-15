package net.trduc.magicabilitiesfork.intrinsics.item;

import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class IntrinsicBook {

    private IntrinsicBook() {
    }

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "intrinsic_book_id");
    }

    public static ItemStack create(Plugin plugin, IntrinsicId id) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Intrinsic Book: " + ChatColor.GOLD + displayName(id));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to learn this Intrinsic.");
        lore.add(ChatColor.DARK_GRAY + "Dropped by defeated bosses.");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, id.name());
        book.setItemMeta(meta);
        return book;
    }

    public static IntrinsicId getIntrinsicId(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(key(plugin), PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return IntrinsicId.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String displayName(IntrinsicId id) {
        String[] parts = id.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1 && part.matches("[1-5]")) {
                sb.append(toRoman(Integer.parseInt(part)));
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
            }
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private static String toRoman(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(n);
        }
    }
}
