package com.keurig.combatlogger.handler;

import java.util.UUID;

/**
 * Pure combat-pair validation shared by event handling and tests.
 */
public final class CombatRules {

    private CombatRules() {
    }

    public static boolean isValidPlayerPair(UUID attackerId, UUID targetId) {
        return attackerId != null && targetId != null && !attackerId.equals(targetId);
    }
}
