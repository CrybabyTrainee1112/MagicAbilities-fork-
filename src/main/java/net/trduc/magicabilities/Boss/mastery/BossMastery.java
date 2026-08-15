package net.trduc.magicabilitiesfork.Boss.mastery;

import java.util.Objects;

public class BossMastery {
    private final String bossTypeId;
    private int tier;
    private int wins;
    private int losses;

    public BossMastery(String bossTypeId, int tier, int wins, int losses) {
        this.bossTypeId = Objects.requireNonNull(bossTypeId, "Boss type ID cannot be null");
        this.tier = tier;
        this.wins = wins;
        this.losses = losses;
    }

    public static BossMastery initial(String bossTypeId) {
        return new BossMastery(bossTypeId, 0, 0, 0);
    }

    public String getBossTypeId() {
        return bossTypeId;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public int getWins() {
        return wins;
    }

    public void incrementWins() {
        this.wins++;
    }

    public int getLosses() {
        return losses;
    }

    public void incrementLosses() {
        this.losses++;
    }

    @Override
    public String toString() {
        return "BossMastery{" + bossTypeId + ", tier=" + tier + ", wins=" + wins + ", losses=" + losses + '}';
    }
}
