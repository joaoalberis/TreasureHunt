package com.github.joaoalberis.treasurehunt.gui.delete;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.Map;

public class TreasureDeleteConfirmListener implements Listener {

    private final DatabaseManager dbManager;
    private final Plugin plugin;
    private final TreasureCache cache;

    public TreasureDeleteConfirmListener(DatabaseManager dbManager, Plugin plugin, TreasureCache cache) {
        this.dbManager = dbManager;
        this.plugin = plugin;
        this.cache = cache;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof TreasureDeleteConfirmInventory delInv)) return;
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

        ItemStack current = event.getCurrentItem();
        if (current == null) return;
        Material mat = current.getType();

        if (mat == Material.LIME_CONCRETE) {
            TreasureModel target = delInv.getTreasure();
            if (target == null) {
                MessageUtil.send(player, "generic.internal-error");
                player.closeInventory();
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    boolean removed = dbManager.deleteById(target.getId());
                    if (!removed) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            MessageUtil.send(player, "treasure.not-found", Map.of("id", target.getId()));
                            player.closeInventory();
                        });
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        cache.getById(target.getId()).ifPresent(cache::remove);
                        cache.removeClaimsForTreasure(target.getId());

                        MessageUtil.send(player, "treasure.deleted", Map.of("id", target.getId()));
                        player.closeInventory();
                    });
                } catch (SQLException ex) {
                    Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "generic.error-loading"));
                }
            });


        } else if (mat == Material.RED_CONCRETE) {
            MessageUtil.send(player, "generic.cancelled");
            player.closeInventory();
        }
    }

}
