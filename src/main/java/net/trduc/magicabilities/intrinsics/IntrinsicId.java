package net.trduc.magicabilitiesfork.intrinsics;

public enum IntrinsicId {
    BERSERK_1,
    BERSERK_2,
    BERSERK_3,
    ADRENALINE_1,
    ADRENALINE_2,
    ADRENALINE_3,
    ADRENALINE_4,
    ADRENALINE_5;

    public String line() {
        String n = name();
        int lastUnderscore = n.lastIndexOf('_');
        String lastPart = n.substring(lastUnderscore + 1);
        return lastPart.matches("[1-9]") ? n.substring(0, lastUnderscore) : n;
    }

    public int tier() {
        String n = name();
        int lastUnderscore = n.lastIndexOf('_');
        String lastPart = n.substring(lastUnderscore + 1);
        return lastPart.matches("[1-9]") ? Integer.parseInt(lastPart) : 1;
    }
}
