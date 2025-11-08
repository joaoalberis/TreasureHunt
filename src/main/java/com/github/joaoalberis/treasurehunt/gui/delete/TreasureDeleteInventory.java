package com.github.joaoalberis.treasurehunt.gui.delete;

import com.github.joaoalberis.treasurehunt.gui.list.TreasureListInventory;
import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TreasureDeleteInventory implements InventoryHolder {

    private final List<TreasureModel> treasures;
    private int page;
    private final Inventory inventory;
    public final int PAGE_SIZE = 28;
    private final Plugin plugin;
    public static final String TITLE_PREFIX = ChatColor.DARK_GREEN + "Treasure Delete - ";


    public TreasureDeleteInventory(Plugin plugin, List<TreasureModel> treasures, int page) {
        this.plugin = plugin;
        this.treasures = treasures;
        this.page = page;
        this.inventory = plugin.getServer().createInventory(this, 54, TITLE_PREFIX + "(" + page + ")");
        buildInventory();
    }

    private void buildInventory() {
        List<TreasureModel> sorted = treasures.stream()
                .sorted(Comparator.comparing(TreasureModel::getId))
                .collect(Collectors.toList());

        int pages = Math.max(1, (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 1) page = 1;
        if (page > pages) page = pages;

        int start = (page - 1) * PAGE_SIZE;
        int slot = 10;
        for (int i = start; i < Math.min(sorted.size(), start + PAGE_SIZE); i++) {
            TreasureModel t = sorted.get(i);

            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Treasure #" + t.getId());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + t.getWorld());
            lore.add(ChatColor.GRAY + "Coord: " + ChatColor.WHITE + t.getX() + ", " + t.getY() + ", " + t.getZ());
            lore.add(ChatColor.GRAY + "Command: " + ChatColor.WHITE + (t.getCommand().isEmpty() ? "—" : t.getCommand()));
            lore.add("");
            lore.add(ChatColor.GREEN + "Left Click " + ChatColor.GRAY + "- Preview/Confirm");
            lore.add(ChatColor.RED + "Right Click " + ChatColor.GRAY + "- Direct confirm");
            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot, item);
            slot++;
            if ((slot % 9) == 8) slot += 2;
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(ChatColor.YELLOW + "< Previous");
        prev.setItemMeta(pm);
        if (page > 1) inventory.setItem(45, prev);

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pim = pageInfo.getItemMeta();
        pim.setDisplayName(ChatColor.GRAY + "Page " + page + "/" + pages);
        pageInfo.setItemMeta(pim);
        inventory.setItem(49, pageInfo);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(ChatColor.YELLOW + "Next >");
        next.setItemMeta(nm);
        if (page < pages) inventory.setItem(53, next);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        int[] borderSlots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52};
        for (int s : borderSlots) {
            if (inventory.getItem(s) == null) inventory.setItem(s, border);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public List<TreasureModel> getTreasures() { return treasures; }
    public int getPage() { return page; }

    public TreasureListInventory newPage(int page){
        return new TreasureListInventory(plugin, this.getTreasures(), page);
    }

    public void open(Player player){
        player.openInventory(this.getInventory());
    }

}
