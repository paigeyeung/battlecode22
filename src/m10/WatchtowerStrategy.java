package m10;

import battlecode.common.*;

strictfp class WatchtowerStrategy {
    static int turnsNoEnemy = 0;

    /** Called by RobotPlayer */
    static void runWatchtower() throws GameActionException {
        if(GeneralManager.visitedTurns == null) {
            GeneralManager.visitedTurns = new int[GeneralManager.mapWidth][GeneralManager.mapHeight];
        }

        double enemyCombatScore = CombatManager.evaluateLocalCombatScore(GeneralManager.enemyTeam, true);
        double allyCombatScore = CombatManager.evaluateLocalCombatScore(GeneralManager.myTeam, true);

        if(RobotPlayer.rc.getMode().canMove) {
            if(enemyCombatScore > 0) {
                if (RobotPlayer.rc.canTransform()) {
                    RobotPlayer.rc.transform();
                }
                else {
                    GeneralManager.tryMove(getNextWatchtowerDir(ArchonTrackerManager.allyArchonTrackers[ArchonTrackerManager.getNearestAllyArchon(GeneralManager.myLocation)].location),
                            false);
                }
            } else {
                int enemyArchon = ArchonTrackerManager.getNearestEnemyArchon(GeneralManager.myLocation);

                if (enemyArchon != -1) {
                    GeneralManager.tryMove(getNextWatchtowerDir(ArchonTrackerManager.enemyArchonTrackers[enemyArchon].getGuessLocation()),
                            false);
                }
                else {
                    GeneralManager.tryMove(GeneralManager.getRandomDirection(), true);
                }
            }
        }
        else {
            CombatManager.tryAttack();
            if (enemyCombatScore == 0) {
                turnsNoEnemy++;
                if (turnsNoEnemy > 20 && allyCombatScore > 150) {
                    if (RobotPlayer.rc.canTransform())
                        RobotPlayer.rc.transform();
                }
            } else {
                turnsNoEnemy = 0;
            }
        }
    }

    static Direction getNextWatchtowerDir(MapLocation dest) throws GameActionException {
        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);

        if (GeneralManager.myLocation.equals(dest)) return null;
        if (GeneralManager.myLocation.distanceSquaredTo(dest) <= GeneralManager.myLocation.distanceSquaredTo(nearestAllyArchonLocation))
            return GeneralManager.getDirToEncircle(dest,GeneralManager.myType.actionRadiusSquared);

        Direction movementDir = null;

        int f = Integer.MAX_VALUE;

        for (Direction dir : GeneralManager.DIRECTIONS) {
            if (RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int)Math.sqrt(newDist) * 4 + newRubble + 20 * GeneralManager.visitedTurns[adj.x][adj.y];

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj,2);

                for (MapLocation adj2 : adjToAdj) {
                    newF += 2 * GeneralManager.visitedTurns[adj2.x][adj2.y];
                }

                if (newF < f) {
                    f = newF;
                    movementDir = dir;
                }
                else if (newF == f){
                    if (((int)Math.random() * 2) == 0) {
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
