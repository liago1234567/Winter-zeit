package com.mauliwinterevent;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Settings {
    private final JavaPlugin plugin;
    private final DateTimeFormatter mmdd = DateTimeFormatter.ofPattern("MM-dd");

    private final MonthDay start;
    private final MonthDay end;
    private final boolean catchup;
    private final boolean dailyOneClaim;
    private final String title;
    private final String bossbarTitle;
    private final String claimedPrefix;
    private final String availablePrefix;
    private final String lockedPrefix;
    private final String euroSymbol;

    private final Map<String, List<String>> specialDayCommands;
    private final java.util.Map<String, java.util.List<Cosmetic>> specialDayCosmetics = new java.util.HashMap<>();
    private final java.util.List<Cosmetic> defaultCosmetics = new java.util.ArrayList<>();

    private final List<String> defaultCommands;

    // messages
    private final String prefix;
    private final String notInEvent;
    private final String alreadyClaimed;
    private final String dailyLimit;
    private final String claimedSuccess;
    private final String reload;

    public Settings(JavaPlugin plugin) {
        this.plugin = plugin;
        var conf = plugin.getConfig();

        this.start = MonthDay.parse(conf.getString("event.start", "12-01"), mmdd);
        this.end   = MonthDay.parse(conf.getString("event.end", "12-31"), mmdd);
        this.catchup = conf.getBoolean("event.catchup", true);
        this.dailyOneClaim = conf.getBoolean("event.daily_one_claim", true);

        this.title = conf.getString("ui.title", "§b❄ Mauli Adventskalender ❄");
        this.bossbarTitle = conf.getString("ui.bossbar_title", "§bFrohe Winterzeit!");
        this.claimedPrefix = conf.getString("ui.claimed_name_prefix", "§7[✓] §fTag ");
        this.availablePrefix = conf.getString("ui.available_name_prefix", "§aTag ");
        this.lockedPrefix = conf.getString("ui.locked_name_prefix", "§cTag ");
        this.euroSymbol = conf.getString("ui.euro_symbol", "€");

        this.prefix = conf.getString("messages.prefix", "§b[Winter]§7 ");
        this.notInEvent = conf.getString("messages.not_in_event", "Der Adventskalender ist nur vom 01.12. bis 31.12. verfügbar.");
        this.alreadyClaimed = conf.getString("messages.already_claimed_day", "Du hast diesen Tag bereits abgeholt.");
        this.dailyLimit = conf.getString("messages.daily_limit_reached", "Du hast heute bereits ein Geschenk eingelöst. Komm morgen wieder!");
        this.claimedSuccess = conf.getString("messages.claimed_success", "Geschenk für den §b%s. Dezember §7erfolgreich abgeholt!");
        this.reload = conf.getString("messages.reload", "Konfiguration neu geladen.");

        this.specialDayCommands = new HashMap<>();
        ConfigurationSection sd = conf.getConfigurationSection("rewards.special_days");
        if (sd != null) {
            for (String k : sd.getKeys(false)) {
                List<String> cmds = sd.getStringList("rewards.special_days." + k + ".commands");
                specialDayCommands.put(k, cmds);
            }
        }
        this.defaultCommands = conf.getStringList("rewards.default.commands");
        // Cosmetics default and special
        for (String c : conf.getStringList("rewards.default.cosmetics")) {
            Cosmetic cs = Cosmetic.from(c);
            if (cs != null) defaultCosmetics.add(cs);
        }
        ConfigurationSection csd = conf.getConfigurationSection("rewards.special_days");
        if (csd != null) {
            for (String k : csd.getKeys(false)) {
                java.util.List<String> raw = conf.getStringList("rewards.special_days." + k + ".cosmetics");
                java.util.List<Cosmetic> list = new java.util.ArrayList<>();
                for (String c : raw) {
                    Cosmetic cs = Cosmetic.from(c);
                    if (cs != null) list.add(cs);
                }
                specialDayCosmetics.put(k, list);
            }
        }

    }

    public MonthDay start() { return start; }
    public MonthDay end() { return end; }
    public boolean catchup() { return catchup; }
    public boolean dailyOneClaim() { return dailyOneClaim; }
    public String title() { return title; }
    public String bossbarTitle() { return bossbarTitle; }
    public String claimedPrefix() { return claimedPrefix; }
    public String availablePrefix() { return availablePrefix; }
    public String lockedPrefix() { return lockedPrefix; }
    public String euroSymbol() { return euroSymbol; }
    public String msgPrefix() { return prefix; }
    public String msgNotInEvent() { return notInEvent; }
    public String msgAlreadyClaimed() { return alreadyClaimed; }
    public String msgDailyLimit() { return dailyLimit; }
    public String msgClaimedSuccess() { return claimedSuccess; }
    public String msgReload() { return reload; }

    public List<String> commandsFor(LocalDate date) {
        String key = mmdd.format(date);
        return specialDayCommands.getOrDefault(key, defaultCommands);
    }
}

    public java.util.List<Cosmetic> cosmeticsFor(java.time.LocalDate date) {
        String key = mmdd.format(date);
        return specialDayCosmetics.getOrDefault(key, defaultCosmetics);
    }
