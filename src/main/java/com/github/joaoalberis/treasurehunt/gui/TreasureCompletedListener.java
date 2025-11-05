package com.github.joaoalberis.treasurehunt.gui;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String rawTitle = event.getView().getTitle();
        if (rawTitle == null) return;
        String title = ChatColor.stripColor(rawTitle);
        if (!title.startsWith("Players who claimed")) return;

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (rawSlot < 0 || rawSlot >= topSize) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        TreasureCompletedGuiManager.getContext(player.getUniqueId()).ifPresent(ctx -> {
            List<Map.Entry<UUID, Timestamp>> entries = new ArrayList<>(ctx.claims.entrySet());
            int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) TreasureCompletedGuiManager.PAGE_SIZE));
            if (rawSlot == 45 && ctx.page > 1) {
                ctx.page--;
                TreasureCompletedGuiManager.open(player, ctx.treasureId, ctx.claims, ctx.page);
                return;
            }
            if (rawSlot == 53 && ctx.page < pages) {
                ctx.page++;
                TreasureCompletedGuiManager.open(player, ctx.treasureId, ctx.claims, ctx.page);
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
                        ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName())
                        : MessageUtil.getRaw("generic.unknown");

                MessageUtil.send(player, "completed.detail-title", Map.of("name", name));
                if (uuid != null) {
                    MessageUtil.send(player, "completed.uuid-line", Map.of("uuid", uuid));
                }
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        TreasureCompletedGuiManager.removeContext(player.getUniqueId());
    }
}
