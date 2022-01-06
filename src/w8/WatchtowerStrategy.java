package w8;

import battlecode.common.*;

strictfp class WatchtowerStrategy {
    static void runWatchtower(RobotController rc) throws GameActionException {
        // Try to attack
        GeneralManager.tryAttack(rc);
    }
}
