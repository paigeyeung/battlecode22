package m1w1;

import battlecode.common.*;

strictfp class WatchtowerStrategy {
    /** Called by RobotPlayer */
    static void runWatchtower(RobotController rc) throws GameActionException {
        // Try to attack
        CombatManager.tryAttack(rc);
    }
}
