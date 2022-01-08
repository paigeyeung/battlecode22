package m2_c;

import battlecode.common.*;

strictfp class WatchtowerStrategy {
    /** Called by RobotPlayer */
    static void runWatchtower(RobotController rc) throws GameActionException {
        // Try to attack
        CombatManager.tryAttack(rc);
    }
}
