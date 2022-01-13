package testplayer1;

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
//            if(GeneralManager.getNextDir(dest) != null)
//                return GeneralManager.getOppositeDirection(GeneralManager.getNextDir(dest));

        return GeneralManager.getNextDir(dest);

//        if (longerDistanceSoldierCount > shorterDistanceSoldierCount + 3)
//            f += 10000;
//
//        if (longerDistanceSoldierCount < shorterDistanceSoldierCount - 3) {
//            f -= 100;
//        }
//
//        int enemySoldierCount = 0;
//        RobotInfo[] nearbyEnemyRobots = RobotPlayer.rc.senseNearbyRobots(16, RobotPlayer.rc.getTeam().opponent());
//        for(RobotInfo robot : nearbyEnemyRobots) {
//            if (robot.getType().equals(RobotType.SOLDIER)) {
//                enemySoldierCount++;
//            }
//        }
//
//        if (RobotPlayer.rc.getMapHeight()*RobotPlayer.rc.getMapWidth() > 1000)
//            f += (int)(((RobotPlayer.rc.getMapHeight() * RobotPlayer.rc.getMapWidth()) / 8) * ((double)friendlySoldierCount * 0.5 - enemySoldierCount + 1.2));
//        else
//            f += (int)(((RobotPlayer.rc.getMapHeight() + RobotPlayer.rc.getMapWidth()) * 20) * ((double)friendlySoldierCount * 0.5 - enemySoldierCount + 1.2));
//
//
//        if(RobotPlayer.rc.getLocation().distanceSquaredTo(nearestAllyArchonLocation) > (RobotPlayer.rc.getMapHeight() + RobotPlayer.rc.getMapWidth())/2) {
//            f = Integer.MAX_VALUE;
//        }
//
//        if(friendlySoldierCount > 5 && enemySoldierCount < friendlySoldierCount - 3) {
//            f = Integer.MAX_VALUE;
//        }
//        else if(friendlySoldierCount > 2 && RobotPlayer.rc.getLocation().distanceSquaredTo(nearestAllyArchonLocation) < friendlySoldierCount) {
//            return GeneralManager.getDirToEncircle(nearestAllyArchonLocation, friendlySoldierCount^2);
//        }
//
//        Direction movementDir = null;
//
//        for(Direction dir : GeneralManager.DIRECTIONS) {
//            if(RobotPlayer.rc.canMove(dir)) {
//                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
//                int newF = GeneralManager.getSqDistance(adj, dest) - GeneralManager.getSqDistance(currLoc, dest)
//                        + RobotPlayer.rc.senseRubble(adj) + (int)(100 * (double)visitedTurns[adj.x][adj.y] / GeneralManager.turnsAlive);
//                if(newF < f) {
//                    f = newF;
//                    movementDir = dir;
//                }
//                else if(newF == f){
//                    if(((int)Math.random() * 2) == 0) {
//                        f = newF;
//                        movementDir = dir;
//                    }
//                }
//            }
//        }
//        if (movementDir != null) {
//            MapLocation adj = RobotPlayer.rc.adjacentLocation(movementDir);
//            visitedTurns[adj.x][adj.y] = GeneralManager.turnsAlive;
//        }
//        return movementDir;
    }
}
