package m10;

import battlecode.common.*;

import static m10.GeneralManager.visitedTurns;

strictfp class SageStrategy {

    /** Called by RobotPlayer */
    static void runSage() throws GameActionException {
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

        CombatManager.COMBAT_DROID_ACTIONS action = CombatManager.getCombatDroidAction();
        if (action == CombatManager.COMBAT_DROID_ACTIONS.ATTACK) {
            RobotInfo[] allies = RobotPlayer.rc.senseNearbyRobots(RobotPlayer.rc.getType().actionRadiusSquared, RobotPlayer.rc.getTeam());
            RobotInfo[] enemies = RobotPlayer.rc.senseNearbyRobots(RobotPlayer.rc.getType().actionRadiusSquared, RobotPlayer.rc.getTeam().opponent());
            MapLocation[] leadLocations = RobotPlayer.rc.senseNearbyLocationsWithLead(RobotPlayer.rc.getType().actionRadiusSquared);

            int allyArchonCount = 0, allyDroidCount = 0, enemyArchonCount = 0, enemyDroidCount = 0;

            for(RobotInfo ally : allies) {
                if(ally.type.canAttack()) allyDroidCount++;
                if(ally.type.equals(RobotType.ARCHON)) allyArchonCount++;
            }

            for(RobotInfo enemy : enemies) {
                if(enemy.type.canAttack()) enemyDroidCount++;
                if(enemy.type.equals(RobotType.ARCHON)) enemyArchonCount++;
            }

            if(enemyArchonCount >= 2 && allyArchonCount == 0 &&
                    RobotPlayer.rc.canEnvision(AnomalyType.FURY)) {
                RobotPlayer.rc.envision(AnomalyType.FURY);
            }
            else if(CombatManager.evaluateLocalCombatScore(RobotPlayer.rc.getTeam(), false) <
                    CombatManager.evaluateLocalCombatScore(RobotPlayer.rc.getTeam().opponent(), true) &&
                    RobotPlayer.rc.canEnvision(AnomalyType.CHARGE)) {
                RobotPlayer.rc.envision(AnomalyType.CHARGE);
            }
            else if(enemyArchonCount >= 1 && allyArchonCount == 0 &&
                    RobotPlayer.rc.canEnvision(AnomalyType.ABYSS)) {
                RobotPlayer.rc.envision(AnomalyType.ABYSS);
            }
            else {
                if(ArchonTrackerManager.getCentralEnemyArchon() != -1)
                    GeneralManager.tryMove(getSageDirToEncircle(ArchonTrackerManager.enemyArchonTrackers[ArchonTrackerManager.getCentralEnemyArchon()].getGuessLocation(),
                        4), false);
            }
        }
        else if (action == CombatManager.COMBAT_DROID_ACTIONS.RETREAT) {
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
            GeneralManager.tryMove(getSageDirToEncircle(nearestAllyArchonLocation,4), false);
        }
        if (action == CombatManager.COMBAT_DROID_ACTIONS.HOLD) {
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
            GeneralManager.tryMove(getSageDirToEncircle(nearestAllyArchonLocation,4), false);
        }
    }

    static Direction getSageDirToEncircle(MapLocation loc, int rSq) throws GameActionException {
        double r = Math.sqrt(rSq);

        int xOffset = 1000, yOffset = 1000;

        while (!GeneralManager.onMap(loc.x + xOffset, loc.y + yOffset)){
            xOffset = (int) (Math.random() * ((int) r + 1)) * ((int) (Math.random() * 3) - 1);
            yOffset = (int) Math.ceil(Math.sqrt(rSq - xOffset * xOffset)) * ((int) (Math.random() * 3) - 1);
        }

        return getNextSageDir(new MapLocation(loc.x + xOffset, loc.y + yOffset));
    }

    static Direction getNextSageDir(MapLocation dest) throws GameActionException {
        MapLocation myLoc = RobotPlayer.rc.getLocation();
        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(RobotPlayer.rc.getLocation());

        if (myLoc.equals(dest)) return null;
        if (myLoc.distanceSquaredTo(dest) <= myLoc.distanceSquaredTo(nearestAllyArchonLocation))
            return GeneralManager.getDirToEncircle(dest, GeneralManager.myType.actionRadiusSquared);

        Direction movementDir = null;

        int f = Integer.MAX_VALUE;

        for (Direction dir : GeneralManager.DIRECTIONS) {
            if (RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int) Math.sqrt(newDist) * 4 + newRubble + 20 * visitedTurns[adj.x][adj.y];

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj, 2);

                for (MapLocation adj2 : adjToAdj) {
                    newF += 2 * visitedTurns[adj2.x][adj2.y];
                }

                if (newF < f) {
                    f = newF;
                    movementDir = dir;
                } else if (newF == f) {
                    if (((int) Math.random() * 2) == 0) {
                        f = newF;
                        movementDir = dir;
                    }
                }
            }
        }
        if (movementDir != null) {
            MapLocation adj = RobotPlayer.rc.adjacentLocation(movementDir);
            visitedTurns[adj.x][adj.y]++;
        }
        return movementDir;
    }
}