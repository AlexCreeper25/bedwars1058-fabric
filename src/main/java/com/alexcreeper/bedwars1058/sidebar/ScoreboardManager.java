package com.alexcreeper.bedwars1058.sidebar;

import com.alexcreeper.bedwars1058.BedWars;
import com.alexcreeper.bedwars1058.arena.Arena;
import com.alexcreeper.bedwars1058.arena.ArenaStatus;
import com.alexcreeper.bedwars1058.arena.Team;
import net.minecraft.scoreboard.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ScoreboardManager {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");

    public static void init() {}

    public static void updateScoreboard(ServerPlayerEntity player, Arena arena) {
        if (player == null || BedWars.getInstance().getServer() == null) return;
        Scoreboard scoreboard = BedWars.getInstance().getServer().getScoreboard(); 
        
        String objName = "bw_" + player.getName().getString().toLowerCase();
        if (objName.length() > 16) objName = objName.substring(0, 16);

        ScoreboardObjective objective = scoreboard.getNullableObjective(objName);
        if (objective != null) scoreboard.removeObjective(objective);
        
        objective = scoreboard.addObjective(objName, ScoreboardCriterion.DUMMY, Text.literal("BED WARS"), ScoreboardCriterion.RenderType.INTEGER, true, null);
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);

        List<String> lines = Collections.emptyList();
        if (arena.getGroup() != null) {
            if (arena.getStatus() == ArenaStatus.WAITING) lines = arena.getGroup().getScoreboardWaiting();
            else if (arena.getStatus() == ArenaStatus.STARTING) lines = arena.getGroup().getScoreboardStarting();
            else lines = arena.getGroup().getScoreboardPlaying();
        }

        int score = lines.size();
        for (String line : lines) {
            String parsed = parsePlaceholders(line, arena, player);
            if (parsed == null) continue;
            
            ScoreHolder holder = ScoreHolder.fromName(parsed.replace("&", "\u00a7"));
            ScoreAccess sc = scoreboard.getOrCreateScore(holder, objective);
            sc.setScore(score--);
        }
    }

    private static String parsePlaceholders(String line, Arena arena, ServerPlayerEntity player) {
        String out = line
            .replace("{date}", DATE_FORMAT.format(new Date()))
            .replace("{map}", arena.getName())
            .replace("{on}", String.valueOf(arena.getPlayers().size()))
            .replace("{max}", String.valueOf(arena.getMaxPlayers()))
            .replace("{time}", String.valueOf(arena.getCountdown()))
            .replace("{next_event}", "Diamond II");

        if (out.contains("{team_status_")) {
            for (Team t : arena.getTeams()) {
                String key = "{team_status_" + t.getName() + "}";
                if (out.contains(key)) {
                    String status;
                    if (t.isBedDestroyed()) {
                        long alive = t.getMembers().stream().filter(uuid -> arena.getPlayers().contains(uuid) && !arena.getSpectators().contains(uuid)).count();
                        status = (alive > 0) ? "&a" + alive : "&c\u2718"; 
                    } else status = "&a\u2714"; 
                    out = out.replace(key, status);
                }
                String letterKey = "{team_" + t.getName().substring(0,1).toUpperCase() + "}";
                if (out.contains(letterKey)) out = out.replace(letterKey, getColorCode(t.getColor()) + t.getName().substring(0,1));
            }
            if (out.contains("{team_status_")) return null; 
        }
        return out;
    }
    
    private static String getColorCode(net.minecraft.util.Formatting fm) {
        if (fm == null) return "&f";
        switch(fm) {
            case RED: return "&c"; case BLUE: return "&9"; case GREEN: return "&a";
            case YELLOW: return "&e"; case AQUA: return "&b"; case WHITE: return "&f";
            case LIGHT_PURPLE: return "&d"; case GRAY: return "&7"; case DARK_GRAY: return "&8";
            default: return "&f";
        }
    }
}