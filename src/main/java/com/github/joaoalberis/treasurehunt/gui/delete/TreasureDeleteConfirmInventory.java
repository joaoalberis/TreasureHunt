package com.github.joaoalberis.treasurehunt.gui.delete;

import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class TreasureDeleteConfirmInventory implements InventoryHolder {

    private final TreasureModel treasure;
    private final Inventory inventory;
    private final Plugin plugin;
    public static final String TITLE_PREFIX = ChatColor.DARK_GREEN + "Confirm deletion - #";


    public TreasureDeleteConfirmInventory(Plugin plugin, TreasureModel treasure) {
        this.treasure = treasure;
        this.plugin = plugin;
        this.inventory = plugin.getServer().createInventory(this, 27, TITLE_PREFIX + treasure.getId());
        buildInventory();
    }

    private void buildInventory() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Delete Treasure #" + treasure.getId());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + treasure.getWorld());
        lore.add(ChatColor.GRAY + "Coord: " + ChatColor.WHITE + treasure.getX() + ", " + treasure.getY() + ", " + treasure.getZ());
        lore.add("");
        lore.add(ChatColor.RED + "This will remove the treasure from the database.");
        im.setLore(lore);
        info.setItemMeta(im);
        inventory.setItem(13, info);

        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName(ChatColor.GREEN + "Confirm Delete");
        confirm.setItemMeta(cm);
        inventory.setItem(11, confirm);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta canm = cancel.getItemMeta();
        canm.setDisplayName(ChatColor.RED + "Cancel");
        cancel.setItemMeta(canm);
        inventory.setItem(15, cancel);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public TreasureModel getTreasure() {
        return treasure;
    }
}
