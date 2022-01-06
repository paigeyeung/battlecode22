package w8;

import battlecode.common.*;

strictfp class WatchtowerStrategy {
    /** Called by RobotPlayer */
    static void runWatchtower(RobotController rc) throws GameActionException {
        // Try to attack
        GeneralManager.tryAttack(rc);
    }
}
