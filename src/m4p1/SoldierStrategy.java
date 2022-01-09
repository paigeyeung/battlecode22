package m4p1;

import battlecode.common.*;

strictfp class SoldierStrategy {
    static int[][] visitedTurns;

    /** Called by RobotPlayer */
    static void runSoldier() throws GameActionException {
        if (visitedTurns == null) {
            visitedTurns = new int[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
            for(int i = 0; i < visitedTurns.length; i++) {
                for(int j = 0; j < visitedTurns[i].length; j++) {
                    visitedTurns[i][j] = 0;
                }
            }
        }

        if (!ArchonTrackerManager.receivedArchonTrackers) {
            return;
        }

        MapLocation myLocation = RobotPlayer.rc.getLocation();

        CombatManager.COMBAT_DROID_ACTIONS action = CombatManager.getCombatDroidAction();
        if (action == CombatManager.COMBAT_DROID_ACTIONS.ATTACK) {
            if (CombatManager.tryAttack()) {
                // Try to attack
            }
            else {
                MapLocation visibleAttackTarget = CombatManager.getAttackTarget(RobotPlayer.rc.getType().visionRadiusSquared);
                if (visibleAttackTarget != null) {
                    // Move towards nearest visible enemy
                    GeneralManager.tryMove(getNextSoldierDir(visibleAttackTarget), false);
                }
                else {
                    MapLocation nearestEnemyArchonGuessLocation = ArchonTrackerManager.getNearestEnemyArchonGuessLocation(myLocation);
                    if (nearestEnemyArchonGuessLocation != null) {
                        // If no enemies are visible, move towards nearest enemy Archon
                        GeneralManager.tryMove(getNextSoldierDir(nearestEnemyArchonGuessLocation), false);
                    }
                    else {
                        // Unless there is no known enemy Archon
                        // Then move towards the center of the map
                        GeneralManager.tryMove(getNextSoldierDir(GeneralManager.getMapCenter()), true);
                    }
                }
            }
        }
        else if (action == CombatManager.COMBAT_DROID_ACTIONS.RETREAT) {
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(myLocation);
            if (myLocation.distanceSquaredTo(nearestAllyArchonLocation) > 10) {
                // Move towards nearest ally Archon
                GeneralManager.tryMove(getNextSoldierDir(nearestAllyArchonLocation), true);
            }
            else {
                action = CombatManager.COMBAT_DROID_ACTIONS.HOLD;
            }
        }
        if (action == CombatManager.COMBAT_DROID_ACTIONS.HOLD) {
            if (CombatManager.tryAttack()) {
                // Try to attack
            }
        }
    }

    static Direction getNextSoldierDir(MapLocation dest) throws GameActionException {
        MapLocation myLoc = RobotPlayer.rc.getLocation();
        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(RobotPlayer.rc.getLocation());

        int f = 0;

        if(myLoc.equals(dest)) return null;
        if(myLoc.distanceSquaredTo(dest) < myLoc.distanceSquaredTo(nearestAllyArchonLocation))
            return GeneralManager.getDirToEncircle(dest,myLoc.distanceSquaredTo(dest) - 2);

        RobotInfo[] nearbyTeamRobots = RobotPlayer.rc.senseNearbyRobots(20,RobotPlayer.rc.getTeam());

        int friendlySoldierCount = 0;
        int longerDistanceSoldierCount = 0, shorterDistanceSoldierCount = 0;
        for(RobotInfo robot : nearbyTeamRobots) {
            if (robot.getType().equals(RobotType.SOLDIER)) {
                friendlySoldierCount++;
                if(robot.location.distanceSquaredTo(dest) > RobotPlayer.rc.getLocation().distanceSquaredTo(dest)) {
                    longerDistanceSoldierCount++;
                }
                else if (robot.location.distanceSquaredTo(dest) <= RobotPlayer.rc.getLocation().distanceSquaredTo(dest)) {
                    shorterDistanceSoldierCount++;
                }
            }
        }

        if(friendlySoldierCount < 2)
            return null;

        return GeneralManager.getNextDir(dest);
    }
}
