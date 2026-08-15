package net.trduc.magicabilitiesfork.intrinsics.item;

import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Collections;

public final class IntrinsicBookRecipes {

    private IntrinsicBookRecipes() {
    }

    public static void registerAll(Plugin plugin) {
        for (IntrinsicId from : IntrinsicId.values()) {
            IntrinsicId to = nextTierOf(from);
            if (to == null) continue;

            NamespacedKey key = new NamespacedKey(plugin, "intrinsic_upgrade_" + from.name().toLowerCase());
            ItemStack result = IntrinsicBook.create(plugin, to);
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);

            RecipeChoice.ExactChoice ingredient = new RecipeChoice.ExactChoice(
                    Collections.singletonList(IntrinsicBook.create(plugin, from)));
            recipe.addIngredient(ingredient);
            recipe.addIngredient(ingredient);
            recipe.addIngredient(ingredient);

            plugin.getServer().addRecipe(recipe);
        }
    }

    private static IntrinsicId nextTierOf(IntrinsicId id) {
        String name = id.name();
        int lastUnderscore = name.lastIndexOf('_');
        String lastPart = name.substring(lastUnderscore + 1);
        if (!lastPart.matches("[1-9]")) return null;
        int tier = Integer.parseInt(lastPart);
        String candidateName = name.substring(0, lastUnderscore) + "_" + (tier + 1);
        try {
            return IntrinsicId.valueOf(candidateName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
