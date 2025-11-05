package com.github.joaoalberis.treasurehunt.commands;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.gui.TreasureGuiManager;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TreasureList implements TreasureBaseCommand{

    private final DatabaseManager dbManager;
    private static final int PAGE_SIZE = 10;
    private final Plugin plugin;
    private final TreasureCache cache;

    public TreasureList(DatabaseManager dbManager, Plugin plugin, TreasureCache cache) {
        this.dbManager = dbManager;
        this.plugin = plugin;
        this.cache = cache;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String usage() {
        return "/tc list";
    }

    @Override
    public String permission() {
        return "albis.treasure.list";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(permission()) && !sender.isOp()) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }

        if (sender instanceof Player player) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Map<String, TreasureModel> map = dbManager.loadAllTreasures();
                    List<TreasureModel> list = new ArrayList<>(map.values());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            TreasureGuiManager.openListGui(player, list, 1));
                } catch (SQLException e) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            MessageUtil.send(player, "generic.error-loading"));
                    plugin.getLogger().log(Level.SEVERE, "SQL error", e);
                }
            });
            return true;
        }

        int page = 1;
        if (args.length >= 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {}
        }

        try {
            Map<String, TreasureModel> treasures = dbManager.loadAllTreasures();
            if (treasures.isEmpty()) {
                MessageUtil.send(sender, "treasure.list-empty");
                return true;
            }

            List<TreasureModel> sorted = treasures.values().stream()
                    .sorted(Comparator.comparing(TreasureModel::getId))
                    .collect(Collectors.toList());

            int total = sorted.size();
            int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
            if (page > totalPages) page = totalPages;

            int start = (page - 1) * PAGE_SIZE;
            int end = Math.min(total, start + PAGE_SIZE);

            MessageUtil.send(sender, "treasure.list-header");

            for (int i = start; i < end; i++) {
                TreasureModel t = sorted.get(i);
                MessageUtil.send(sender, "treasure.list-entry", Map.of(
                        "id", t.getId(),
                        "world", t.getWorld(),
                        "x", String.valueOf(t.getX()),
                        "y", String.valueOf(t.getY()),
                        "z", String.valueOf(t.getZ())
                ));
            }

            MessageUtil.send(sender, "pagination.info", Map.of("page", String.valueOf(page), "pages", String.valueOf(totalPages)));
            if (page < totalPages) {
                MessageUtil.send(sender, "pagination.next", Map.of("next", String.valueOf(page + 1)));
            }

        } catch (SQLException e) {
            MessageUtil.send(sender, "generic.error-loading");
            plugin.getLogger().log(Level.SEVERE, "SQL error", e);
        }
        return true;
    }
}
