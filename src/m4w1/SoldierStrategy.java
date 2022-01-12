package m4w1;

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
                        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(myLocation);
                        // Unless there is no known enemy Archon
                        // Then move towards the center of the map
//                        GeneralManager.tryMove(getNextSoldierDir(GeneralManager.getMapCenter()), true);
                        GeneralManager.tryMove(GeneralManager.getDirToEncircle(nearestAllyArchonLocation,9), false);
                    }
                }
            }
        }
        else if (action == CombatManager.COMBAT_DROID_ACTIONS.RETREAT) {
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(myLocation);
//            if (myLocation.distanceSquaredTo(nearestAllyArchonLocation) > 10) {
//                // Move towards nearest ally Archon
//                GeneralManager.tryMove(getNextSoldierDir(nearestAllyArchonLocation), true);
//            }
//            else {
//                action = CombatManager.COMBAT_DROID_ACTIONS.HOLD;
//            }
            GeneralManager.tryMove(GeneralManager.getDirToEncircle(nearestAllyArchonLocation,9), true);
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

        if(myLoc.equals(dest)) return null;
        if(myLoc.distanceSquaredTo(dest) <= myLoc.distanceSquaredTo(nearestAllyArchonLocation))
            return GeneralManager.getDirToEncircle(dest,RobotPlayer.rc.getType().actionRadiusSquared);

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

//        if(friendlySoldierCount < 2)
//            return null;

        if(friendlySoldierCount < 9 && (myLoc.distanceSquaredTo(nearestAllyArchonLocation) <= 25 &&
                myLoc.distanceSquaredTo(nearestAllyArchonLocation) >= 9))
            return null;

        Direction movementDir = null;

        int f = Integer.MAX_VALUE;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = newDist + newRubble/5 + 10*visitedTurns[adj.x][adj.y];

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj,2);

                for(MapLocation adj2 : adjToAdj) {
                    newF += visitedTurns[adj2.x][adj2.y];
                }

                if(newF < f) {
                    f = newF;
                    movementDir = dir;
                }
                else if(newF == f){
                    if(((int)Math.random()*2)==0) {
                        f = newF;
                        movementDir = dir;
                    }
                }
            }
        }
        if(Math.random() < 0.5) {
            if (movementDir != null) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(movementDir);
                visitedTurns[adj.x][adj.y]++;
            }
            return movementDir;
        }

        return null;
    }
}
