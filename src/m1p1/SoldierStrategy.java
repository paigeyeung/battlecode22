package m1p1;

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
        MapLocation currLoc = rc.getLocation();
        if(currLoc.equals(dest)) return null;
        RobotInfo[] nearbyTeamRobots = rc.senseNearbyRobots(16,rc.getTeam());

        int friendlySoldierCount = 0;
        for(RobotInfo robot : nearbyTeamRobots) {
            if (robot.getType().equals(RobotType.SOLDIER)) {
                friendlySoldierCount++;
            }
        }

        int enemySoldierCount = 0;
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(16,rc.getTeam().opponent());
        for(RobotInfo robot : nearbyEnemyRobots) {
            if (robot.getType().equals(RobotType.SOLDIER)) {
                enemySoldierCount++;
            }
        }

        int f = 0;

        if (rc.getMapHeight()*rc.getMapWidth() > 1000)
            f = ((rc.getMapHeight()*rc.getMapWidth())/8)*(friendlySoldierCount - enemySoldierCount + 1);
        else
            f = ((rc.getMapHeight()+rc.getMapWidth())*20)*(friendlySoldierCount - enemySoldierCount + 1);

        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchon(rc.getLocation()).location;

        if(rc.getLocation().distanceSquaredTo(nearestAllyArchonLocation) > (rc.getMapHeight()+rc.getMapWidth())/2) {
            f = Integer.MAX_VALUE;
        }

        if(friendlySoldierCount > 5 && enemySoldierCount < friendlySoldierCount - 3) {
            f = Integer.MAX_VALUE;
        }
        else if(friendlySoldierCount > 2 && rc.getLocation().distanceSquaredTo(nearestAllyArchonLocation) < friendlySoldierCount) {
            return GeneralManager.getDirForEncircle(rc, nearestAllyArchonLocation, friendlySoldierCount^2);
        }

        Direction movementDir = null;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(rc.canMove(dir)) {
                MapLocation adj = rc.adjacentLocation(dir);
                int newF = GeneralManager.getSqDistance(adj, dest) + rc.senseRubble(adj) + (int)(100 * (double)visitedTurns[adj.x][adj.y] / GeneralManager.turnsAlive);
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
        if (movementDir != null) {
            MapLocation adj = rc.adjacentLocation(movementDir);
            visitedTurns[adj.x][adj.y] = GeneralManager.turnsAlive;
        }
        return movementDir;
    }
}
