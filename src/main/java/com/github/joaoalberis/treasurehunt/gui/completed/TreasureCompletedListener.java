package com.github.joaoalberis.treasurehunt.gui.completed;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TreasureCompletedListener implements Listener {

    private final DatabaseManager dbManager;
    private final Plugin plugin;
    private final TreasureCache cache;

    public TreasureCompletedListener(DatabaseManager dbManager, Plugin plugin, TreasureCache cache) {
        this.dbManager = dbManager;
        this.plugin = plugin;
        this.cache = cache;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof TreasureCompletedInventory myInventory) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            event.setCancelled(true);

            int rawSlot = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (rawSlot < 0 || rawSlot >= topSize) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            List<Map.Entry<UUID, Timestamp>> entries = new ArrayList<>(myInventory.getClaims().entrySet());
            int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) myInventory.PAGE_SIZE));

            String displayName = "";
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            }

            if (clicked.getType() == Material.ARROW && displayName.equalsIgnoreCase("< Previous") && myInventory.getPage() > 1) {
                int page = myInventory.getPage();
                myInventory.newPage(plugin, page -1).open(player);
                return;
            }
            if (clicked.getType() == Material.ARROW && displayName.equalsIgnoreCase("Next >") && myInventory.getPage() < pages) {
                int page = myInventory.getPage();
                myInventory.newPage(plugin, page + 1).open(player);
                return;
            }

            if (clicked.getType() == Material.PLAYER_HEAD) {
                String uuid = null;
                if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                    for (String l : clicked.getItemMeta().getLore()) {
                        if (l.toLowerCase().contains("uuid:")) {
                            uuid = ChatColor.stripColor(l).replace("UUID:", "").trim();
                            break;
                        }
                    }
                }
                String name = clicked.getItemMeta() != null && clicked.getItemMeta().hasDisplayName()
                        ? ChatColor.stripColor(displayName)
                        : MessageUtil.getRaw("generic.unknown");

                MessageUtil.send(player, "completed.detail-title", Map.of("name", name));
                if (uuid != null) {
                    MessageUtil.send(player, "completed.uuid-line", Map.of("uuid", uuid));
                }
            }
        }
    }

}
