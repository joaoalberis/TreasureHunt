package com.github.joaoalberis.treasurehunt.commands;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.gui.delete.TreasureDeleteConfirmInventory;
import com.github.joaoalberis.treasurehunt.gui.delete.TreasureDeleteInventory;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class TreasureDelete implements TreasureBaseCommand{

    private final DatabaseManager dbManager;
    private final Plugin plugin;
    private TreasureCache cache;

    public TreasureDelete(DatabaseManager dbManager, Plugin plugin, TreasureCache cache) {
        this.dbManager = dbManager;
        this.plugin = plugin;
        this.cache = cache;
    }

    @Override
    public String name() {
        return "delete";
    }

    @Override
    public String usage() {
        return "/tc delete <id>";
    }

    @Override
    public String permission() {
        return "albis.treasure.delete";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(permission()) && !sender.isOp()){
            MessageUtil.send(sender, "no-permission");
            return true;
        }
        try {
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    MessageUtil.send(sender, "only-player");
                    return true;
                }

                Player player = (Player) sender;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        Map<String, TreasureModel> map = dbManager.loadAllTreasures();
                        List<TreasureModel> list = new ArrayList<>(map.values());

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            TreasureDeleteInventory inventory = new TreasureDeleteInventory(plugin, list, 1);
                            player.openInventory(inventory.getInventory());
                        });
                    } catch (SQLException e) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                MessageUtil.send(player, "generic.error-loading"));
                        plugin.getLogger().log(Level.SEVERE, "SQL error", e);

                    }
                });
                return true;
            }

            String id = args[0];

            if (sender instanceof Player player) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        Map<String, TreasureModel> all = dbManager.loadAllTreasures();
                        TreasureModel model = all.values().stream()
                                .filter(t -> t.getId().equalsIgnoreCase(id))
                                .findFirst()
                                .orElse(null);

                        if (model == null) {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    MessageUtil.send(player, "treasure.not-found", Map.of("id", id))
                            );
                            return;
                        }


                        Bukkit.getScheduler().runTask(plugin, () -> {
                            TreasureDeleteConfirmInventory inventory = new TreasureDeleteConfirmInventory(plugin, model);
                            player.openInventory(inventory.getInventory());
                        });

                    } catch (SQLException e) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                MessageUtil.send(player, "generic.error-loading"));
                        plugin.getLogger().log(Level.SEVERE, "SQL error", e);

                    }
                });
                return true;
            }

            boolean removed = dbManager.deleteById(id);
            if (!removed) {
                MessageUtil.send(sender, "treasure.not-found", Map.of("id", id));
                return true;
            }

            cache.getById(id).ifPresent(cache::remove);
            MessageUtil.send(sender, "treasure.deleted", Map.of("id", id));

        } catch (SQLException e) {
            MessageUtil.send(sender, "generic.error-loading");
            plugin.getLogger().log(Level.SEVERE, "SQL error", e);
        }

        return true;
    }
}
