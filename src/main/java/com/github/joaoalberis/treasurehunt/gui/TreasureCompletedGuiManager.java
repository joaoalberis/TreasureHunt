package com.github.joaoalberis.treasurehunt.gui;

import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreasureCompletedGuiManager {

    public static final int PAGE_SIZE = 45;
    private static final String TITLE_PREFIX = ChatColor.DARK_AQUA + "Players who claimed";

    public static class CompletedContext {
        public final Map<UUID, Timestamp> claims;
        public int page;
        public final String treasureId;

        public CompletedContext(Map<UUID, Timestamp> claims, String treasureId, int page) {
            this.claims = claims;
            this.treasureId = treasureId;
            this.page = page;
        }
    }

    private static final Map<UUID, CompletedContext> contexts = new ConcurrentHashMap<>();

    public static Optional<CompletedContext> getContext(UUID playerId) {
        return Optional.ofNullable(contexts.get(playerId));
    }

    public static void removeContext(UUID playerId) {
        contexts.remove(playerId);
    }

    public static void open(Player viewer, String treasureId, Map<UUID, Timestamp> claims, int page) {
        if (claims == null || claims.isEmpty()) {
            MessageUtil.send(viewer, "completed.empty");
            return;
        }

        List<Map.Entry<UUID, Timestamp>> entries = new ArrayList<>(claims.entrySet());
        int total = entries.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (page < 1) page = 1;
        if (page > pages) page = pages;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + " - " + treasureId + " (" + page + "/" + pages + ")");

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(total, start + PAGE_SIZE);
        int slot = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Timestamp> entry = entries.get(i);
            UUID uuid = entry.getKey();
            Timestamp timestamp = entry.getValue();
            OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skull = (SkullMeta) head.getItemMeta();
            try {
                skull.setOwningPlayer(off);
            } catch (Throwable ignored) { }
            skull.setDisplayName(ChatColor.GOLD + (off.getName() != null ? off.getName() : MessageUtil.getRaw("generic.unknown")));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + uuid.toString());
            lore.add(ChatColor.GRAY + "Status: " + (off.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"));
            lore.add(ChatColor.GRAY + "Claimed at: " + ChatColor.WHITE + sdf.format(timestamp));

            skull.setLore(lore);
            head.setItemMeta(skull);

            inv.setItem(slot++, head);
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(ChatColor.YELLOW + "< Previous");
        prev.setItemMeta(pm);
        if (page > 1) inv.setItem(45, prev);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GRAY + "Page " + page + "/" + pages);
        info.setItemMeta(im);
        inv.setItem(49, info);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(ChatColor.YELLOW + "Next >");
        next.setItemMeta(nm);
        if (page < pages) inv.setItem(53, next);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        for (int i = 46; i <= 52; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }

        CompletedContext ctx = new CompletedContext(claims, treasureId, page);
        contexts.put(viewer.getUniqueId(), ctx);

        viewer.openInventory(inv);
    }
}
