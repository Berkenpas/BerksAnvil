package dev.berkenpas.berksanvil;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class BerksAnvilPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);

        if (left == null || right == null) return;

        ItemStack result = left.clone();

        // Handle enchanted book application
        if (right.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) right.getItemMeta();
            Map<Enchantment, Integer> storedEnchants = bookMeta.getStoredEnchants();
            for (Map.Entry<Enchantment, Integer> entry : storedEnchants.entrySet()) {
                result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
            event.setResult(result);
            inventory.setRepairCost(1); // Custom cost but doesn't trigger "Too Expensive"
        }
        // Handle combining same items (e.g. swords with enchants)
        else if (left.getType() == right.getType()) {
            Map<Enchantment, Integer> leftEnchants = left.getEnchantments();
            Map<Enchantment, Integer> rightEnchants = right.getEnchantments();
            for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
                int level = Math.max(entry.getValue(), leftEnchants.getOrDefault(entry.getKey(), 0));
                result.addUnsafeEnchantment(entry.getKey(), level);
            }
            event.setResult(result);
            inventory.setRepairCost(1);
        }
    }
} 
