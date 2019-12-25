/*
 * This file is part of FlightControl, which is licensed under the MIT License
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

package org.spazzinq.flightcontrol.manager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.spazzinq.flightcontrol.FlightControl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.UUID;

import static org.spazzinq.flightcontrol.FlightControl.msg;

public final class UpdateManager {
    HashSet<UUID> notified = new HashSet<>();

    private String version, newVersion;
    private boolean downloaded;

    public UpdateManager(String version) {
        this.version = version;
    }

    public boolean exists() {
        try {
            newVersion = new BufferedReader(new InputStreamReader(new URL("https://api.spigotmc.org/legacy/update.php?resource=55168").openConnection().getInputStream())).readLine();
        } catch (Exception ignored) {
            return false;
        }
        return version.matches("\\d+(\\.\\d+)?") && newVersion.matches("\\d+(\\.\\d+)?") ? Double.parseDouble(newVersion) > Double.parseDouble(version) : !version.equals(newVersion);
    }

    private void dl() {
        if (exists()) {
            try {
                URL website = new URL("https://github.com/Spazzinq/FlightControl/releases/download/" + newVersion + "/flightcontrol.jar");
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(new File("plugins/flightcontrol.jar"));
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();
                downloaded = true;
            } catch (Exception ignored) {
            }
        }
    }

    public String newVer() {
        return newVersion;
    }

    public void install(CommandSender s, boolean silentCheck) {
        if (exists()) {
            if (!downloaded) {
                dl();
                if (Bukkit.getPluginManager().isPluginEnabled("Plugman")) {
                    msg(s, "&a&lFlightControl &7» &aAutomatic installation finished (the config has automatically updated too)! Welcome to flightcontrol " + newVer() + "!");
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "plugman reload flightcontrol");
                } else
                    msg(s, "&a&lFlightControl &7» &aVersion &f" + newVer() + " &aupdate downloaded. Restart (or reload) the server to apply the update.");
            } else
                msg(s, "&a&lFlightControl &7» &aVersion &f" + newVer() + " &aupdate has already been downloaded. Restart (or reload) the server to apply the update.");
        } else if (!silentCheck) {
            msg(s, "&a&lFlightControl &7» &aNo updates found.");
        }
    }

    public void notify(Player p) {
        // exists()
        if (!notified.contains(p.getUniqueId())) {
            notified.add(p.getUniqueId());
            FlightControl.msg(p, "&e&lFlightControl &7» &eWoot woot! Version &f" + newVer() + "&e is now available! " +
                                       "Update with \"/fc update\" and check out the new features: &fhttps://www.spigotmc.org/resources/flightcontrol.55168/");
        }
    }
}