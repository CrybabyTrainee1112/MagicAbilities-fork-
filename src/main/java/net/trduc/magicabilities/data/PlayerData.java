package net.trduc.magicabilitiesfork.data;

import net.trduc.magicabilitiesfork.powers.PowerType;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PlayerData {
    private static final HashMap<Player, PlayerData> playerData = new HashMap<>();
    private final String name;
    private PowerType powerType;
    private HashMap<Integer, Integer> binds;
    private boolean enabled;
    private boolean auraEnabled;
    private String randomBag;

    public PlayerData(String name, PowerType powerType, HashMap<Integer, Integer> binds, boolean enabled, boolean auraEnabled, String randomBag) {
        this.name = name;
        this.powerType = powerType;
        this.binds = binds;
        this.enabled = enabled;
        this.auraEnabled = auraEnabled;
        this.randomBag = randomBag == null ? "" : randomBag;
    }
    public static PlayerData getPlayerData(Player p){
        return playerData.get(p);
    }
    public static void setPlayerDataFromDb(Player p, DbManager db){
        String playerName = p.getName();
        PlayerData pd = db.getPlayerData(playerName);
        if (pd == null) {
            HashMap<Integer, Integer> defaultBinds = new HashMap<>();
            for (int i = 0; i < 9; i++) defaultBinds.put(i, i);
            pd = new PlayerData(playerName, PowerType.NONE, defaultBinds, true, true, "");
        }
        playerData.put(p, pd);
    }
    public static void savePlayerDataToDb(Player p, DbManager db){
        String playerName = p.getName();
        db.updatePlayer(playerName, playerData.get(p));
    }
    public static void removePlayerData(Player p){
        playerData.remove(p);
    }
    public String getName() {
        return name;
    }

    public PowerType getPower() {
        return powerType;
    }

    public void setPower(PowerType powerType) {
        this.powerType = powerType;
    }

    public HashMap<Integer, Integer> getBinds() {
        return binds;
    }

    public void setBinds(HashMap<Integer, Integer> binds) {
        this.binds = binds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAuraEnabled() {
        return auraEnabled;
    }

    public void setAuraEnabled(boolean auraEnabled) {
        this.auraEnabled = auraEnabled;
    }

    public String getRandomBag() {
        return randomBag;
    }

    public void setRandomBag(String randomBag) {
        this.randomBag = randomBag == null ? "" : randomBag;
    }
}

