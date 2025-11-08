package com.mauliwinterevent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.*;
import com.mauliwinterevent.Cosmetic;

public class AdventManager implements Listener {
    private final MauliWinterEventPlugin plugin;
    private final Storage storage;
    private Settings settings;
    private final Map<UUID, Inventory> open = new HashMap<>();
    private BossBar bar;

    public AdventManager(MauliWinterEventPlugin plugin, Storage storage, Settings settings) {
        this.plugin = plugin;
        this.storage = storage;
        this.settings = settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    private boolean isWithinEvent(LocalDate now) {
        MonthDay md = MonthDay.from(now);
        MonthDay s = settings.start();
        MonthDay e = settings.end();
        return (md.compareTo(s) >= 0 && md.compareTo(e) <= 0);
    }

    public void openCalendar(Player p) {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        if (!isWithinEvent(now)) {
            p.sendMessage(settings.msgPrefix() + settings.msgNotInEvent());
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, settings.title());
        Set<Integer> claimed = storage.getClaimedDays(p.getUniqueId());
        int currentDay = (now.getMonthValue() == 12) ? now.getDayOfMonth() : 31; // safeguard

        // layout: days 1..31 across the grid (skip borders if desired), here simply fill from slot 10
        int[] slots = calendarSlots();
        for (int day = 1; day <= 31; day++) {
            int slot = slots[day-1];
            boolean isClaimed = claimed.contains(day);
            boolean isAvailable = day <= currentDay || settings.catchup();

            ItemStack it;
            if (isClaimed) {
                it = new ItemStack(Material.GREEN_CONCRETE);
                setName(it, settings.claimedPrefix() + day);
                setLore(it, List.of("§7Abgeholt."));
            } else if (isAvailable) {
                it = new ItemStack(Material.CHEST);
                setName(it, settings.availablePrefix() + day);
                setLore(it, List.of("§7Klicke, um dein Geschenk zu erhalten."));
            } else {
                it = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                setName(it, settings.lockedPrefix() + day);
                setLore(it, List.of("§7Noch nicht verfügbar."));
            }
            inv.setItem(slot, it);
        }

        p.openInventory(inv);
        open.put(p.getUniqueId(), inv);
    }

    private void setName(ItemStack is, String name) {
        ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(name);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        is.setItemMeta(meta);
    }

    private void setLore(ItemStack is, List<String> lore) {
        ItemMeta meta = is.getItemMeta();
        meta.setLore(lore);
        is.setItemMeta(meta);
    }

    // nice spread layout (31 items) inside 54 slots
    private int[] calendarSlots() {
        return new int[]{
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,
                37,38,39,40,41,42,43,
                46,47,48,49
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory inv = open.get(p.getUniqueId());
        if (inv == null) return;
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        // Determine clicked day by matching slot to calendarSlots
        int raw = e.getRawSlot();
        int day = slotToDay(raw);
        if (day == -1) return;

        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        if (!isWithinEvent(now)) {
            p.sendMessage(settings.msgPrefix() + settings.msgNotInEvent());
            return;
        }

        if (storage.hasClaimedDay(p.getUniqueId(), day)) {
            p.sendMessage(settings.msgPrefix() + settings.msgAlreadyClaimed());
            return;
        }

        if (settings.dailyOneClaim()) {
            if (storage.hasDailyClaim(p.getUniqueId(), now)) {
                p.sendMessage(settings.msgPrefix() + settings.msgDailyLimit());
                return;
            }
        }

        int currentDay = (now.getMonthValue() == 12) ? now.getDayOfMonth() : 31;
        boolean allowed = settings.catchup() ? (day <= 31) : (day <= currentDay);
        if (!allowed) {
            p.sendMessage(settings.msgPrefix() + settings.msgNotInEvent());
            return;
        }

        // Dispatch rewards
        List<String> cmds = settings.commandsFor(LocalDate.of(now.getYear(), 12, day));
        for (String c : cmds) {
            String run = c.replace("%player%", p.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
        }

        // cosmetics unlocks
        for (Cosmetic c : settings.cosmeticsFor(java.time.LocalDate.of(now.getYear(), 12, day))) {
            // Unlock cosmetic via storage
            storage.unlockCosmetic(p.getUniqueId(), c);
            p.sendMessage(settings.msgPrefix() + "Neuer Cosmetic freigeschaltet: §f" + c.name());
        }

        // store claim & daily
        storage.addClaim(p.getUniqueId(), day, now);
        if (settings.dailyOneClaim()) storage.setDailyClaim(p.getUniqueId(), now);

        // feedback
        p.sendTitle("§bGeschenk abgeholt!", "§7Tag " + day + " §8| §fViel Spaß!", 10, 40, 10);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        showBossbar(p);

        p.closeInventory();
        p.sendMessage(settings.msgPrefix() + String.format(settings.msgClaimedSuccess(), day));
    }

    private int slotToDay(int slot) {
        int[] slots = calendarSlots();
        for (int i = 0; i < slots.length; i++) if (slots[i] == slot) return i + 1;
        return -1;
    }

    private void showBossbar(Player p) {
        if (bar != null) bar.removeAll();
        bar = Bukkit.createBossBar(settings.bossbarTitle(), BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(p);
        bar.setProgress(1.0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bar != null) {
                bar.removeAll();
                bar = null;
            }
        }, 60L);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        open.remove(e.getPlayer().getUniqueId());
    }
}
