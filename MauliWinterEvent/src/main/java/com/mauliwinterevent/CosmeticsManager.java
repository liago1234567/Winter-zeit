package com.mauliwinterevent;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CosmeticsManager implements Listener {
    private final JavaPlugin plugin;
    private final Storage storage;
    private final Settings settings;

    private final Map<UUID, Inventory> open = new HashMap<>();
    private final Set<UUID> activeSnow = new HashSet<>();
    private final Set<UUID> activeHalo = new HashSet<>();

    public CosmeticsManager(JavaPlugin plugin, Storage storage, Settings settings) {
        this.plugin = plugin;
        this.storage = storage;
        this.settings = settings;
    }

    public void openMenu(Player p) {
        String title = plugin.getConfig().getString("cosmetics.gui_title", "§b❄ Winter Cosmetics ❄");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // For each known cosmetic, show unlocked/locked and active state
        int slot = 11;
        for (Cosmetic c : Cosmetic.values()) {
            boolean unlocked = storage.isCosmeticUnlocked(p.getUniqueId(), c);
            ItemStack it;
            if (unlocked) {
                it = new ItemStack(Material.LIGHT_BLUE_DYE);
            } else {
                it = new ItemStack(Material.GRAY_DYE);
            }
            ItemMeta meta = it.getItemMeta();
            String name = nameOf(c);
            meta.setDisplayName((unlocked ? "§a" : "§7") + name);
            List<String> lore = loreOf(c);
            if (unlocked) {
                boolean active = isActive(p.getUniqueId(), c);
                lore.add("");
                lore.add(active ? "§bAktiv" : "§7Klicke zum Aktivieren");
            } else {
                lore.add("");
                lore.add("§cNicht freigeschaltet");
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
            inv.setItem(slot, it);
            slot += 2;
        }

        open.put(p.getUniqueId(), inv);
        p.openInventory(inv);
    }

    private String nameOf(Cosmetic c) {
        switch (c) {
            case SNOW_TRAIL: return "Schnee-Spur";
            case AURORA_HALO: return "Aurora-Halo";
        }
        return c.name();
    }

    private List<String> loreOf(Cosmetic c) {
        if (c == Cosmetic.SNOW_TRAIL)
            return new ArrayList<>(Arrays.asList("§7Hinterlasse eine Schnee-Partikelspur."));
        if (c == Cosmetic.AURORA_HALO)
            return new ArrayList<>(Arrays.asList("§7Sanfte Partikel um deinen Kopf."));
        return new ArrayList<>();
    }

    public void unlock(Player p, Cosmetic cosmetic) {
        if (cosmetic == null) return;
        if (storage.isCosmeticUnlocked(p.getUniqueId(), cosmetic)) return;
        storage.unlockCosmetic(p.getUniqueId(), cosmetic);
        p.sendMessage("§b[Winter]§7 Neuer Cosmetic freigeschaltet: §f" + nameOf(cosmetic));
    }

    private boolean isActive(UUID id, Cosmetic c) {
        return switch (c) {
            case SNOW_TRAIL -> activeSnow.contains(id);
            case AURORA_HALO -> activeHalo.contains(id);
        };
    }

    private void toggle(UUID id, Cosmetic c) {
        switch (c) {
            case SNOW_TRAIL -> { if (!activeSnow.add(id)) activeSnow.remove(id); }
            case AURORA_HALO -> { if (!activeHalo.add(id)) activeHalo.remove(id); }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (activeSnow.contains(id)) {
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 6, 0.2, 0.05, 0.2, 0.0);
        }
        if (activeHalo.contains(id)) {
            var loc = p.getLocation().add(0, 1.9, 0);
            p.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 0.2, 0.1, 0.2, 0.0);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory inv = open.get(p.getUniqueId());
        if (inv == null) return;
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);
        int slot = e.getRawSlot();
        Cosmetic selected = null;
        if (slot == 11) selected = Cosmetic.SNOW_TRAIL;
        if (slot == 13) selected = Cosmetic.AURORA_HALO;
        if (selected == null) return;

        if (!storage.isCosmeticUnlocked(p.getUniqueId(), selected)) {
            p.sendMessage("§cNicht freigeschaltet.");
            return;
        }

        toggle(p.getUniqueId(), selected);
        p.sendMessage("§7Cosmetic §f" + nameOf(selected) + " §7" + (isActive(p.getUniqueId(), selected) ? "§aaktiviert" : "§cdeaktiviert"));
        // refresh menu
        openMenu(p);
    }
}
