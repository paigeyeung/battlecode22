package m6;

import battlecode.common.*;

strictfp class WatchtowerStrategy {
    /** Called by RobotPlayer */
    static void runWatchtower() throws GameActionException {
        // Try to attack
        CombatManager.tryAttack();
    }
}
