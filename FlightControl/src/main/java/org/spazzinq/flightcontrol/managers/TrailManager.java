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

package org.spazzinq.flightcontrol.managers;

import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.spazzinq.flightcontrol.FlightControl;

import java.util.HashMap;
import java.util.Iterator;

public final class TrailManager {
    private FlightControl pl;

    @Getter private HashMap<Player, BukkitTask> partTasks = new HashMap<>();

    public TrailManager(FlightControl pl) {
        this.pl = pl;
    }

    public void trailCheck(Player p) {
        if (pl.getParticles() != null && pl.getConfigManager().isTrail() && pl.getPlayerManager().getFlightPlayer(p).hasTrail()) {
            partTasks.put(p, new BukkitRunnable() {
                @Override public void run() {
                    if (!(p.getGameMode() == GameMode.SPECTATOR || pl.getHookManager().getVanish().vanished(p) || p.hasPotionEffect(PotionEffectType.INVISIBILITY))) {
                        Location l = p.getLocation();
                        // For some terrible reason the locations are never in the correct spot so you have to time them later
                        new BukkitRunnable() { @Override public void run() { pl.getParticles().spawn(l); } }.runTaskLater(pl, 2);
                    }
                }
            }.runTaskTimerAsynchronously(pl, 0, 4));
        }
    }

    public void trailRemove(Player p) {
        BukkitTask task = partTasks.remove(p); if (task != null) task.cancel();
    }

    public void disableEnabledTrails() {
        Iterator<Player> currentTrails = partTasks.keySet().iterator();
        // Throws a ConcurrentModificationException as a for-each
        //noinspection WhileLoopReplaceableByForEach
        while (currentTrails.hasNext()) {
            trailRemove(currentTrails.next());
        }
    }
}