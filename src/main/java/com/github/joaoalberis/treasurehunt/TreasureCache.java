package com.github.joaoalberis.treasurehunt;

import com.github.joaoalberis.treasurehunt.database.DatabaseManager;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TreasureCache {

    private final Map<String, TreasureModel> treasuresById = new ConcurrentHashMap<>();
    private final Map<String, TreasureModel> treasuresByPos = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> claimedByPlayer = new ConcurrentHashMap<>();

    private final DatabaseManager dbManager;
    private final Plugin plugin;


    public TreasureCache(DatabaseManager dbManager, Plugin plugin) {
        this.dbManager = dbManager;
        this.plugin = plugin;
    }

    public void loadClaims(Map<UUID, Set<String>> claims) {
        claimedByPlayer.clear();
        if (claims == null) return;
        for (Map.Entry<UUID, Set<String>> e : claims.entrySet()) {
            Set<String> concurrentSet = ConcurrentHashMap.newKeySet();
            concurrentSet.addAll(e.getValue());
            claimedByPlayer.put(e.getKey(), concurrentSet);
        }
    }

    public void addClaim(UUID player, String treasureId) {
        claimedByPlayer.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(treasureId);
    }

    public void removeClaim(UUID player, String treasureId) {
        Set<String> set = claimedByPlayer.get(player);
        if (set != null) set.remove(treasureId);
    }

    public void removeClaimsForTreasure(String treasureId) {
        for (Set<String> set : claimedByPlayer.values()) {
            set.remove(treasureId);
        }
    }

    public Set<String> getClaimsOfPlayer(UUID player) {
        return Collections.unmodifiableSet(claimedByPlayer.getOrDefault(player, Collections.emptySet()));
    }

    public Set<UUID> getPlayersForTreasure(String treasureId) {
        return claimedByPlayer.entrySet().stream()
                .filter(e -> e.getValue().contains(treasureId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String idKey(String id) {
        return id == null ? null : id.toLowerCase(Locale.ROOT);
    }

    public void loadAll() {
        plugin.getLogger().info("Loading treasures from database...");
        try {
            Map<String, TreasureModel> all = dbManager.loadAllTreasures();
            treasuresById.clear();
            treasuresByPos.clear();

            for (TreasureModel t : all.values()) {
                String idKey = idKey(t.getId());
                treasuresById.put(idKey, t);
                String posKey = DatabaseManager.posKey(t.getWorld(), t.getX(), t.getY(), t.getZ());
                treasuresByPos.put(posKey, t);
            }
            plugin.getLogger().info("Loaded " + treasuresById.size() + " treasures into cache.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load treasures into cache", e);
        }
    }

    public void put(TreasureModel treasure) {
        treasuresById.put(idKey(treasure.getId()), treasure);
        treasuresByPos.put(DatabaseManager.posKey(treasure.getWorld(), treasure.getX(), treasure.getY(), treasure.getZ()), treasure);
    }

    public void remove(TreasureModel treasure) {
        treasuresById.remove(idKey(treasure.getId()));
        treasuresByPos.entrySet().removeIf(e -> e.getValue().getId().equalsIgnoreCase(treasure.getId()));
    }

    public Optional<TreasureModel> getById(String id) {
        return Optional.ofNullable(treasuresById.get(idKey(id)));
    }

    public Optional<TreasureModel> getByPos(String world, int x, int y, int z) {
        return Optional.ofNullable(treasuresByPos.get(DatabaseManager.posKey(world, x, y, z)));
    }

    public Collection<TreasureModel> getAll() {
        return Collections.unmodifiableCollection(treasuresById.values());
    }

    public boolean isEmpty() {
        return treasuresById.isEmpty();
    }
}
