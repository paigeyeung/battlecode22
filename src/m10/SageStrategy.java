package m10;

import battlecode.common.*;

import java.util.Arrays;

strictfp class SageStrategy {

    /** Called by RobotPlayer */
    static void runSage() throws GameActionException {
        if (GeneralManager.visitedTurns == null) {
            GeneralManager.visitedTurns = new int[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
            for (int i = 0; i < GeneralManager.visitedTurns.length; i++) {
                for (int j = 0; j < GeneralManager.visitedTurns[i].length; j++) {
                    GeneralManager.visitedTurns[i][j] = 0;
                }
            }
        }

        if (!ArchonTrackerManager.receivedArchonTrackers) {
            return;
        }

        CombatManager.COMBAT_DROID_ACTIONS action = CombatManager.getCombatDroidAction();
        if (action == CombatManager.COMBAT_DROID_ACTIONS.ATTACK) {
            RobotInfo[] allies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.actionRadiusSquared, GeneralManager.myTeam);
            RobotInfo[] enemies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.actionRadiusSquared, GeneralManager.enemyTeam);
            MapLocation[] leadLocations = RobotPlayer.rc.senseNearbyLocationsWithLead(GeneralManager.myType.actionRadiusSquared);

            int allyArchonCount = 0, allyDroidCount = 0, enemyArchonCount = 0, enemyDroidCount = 0;
            for (RobotInfo ally : allies) {
                if (Arrays.asList(GeneralManager.DROIDS).contains(ally.getType())) allyDroidCount++;
                if (ally.getType().equals(RobotType.ARCHON)) allyArchonCount++;
            }
            for (RobotInfo enemy : enemies) {
                if (Arrays.asList(GeneralManager.DROIDS).contains(enemy.getType())) enemyDroidCount++;
                if (enemy.getType().equals(RobotType.ARCHON)) enemyArchonCount++;
            }

            if (enemyArchonCount >= 2 && allyArchonCount == 0 && RobotPlayer.rc.canEnvision(AnomalyType.FURY)) {
                RobotPlayer.rc.envision(AnomalyType.FURY);
            }
            else if ((CombatManager.evaluateLocalCombatScore(GeneralManager.myTeam, false) <
                    CombatManager.evaluateLocalCombatScore(GeneralManager.enemyTeam, true) ||
                    enemyDroidCount > 6 || (enemyDroidCount > 1 && RobotPlayer.rc.getHealth() <= 30)) &&
                    RobotPlayer.rc.canEnvision(AnomalyType.CHARGE)) {
                RobotPlayer.rc.envision(AnomalyType.CHARGE);
            }
            else if (enemyArchonCount >= 1 && allyArchonCount == 0 && RobotPlayer.rc.canEnvision(AnomalyType.FURY)) {
                RobotPlayer.rc.envision(AnomalyType.FURY);
            }
            else {
                if (ArchonTrackerManager.getCentralEnemyArchon() != -1)
                    GeneralManager.tryMove(getSageDirToEncircle(ArchonTrackerManager.enemyArchonTrackers[ArchonTrackerManager.getCentralEnemyArchon()].getGuessLocation(),
                        4), false);
                else
                    GeneralManager.tryMove(GeneralManager.DIRECTIONS[GeneralManager.rng.nextInt(GeneralManager.DIRECTIONS.length)], true);

                if (ArchonTrackerManager.getNearestEnemyArchonGuessLocation(GeneralManager.myLocation) != null &&
                        GeneralManager.myLocation.distanceSquaredTo(ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation))
                    > GeneralManager.myLocation.distanceSquaredTo(ArchonTrackerManager.getNearestEnemyArchonGuessLocation(GeneralManager.myLocation))) {
                    if (leadLocations.length > 2 && RobotPlayer.rc.canEnvision(AnomalyType.ABYSS)) {
                        RobotPlayer.rc.envision(AnomalyType.ABYSS);
                    }
                }
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

        while (!GeneralManager.onMap(loc.x + xOffset, loc.y + yOffset)) {
            xOffset = (int) (Math.random() * ((int) r + 1)) * ((int) (Math.random() * 3) - 1);
            yOffset = (int) Math.ceil(Math.sqrt(rSq - xOffset * xOffset)) * ((int) (Math.random() * 3) - 1);
        }

        return getNextSageDir(new MapLocation(loc.x + xOffset, loc.y + yOffset));
    }

    static Direction getNextSageDir(MapLocation dest) throws GameActionException {
        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(RobotPlayer.rc.getLocation());

        if (GeneralManager.myLocation.equals(dest)) return null;
        if (GeneralManager.myLocation.distanceSquaredTo(dest) <= GeneralManager.myLocation.distanceSquaredTo(nearestAllyArchonLocation))
            return GeneralManager.getDirToEncircle(dest, GeneralManager.myType.actionRadiusSquared);

        Direction movementDir = null;

        int f = Integer.MAX_VALUE;

        for (Direction dir : GeneralManager.DIRECTIONS) {
            if (RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int) Math.sqrt(newDist) * 4 + newRubble + 20 * GeneralManager.visitedTurns[adj.x][adj.y];

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj, 2);

                for (MapLocation adj2 : adjToAdj) {
                    newF += 2 * GeneralManager.visitedTurns[adj2.x][adj2.y];
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
            GeneralManager.visitedTurns[adj.x][adj.y]++;
        }
        return movementDir;
    }
}