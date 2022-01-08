package m3w1;

import battlecode.common.*;

strictfp class MinerStrategy {
    static boolean[][] visited = null;
    /** Called by RobotPlayer **/

    static void runMiner() throws GameActionException {
        MapLocation myLocation = RobotPlayer.rc.getLocation();

        if (visited == null) {
            visited = new boolean[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
        }

        // Try to mine gold
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (RobotPlayer.rc.canMineGold(mineLocation)) {
                    RobotPlayer.rc.mineGold(mineLocation);
                }
            }
        }

        // Try to mine lead
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (RobotPlayer.rc.canMineLead(mineLocation)) {
                    RobotPlayer.rc.mineLead(mineLocation);
                }
            }
        }

        Direction dir = getNextMiningDir();

        if (dir != null) {
            GeneralManager.tryMove(dir, false);
        }
    }

    // Get direction to get more resources
    static Direction getNextMiningDir() throws GameActionException {
        MapLocation myLoc = RobotPlayer.rc.getLocation();

        ArchonTrackerManager.AllyArchonTracker nearestAllyArchon = ArchonTrackerManager.getNearestAllyArchon(myLoc);
        int distToNearestAllyArchon = myLoc.distanceSquaredTo(nearestAllyArchon.location);
        int f = 200/distToNearestAllyArchon;

        // See if any enemy attack bots
        Team opponent = RobotPlayer.rc.getTeam().opponent();
        RobotInfo[] enemies = RobotPlayer.rc.senseNearbyRobots(RobotPlayer.rc.getType().visionRadiusSquared, opponent);

        boolean hostileEnemiesNearby = false;
        for (RobotInfo enemy : enemies) {
            if (enemy.type.canAttack()) {
                hostileEnemiesNearby = true;
            }
        }

        // See if any ally bots
//        RobotInfo[] allies = RobotPlayer.rc.senseNearbyRobots(2, RobotPlayer.rc.getTeam());
//        if(allies.length > 3) {
//            f += 15*allies.length;
//        }

        Direction movementDir = null;
//        ArchonTrackerManager.AllyArchonTracker nearestAllyArchon = ArchonTrackerManager.getNearestAllyArchon(myLoc);
//        int distToNearestAllyArchon = myLoc.distanceSquaredTo(nearestAllyArchon.location);

        MapLocation[] nearestGoldLocations = RobotPlayer.rc.senseNearbyLocationsWithGold(RobotPlayer.rc.getType().visionRadiusSquared);
        MapLocation[] nearestLeadLocations = RobotPlayer.rc.senseNearbyLocationsWithGold(RobotPlayer.rc.getType().visionRadiusSquared);

        if (distToNearestAllyArchon < 4 && !hostileEnemiesNearby) {
            if(nearestGoldLocations.length > 0) {
                MapLocation nearestGoldLocation = null;
                for(MapLocation loc : nearestGoldLocations) {
                    if(RobotPlayer.rc.canSenseRobotAtLocation(loc) && RobotPlayer.rc.senseRobotAtLocation(loc).getTeam().equals(RobotPlayer.rc.getTeam())) {
                        if(nearestGoldLocation == null ||
                                myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(nearestGoldLocation)) {
                            nearestGoldLocation = loc;
                        }
                    }
                }
                return GeneralManager.getNextDir(nearestGoldLocation);
            }
            if(nearestLeadLocations.length > 0) {
                MapLocation nearestLeadLocation = null;
                for(MapLocation loc : nearestLeadLocations) {
                    if(RobotPlayer.rc.canSenseRobotAtLocation(loc) && RobotPlayer.rc.senseRobotAtLocation(loc).getTeam().equals(RobotPlayer.rc.getTeam())) {
                        if(nearestLeadLocation == null ||
                                myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(nearestLeadLocation)) {
                            nearestLeadLocation = loc;
                        }
                    }
                }
                return GeneralManager.getNextDir(nearestLeadLocation);
            }
        }

        for(MapLocation adj : RobotPlayer.rc.getAllLocationsWithinRadiusSquared(myLoc,2)) {
            f -= 2*RobotPlayer.rc.senseLead(adj) + 5*RobotPlayer.rc.senseGold(adj);
        }

//        MapLocation enemyArchonLoc = ArchonTrackerManager.getNearestEnemyArchon(myLoc).guessLocation;
//        boolean closeToEnemyArchon = GeneralManager.getSqDistance(myLoc, enemyArchonLoc) < 50 ? true : false;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int)(newRubble*0.5);
//                if(closeToEnemyArchon) {
//                    f -= 10*(GeneralManager.getSqDistance(myLoc, enemyArchonLoc)
//                            - GeneralManager.getSqDistance(myLoc, enemyArchonLoc)
//                            - GeneralManager.getSqDistance(myLoc, enemyArchonLoc));
//                }

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj,2);

                // Account for resources, visited locations
                for(MapLocation adj2 : adjToAdj) {
                    newF -= RobotPlayer.rc.senseLead(adj2) + RobotPlayer.rc.senseGold(adj2)*5;
                    if(!visited[adj2.x][adj2.y]) newF -= 5;
                }

                // Account for enemies by adding to cost
                for(RobotInfo enemy : enemies) {
                    if(enemy.type.canAttack()) {
                        newF -= GeneralManager.getSqDistance(adj, enemy.location) - GeneralManager.getSqDistance(myLoc, enemy.location);;
                    }
                }

                if (visited[adj.x][adj.y]) newF += 90;

                int e = 0;

                // If cost smaller than previous smallest cost, move
                if(newF < f - e) {
                    f = newF;
                    movementDir = dir;
                }
                else if(newF <= f + e){
                    if(((int)(Math.random()*2)==0)) {
                        f = newF;
                        movementDir = dir;
                    }
                }
            }
        }

        if (movementDir != null) {
            MapLocation adj = RobotPlayer.rc.adjacentLocation(movementDir);
            visited[adj.x][adj.y] = true;
        }
        return movementDir;
    }

    // Get direction to get more resources
    static Direction getNextMiningDir(MapLocation target) throws GameActionException {
        MapLocation currLoc = RobotPlayer.rc.getLocation();

        // See if any enemy attack bots
        Team opponent = RobotPlayer.rc.getTeam().opponent();
        RobotInfo[] enemies = RobotPlayer.rc.senseNearbyRobots(30, opponent);

        Direction movementDir = null;

        MapLocation nearestFriendlyArchonLoc = ArchonTrackerManager.getNearestAllyArchon(currLoc).location;

        int f = 200/currLoc.distanceSquaredTo(nearestFriendlyArchonLoc);
        for(MapLocation adj : RobotPlayer.rc.getAllLocationsWithinRadiusSquared(currLoc,2)) {
            f -= 5*RobotPlayer.rc.senseLead(adj) + 15*RobotPlayer.rc.senseGold(adj);
        }

//        MapLocation enemyArchonLoc = ArchonTrackerManager.getNearestEnemyArchon(currLoc).guessLocation;

//        boolean closeToEnemyArchon = false;
//        if(GeneralManager.getSqDistance(currLoc, enemyArchonLoc) < 50) {
//            f -= GeneralManager.getSqDistance(currLoc, enemyArchonLoc);
//            closeToEnemyArchon = true;
//        }
//

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int)(newRubble*0.5);
//                if(closeToEnemyArchon) {
//                    f -= GeneralManager.getSqDistance(currLoc, enemyArchonLoc);
//                }

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj,2);

                // Account for resources, visited locations
                for(MapLocation adj2 : adjToAdj) {
                    newF -= RobotPlayer.rc.senseLead(adj2) + RobotPlayer.rc.senseGold(adj2)*5;
                    if(!visited[adj2.x][adj2.y]) newF -= 40;
                }

                // Account for enemies by adding to cost
                for(RobotInfo enemy : enemies) {
                    if(enemy.type.canAttack()) {
                        newF -= 5*(GeneralManager.getSqDistance(adj, enemy.location)-GeneralManager.getSqDistance(currLoc, enemy.location));
                    }
                }

                if (visited[adj.x][adj.y]) newF += 300;

                int e = 0;

                // If cost smaller than previous smallest cost, move
                if(newF < f - e) {
                    f = newF;
                    movementDir = dir;
                }
                else if(newF <= f + e){
                    if(((int)(Math.random()*2)==0)) {
                        f = newF;
                        movementDir = dir;
                    }
                }
            }
        }

        if (movementDir != null) {
            MapLocation adj = RobotPlayer.rc.adjacentLocation(movementDir);
            visited[adj.x][adj.y] = true;
        }
        return movementDir;
    }
}
