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
        // 1) Cannot combine a book in the left slot with an item in the right slot
        if (leftBook && !rightBook) {
            return;
        }
        // 2) Cannot combine two different items
        if (!leftBook && !rightBook && left.getType() != right.getType()) {
            return;
        }

        // Base is always the left slot; sacrifice is the right
        ItemStack result = left.clone();
        ItemMeta baseMeta  = metaLeft;
        ItemMeta otherMeta = metaRight;
        ItemMeta metaOut   = result.getItemMeta();

        // Gather enchantments
        Map<Enchantment,Integer> baseEnchants  = getEnchantMap(baseMeta);
        Map<Enchantment,Integer> otherEnchants = getEnchantMap(otherMeta);

        // Combine enchantments
        Map<Enchantment,Integer> combined = combineEnchants(baseEnchants, otherEnchants);

        // Apply combined enchants
        if (metaOut instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) metaOut;
            esm.getStoredEnchants().keySet().forEach(esm::removeStoredEnchant);
            combined.forEach((ench, lvl) -> esm.addStoredEnchant(ench, lvl, true));
        } else {
            metaOut.getEnchants().keySet().forEach(metaOut::removeEnchant);
            combined.forEach((ench, lvl) -> metaOut.addEnchant(ench, lvl, true));
        }

        result.setItemMeta(metaOut);
        event.setResult(result);

        // Clamp cost
        int rawCost = calculateVanillaCost(baseEnchants, otherEnchants);
        event.getInventory().setRepairCost(Math.min(rawCost, HARD_MAX_COST));
    }

    /** Retrieves enchantments from meta, handling enchanted books. */
    private Map<Enchantment,Integer> getEnchantMap(ItemMeta meta) {
        if (meta instanceof EnchantmentStorageMeta) {
            return new HashMap<>(((EnchantmentStorageMeta) meta).getStoredEnchants());
        } else {
            return new HashMap<>(meta.getEnchants());
        }
    }

    /** Merge two enchantment maps with vanilla rules. */
    private Map<Enchantment, Integer> combineEnchants(
            Map<Enchantment,Integer> a,
            Map<Enchantment,Integer> b
    ) {
        Map<Enchantment, Integer> out = new HashMap<>();
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
        for (var entry : b.entrySet()) {
            out.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return out;
    }

    /** Simple cost: sum of all enchant levels from both inputs. */
    private int calculateVanillaCost(
            Map<Enchantment,Integer> a,
            Map<Enchantment,Integer> b
    ) {
        int cost = 0;
        for (int lvl : a.values()) cost += lvl;
        for (int lvl : b.values()) cost += lvl;
        return cost;
    }
}
