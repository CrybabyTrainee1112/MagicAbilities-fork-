package net.trduc.magicabilitiesfork.misc;

import net.trduc.magicabilitiesfork.data.DbManager;
import net.trduc.magicabilitiesfork.data.PowerTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamColorSync {

    private static final String PREFIX = "pt_";

    public static final ChatColor[] SUGGESTED_COLORS = new ChatColor[]{
            ChatColor.RED, ChatColor.DARK_RED, ChatColor.GOLD, ChatColor.YELLOW,
            ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.AQUA, ChatColor.DARK_AQUA,
            ChatColor.BLUE, ChatColor.DARK_BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE
    };

    private static final java.util.Random RNG = new java.util.Random();

    public static ChatColor parseColor(String raw){
        if (raw == null) return null;
        try{
            ChatColor c = ChatColor.valueOf(raw.trim().toUpperCase());
            return c.isColor() ? c : null;
        } catch (Exception e){
            return null;
        }
    }

    public static ChatColor randomColor(){
        return SUGGESTED_COLORS[RNG.nextInt(SUGGESTED_COLORS.length)];
    }

    public static String suggestedColorsList(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SUGGESTED_COLORS.length; i++){
            ChatColor c = SUGGESTED_COLORS[i];
            sb.append(c).append(c.name());
            if (i < SUGGESTED_COLORS.length - 1) sb.append(ChatColor.GRAY).append(", ");
        }
        return sb.toString();
    }

    private static String scoreboardTeamName(String teamName){
        String key = PREFIX + Integer.toHexString(teamName.hashCode());
        return key.length() > 16 ? key.substring(0, 16) : key;
    }

    private static Team getOrCreateTeam(Scoreboard sb, String teamName, ChatColor color){
        String key = scoreboardTeamName(teamName);
        Team team = sb.getTeam(key);
        if (team == null) team = sb.registerNewTeam(key);
        team.setColor(color == null ? ChatColor.WHITE : color);
        return team;
    }

    public static void syncTeamColor(String teamName, String color){
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        getOrCreateTeam(sb, teamName, parseColor(color));
    }

    public static void removeTeamScoreboard(String teamName){
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = sb.getTeam(scoreboardTeamName(teamName));
        if (t != null) t.unregister();
    }

    public static void syncPlayer(DbManager db, Player p){
        if (p == null || !p.isOnline()) return;
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Team t : sb.getTeams()){
            if (t.getName().startsWith(PREFIX) && t.hasEntry(p.getName())){
                t.removeEntry(p.getName());
            }
        }

        String teamName = db.getPlayerTeam(p.getName());
        if (teamName == null) return;
        PowerTeam pt = db.getPowerTeam(teamName);
        if (pt == null) return;

        Team team = getOrCreateTeam(sb, teamName, parseColor(pt.getColor()));
        if (!team.hasEntry(p.getName())) team.addEntry(p.getName());
    }

    public static void syncAllOnline(DbManager db){
        for (Player p : Bukkit.getOnlinePlayers()) syncPlayer(db, p);
    }
}
