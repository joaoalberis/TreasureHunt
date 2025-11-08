package com.github.joaoalberis.treasurehunt.gui.delete;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;


public class TreasureDeleteListener implements Listener {

    private final DatabaseManager dbManager;
    private final Plugin plugin;
    private final TreasureCache cache;

    public TreasureDeleteListener(DatabaseManager dbManager, Plugin plugin, TreasureCache cache) {
        this.dbManager = dbManager;
        this.plugin = plugin;
        this.cache = cache;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof TreasureDeleteInventory invDel)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (rawSlot < 0 || rawSlot >= topSize) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String displayName = "";
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        }

        int page = invDel.getPage();
        int pages = Math.max(1, (invDel.getTreasures().size() + invDel.PAGE_SIZE - 1) / invDel.PAGE_SIZE);

        if (clicked.getType() == Material.ARROW && displayName.equalsIgnoreCase("< Previous") && page > 1) {
            invDel.newPage(page - 1).open(player);
            return;
        }
        if (clicked.getType() == Material.ARROW && displayName.equalsIgnoreCase("Next >") && page < pages) {
            invDel.newPage(page + 1).open(player);
            return;
        }

        Material type = clicked.getType();
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
            String display = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                    ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName())
                    : null;
            if (display == null || !display.startsWith("Treasure #")) return;
            String id = display.replace("Treasure #", "").trim();
            TreasureModel selected = invDel.getTreasures().stream().filter(t -> t.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (selected == null) {
                MessageUtil.send(player, "generic.not-in-context");
                return;
            }
            TreasureDeleteConfirmInventory inv = new TreasureDeleteConfirmInventory(plugin, selected);
            player.openInventory(inv.getInventory());
        }
    }

}
