package com.github.joaoalberis.treasurehunt.gui;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

public class TreasureGuiListener implements Listener {

    private final DatabaseManager dbManager;
    private final Plugin plugin;
    private final TreasureCache cache;

    public TreasureGuiListener(DatabaseManager dbManager, Plugin plugin, TreasureCache cache) {
        this.dbManager = dbManager;
        this.plugin = plugin;
        this.cache = cache;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null) return;
        String rawTitle = event.getView().getTitle();
        if (rawTitle == null) return;

        String title = ChatColor.stripColor(rawTitle);

        if (!title.startsWith("Treasure - Delete") && !title.startsWith("Treasure - List") && !title.startsWith("Confirm deletion")) {
            return;
        }

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

        if (title.startsWith("Treasure - List")) {
            TreasureGuiManager.getContext(player.getUniqueId()).ifPresent(ctx -> {
                if (rawSlot == 45 && ctx.page > 1) {
                    ctx.page = Math.max(1, ctx.page - 1);
                    TreasureGuiManager.openListGui(player, new ArrayList<>(ctx.treasures), ctx.page);
                    return;
                }
                if (rawSlot == 53 && ctx.page < Math.max(1, (ctx.treasures.size() + TreasureGuiManager.PAGE_SIZE - 1) / TreasureGuiManager.PAGE_SIZE)) {
                    ctx.page = ctx.page + 1;
                    TreasureGuiManager.openListGui(player, new ArrayList<>(ctx.treasures), ctx.page);
                    return;
                }

                Material type = clicked.getType();
                if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                    String display = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                            ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName())
                            : null;
                    if (display == null || !display.startsWith("Treasure #")) return;
                    String id = display.replace("Treasure #", "").trim();
                    TreasureModel selected = ctx.treasures.stream().filter(t -> t.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
                    if (selected == null) {
                        MessageUtil.send(player, "generic.not-in-context");
                        return;
                    }

                    MessageUtil.send(player, "treasure.created-info", Map.of(
                            "id", selected.getId(),
                            "world", selected.getWorld(),
                            "x", String.valueOf(selected.getX()),
                            "y", String.valueOf(selected.getY()),
                            "z", String.valueOf(selected.getZ())
                    ));
                }
            });
            return;
        }

        if (title.startsWith("Treasure - Delete")) {
            TreasureGuiManager.getContext(player.getUniqueId()).ifPresent(ctx -> {
                if (rawSlot == 45 && ctx.page > 1) {
                    ctx.page = Math.max(1, ctx.page - 1);
                    TreasureGuiManager.openDeleteSelection(player, new ArrayList<>(ctx.treasures), ctx.page);
                    return;
                }
                if (rawSlot == 53 && ctx.page < Math.max(1, (ctx.treasures.size() + TreasureGuiManager.PAGE_SIZE - 1) / TreasureGuiManager.PAGE_SIZE)) {
                    ctx.page = ctx.page + 1;
                    TreasureGuiManager.openDeleteSelection(player, new ArrayList<>(ctx.treasures), ctx.page);
                    return;
                }

                Material type = clicked.getType();
                if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                    String display = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                            ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName())
                            : null;
                    if (display == null || !display.startsWith("Treasure #")) return;
                    String id = display.replace("Treasure #", "").trim();
                    TreasureModel selected = ctx.treasures.stream().filter(t -> t.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
                    if (selected == null) {
                        MessageUtil.send(player, "generic.not-in-context");
                        return;
                    }
                    TreasureGuiManager.openConfirmGui(player, selected);
                }
            });
            return;
        }

        if (title.startsWith("Confirm deletion")) {
            TreasureGuiManager.getContext(player.getUniqueId()).ifPresent(ctx -> {
                ItemStack current = event.getCurrentItem();
                if (current == null) return;
                Material mat = current.getType();

                if (mat == Material.LIME_CONCRETE) {
                    TreasureModel target = ctx.selected;
                    if (target == null) {
                        MessageUtil.send(player, "generic.internal-error");
                        player.closeInventory();
                        TreasureGuiManager.removeContext(player.getUniqueId());
                        return;
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            boolean removed = dbManager.deleteById(target.getId());
                            if (!removed) {
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    MessageUtil.send(player, "treasure.not-found", Map.of("id", target.getId()));
                                    player.closeInventory();
                                    TreasureGuiManager.removeContext(player.getUniqueId());
                                });
                                return;
                            }

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                cache.getById(target.getId()).ifPresent(cache::remove);
                                cache.removeClaimsForTreasure(target.getId());

                                MessageUtil.send(player, "treasure.deleted", Map.of("id", target.getId()));
                                player.closeInventory();
                                TreasureGuiManager.removeContext(player.getUniqueId());
                            });
                        } catch (SQLException ex) {
                            Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "generic.error-loading"));
                        }
                    });


                } else if (mat == Material.RED_CONCRETE) {
                    MessageUtil.send(player, "generic.cancelled");
                    player.closeInventory();
                    TreasureGuiManager.removeContext(player.getUniqueId());
                }
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                TreasureGuiManager.removeContext(player.getUniqueId());
                return;
            }

            if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null) {
                TreasureGuiManager.removeContext(player.getUniqueId());
                return;
            }

            String topTitle = player.getOpenInventory().getTitle();
            if (topTitle == null) {
                TreasureGuiManager.removeContext(player.getUniqueId());
                return;
            }

            String stripped = ChatColor.stripColor(topTitle);
            if (!stripped.startsWith("Treasure - Delete") && !stripped.startsWith("Confirm deletion")) {
                TreasureGuiManager.removeContext(player.getUniqueId());
            }
        });
    }
}
