package com.harmfy.ascendantskills;

public final class ClientPerkCache {
    private static final long STALE_AFTER_MILLIS = 2_000L;
    private static int minerQuarryStacks;
    private static double minerQuarryPerStack;
    private static boolean universalPickaxe;
    private static long lastUpdateMillis;

    private ClientPerkCache() {
    }

    public static void accept(PerkHudPayload payload) {
        minerQuarryStacks = Math.max(0, payload.minerQuarryStacks());
        minerQuarryPerStack = Math.max(0.0D, payload.minerQuarryPerStack());
        universalPickaxe = payload.universalPickaxe();
        lastUpdateMillis = System.currentTimeMillis();
    }

    public static double minerQuarryRhythmBonus() {
        if (isStale()) {
            return 0.0D;
        }
        return minerQuarryStacks * minerQuarryPerStack;
    }

    public static boolean hasUniversalPickaxeHint() {
        return !isStale() && universalPickaxe;
    }

    private static boolean isStale() {
        return System.currentTimeMillis() - lastUpdateMillis > STALE_AFTER_MILLIS;
    }
}
