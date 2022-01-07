package w10;

import battlecode.common.*;

strictfp class SoldierStrategy {
    /** Called by RobotPlayer */
    static void runSoldier(RobotController rc) throws GameActionException {
        if (!ArchonTrackerManager.receivedArchonTrackers) {
            return;
        }

        MapLocation myLocation = rc.getLocation();

        CombatManager.COMBAT_DROID_ACTIONS action = CombatManager.getCombatDroidAction(rc);
        if (action == CombatManager.COMBAT_DROID_ACTIONS.ATTACK) {
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
        else if (action == CombatManager.COMBAT_DROID_ACTIONS.RETREAT) {
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchon(myLocation).location;
            if (myLocation.distanceSquaredTo(nearestAllyArchonLocation) > 10) {
                // Move towards nearest ally Archon
                GeneralManager.tryMove(rc, myLocation.directionTo(nearestAllyArchonLocation), true);
            }
            else {
                action = CombatManager.COMBAT_DROID_ACTIONS.HOLD;
            }
        }
        if (action == CombatManager.COMBAT_DROID_ACTIONS.HOLD) {
            if (CombatManager.tryAttack(rc)) {
                // Try to attack
            }
        }
    }
}
