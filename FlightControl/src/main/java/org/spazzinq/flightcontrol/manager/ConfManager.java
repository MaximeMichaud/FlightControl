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

import com.google.common.io.Files;
import lombok.Getter;
import lombok.Setter;
import org.spazzinq.flightcontrol.FlightControl;
import org.spazzinq.flightcontrol.api.objects.Sound;
import org.spazzinq.flightcontrol.object.CommentConf;
import org.spazzinq.flightcontrol.util.MathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public final class ConfManager {
    private final FlightControl pl;
    private boolean ignoreReload;

    private final File confFile;
    @Getter private CommentConf conf;

    @Getter @Setter private boolean autoEnable;
    @Getter @Setter private boolean autoUpdate;
    @Getter @Setter private boolean inGameSupport;
    @Getter @Setter private boolean combatChecked;
    @Getter @Setter private boolean townyOwn;
    @Getter @Setter private boolean townyWarDisable;
    @Getter @Setter private boolean landsOwnEnable;
    @Getter @Setter private boolean landsTrusted;
    @Getter @Setter private boolean cancelFall;
    @Getter @Setter private boolean vanishBypass;
    @Getter @Setter private boolean trail;
    @Getter @Setter private boolean everyEnable;
    @Getter @Setter private boolean everyDisable;
    @Getter @Setter private boolean useFacEnemyRange;
    @Getter @Setter private double facEnemyRange;
    @Getter @Setter private float defaultFlightSpeed;
    @Getter @Setter private Sound eSound, dSound, cSound, nSound;

    public ConfManager(FlightControl pl) {
        this.pl = pl;
        confFile = new File(pl.getDataFolder(), "config.yml");
    }

    public boolean loadConf() {
        boolean reloaded = false;

        if (!ignoreReload) {
            ignoreReload = true;
            conf = new CommentConf(confFile, pl.getResource("config.yml"));

            if (conf.isBoolean("auto_update")) {
                migrateFromVersion3();
            }

            updateConfig();

            // booleans
            autoUpdate = conf.getBoolean("settings.auto_update");
            autoEnable = conf.getBoolean("settings.auto_enable_flight");
            combatChecked = conf.getBoolean("settings.disable_flight_in_combat");
            cancelFall = conf.getBoolean("settings.prevent_fall_damage");
            vanishBypass = conf.getBoolean("settings.vanish_bypass");

            townyOwn = conf.getBoolean("towny.enable_own_town");
            townyWarDisable = conf.getBoolean("towny.negate_during_war");

            landsOwnEnable = conf.getBoolean("lands.enable_own_land");
            landsTrusted = conf.getBoolean("lands.include_trusted");

            // ints
            int range = conf.getInt("factions.disable_enemy_range");
            if (useFacEnemyRange = (range != -1)) facEnemyRange = range;

            // floats
            defaultFlightSpeed = MathUtil.calcConvertedSpeed((float) conf.getDouble("settings.flight_speed"));

            // Load other stuff that have separate methods
            loadSounds();
            loadTrail();

            // Prevent reloading for the next 250ms
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ignoreReload = false;
                }
            }, 250);

            reloaded = true;
        }
        return reloaded;
    }

    // TODO Add
    public void updateConfig() {
        boolean modified = false;

        // 4.1.0 - moved to lang.yml
        if (conf.isConfigurationSection("messages")) {
            pl.getLogger().info("Removed the messages section from config.yml!");
            conf.deleteNode("messages");
            modified = true;
        }

        // 4.2.0 - add include_trusted to lands
        if (!conf.isBoolean("lands.include_trusted")) {
            pl.getLogger().info("Added \"include_trusted\" to lands configuration section!");
            conf.addSubnodes(Collections.singleton("include_trusted: false"), "lands.enable_own_land");
            modified = true;
        }

        if (modified) {
            conf.save();
        }
    }

    private void loadTrail() {
        trail = conf.getBoolean("trail.enabled");

        if (trail) {
            pl.getParticleManager().setParticle(conf.getString("trail.particle"));
            pl.getParticleManager().setAmount(conf.getInt("trail.amount"));
            String offset = conf.getString("trail.rgb");
            if (offset != null && (offset = offset.replaceAll("\\s+", "")).split(",").length == 3) {
                String[] xyz = offset.split(",");
                pl.getParticleManager().setRBG(xyz[0].matches("-?\\d+(.(\\d+)?)?") ? Integer.parseInt(xyz[0]) : 0,
                        xyz[1].matches("-?\\d+(.(\\d+)?)?") ? Integer.parseInt(xyz[1]) : 0,
                        xyz[2].matches("-?\\d+(.(\\d+)?)?") ? Integer.parseInt(xyz[2]) : 0);
            }
        }
    }

    private void loadSounds() {
        everyEnable = conf.getBoolean("sounds.every_enable");
        everyDisable = conf.getBoolean("sounds.every_disable");
        eSound = getSound("sounds.enable");
        dSound = getSound("sounds.disable");
        cSound = getSound("sounds.can_enable");
        nSound = getSound("sounds.cannot_enable");
    }

    private Sound getSound(String key) {
        if (conf.isConfigurationSection(key)) {
            String s = conf.getString(key + ".sound").toUpperCase().replaceAll("\\.", "_");
            if (Sound.is(s)) {
                return new Sound(s, (float) conf.getDouble(key + ".volume"), (float) conf.getDouble(key + ".pitch"));
            }
        }
        return null;
    }

    // FILE CONFIG METHODS
    public void set(String path, Object value) {
        ignoreReload = true;
        conf.set(path, value);
        conf.save();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                ignoreReload = false;
            }
        }, 500);
    }

    private void migrateFromVersion3() {
        try {
            //noinspection UnstableApiUsage
            Files.move(confFile, new File(pl.getDataFolder(), "config_old.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        pl.saveDefaultConfig();
    }
}