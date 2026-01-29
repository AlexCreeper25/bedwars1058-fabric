package com.alexcreeper.bedwars1058.arena;

import com.alexcreeper.bedwars1058.BedWars;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ArenaTicker {

    public static void register() {
        // Runs 20 times per second (every game tick)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Loop through all loaded arenas and tick them
            for (Arena arena : BedWars.getInstance().getArenaManager().getArenas()) {
                arena.tick();
            }
        });
    }
}