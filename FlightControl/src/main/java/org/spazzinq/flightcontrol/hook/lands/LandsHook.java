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

package org.spazzinq.flightcontrol.hook.lands;

import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.LandChunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.spazzinq.flightcontrol.FlightControl;

import java.util.UUID;

public class LandsHook extends LandsBase {
    private final LandsIntegration landsIntegration;

    public LandsHook(FlightControl pl) {
        landsIntegration = new LandsIntegration(pl, false);
    }

    @Override public boolean landsOwn(Player p) {
        LandChunk chunk = landsIntegration.getLandChunk(p.getLocation());

//        if (chunk == null || !p.getUniqueId().equals(chunk.getOwnerUID())) {
//            if (chunk == null) {
//                p.sendMessage("The LandChunk is null!");
//            } else {
//                p.sendMessage(p.getUniqueId() + " " + chunk.getOwnerUID() + " " + (p.getUniqueId().equals(chunk.getOwnerUID())));
//            }
//        }

        return chunk != null && p.getUniqueId().equals(chunk.getOwnerUID());
    }

    @Override public boolean landsTrusted(Player p) {
        LandChunk chunk = landsIntegration.getLandChunk(p.getLocation());

        return chunk != null && chunk.getTrustedPlayers().contains(p.getUniqueId());
    }

    @Override public UUID getOwnerUUID(Location location) {
        LandChunk chunk = landsIntegration.getLandChunk(location);

        return chunk != null ? chunk.getOwnerUID() : null;
    }

    @Override public boolean isHooked() {
        return true;
    }
}
