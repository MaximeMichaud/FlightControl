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

package org.Spazzinq.FlightControl;

import com.earth2me.essentials.Essentials;
import net.minelink.ctplus.CombatTagPlus;
import org.Spazzinq.FlightControl.Hooks.Combat.AntiLogging;
import org.Spazzinq.FlightControl.Hooks.Combat.Combat;
import org.Spazzinq.FlightControl.Hooks.Combat.LogX;
import org.Spazzinq.FlightControl.Hooks.Combat.TagPlus;
import org.Spazzinq.FlightControl.Hooks.Factions.Factions;
import org.Spazzinq.FlightControl.Hooks.Factions.Massive;
import org.Spazzinq.FlightControl.Hooks.Factions.UUIDSavage;
import org.Spazzinq.FlightControl.Hooks.Plot.NewSquared;
import org.Spazzinq.FlightControl.Hooks.Plot.Plot;
import org.Spazzinq.FlightControl.Hooks.Plot.OldSquared;
import org.Spazzinq.FlightControl.Hooks.Vanish.Ess;
import org.Spazzinq.FlightControl.Hooks.Vanish.PremiumSuper;
import org.Spazzinq.FlightControl.Hooks.Vanish.Vanish;
import org.Spazzinq.FlightControl.Multiversion.Regions;
import org.Spazzinq.FlightControl.Multiversion.v13.Regions13;
import org.Spazzinq.FlightControl.Multiversion.v8.Regions8;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class FlightControl extends org.bukkit.plugin.java.JavaPlugin {
    private Config c;
    private PluginManager pm = Bukkit.getPluginManager();
    boolean is13 = getServer().getVersion().contains("1.13");
    private ArrayList<Player> notif = new ArrayList<>();
    ArrayList<Player> fall = new ArrayList<>();

    Regions regions = pm.getPlugin("WorldGuard") != null ? (is13 ? new Regions13() : new Regions8()) : new Regions();
    private Plot plot = pm.getPlugin("PlotSquared") != null ? (pm.getPlugin("PlotSquared").getDescription().getVersion().split(".")[0].matches("17|18|19") ? new OldSquared() : new NewSquared()) : new Plot();
    private Combat combat = new Combat();
    private Factions fac = pm.getPlugin("Factions") != null ? (pm.getPlugin("MassiveCore") != null ? new Massive() : new UUIDSavage()) : new Factions();
    Vanish vanish = new Vanish();

    private boolean configWarning = true;

	public void onEnable() {
        getCommand("flightcontrol").setTabCompleter((commandSender, command, s, strings) ->
                new ArrayList<>(Arrays.asList("update", "actionbar", "combat", "falldamage", "trails", "vanishbypass", "clean", "command")));

        if (pm.getPlugin("CombatLogX") != null) combat = new LogX();
        else if (pm.getPlugin("CombatTagPlus") != null) combat = new TagPlus(((CombatTagPlus) pm.getPlugin("CombatTagPlus")).getTagManager());
        else if (pm.getPlugin("AntiCombatLogging") != null) combat = new AntiLogging();
        if (pm.getPlugin("PremiumVanish") != null || pm.getPlugin("SuperVanish") != null) vanish = new PremiumSuper();
        else if (pm.getPlugin("Essentials") != null) vanish = new Ess((Essentials) pm.getPlugin("Essentials"));

	    c = new Config(this); new Listener(this); new Actionbar(this); new Update(getDescription().getVersion());

        if (Update.exists()) new BukkitRunnable() {
            public void run() { getLogger().info("FlightControl " + Update.newVer() + " is available for update. Perform /fc update to update and " +
                    "visit https://www.spigotmc.org/resources/flightcontrol.55168/ to view the changes (that may affect your configuration)."); }
        }.runTaskLater(this, 40);
    }
	public void onDisable() { Config.save(); }

    public boolean onCommand(CommandSender s, org.bukkit.command.Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("flightcontrol")) {
            if (s instanceof ConsoleCommandSender || s.hasPermission("flightcontrol.admin")) {
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("reload")) { c.reloadConfig(); msg(s, "&a&lFlightControl &7» &aConfiguration successfully reloaded!"); }
                    else if (args[0].equalsIgnoreCase("update")) if (Update.exists()) {
                        if (!Update.dled) { Update.dl(); if (pm.getPlugin("Plugman") != null) {
                            msg(s, "&a&lFlightControl &7» &aAutomatic installation finished (you may need to reset the configuration)! Welcome to FlightControl " + Update.newVer() + " :D");
                            getServer().dispatchCommand(Bukkit.getConsoleSender(), "plugman reload FlightControl");
                        } else msg(s, "&a&lFlightControl &7» &aUpdate downloaded. Restart (or reload) the server to apply the update (you may need to reset the plugin config).", false); }
                        else msg(s, "&a&lFlightControl &7» &aThe update to version " + Update.newVer() + " has already been downloaded. Please restart (or reload) the server to apply the update.", false);
                    } else msg(s, "&a&lFlightControl &7» &aNo updates found.");
                    else if (args[0].equalsIgnoreCase("combat")) toggleOption(s, Config.useCombat = !Config.useCombat, "Combat Disabling");
                    else if (args[0].equalsIgnoreCase("falldamage")) toggleOption(s, Config.cancelFall = !Config.cancelFall, "Prevent Fall Damage");
                    else if (args[0].equalsIgnoreCase("trails")) toggleOption(s, Config.flightTrail = !Config.flightTrail, "Trails");
                    else if (args[0].equalsIgnoreCase("vanishbypass")) toggleOption(s, Config.vanishBypass = !Config.vanishBypass, "Vanish Bypass");
                    else if (args[0].equalsIgnoreCase("actionbar")) toggleOption(s, Config.actionBar = !Config.actionBar, "Actionbar Notifications");
                    else if (args[0].equalsIgnoreCase("command")) { toggleOption(s, Config.command = !Config.command, "Command"); flyCommand(); }
                    else if (args[0].equalsIgnoreCase("clean")) { saveConfig(); msg(s, "&a&lFlightControl &7» &aConfiguration cleaned!"); }
                    else if (args[0].equalsIgnoreCase("support")) {
                        toggleOption(s, Config.support = !Config.support, "Live Support");
                        Player spazzinq = getServer().getPlayer("Spazzinq");
                        if (Config.support) {
                            msg(s, "&e&lFlightControl &eWarning &7» &fLive support enables Spazzinq to check debug information on why flight is disabled. " +
                                    "You can disable support at any time by repeating the command, and the access only lasts until you restart FlightControl/the server.");
                            if (spazzinq != null) if (spazzinq.isOnline()) msg(spazzinq, "&c&lFlightControl &7» &c" + s.getName() + " has requested support.");
                        }
                    }
                    else if (args[0].equalsIgnoreCase("debug"))
                        if (s instanceof Player) debug((Player) s); else getLogger().info("Only players can use this command (it's information based on the player's location)");
                    else sendHelp(s);
                } else sendHelp(s);
            } else if (args.length == 1 && args[0].equalsIgnoreCase("debug") && s instanceof Player && s.getName().equals("Spazzinq")) {
                if (Config.support) debug((Player) s);
                else msg(s, "&c&lFlightControl &7» &cSorry bud, you don't have permission to view debug information :I");
            }
            else msg(s, Config.permDenied);
        } else if (cmd.getName().equalsIgnoreCase("fly")) {
            if (s instanceof Player)
                if (s.hasPermission("essentials.fly") || s.hasPermission("flightcontrol.fly")) {
                    Player p = (Player) s;
                    if (p.getAllowFlight()) { disableFlight(p); notif.add(p); }
                    else checkCMD(p);
                } else msg(s, Config.permDenied);
            else getLogger().info("Only players can use this command (the console can't fly, can it?)");
        } else if (cmd.getName().equalsIgnoreCase("toggletrail")) {
            if (s instanceof Player) {
                String uuid = ((Player) s).getUniqueId().toString();
                boolean o = Config.trailPrefs.contains(uuid);
                if (o) {
                    Config.trailPrefs.remove(uuid);
                    msg(s, Config.eTrail, Config.actionBar);
                } else {
                    Config.trailPrefs.add(uuid);
                    msg(s, Config.dTrail, Config.actionBar);
                }
            } else getLogger().info("Only players can use this command (the console isn't a player!)");
        }
        return true;
    }

    private void toggleOption(CommandSender s, Boolean o, String prefix) {
        msg(s, (prefix.equals("Trail") ? "&a&l" : "&a&lFlightControl &a") + prefix + " &7» "
                + (o ? "&aEnabled" : "&cDisabled"));
        if (!prefix.equals("Trail") && !prefix.equals("Live Support") && configWarning) {
            msg(s, "&e&lFlightControl &eWarning &7» &fTo prevent the removal of instructions, the option was not changed in the config. " +
                    "(Psst! You can quickly change it in the config then reload the plugin using /fc reload.)");
            configWarning = false;
        }
    }

    private void sendHelp(CommandSender s) {
        msg(s, " \n&a&lFlightControl &f" + getDescription().getVersion() + "\n" +
                "&aBy &fSpazzinq\n " +
                "\n&a/fc &7» &fHelp\n" +
                "&a/fc update &7» &fUpdate FlightControl\n" +
                "&a/fc actionbar &7» &fSend notifications through action bar\n" +
                "&a/fc combat &7» &fToggle automatic combat disabling\n" +
                "&a/fc falldamage &7» &fToggle fall damage prevention\n" +
                "&a/fc trails &7» &fToggle trails for the server\n" +
                "&a/fc vanishbypass &7» &fToggle vanish bypass\n" +
                "&a/fc clean &7» &fRemove instructions in config (ADVANCED users)\n" +
                "&a/fc command &7» &fUse /fly instead of automatic flight\n" +
                "\n&a/tt &7» &fPersonal trail toggle");
    }

    private void msg(CommandSender s, String msg) { msg(s, msg, false); }
    private void msg(CommandSender s, String msg, boolean actionBar) {
	    if (msg != null && !msg.isEmpty()) {
            if (s instanceof ConsoleCommandSender) msg = msg.replaceAll("FlightControl &7» ", "[FlightControl] ").replaceAll("»", "-");
	        msg = ChatColor.translateAlternateColorCodes('&', msg);
	        if (actionBar && s instanceof Player) Actionbar.send((Player) s, msg);
	        else s.sendMessage(msg);
        }
	}

    private void checkCMD(Player p) { check(p, p.getLocation(), true); }
    void check(Player p) { check(p, p.getLocation(), false); }
    void check(Player p, Location l, boolean cmd) {
        if (!p.hasPermission("flightcontrol.bypass") && !(vanish.vanished(p) && Config.vanishBypass) && p.getGameMode() != GameMode.SPECTATOR) {
            String world = l.getWorld().getName();
            String region = regions.region(l);
            boolean enable = fac.rel(p)
                    || p.hasPermission("flightcontrol.flyall")
                    || p.hasPermission("flightcontrol.fly." + world)
                    || region != null && p.hasPermission("flightcontrol.fly." + world + "." + region)
                    || !Config.worldBL && Config.worlds.contains(world)
                    || Config.regionBL && Config.regions.containsKey(world) && Config.regions.get(world).contains(region),
                    disable = combat.tagged(p) || plot.dFlight(world, l.getBlockX(), l.getBlockY(), l.getBlockZ())
                            || p.hasPermission("flightcontrol.fly." + world)
                            || region != null && p.hasPermission("flightcontrol.nofly." + world + "." + region)
                            || Config.worldBL && Config.worlds.contains(world)
                            || !Config.regionBL && Config.regions.containsKey(world) && Config.regions.get(world).contains(region);
            if (p.getAllowFlight()) { if (disable || !enable) disableFlight(p);
            } else {
                if (enable && !disable) canEnable(p, cmd);
                else if (cmd) cannotEnable(p);
            }
        } else if (!p.getAllowFlight()) canEnable(p, cmd);
    }

    private void canEnable(Player p, boolean cmd) {
        if (!Config.command || cmd) enableFlight(p);
        else if (!notif.contains(p)) { notif.add(p); Sound.play(p, Config.cSound); msg(p, Config.cFlight, Config.actionBar); }
    }
    private void cannotEnable(Player p) { Sound.play(p, Config.nSound); msg(p, Config.nFlight, Config.actionBar); }
    private void enableFlight(Player p) {
	    p.setAllowFlight(true);
        Sound.play(p, Config.eSound);
        msg(p, Config.eFlight, Config.actionBar);
    }

    private void disableFlight(Player p) {
        if (Config.command) notif.remove(p);
        if (Config.cancelFall && p.isFlying()) { fall.add(p);
            new BukkitRunnable() { public void run() { fall.remove(p); } }.runTaskLater(this, 120); }
        p.setAllowFlight(false);
        p.setFlying(false);
        Sound.play(p, Config.dSound);
        msg(p, Config.dFlight, Config.actionBar);
    }

    void flyCommand() {
        try {
            Field cmdMap = Bukkit.getServer().getClass().getDeclaredField("commandMap"), knownCMDS = SimpleCommandMap.class.getDeclaredField("knownCommands");
            Constructor<PluginCommand> plCMD = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            cmdMap.setAccessible(true); knownCMDS.setAccessible(true); plCMD.setAccessible(true);
            CommandMap map = (CommandMap) cmdMap.get(Bukkit.getServer());
            @SuppressWarnings("unchecked") Map<String, Command> kCMDMap = (Map<String, Command>) knownCMDS.get(cmdMap.get(Bukkit.getServer()));
            PluginCommand fly = plCMD.newInstance("fly", this);
            String plName = getDescription().getName();
            if (Config.command) {
                fly.setDescription("Enables flight");
                map.register(plName, fly);
                kCMDMap.put(plName.toLowerCase() + ":fly", fly);
                kCMDMap.put("fly", fly);
                fly.setExecutor(this);
            } else if (getCommand("fly") != null && getCommand("fly").getPlugin() == this) {
                kCMDMap.remove(plName.toLowerCase() + ":fly");
                kCMDMap.remove("fly");
                if (pm.getPlugin("Essentials") != null) {
                    map.register("Essentials", fly);
                    fly.setExecutor(pm.getPlugin("Essentials"));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void debug(Player p) {
	    boolean hasFac = !Factions.class.equals(fac.getClass());
        Location l = p.getLocation();
        String world = l.getWorld().getName();
        String region = regions.region(l);
        String currentC = "";
        if (hasFac) for (String c : Config.categories.keySet()) if (p.hasPermission("flightcontrol.factions." + c)) currentC = currentC.concat(c + "=" + Config.categories.get(c).toString());
        msg(p, (hasFac ? currentC + "\n \n" : "") + region + "\n" + Config.regions  + "\n \n&a&lEnable\n" +
                (hasFac ? "&aFC &7» &f" + fac.rel(p) + "\n" : "") +
                "&aAll &7» &f" + p.hasPermission("flightcontrol.flyall") + "\n" +
                "&aPWorld &7» &f" + p.hasPermission("flightcontrol.fly." + world) + "\n" +
                "&aPRegion &7» &f" + (region != null && p.hasPermission("flightcontrol.fly." + world + "." + region)) + "\n" +
                "&aCWorld &7» &f" + Config.worlds.contains(world) + "\n" +
                "&aCRegion &7» &f" + (Config.regions.containsKey(world) && Config.regions.get(world).contains(region)) + "\n \n" +
                "&c&lDisable\n" +
                "&cCombat &7» &f" + combat.tagged(p) + "\n" +
                "&cPlot &7» &f" + plot.dFlight(world, l.getBlockX(), l.getBlockY(), l.getBlockZ()) + "\n" +
                "&cPRegion &7» &f" + (region != null && p.hasPermission("flightcontrol.nofly." + world + "." + region)) + "\n");
    }
}