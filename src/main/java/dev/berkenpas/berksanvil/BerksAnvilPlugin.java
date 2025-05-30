package dev.berkenpas.berksanvil;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.enchantments.Enchantment;

public class BerksAnvilPlugin extends JavaPlugin implements Listener {

    private static final int HARD_MAX_COST = 39;

    @Override
    public void onEnable() {
        // Register this class as an event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack left  = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);
        if (left == null || right == null) return;

        ItemMeta metaLeft  = left.getItemMeta();
        ItemMeta metaRight = right.getItemMeta();
        boolean leftBook   = metaLeft  instanceof EnchantmentStorageMeta;
        boolean rightBook  = metaRight instanceof EnchantmentStorageMeta;

        // Mirror vanilla behavior:
        // 1) Book left + item right is invalid
        if (leftBook && !rightBook) return;
        // 2) Two different items invalid
        if (!leftBook && !rightBook && left.getType() != right.getType()) return;

        // Base is always the left slot; sacrifice is the right slot
        ItemStack result = left.clone();
        ItemMeta baseMeta  = metaLeft;
        ItemMeta otherMeta = metaRight;
        ItemMeta metaOut   = result.getItemMeta();

        // Gather enchantments
        Map<Enchantment,Integer> baseEnchants  = getEnchantMap(baseMeta);
        Map<Enchantment,Integer> otherEnchants = getEnchantMap(otherMeta);

        // Combine enchantments
        Map<Enchantment,Integer> combined = combineEnchants(baseEnchants, otherEnchants);

        // Apply combined enchantments
        if (metaOut instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) metaOut;
            esm.getStoredEnchants().keySet().forEach(esm::removeStoredEnchant);
            combined.forEach((ench, lvl) -> esm.addStoredEnchant(ench, lvl, true));
        } else {
            metaOut.getEnchants().keySet().forEach(metaOut::removeEnchant);
            combined.forEach((ench, lvl) -> metaOut.addEnchant(ench, lvl, true));
        }

        // Set the result and clamp cost
        result.setItemMeta(metaOut);
        int vanillaCost = event.getInventory().getRepairCost();
        event.getInventory().setRepairCost(Math.min(vanillaCost, HARD_MAX_COST));
        event.setResult(result);
    }

    /**
     * Retrieve enchantments from ItemMeta, handling enchanted books.
     */
    private Map<Enchantment,Integer> getEnchantMap(ItemMeta meta) {
        if (meta instanceof EnchantmentStorageMeta) {
            return new HashMap<>(((EnchantmentStorageMeta) meta).getStoredEnchants());
        } else {
            return new HashMap<>(meta.getEnchants());
        }
    }

    /**
     * Merge two enchantment maps with vanilla rules:
     *  - Same level → +1 (capped)
     *  - Different levels → higher
     *  - Unique enchants → carried
     */
    private Map<Enchantment, Integer> combineEnchants(
            Map<Enchantment,Integer> a,
            Map<Enchantment,Integer> b
    ) {
        Map<Enchantment, Integer> out = new HashMap<>();
        // Process base enchants
        for (var entry : a.entrySet()) {
            Enchantment ench = entry.getKey();
            int lvlA = entry.getValue();
            int lvlB = b.getOrDefault(ench, 0);
            if (lvlB > 0) {
                if (lvlA == lvlB) {
                    out.put(ench, Math.min(lvlA + 1, ench.getMaxLevel()));
                } else {
                    out.put(ench, Math.max(lvlA, lvlB));
                }
            } else {
                out.put(ench, lvlA);
            }
        }
        // Add sac enchants that weren't in base
        for (var entry : b.entrySet()) {
            out.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return out;
    }
}
