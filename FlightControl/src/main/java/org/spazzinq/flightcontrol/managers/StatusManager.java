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

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.spazzinq.flightcontrol.FlightControl;
import org.spazzinq.flightcontrol.objects.Category;
import org.spazzinq.flightcontrol.objects.Evaluation;
import org.spazzinq.flightcontrol.objects.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatusManager {
    private FlightControl pl;

    public StatusManager(FlightControl pl) {
        this.pl = pl;
    }

    Evaluation checkFlight(Player p, Location l) {
        World world = l.getWorld();
        String worldName = world.getName(),
               regionName = pl.getHookManager().getWorldGuard().getRegionName(l);
        Region region = new Region(world, regionName);

        if (regionName != null) pl.defaultPerms(worldName + "." + regionName); // Register new regions dynamically

        // TODO Cached category grabbing
        Category category = null;
        for (Category c : pl.getCategoryManager().getCategories()) {
            List<Category> categories = new ArrayList<>();
            // TODO Remove legacy factions category permission
            if (p.hasPermission("flightcontrol.category." + c.getName()) || p.hasPermission("flightcontrol.factions." + c.getName())) {
                categories.add(c);
            }
            // Locate the highest priority category
            Collections.sort(categories);
            category = categories.isEmpty() ? null : categories.get(0);
        }
        if (category == null) {
            category = pl.getCategoryManager().getGlobal();
        }

        boolean hasWorlds = category.getWorlds() != null,
                hasRegions = category.getRegions() != null,
                hasFactions = category.getFactions() != null;

        boolean enableCategoryCheck =
                // World check
                hasWorlds && category.getWorlds().getEnabled().contains(world)
                // Region check
                || hasRegions && category.getRegions().getEnabled().contains(region)
                // Factions check
                || hasFactions && pl.getHookManager().getFactions().rel(p, category.getFactions().getEnabled());
        boolean enableHookCheck =
                // Plot check
                pl.getHookManager().getPlot().flightAllowed(worldName, l.getBlockX(), l.getBlockY(), l.getBlockZ())
                // Towny check
                || (pl.getConfigManager().isOwnTown() || p.hasPermission("flightcontrol.owntown")) && pl.getHookManager().getTowny().ownTown(p) && !(pl.getConfigManager().isTownyWar() && pl.getHookManager().getTowny().wartime())
                // Lands check
                || (pl.getConfigManager().isOwnLand() || p.hasPermission("flightcontrol.ownland")) && pl.getHookManager().getLands().ownLand(p);
        boolean enablePermissionCheck =
                // Global perm check
                p.hasPermission("flightcontrol.flyall")
                // World perm check
                || p.hasPermission("flightcontrol.fly." + worldName)
                // Region perm check
                || regionName != null && p.hasPermission("flightcontrol.fly." + worldName + "." + regionName);
        boolean tempFly = pl.getPlayerManager().getFlightPlayer(p).hasTempFly();

        boolean disableCategoryCheck =
                // World check
                hasWorlds && category.getWorlds().getDisabled().contains(world)
                // Region check
                || hasRegions && category.getRegions().getDisabled().contains(region)
                // Factions check
                || hasFactions && pl.getHookManager().getFactions().rel(p, category.getFactions().getDisabled());
        boolean disableHookCheck =
                pl.getHookManager().getCombat().tagged(p)
                || pl.getHookManager().getPlot().flightDenied(worldName, l.getBlockX(), l.getBlockY(), l.getBlockZ());
        boolean disablePermissionCheck =
                // World perm check
                p.hasPermission("flightcontrol.nofly." + worldName)
                // Region perm check
                || regionName != null && p.hasPermission("flightcontrol.nofly." + worldName + "." + regionName);

        return new Evaluation(disableCategoryCheck || disableHookCheck || disablePermissionCheck || enemyCheck(p, l),
                              enableCategoryCheck || enableHookCheck || enablePermissionCheck || tempFly);
    }

    // TODO Optimize?
    private boolean enemyCheck(Player p, Location l) {
        boolean disable = false;

        // Prevent comparing 2 different worlds
        if (pl.getConfigManager().isUseFacEnemyRange() && p.getWorld().equals(l.getWorld())) {
            List<Player> worldPlayers = l.getWorld().getPlayers();
            worldPlayers.remove(p);
            List<Entity> nearbyEntities = p.getNearbyEntities(pl.getConfigManager().getFacEnemyRange(), pl.getConfigManager().getFacEnemyRange(), pl.getConfigManager().getFacEnemyRange());

            if (nearbyEntities.size() <= worldPlayers.size()) {
                for (Entity e : nearbyEntities)
                    if (e instanceof Player) {
                        Player otherP = (Player) e;
                        // Distance is calculated a second time to match the shape of the other distance calculation
                        // (this would be a cube while the other would be a sphere otherwise)
                        if (pl.getHookManager().getFactions().isEnemy(p, otherP) && l.distance(otherP.getLocation()) <= pl.getConfigManager().getFacEnemyRange()) {
                            if (otherP.isFlying()) pl.getFlightManager().check(otherP);
                            disable = true;
                        }
                    }
            } else {
                for (Player otherP : worldPlayers)
                    if (pl.getHookManager().getFactions().isEnemy(p, otherP) && l.distance(otherP.getLocation()) <= pl.getConfigManager().getFacEnemyRange()) {
                        if (otherP.isFlying()) pl.getFlightManager().check(otherP);
                        disable = true;
                    }
            }
        }
        return disable;
    }
}