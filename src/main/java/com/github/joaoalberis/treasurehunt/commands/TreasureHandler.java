package com.github.joaoalberis.treasurehunt.commands;

import com.github.joaoalberis.treasurehunt.TreasureCache;
import com.github.joaoalberis.treasurehunt.TreasureHunt;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class TreasureHandler implements CommandExecutor {

    private final Map<String, TreasureBaseCommand> subs = new LinkedHashMap<>();
    private final Plugin plugin;
    private final DatabaseManager dbManager;
    private final TreasureCache cache;

    public TreasureHandler(TreasureHunt treasureHunt, DatabaseManager dbManager, TreasureCache cache) {
        this.plugin = treasureHunt;
        this.dbManager = dbManager;
        this.cache = cache;
        register(new TreasureCreate(cache, plugin));
        register(new TreasureHelp());
        register(new TreasureDelete(dbManager, plugin, cache));
        register(new TreasureList(dbManager, plugin, cache));
        register(new TreasureCompleted(dbManager, plugin, cache));
    }

    private void register(TreasureBaseCommand cmd) {
        subs.put(cmd.name().toLowerCase(Locale.ROOT), cmd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0){
            subs.get("help").execute(sender, label, args);
        }else {
            String arg = args[0].toLowerCase(Locale.ROOT);
            TreasureBaseCommand handler = subs.get(arg);
            if (handler == null){
                subs.get("help").execute(sender, label, new String[0]);
                return true;
            }
            String[] subArgs = (args.length > 1)
                    ? Arrays.copyOfRange(args, 1, args.length)
                    : new String[0];
            handler.execute(sender, label, subArgs);
        }
        return true;
    }

}
