/*
 * This file is part of FlightControl-parent, which is licensed under the MIT License
 *
 * Copyright (c) 2019 Spazzinq
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.spazzinq.flightcontrol;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.spazzinq.flightcontrol.api.objects.Sound;
import org.spazzinq.flightcontrol.objects.Category;
import org.spazzinq.flightcontrol.objects.CommentedConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ConfigManager {
    private FlightControl pl;
    private PluginManager pm;

    CommentedConfig configData;
    private File f;

    @Getter @Setter
    boolean autoEnable, autoUpdate, support,
            worldBL, regionBL, combatChecked,
            ownTown, townyWar, ownLand,
            fallCancelled, vanishBypass, trail,
            byActionBar, everyEnable, useFacEnemyRange;
    @Getter @Setter
    double facEnemyRange;
    @Getter @Setter float flightSpeed;
    @Getter
    String dFlight, eFlight, cFlight, nFlight, disableTrail, enableTrail;
    @Getter
    String noPermission;
    Sound eSound, dSound, cSound, nSound;
    HashSet<String> worlds;
    HashMap<String, List<String>> regions;
    HashMap<String, Category> categories;

    ConfigManager(FlightControl pl) {
        this.pl = pl;
        pm = pl.getServer().getPluginManager();
        f = new File(pl.getDataFolder(), "config.yml");

        reloadConfig();
        updateConfig();
    }

    void reloadConfig() {
        pl.saveDefaultConfig();
        try {
            configData = new CommentedConfig(f, pl.getResource("config.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // booleans
        autoUpdate = configData.getBoolean("auto_update");
        autoEnable = configData.getBoolean("settings.auto_enable");
        worldBL = configData.isList("worlds.disable");
        regionBL = configData.isConfigurationSection("regions.disable");
        combatChecked = configData.getBoolean("settings.disable_flight_in_combat");
        ownTown = configData.getBoolean("towny.enable_own_town");
        townyWar = configData.getBoolean("towny.disable_during_war");
        ownLand = configData.getBoolean("lands.enable_own_land");
        fallCancelled = configData.getBoolean("settings.prevent_fall_damage");
        vanishBypass = configData.getBoolean("settings.vanish_bypass");
        byActionBar = configData.getBoolean("messages.actionbar");

        // ints
        int range = configData.getInt("settings.disable_enemy_range");
        if (useFacEnemyRange = (range != -1)) facEnemyRange = range;

        // floats
        flightSpeed = pl.calcActualSpeed((float) configData.getDouble("settings.flight_speed"));

        // Messages
        dFlight = configData.getString("messages.flight.disable");
        dFlight = configData.getString("messages.flight.disable");
        eFlight = configData.getString("messages.flight.enable");
        cFlight = configData.getString("messages.flight.can_enable");
        nFlight = configData.getString("messages.flight.cannot_enable");
        disableTrail = configData.getString("messages.trail.disable");
        enableTrail = configData.getString("messages.trail.enable");
        noPermission = configData.getString("messages.permission_denied");

        // Load other stuff that have separate methods
        loadWorlds();
        loadSounds();
        loadTrail();

        // Reassign it anyways because it'll cause an NPE
        regions = new HashMap<>();
        if (pm.isPluginEnabled("WorldGuard")) {
            loadRegions();
        }
        if (pm.isPluginEnabled("Factions")) loadCategories();

        // Region permission registering
        for (World w : Bukkit.getWorlds()) {
            String name = w.getName();
            pl.defaultPerms(name);
            for (String rg : pl.worldGuard.getRegions(w)) pl.defaultPerms(name + "." + rg);
        }
    }

    private void updateConfig() {
        boolean modified = false;
        // 3
        if (!configData.isConfigurationSection("towny")) {
            configData.addNode("trail", "towny:");
            configData.addSubnodes("towny", Arrays.asList("disable_during_war: false", "enable_own_town: false"));
            modified = true;
        }
        if (!configData.isBoolean("sounds.every_enable")) {
            configData.addSubnode("sounds", "every_enable: false");
            modified = true;
        }
        // 3.1
        if (!(configData.isInt("settings.flight_speed") || configData.isDouble("settings.flight_speed"))) {
            configData.addSubnode("settings.command", "flight_speed: 1.0");
            modified = true;
        }
        if (!configData.isInt("settings.disable_enemy_range")) {
            configData.addSubnode("settings.vanish_bypass", "disable_enemy_range: -1");
            modified = true;
        }
        if (!configData.isBoolean("auto_update")) {
            configData.addNode("settings", "auto_update: true");
            modified = true;
        }
        // 3.3
        if (!configData.isBoolean("settings.auto_enable")) {
            configData.addSubnode("settings",  "auto_enable: "
                    + (configData.getBoolean("settings.command") ? "false" : "true"));
            modified = true;
        }
        if (configData.isBoolean("settings.command")) {
            configData.removeNode("settings.command");
            modified = true;
        }
        // 3.5
        if (!configData.isConfigurationSection("lands")) {
            configData.addNode("towny", "lands:");
            configData.addSubnode("lands", "enable_own_land");
            modified = true;
        }

        if (modified) save();
    }

    // LOAD SECTION
    private void loadWorlds() {
        worlds = new HashSet<>();
        ConfigurationSection worldsCS = load(configData, "worlds");
        if (worldsCS != null) {
            List<String> type = worldsCS.getStringList(worldBL ? "disable" : "enable");
            if (type != null) for (String w : type) if (Bukkit.getWorld(w) != null) worlds.add(w);
        }
    }

    private void loadRegions() {
        ConfigurationSection regionsCS = load(configData, "regions");
        if (regionsCS != null) addRegions(regionsCS.getConfigurationSection(regionBL ? "disable" : "enable"));
    }

    private void loadCategories() {
        categories = new HashMap<>();
        ConfigurationSection facs = load(configData, "factions");
        if (facs != null) for (String cName : facs.getKeys(false)) {
            // Register permission defaults
            if (pm.getPermission("flightcontrol.factions." + cName) == null)
                pm.addPermission(new Permission("flightcontrol.factions." + cName, PermissionDefault.FALSE));
            ConfigurationSection categorySect = load(facs, cName);
            if (categorySect != null) {
                String type = categorySect.isList("disable") ? "disable" : (categorySect.isList("enable") ? "enable" : null);
                if (type != null)
                    categories.put(cName, createCategory(categorySect.getStringList(type), "disable".equals(type)));
                else
                    pl.getLogger().warning("Factions category \"" + cName + "\" is invalid! (missing \"enable\"/\"disable\")");
            }
        }
    }

    private void loadTrail() {
        trail = configData.getBoolean("trail.enabled");

        if (trail) {
            pl.particles.setParticle(configData.getString("trail.particle"));
            pl.particles.setAmount(configData.getInt("trail.amount"));
            String offset = configData.getString("trail.rgb");
            if (offset != null && (offset = offset.replaceAll("\\s+", "")).split(",").length == 3) {
                String[] xyz = offset.split(",");
                pl.particles.setRBG(xyz[0].matches("-?\\d+(.(\\d+)?)?") ? Integer.parseInt(xyz[0]) : 0,
                        xyz[1].matches("-?\\d+(.(\\d+)?)?") ? Integer.parseInt(xyz[1]) : 0,
                        xyz[2].matches("-?\\d+(.(\\d+)?)?") ? Integer.parseInt(xyz[2]) : 0);
            }
        }
    }

    private void loadSounds() {
        everyEnable = configData.getBoolean("sounds.every_enable");
        eSound = getSound("sounds.enable");
        dSound = getSound("sounds.disable");
        cSound = getSound("sounds.can_enable");
        nSound = getSound("sounds.cannot_enable");
    }

    // LOAD HELPER METHODS
    static ConfigurationSection load(ConfigurationSection c, String type) {
        if (c.isConfigurationSection(type)) {
            ConfigurationSection typeS = c.getConfigurationSection(type);
            Set<String> typeKeys = new HashSet<>();
            for (String key : typeS.getKeys(true)) {
                String[] keyParts = key.split("\\.");
                // Get last part of key
                if (key.contains(".")) key = keyParts[keyParts.length - 1];
                typeKeys.add(key);
            }
            if (!typeKeys.isEmpty() && (typeKeys.contains("enable") || typeKeys.contains("disable"))) return typeS;
        }
        return null;
    }

    private Sound getSound(String key) {
        if (configData.isConfigurationSection(key)) {
            String s = configData.getString(key + ".sound").toUpperCase().replaceAll("\\.", "_");
            if (Sound.is(s)) {
                return new Sound(s, (float) configData.getDouble(key + ".volume"), (float) configData.getDouble(key + ".pitch"));
            }
        }
        return null;
    }

    private void addRegions(ConfigurationSection c) {
        if (c != null) for (String w : c.getKeys(false)) {
            if (Bukkit.getWorld(w) != null && c.isList(w)) {
                ArrayList<String> rgs = new ArrayList<>();
                for (String rg : c.getStringList(w)) if (pl.worldGuard.hasRegion(w, rg)) rgs.add(rg);
                regions.put(w, rgs);
            }
        }
    }

    private Category createCategory(List<String> types, boolean blacklist) {
        if (types != null && !types.isEmpty() && (types.contains("OWN") || types.contains("ALLY") || types.contains("TRUCE") || types.contains("NEUTRAL") || types.contains("ENEMY") || types.contains("WARZONE") || types.contains("SAFEZONE") || types.contains("WILDERNESS"))) {
            return new Category(blacklist, types.contains("OWN"), types.contains("ALLY"), types.contains("TRUCE"), types.contains("NEUTRAL"), types.contains("ENEMY"), types.contains("WARZONE"), types.contains("SAFEZONE"), types.contains("WILDERNESS"));
        }
        return null;
    }

    // FILE CONFIG METHODS
    public void set(String path, Object value) {
        configData.set(path, value);
        save();
    }
    private void save() {
        try {
            configData.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}