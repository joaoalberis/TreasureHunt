package com.github.joaoalberis.treasurehunt.events;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.commands.TreasureCreate;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.models.PendingCreation;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class RightClickTreasure implements Listener {

    private final Plugin plugin;
    private final DatabaseManager dbManager;
    private final TreasureCache cache;

    public RightClickTreasure(Plugin plugin, DatabaseManager dbManager, TreasureCache cache) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.cache = cache;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRightClickCreatedTreasure(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Optional<PendingCreation> optPendingCreation = TreasureCreate.getPendingCreation(player.getUniqueId());
        if (optPendingCreation.isEmpty()) return;

        PendingCreation pendingCreation = optPendingCreation.get();

        String worldName = player.getWorld().getName();
        int x = event.getClickedBlock().getX();
        int y = event.getClickedBlock().getY();
        int z = event.getClickedBlock().getZ();

        String posKey = DatabaseManager.posKey(worldName, x, y, z);
        if (cache.getByPos(worldName, x, y, z).isPresent()) {
            MessageUtil.send(player, "treasure.exists");
            TreasureCreate.removePendingCreation(player.getUniqueId());
            event.setCancelled(true);
            return;
        }

        TreasureModel model = new TreasureModel(pendingCreation.getId(), pendingCreation.getCommand(), worldName, x, y, z);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                dbManager.upsertTreasure(model.getId(), worldName, x, y, z, model.getCommand());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    cache.put(model);
                    TreasureCreate.removePendingCreation(player.getUniqueId());
                    MessageUtil.send(player, "treasure.created");
                    MessageUtil.send(player, "treasure.created-info", Map.of(
                            "id", model.getId(),
                            "x", String.valueOf(x),
                            "y", String.valueOf(y),
                            "z", String.valueOf(z),
                            "world", worldName
                    ));
                });
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to save treasure " + model.getId() + " to DB: " + ex.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MessageUtil.send(player, "generic.error-saving");
                    TreasureCreate.removePendingCreation(player.getUniqueId());
                });
            }
        });

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRightClickClaimTreasure(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        int x = event.getClickedBlock().getX();
        int y = event.getClickedBlock().getY();
        int z = event.getClickedBlock().getZ();

        TreasureModel treasure = cache.getByPos(worldName, x, y, z).orElse(null);
        if (treasure == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String treasureId = treasure.getId();

        if (cache.getClaimsOfPlayer(uuid).contains(treasureId)) {
            MessageUtil.send(player, "treasure.already-claimed");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean inserted = false;
            try {
                inserted = dbManager.insertClaimIfNotExists(uuid, treasureId);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Insert claim failed: {0}", ex.getMessage());
                return;
            }

            if (!inserted) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtil.send(player, "treasure.already-claimed"));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                cache.addClaim(uuid, treasureId);

                String command = treasure.getCommand() == null ? "" : treasure.getCommand().trim();
                if (command.startsWith("/")) command = command.substring(1);
                if (!command.isBlank()) {
                    command = command.replace("%player%", player.getName());
                    player.getServer().dispatchCommand(player.getServer().getConsoleSender(), command);
                }
                MessageUtil.send(player, "treasure.claimed", Map.of("id", treasureId));
            });
        });

        event.setCancelled(true);

    }

}
