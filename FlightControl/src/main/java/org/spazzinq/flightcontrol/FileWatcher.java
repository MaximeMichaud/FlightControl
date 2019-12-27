package org.spazzinq.flightcontrol;

import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

class FileWatcher extends BukkitRunnable {
    private FlightControl pl;
    private WatchService watcher;

    private static final String CATEGORIES = "categories.yml",
                                CONFIG = "config.yml",
                                LANG = "lang.yml";

    FileWatcher(FlightControl pl, Path dataPath) throws IOException {
        this.pl = pl;
        watcher = FileSystems.getDefault().newWatchService();

        // Only watch modifications and creations
        dataPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
    }

    @Override
    public void run() {
        WatchKey key = watcher.poll();

        if (key != null) {
            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Can still occur even though listening for ENTRY_CREATE and ENTRY_MODIFY
                if (kind == OVERFLOW) {
                    continue;
                }

                // context = path
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                String fileString = ev.context().toString();
                boolean playerStateChanged = false;

                pl.getLogger().warning(fileString);
                // TODO Do I need to ignore now?
                switch (fileString) {
                    case CATEGORIES:
                        logChanges(CATEGORIES);
                        pl.getCategoryManager().reloadCategories();
                        playerStateChanged = true;
                        break;
                    case CONFIG:
                        if (pl.getConfManager().reloadConf()) {
                            logChanges(CONFIG);
                            // If flight_speed is updated!
                            pl.getPlayerManager().reloadPlayerData();
                        }
                        playerStateChanged = true;
                        break;
                    case LANG:
                        logChanges(LANG);
                        pl.getLangManager().reloadLang();
                        break;
                    default:
                        break;
                }
                if (playerStateChanged) {
                    pl.checkPlayers();
                }
            }
            boolean valid = key.reset();
            if (!valid) {
                cancel();
            }
        }
    }

    private void logChanges(String filename) {
        pl.getLogger().info("Detected changes in " + filename + "! Loading changes...");
    }
}