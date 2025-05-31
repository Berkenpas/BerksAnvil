package dev.berkenpas.berksanvil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BerksAnvilPlugin extends JavaPlugin implements Listener {
    private static final Map<Enchantment, Integer> ENCHANTMENT_BASE_COSTS = new HashMap<>();
    static {
        addCost("protection", 1);
        addCost("fire_protection", 1);
        addCost("feather_falling", 1);
        addCost("blast_protection", 2);
        addCost("projectile_protection", 1);
        addCost("thorns", 4);
        addCost("respiration", 2);
        addCost("depth_strider", 2);
        addCost("aqua_affinity", 2);
        addCost("frost_walker", 2);
        addCost("binding_curse", 4);
        addCost("swift_sneak", 4);
        addCost("sharpness", 1);
        addCost("smite", 1);
        addCost("bane_of_arthropods", 1);
        addCost("knockback", 1);
        addCost("fire_aspect", 2);
        addCost("looting", 2);
        addCost("sweeping", 2);
        addCost("efficiency", 1);
        addCost("silk_touch", 4);
        addCost("unbreaking", 1);
        addCost("fortune", 2);
        addCost("mending", 2);
        addCost("power", 1);
        addCost("punch", 2);
        addCost("flame", 2);
        addCost("infinity", 4);
        addCost("luck_of_the_sea", 2);
        addCost("lure", 2);
        addCost("impaling", 2);
        addCost("riptide", 2);
        addCost("loyalty", 1);
        addCost("channeling", 4);
        addCost("multishot", 2);
        addCost("piercing", 1);
        addCost("quick_charge", 1);
        addCost("soul_speed", 4);
        addCost("vanishing_curse", 4);
        // Add more as needed
    }

    private static void addCost(String key, int cost) {
        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(key));
        if (ench != null) {
            ENCHANTMENT_BASE_COSTS.put(ench, cost);
        }
    }

        @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) return;

        // Handle Creative-style enchanted book application
        if (right.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) right.getItemMeta();
            Map<Enchantment, Integer> bookEnchants = bookMeta.getStoredEnchants();
            if (bookEnchants.isEmpty()) return;

            ItemStack forcedResult = left.clone();
            int baseCost = 0;
            for (Map.Entry<Enchantment, Integer> entry : bookEnchants.entrySet()) {
                Enchantment ench = entry.getKey();
                int level = entry.getValue();

                // Apply all book enchantments (even "illegal" ones, like creative)
                forcedResult.addUnsafeEnchantment(ench, level);
                int enchCost = ENCHANTMENT_BASE_COSTS.getOrDefault(ench, 1) * level;
                baseCost += enchCost;
            }

            // Simplified work penalty: +1 for every time combined in the anvil
            int workPenalty = getSimpleWorkPenalty(left) + 1; // always increase by 1 for a new anvil use
            int totalCost = baseCost + workPenalty;

            // Cap at 39
            int repairCost = Math.min(39, totalCost);

            // Set result and cost
            event.setResult(forcedResult);
            inv.setRepairCost(repairCost);
            return;
        }

        // Fallback: If Paper already gives a result, just cap the cost at 39 if needed
        ItemStack vanillaResult = event.getResult();
        if (vanillaResult != null && vanillaResult.getType() != Material.AIR) {
            if (inv.getRepairCost() > 39) inv.setRepairCost(39);
            return;
        }
    }

    // Simplified "work penalty": Instead of reading the NBT "RepairCost", just count unsafe enchantments
    // In vanilla, RepairCost NBT increases each time you use anvil; here we just simulate +1 per use
    private int getSimpleWorkPenalty(ItemStack item) {
        // Could be improved by storing/reading actual repair cost in NBT via a library like PersistentDataContainer
        // but for now we keep it simple
        return 0;
    }
}
