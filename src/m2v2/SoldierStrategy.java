package m2v2;

import battlecode.common.*;

strictfp class SoldierStrategy {
    static int[][] visitedTurns;

    /** Called by RobotPlayer */
    static void runSoldier(RobotController rc) throws GameActionException {
        if (visitedTurns == null) {
            visitedTurns = new int[rc.getMapWidth()+1][rc.getMapHeight()+1];
            for(int i = 0; i < visitedTurns.length; i++) {
                for(int j = 0; j < visitedTurns[i].length; j++) {
                    visitedTurns[i][j] = 0;
                }
            }
        }

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
                    GeneralManager.tryMove(rc, getNextSoldierDir(rc,visibleAttackTarget), false);
                }
                else {
                    // If no enemies are visible, move towards nearest enemy Archon
                    GeneralManager.tryMove(rc, getNextSoldierDir(rc,ArchonTrackerManager.getNearestEnemyArchon(myLocation).guessLocation), false);
                }
            }
        }
        else if (action == CombatManager.COMBAT_DROID_ACTIONS.RETREAT) {
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchon(myLocation).location;
            if (myLocation.distanceSquaredTo(nearestAllyArchonLocation) > 10) {
                // Move towards nearest ally Archon
                GeneralManager.tryMove(rc, getNextSoldierDir(rc,nearestAllyArchonLocation), true);
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

    static Direction getNextSoldierDir(RobotController rc, MapLocation dest) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        int f = 0;

        if(myLoc.distanceSquaredTo(dest) < 9) return null;

        RobotInfo[] nearbyTeamRobots = rc.senseNearbyRobots(20,rc.getTeam());

        int friendlySoldierCount = 0;
        int longerDistanceSoldierCount = 0, shorterDistanceSoldierCount = 0;
        for(RobotInfo robot : nearbyTeamRobots) {
            if (robot.getType().equals(RobotType.SOLDIER)) {
                friendlySoldierCount++;
                if(robot.location.distanceSquaredTo(dest) >= rc.getLocation().distanceSquaredTo(dest)) {
                    longerDistanceSoldierCount++;
                }
                else if (robot.location.distanceSquaredTo(dest) < rc.getLocation().distanceSquaredTo(dest)) {
                    shorterDistanceSoldierCount++;
                }
            }
        }
//
//        // If more nearby friendly soldiers are farther from the destination, add to cost for moving
//        if (longerDistanceSoldierCount > shorterDistanceSoldierCount + 3)
//            f += 100;
//
//        // If more nearby friendly soldiers are closer to the destination, add to cost for moving
//        if (longerDistanceSoldierCount < shorterDistanceSoldierCount - 2) {
//            f -= 100;
//        }

        int enemySoldierCount = 0;
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(16,rc.getTeam().opponent());
        for(RobotInfo robot : nearbyEnemyRobots) {
            if (robot.getType().equals(RobotType.SOLDIER)) {
                enemySoldierCount++;
            }
        }

//        if (Integer.max(rc.getMapHeight(),rc.getMapWidth()) > 40)
//            f += (int)(((rc.getMapHeight()*rc.getMapWidth())/8)*((double)friendlySoldierCount*-1 + enemySoldierCount + 1.2));
//        else
//            f += (int)(((rc.getMapHeight()+rc.getMapWidth())*20)*((double)friendlySoldierCount*-1 + enemySoldierCount + 1.2));
//
        f += ((double)friendlySoldierCount*-1 + enemySoldierCount + 1.1) * myLoc.distanceSquaredTo(dest);

        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchon(rc.getLocation()).location;

        if(rc.getLocation().distanceSquaredTo(nearestAllyArchonLocation) > (rc.getMapHeight()+rc.getMapWidth())/2) {
            f = Integer.MAX_VALUE;
        }

        if(friendlySoldierCount > 5 && enemySoldierCount < friendlySoldierCount - 3) {
            f = Integer.MAX_VALUE;
        }
        else if(friendlySoldierCount > 2 && rc.getLocation().distanceSquaredTo(nearestAllyArchonLocation) < friendlySoldierCount) {
            return GeneralManager.getDirToEncircle(rc, nearestAllyArchonLocation, friendlySoldierCount^2);
        }

        Direction movementDir = null;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(rc.canMove(dir)) {
                MapLocation adj = rc.adjacentLocation(dir);
                int newF = (GeneralManager.getSqDistance(adj, dest)-GeneralManager.getSqDistance(myLoc, dest))
                        + rc.senseRubble(adj) + (int)(100 * (double)visitedTurns[adj.x][adj.y] / GeneralManager.turnsAlive);
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
                DebugManager.log(rc,"f: "+f+", newF"+newF);
            }
        }
        if (movementDir != null) {
            MapLocation adj = rc.adjacentLocation(movementDir);
            visitedTurns[adj.x][adj.y] = GeneralManager.turnsAlive;
        }
        return movementDir;
    }
}
