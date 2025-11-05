package com.github.joaoalberis.treasurehunt.commands;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.gui.TreasureCompletedGuiManager;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class TreasureCompleted implements TreasureBaseCommand{

    private final DatabaseManager dbManager;
    private final Plugin plugin;
    private final TreasureCache cache;

    public TreasureCompleted(DatabaseManager dbManager, Plugin plugin, TreasureCache cache) {
        this.dbManager = dbManager;
        this.plugin = plugin;
        this.cache = cache;
    }

    @Override
    public String name() {
        return "completed";
    }

    @Override
    public String usage() {
        return "/tc completed <id>";
    }

    @Override
    public String permission() {
        return "albis.treasure.completed";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(sender, "completed.usage");
            return true;
        }
        if (!sender.hasPermission(permission()) && !sender.isOp()){
            MessageUtil.send(sender, "no-permission");
            return true;
        }

        String treasureId = args[0];

        if (sender instanceof Player player) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Map<UUID, Timestamp> claims = dbManager.loadClaimsWithDateForTreasure(treasureId);
                    if (claims == null || claims.isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                MessageUtil.send(sender, "completed.empty")
                        );
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> TreasureCompletedGuiManager.open(player, treasureId, claims, 1));
                } catch (SQLException ex) {
                    Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "generic.error-loading"));
                    plugin.getLogger().log(Level.SEVERE, "SQL error while loading claims with date for treasure " + treasureId, ex);
                }
            });
            return true;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {}
        }

        try {
            Map<UUID, Timestamp> claims = dbManager.loadClaimsWithDateForTreasure(treasureId);
            if (claims == null || claims.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtil.send(sender, "completed.empty")
                );
                return true;
            }


            List<Map.Entry<UUID, Timestamp>> entries = new ArrayList<>(claims.entrySet());
            entries.sort(Comparator.comparing(e -> e.getValue()));

            final int PAGE_SIZE = 10;
            int total = entries.size();
            int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page > pages) page = pages;

            int start = (page - 1) * PAGE_SIZE;
            int end = Math.min(total, start + PAGE_SIZE);

            MessageUtil.send(sender, "completed.header", Map.of("count", String.valueOf(total), "page", String.valueOf(page), "pages", String.valueOf(pages)));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (int i = start; i < end; i++) {
                Map.Entry<UUID, Timestamp> entry = entries.get(i);
                UUID uuid = entry.getKey();
                Timestamp ts = entry.getValue();
                String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                        .orElse(MessageUtil.getRaw("generic.unknown"));
                String formatted = ts == null ? "-" : sdf.format(ts);

                MessageUtil.send(sender, "completed.entry", Map.of(
                        "name", name,
                        "uuid", uuid.toString(),
                        "date", formatted
                ));
            }
            MessageUtil.send(sender, "completed.usage-hint", Map.of("label", label, "id", treasureId));

        } catch (SQLException ex) {
            MessageUtil.send(sender, "generic.error-loading");
            plugin.getLogger().log(Level.SEVERE, "SQL error", ex);

        }

        return true;
    }
}
