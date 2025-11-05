package com.github.joaoalberis.treasurehunt.commands;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.models.PendingCreation;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreasureCreate implements TreasureBaseCommand {
    public static final Map<UUID, PendingCreation> pendingCreations = new ConcurrentHashMap<>();
    private final TreasureCache cache;
    private final Plugin plugin;

    public TreasureCreate(TreasureCache cache, Plugin plugin) {
        this.cache = cache;
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public String usage() {
        return "/tc create <id> <command>";
    }

    @Override
    public String permission() {
        return "albis.treasure.create";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "only-player");
            return true;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "create.usage");
            return true;
        }

        if (!player.hasPermission(permission()) && !player.isOp()){
            MessageUtil.send(player, "no-permission");
            return true;
        }

        String id = args[0];

        String command = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (cache.getById(id).isPresent()) {
            MessageUtil.send(player, "treasure.exists");
            return true;
        }
        pendingCreations.put(player.getUniqueId(), new PendingCreation(id, command));

        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                () -> pendingCreations.remove(uuid), 20L * 120);


        MessageUtil.send(player, "treasure.create-mode");
        MessageUtil.send(player, "treasure.created-info-id", Map.of("id", id));
        MessageUtil.send(player, "treasure.created-info-command", Map.of("command", command));
        return true;
    }
}
