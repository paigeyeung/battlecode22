package w8;

import battlecode.common.*;

strictfp class SoldierStrategy {
    /** Called by RobotPlayer */
    static void runSoldier(RobotController rc) throws GameActionException {
        if (!ArchonTrackerManager.receivedArchonTrackers) {
            return;
        }

        MapLocation myLocation = rc.getLocation();

        CombatManager.HOSTILE_DROID_ACTIONS action = CombatManager.getHostileDroidAction(rc);
        if (action == CombatManager.HOSTILE_DROID_ACTIONS.ATTACK) {
            if (CombatManager.tryAttack(rc)) {
                // Try to attack
            }
            else {
                MapLocation visibleAttackTarget = CombatManager.getAttackTarget(rc, rc.getType().visionRadiusSquared);
                if (visibleAttackTarget != null) {
                    // Move towards nearest visible enemy
                    GeneralManager.tryMove(rc, myLocation.directionTo(visibleAttackTarget), true);
                }
                else {
                    // If no enemies are visible, move towards nearest enemy Archon
                    GeneralManager.tryMove(rc, myLocation.directionTo(ArchonTrackerManager.getNearestEnemyArchon(myLocation).guessLocation), true);
                }
            }
        }
        else if (action == CombatManager.HOSTILE_DROID_ACTIONS.RETREAT) {
            MapLocation myNearestArchonLocation = ArchonTrackerManager.getNearestMyArchon(myLocation).location;
            if (myLocation.distanceSquaredTo(myNearestArchonLocation) > 10) {
                // Move towards nearest my Archon
                GeneralManager.tryMove(rc, myLocation.directionTo(myNearestArchonLocation), true);
            }
            else {
                action = CombatManager.HOSTILE_DROID_ACTIONS.HOLD;
            }
        }
        if (action == CombatManager.HOSTILE_DROID_ACTIONS.HOLD) {
            if (CombatManager.tryAttack(rc)) {
                // Try to attack
            }
        }
    }
}
