package com.github.joaoalberis.treasurehunt.gui.completed;

import com.github.joaoalberis.treasurehunt.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class TreasureCompletedInventory implements InventoryHolder {

    public final int PAGE_SIZE = 45;
    private final String TITLE_PREFIX = ChatColor.DARK_AQUA + "Players who claimed";

    private final Inventory inventory;
    private final String treasureId;
    private final Map<UUID, Timestamp> claims;
    private int page;


    public TreasureCompletedInventory(Plugin plugin, String treasureId, Map<UUID, Timestamp> claims, int page) {
        this.treasureId = treasureId;
        this.claims = claims == null ? Collections.emptyMap() : new LinkedHashMap<>(claims);
        this.page = Math.max(1, page);
        String title = TITLE_PREFIX + " - " + treasureId + " (" + this.page + ")";
        this.inventory = plugin.getServer().createInventory(this, 54, title);
        buildInventory();
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public String getTreasureId() {
        return treasureId;
    }

    public Map<UUID, Timestamp> getClaims() {
        return Collections.unmodifiableMap(claims);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int newPage) {
        this.page = newPage;
    }

    private void buildInventory() {
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, null);

        if (claims.isEmpty()) {
            ItemStack info = new ItemStack(Material.PAPER);
            ItemMeta im = info.getItemMeta();
            im.setDisplayName(ChatColor.GRAY + "No claims found");
            info.setItemMeta(im);
            inventory.setItem(13, info);
            return;
        }

        List<Map.Entry<UUID, Timestamp>> entries = new ArrayList<>(claims.entrySet());
        int total = entries.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (page > pages) page = pages;

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(total, start + PAGE_SIZE);
        int slot = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Timestamp> entry = entries.get(i);
            UUID uuid = entry.getKey();
            Timestamp ts = entry.getValue();
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
            lore.add(ChatColor.GRAY + "Claimed at: " + ChatColor.WHITE + (ts == null ? "-" : sdf.format(ts)));
            skull.setLore(lore);
            head.setItemMeta(skull);

            inventory.setItem(slot++, head);
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(ChatColor.YELLOW + "< Previous");
        prev.setItemMeta(pm);
        if (page > 1) inventory.setItem(45, prev);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GRAY + "Page " + page + "/" + pages);
        info.setItemMeta(im);
        inventory.setItem(49, info);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(ChatColor.YELLOW + "Next >");
        next.setItemMeta(nm);
        if (page < pages) inventory.setItem(53, next);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        int[] borderSlots = {46,47,48,50,51,52};
        for (int s : borderSlots) {
            if (inventory.getItem(s) == null) inventory.setItem(s, border);
        }
    }

    public TreasureCompletedInventory newPage(Plugin plugin, int newPage) {
        return new TreasureCompletedInventory(plugin, this.treasureId, this.claims, newPage);
    }

    public void open(Player player) {
        player.openInventory(this.inventory);
    }
}
