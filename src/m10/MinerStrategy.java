package m10;

import battlecode.common.*;

strictfp class MinerStrategy {
    static boolean[][] visited = null;
    static boolean depleteLead = false;

    /** Called by RobotPlayer **/
    static void runMiner() throws GameActionException {
        if (visited == null) {
            visited = new boolean[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
        }

        // Try to mine gold
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(GeneralManager.myLocation.x + dx, GeneralManager.myLocation.y + dy);
                while (RobotPlayer.rc.canMineGold(mineLocation)) {
                    RobotPlayer.rc.mineGold(mineLocation);
                }
            }
        }

        // Try to mine lead
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(GeneralManager.myLocation.x + dx, GeneralManager.myLocation.y + dy);
                while (RobotPlayer.rc.canMineLead(mineLocation) && (depleteLead || RobotPlayer.rc.senseLead(mineLocation) > 1)) {
                    RobotPlayer.rc.mineLead(mineLocation);
                }
            }
        }

        // Move
        if (RobotPlayer.rc.getMovementCooldownTurns() < 10) {
            Direction dir = null;
            if (RobotPlayer.rc.getHealth() <
                CombatManager.HEALTH_PERCENTAGE_THRESHOLD_FOR_DISINTEGRATING * RobotPlayer.rc.getType().getMaxHealth(RobotPlayer.rc.getLevel())) {
                MapLocation nearestAllyArchonLoc = ArchonTrackerManager.getNearestAllyArchonLocation(RobotPlayer.rc.getLocation());
                if((RobotPlayer.rc.getLocation().distanceSquaredTo(nearestAllyArchonLoc) <= 9 && RobotPlayer.rc.senseLead(RobotPlayer.rc.getLocation()) == 0)
                    || RobotPlayer.rc.getLocation().distanceSquaredTo(nearestAllyArchonLoc) <= 1)
                    RobotPlayer.rc.disintegrate();
                else
                    dir = GeneralManager.getNextDir(nearestAllyArchonLoc);
            }
            else {
                dir = getNextMiningDir();
            }
            if (dir != null) {
                GeneralManager.tryMove(dir, false);
            }
        }

        // Calculate depleteLead for next turn
        if (Clock.getBytecodesLeft() > 3000) {
            int depleteLeadScore = 0;

            // If nearby enemy units greater than nearby ally units
            RobotInfo[] nearbyAllies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared, RobotPlayer.rc.getTeam());
            RobotInfo[] nearbyEnemies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared, RobotPlayer.rc.getTeam().opponent());
            depleteLeadScore += nearbyEnemies.length - nearbyAllies.length;

            // If nearest enemy Archon closer to nearest ally Archon
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
            MapLocation nearestEnemyArchonLocation = ArchonTrackerManager.getNearestEnemyArchonGuessLocation(GeneralManager.myLocation);
            if (nearestAllyArchonLocation != null && nearestEnemyArchonLocation != null) {
                int nearestAllyArchonDistanceSquared = GeneralManager.myLocation.distanceSquaredTo(nearestAllyArchonLocation);
                int nearestEnemyArchonDistanceSquared = GeneralManager.myLocation.distanceSquaredTo(nearestEnemyArchonLocation);
                double proportion = (double)nearestAllyArchonDistanceSquared / (nearestAllyArchonDistanceSquared + nearestEnemyArchonDistanceSquared);
                if (proportion > 0.5) {
                    depleteLeadScore += proportion * 8;
                }
            }

            depleteLead = depleteLeadScore >= 5;
        }
    }

    // Get direction to get more resources
    static Direction getNextMiningDir() throws GameActionException {
        MapLocation nearestAllyArchonLoc = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
        int distToNearestAllyArchon = GeneralManager.myLocation.distanceSquaredTo(nearestAllyArchonLoc) + 1;
        int f = 200 / (distToNearestAllyArchon + 1 + RobotPlayer.rc.getRoundNum()/100);
        if (RobotPlayer.rc.senseLead(GeneralManager.myLocation) == 0) {
            f = Integer.MAX_VALUE;
        }

        // See if any enemy attack bots
        Team opponent = RobotPlayer.rc.getTeam().opponent();
        RobotInfo[] enemies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared, opponent);

//        boolean hostileEnemiesNearby = false;
//        for (RobotInfo enemy : enemies) {
//            if (enemy.type.canAttack()) {
//                hostileEnemiesNearby = true;
//            }
//        }

        Direction movementDir = null;
        boolean noResources = true;

        for (MapLocation adj : RobotPlayer.rc.getAllLocationsWithinRadiusSquared(GeneralManager.myLocation, 2)) {
            f -= 2 * RobotPlayer.rc.senseLead(adj) + 5 * RobotPlayer.rc.senseGold(adj);
        }

        for (Direction dir : GeneralManager.DIRECTIONS) {
            if (RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int)(newRubble * 0.5);

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj, 2);

                // Lower cost to move if resources available there or location has not been visited
                for (MapLocation adj2 : adjToAdj) {
                    int senseLead = RobotPlayer.rc.senseLead(adj2);
                    int senseGold = RobotPlayer.rc.senseGold(adj2);
                    if (senseLead == 1 && !depleteLead) senseLead = 0;
                    if (senseLead > 0 || senseGold > 0) noResources = false;
                    newF -= senseLead + senseGold * 5;
                    if (!visited[adj2.x][adj2.y]) newF -= 5;
                }

                // Lower cost to move if move makes miner farther away from enemies
                for (RobotInfo enemy : enemies) {
                    if (enemy.type.canAttack()) {
                        newF -= GeneralManager.getSqDistance(adj, enemy.location) - GeneralManager.getSqDistance(GeneralManager.myLocation, enemy.location);
                    }
                }

                if (visited[adj.x][adj.y]) newF += 40;

                int e = 0;

                // If cost smaller than previous smallest cost, move
                if (newF < f - e) {
                    f = newF;
                    movementDir = dir;
                } else if (newF <= f + e) {
                    if (((int)(Math.random() * 2) == 0)) {
                        f = newF;
                        movementDir = dir;
                    }
                }
            }
        }

        if (noResources) {
            MapLocation resourceLocation = ResourceLocationsManager.minerGetWhereToGo();
            if (resourceLocation != null) {
                return GeneralManager.getNextDir(resourceLocation);
            }
        }

        if (movementDir != null) {
            MapLocation adj = RobotPlayer.rc.adjacentLocation(movementDir);
            visited[adj.x][adj.y] = true;
        }

        return movementDir;
    }
}