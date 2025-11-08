package com.mauliwinterevent;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MauliWinterEventPlugin extends JavaPlugin {

    private Storage storage;
    private AdventManager adventManager;
    private Settings settings;
    private CosmeticsManager cosmetics;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new Settings(this);
        this.storage = new Storage(this);
        this.storage.init();
        this.adventManager = new AdventManager(this, storage, settings);
        this.cosmetics = new CosmeticsManager(this, storage, settings);

        getServer().getPluginManager().registerEvents(adventManager, this);
        getServer().getPluginManager().registerEvents(cosmetics, this);

        getLogger().info("MauliWinterEvent aktiviert.");
    }

    @Override
    public void onDisable() {
        try { storage.close(); } catch (Exception ignored) {}
        getLogger().info("MauliWinterEvent deaktiviert.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("advent")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Nur Spieler.");
                return true;
            }
            if (!p.hasPermission("mauliwinter.advent")) {
                p.sendMessage(settings.msgPrefix() + "Keine Berechtigung.");
                return true;
            }
            adventManager.openCalendar(p);
            return true;
        }


        if (cmd.getName().equalsIgnoreCase("weihnachtscosmetics")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Nur Spieler.");
                return true;
            }
            if (!p.hasPermission("mauliwinter.cosmetics")) {
                p.sendMessage(settings.msgPrefix() + "Keine Berechtigung.");
                return true;
            }
            cosmetics.openMenu(p);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("winterevent")) {
            if (!sender.hasPermission("mauliwinter.admin")) {
                sender.sendMessage(settings.msgPrefix() + "Keine Berechtigung.");
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                this.settings = new Settings(this);
                this.adventManager.setSettings(settings);
                sender.sendMessage(settings.msgPrefix() + settings.msgReload());
                return true;
            }
            sender.sendMessage(settings.msgPrefix() + "Benutzung: /winterevent reload");
            return true;
        }

        return false;
    }

    public Settings getSettings() { return settings; }
}
