package p5;

import battlecode.common.*;

strictfp class SoldierStrategy {
    static void runSoldier(RobotController rc) throws GameActionException {
        if (!ArchonTrackerManager.receivedArchonTrackers) {
            return;
        }

        MapLocation myLocation = rc.getLocation();

        GeneralManager.HOSTILE_DROID_ACTIONS action = GeneralManager.getHostileDroidAction(rc);
        if (action == GeneralManager.HOSTILE_DROID_ACTIONS.ATTACK) {
            if (GeneralManager.tryAttack(rc)) {
                // Try to attack
            }
            else {
                MapLocation visibleAttackTarget = GeneralManager.getAttackTarget(rc, rc.getType().visionRadiusSquared);
                if (visibleAttackTarget != null) {
                    // Move towards nearest visible enemy
//                    GeneralManager.tryMove(rc, myLocation.directionTo(visibleAttackTarget), true);
                    GeneralManager.tryMove(rc,GeneralManager.getNextDir(rc,visibleAttackTarget), false);
                }
                else {
                    // If no enemies are visible, move towards nearest enemy Archon
//                    GeneralManager.tryMove(rc, myLocation.directionTo(ArchonTrackerManager.getNearestEnemyArchon(myLocation).guessLocation), true);
                    GeneralManager.tryMove(rc,GeneralManager.getNextDir(rc,ArchonTrackerManager.getNearestEnemyArchon(myLocation).guessLocation), false);
                }
            }
        }
        else if (action == GeneralManager.HOSTILE_DROID_ACTIONS.RETREAT) {
            MapLocation myNearestArchonLocation = ArchonTrackerManager.getNearestMyArchon(myLocation).location;
            if (myLocation.distanceSquaredTo(myNearestArchonLocation) > 10) {
                // Move towards nearest my Archon
                GeneralManager.tryMove(rc,GeneralManager.getNextDir(rc,myNearestArchonLocation), false);
            }
            else {
                action = GeneralManager.HOSTILE_DROID_ACTIONS.HOLD;
            }
        }
        if (action == GeneralManager.HOSTILE_DROID_ACTIONS.HOLD) {
            if (GeneralManager.tryAttack(rc)) {
                // Try to attack
            }
        }
    }
}
