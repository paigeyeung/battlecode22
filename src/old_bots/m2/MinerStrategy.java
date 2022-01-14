package m2;

import battlecode.common.*;

strictfp class MinerStrategy {
    static boolean[][] visited = null;
    /** Called by RobotPlayer **/

    static void runMiner(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        if (visited == null) {
            visited = new boolean[rc.getMapWidth()+1][rc.getMapHeight()+1];
        }

        // Try to mine gold
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
            }
        }

        // Try to mine lead
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        Direction dir = getNextMiningDir(rc);

        if (dir != null) {
            GeneralManager.tryMove(rc,dir,false);
        }
    }

    // Get direction to get more resources
    static Direction getNextMiningDir(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();

        ArchonTrackerManager.AllyArchonTracker nearestAllyArchon = ArchonTrackerManager.getNearestAllyArchon(myLoc);
        int distToNearestAllyArchon = myLoc.distanceSquaredTo(nearestAllyArchon.location);
        int f = 200/distToNearestAllyArchon;

        // See if any enemy attack bots
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, opponent);

        boolean hostileEnemiesNearby = false;
        for (RobotInfo enemy : enemies) {
            if (enemy.type.canAttack()) {
                hostileEnemiesNearby = true;
            }
        }

        // See if any ally bots
//        RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());
//        if(allies.length > 3) {
//            f += 15*allies.length;
//        }

        Direction movementDir = null;
//        ArchonTrackerManager.AllyArchonTracker nearestAllyArchon = ArchonTrackerManager.getNearestAllyArchon(myLoc);
//        int distToNearestAllyArchon = myLoc.distanceSquaredTo(nearestAllyArchon.location);

        MapLocation[] nearestGoldLocations = rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared);
        MapLocation[] nearestLeadLocations = rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared);

        if (distToNearestAllyArchon < 4 && !hostileEnemiesNearby) {
            if(nearestGoldLocations.length > 0) {
                MapLocation nearestGoldLocation = null;
                for(MapLocation loc : nearestGoldLocations) {
                    if(rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getTeam().equals(rc.getTeam())) {
                        if(nearestGoldLocation == null ||
                                myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(nearestGoldLocation)) {
                            nearestGoldLocation = loc;
                        }
                    }
                }
                return GeneralManager.getNextDir(rc,nearestGoldLocation);
            }
            if(nearestLeadLocations.length > 0) {
                MapLocation nearestLeadLocation = null;
                for(MapLocation loc : nearestLeadLocations) {
                    if(rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getTeam().equals(rc.getTeam())) {
                        if(nearestLeadLocation == null ||
                                myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(nearestLeadLocation)) {
                            nearestLeadLocation = loc;
                        }
                    }
                }
                return GeneralManager.getNextDir(rc,nearestLeadLocation);
            }
        }

        for(MapLocation adj : rc.getAllLocationsWithinRadiusSquared(myLoc,2)) {
            f -= 2*rc.senseLead(adj) + 5*rc.senseGold(adj);
        }

//        MapLocation enemyArchonLoc = ArchonTrackerManager.getNearestEnemyArchon(myLoc).guessLocation;
//        boolean closeToEnemyArchon = GeneralManager.getSqDistance(myLoc, enemyArchonLoc) < 50 ? true : false;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(rc.canMove(dir)) {
                MapLocation adj = rc.adjacentLocation(dir);
                int newRubble = rc.senseRubble(adj);
                int newF = (int)(newRubble*0.5);
//                if(closeToEnemyArchon) {
//                    f -= 10*(GeneralManager.getSqDistance(myLoc, enemyArchonLoc)
//                            - GeneralManager.getSqDistance(myLoc, enemyArchonLoc)
//                            - GeneralManager.getSqDistance(myLoc, enemyArchonLoc));
//                }

                MapLocation[] adjToAdj = rc.getAllLocationsWithinRadiusSquared(adj,2);

                // Account for resources, visited locations
                for(MapLocation adj2 : adjToAdj) {
                    newF -= rc.senseLead(adj2) + rc.senseGold(adj2)*5;
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
            MapLocation adj = rc.adjacentLocation(movementDir);
            visited[adj.x][adj.y] = true;
        }
        return movementDir;
    }

    // Get direction to get more resources
    static Direction getNextMiningDir(RobotController rc, MapLocation target) throws GameActionException {
        MapLocation currLoc = rc.getLocation();

        // See if any enemy attack bots
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(30, opponent);

        Direction movementDir = null;

        MapLocation nearestFriendlyArchonLoc = ArchonTrackerManager.getNearestAllyArchon(currLoc).location;

        int f = 200/currLoc.distanceSquaredTo(nearestFriendlyArchonLoc);
        for(MapLocation adj : rc.getAllLocationsWithinRadiusSquared(currLoc,2)) {
            f -= 5*rc.senseLead(adj) + 15*rc.senseGold(adj);
        }

//        MapLocation enemyArchonLoc = ArchonTrackerManager.getNearestEnemyArchon(currLoc).guessLocation;

//        boolean closeToEnemyArchon = false;
//        if(GeneralManager.getSqDistance(currLoc, enemyArchonLoc) < 50) {
//            f -= GeneralManager.getSqDistance(currLoc, enemyArchonLoc);
//            closeToEnemyArchon = true;
//        }
//

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(rc.canMove(dir)) {
                MapLocation adj = rc.adjacentLocation(dir);
                int newRubble = rc.senseRubble(adj);
                int newF = (int)(newRubble*0.5);
//                if(closeToEnemyArchon) {
//                    f -= GeneralManager.getSqDistance(currLoc, enemyArchonLoc);
//                }

                MapLocation[] adjToAdj = rc.getAllLocationsWithinRadiusSquared(adj,2);

                // Account for resources, visited locations
                for(MapLocation adj2 : adjToAdj) {
                    newF -= rc.senseLead(adj2) + rc.senseGold(adj2)*5;
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
            MapLocation adj = rc.adjacentLocation(movementDir);
            visited[adj.x][adj.y] = true;
        }
        return movementDir;
    }
}
