package com.github.joaoalberis.treasurehunt.gui;

import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TreasureGuiManager {

    public enum Mode { DELETE_SELECT, CONFIRM_DELETE, VIEW_LIST }

    public static final int PAGE_SIZE = 28;
    public static final String TITLE_PREFIX_DELETE = ChatColor.DARK_GREEN + "Treasure - Delete";
    public static final String TITLE_PREFIX_LIST = ChatColor.DARK_BLUE + "Treasure - List";

    public static class GuiContext {
        public final List<TreasureModel> treasures;
        public int page;
        public Mode mode;
        public TreasureModel selected;

        public GuiContext(List<TreasureModel> treasures, int page, Mode mode) {
            this.treasures = treasures;
            this.page = page;
            this.mode = mode;
        }
    }

    private static final Map<UUID, GuiContext> contexts = new ConcurrentHashMap<>();

    public static Optional<GuiContext> getContext(UUID player) {
        return Optional.ofNullable(contexts.get(player));
    }

    public static void removeContext(UUID player) {
        contexts.remove(player);
    }

    /* ------------------- LIST GUI (visualização apenas) ------------------- */
    public static void openListGui(Player player, List<TreasureModel> treasures, int page) {
        List<TreasureModel> sorted = treasures.stream()
                .sorted(Comparator.comparing(TreasureModel::getId))
                .collect(Collectors.toList());

        int pages = Math.max(1, (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 1) page = 1;
        if (page > pages) page = pages;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX_LIST + " - Page " + page + "/" + pages);

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
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slot++;
            if ((slot % 9) == 8) slot += 2;
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(ChatColor.YELLOW + "< Previous");
        prev.setItemMeta(pm);
        if (page > 1) inv.setItem(45, prev);

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pim = pageInfo.getItemMeta();
        pim.setDisplayName(ChatColor.GRAY + "Page " + page + "/" + pages);
        pageInfo.setItemMeta(pim);
        inv.setItem(49, pageInfo);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(ChatColor.YELLOW + "Next >");
        next.setItemMeta(nm);
        if (page < pages) inv.setItem(53, next);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        int[] borderSlots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52};
        for (int s : borderSlots) {
            if (inv.getItem(s) == null) inv.setItem(s, border);
        }

        GuiContext ctx = new GuiContext(sorted, page, Mode.VIEW_LIST);
        contexts.put(player.getUniqueId(), ctx);

        player.openInventory(inv);
    }

    public static void openDeleteSelection(Player player, List<TreasureModel> treasures, int page) {
        List<TreasureModel> sorted = treasures.stream()
                .sorted(Comparator.comparing(TreasureModel::getId))
                .collect(Collectors.toList());

        int pages = Math.max(1, (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 1) page = 1;
        if (page > pages) page = pages;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX_DELETE + " - Page " + page + "/" + pages);

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

            inv.setItem(slot, item);
            slot++;
            if ((slot % 9) == 8) slot += 2;
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(ChatColor.YELLOW + "< Previous");
        prev.setItemMeta(pm);
        if (page > 1) inv.setItem(45, prev);

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pim = pageInfo.getItemMeta();
        pim.setDisplayName(ChatColor.GRAY + "Page " + page + "/" + pages);
        pageInfo.setItemMeta(pim);
        inv.setItem(49, pageInfo);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(ChatColor.YELLOW + "Next >");
        next.setItemMeta(nm);
        if (page < pages) inv.setItem(53, next);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        int[] borderSlots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52};
        for (int s : borderSlots) {
            if (inv.getItem(s) == null) inv.setItem(s, border);
        }

        GuiContext ctx = new GuiContext(sorted, page, Mode.DELETE_SELECT);
        contexts.put(player.getUniqueId(), ctx);

        player.openInventory(inv);
    }

    public static void openConfirmGui(Player player, TreasureModel t) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Confirm deletion - #" + t.getId());

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Delete Treasure #" + t.getId());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + t.getWorld());
        lore.add(ChatColor.GRAY + "Coord: " + ChatColor.WHITE + t.getX() + ", " + t.getY() + ", " + t.getZ());
        lore.add("");
        lore.add(ChatColor.RED + "This will remove the treasure from the database.");
        im.setLore(lore);
        info.setItemMeta(im);
        inv.setItem(13, info);

        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName(ChatColor.GREEN + "Confirm Delete");
        confirm.setItemMeta(cm);
        inv.setItem(11, confirm);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta canm = cancel.getItemMeta();
        canm.setDisplayName(ChatColor.RED + "Cancel");
        cancel.setItemMeta(canm);
        inv.setItem(15, cancel);

        GuiContext ctx = new GuiContext(Collections.singletonList(t), 1, Mode.CONFIRM_DELETE);
        ctx.selected = t;
        contexts.put(player.getUniqueId(), ctx);

        player.openInventory(inv);
    }
}
