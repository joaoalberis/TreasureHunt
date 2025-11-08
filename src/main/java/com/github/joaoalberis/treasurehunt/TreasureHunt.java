package com.github.joaoalberis.treasurehunt;

import com.github.joaoalberis.treasurehunt.commands.TreasureHandler;
import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.events.RightClickTreasure;
import com.github.joaoalberis.treasurehunt.gui.completed.TreasureCompletedListener;
import com.github.joaoalberis.treasurehunt.gui.delete.TreasureDeleteConfirmListener;
import com.github.joaoalberis.treasurehunt.gui.delete.TreasureDeleteListener;
import com.github.joaoalberis.treasurehunt.gui.list.TreasureListListener;
import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class TreasureHunt extends JavaPlugin {

    DatabaseManager dbManager;
    private TreasureCache cache;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        MessageUtil.init(this);
        dbManager = new DatabaseManager(this);
        try {
            dbManager.connect();
            cache = new TreasureCache(dbManager, this);
            cache.loadAll();
            Map<UUID, Set<String>> allClaims = dbManager.loadAllClaims();
            cache.loadClaims(allClaims);
        } catch (SQLException e) {
            getLogger().severe(MessageUtil.getRaw("command-error"));
            this.getLogger().log(Level.SEVERE, "SQL error", e);

            getServer().getPluginManager().disablePlugin(this);
        }
        registerCommands();
        registerEvents();
        getLogger().info(MessageUtil.getRaw("startup.success"));
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new RightClickTreasure(this, dbManager, cache), this);
        getServer().getPluginManager().registerEvents(new TreasureCompletedListener(dbManager, this, cache), this);
        getServer().getPluginManager().registerEvents(new TreasureListListener(dbManager, this, cache), this);
        getServer().getPluginManager().registerEvents(new TreasureDeleteListener(dbManager, this, cache), this);
        getServer().getPluginManager().registerEvents(new TreasureDeleteConfirmListener(dbManager, this, cache), this);
    }

    private void registerCommands() {
        getCommand("treasure").setExecutor(new TreasureHandler(this, dbManager, cache));
    }

    @Override
    public void onDisable() {
        if (dbManager != null) {
            dbManager.close();
            getLogger().info(MessageUtil.getRaw("database.close"));
        }
    }
}
