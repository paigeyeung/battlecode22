package m1p1;

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
        MapLocation currLoc = rc.getLocation();

        // See if any enemy attack bots
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(20, opponent);
//        ArrayList<MapLocation> enemyLocs = new ArrayList<>();

        Direction movementDir = null;
//        int f = - 1 * rc.senseLead(currLoc) - 5 * rc.senseGold(currLoc);
        int distToNearestAllyArchon = currLoc.distanceSquaredTo(ArchonTrackerManager.getNearestAllyArchon(currLoc).location);

        int f = 0;

        for(MapLocation adj : rc.getAllLocationsWithinRadiusSquared(currLoc,2)) {
            f -= 2*rc.senseLead(adj) + 5*rc.senseGold(adj);
        }

        MapLocation enemyArchonLoc = ArchonTrackerManager.getNearestEnemyArchon(currLoc).guessLocation;

        boolean closeToEnemyArchon = false;
        if(GeneralManager.getSqDistance(currLoc, enemyArchonLoc) < 50) {
            closeToEnemyArchon = true;
        }

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(rc.canMove(dir)) {
                MapLocation adj = rc.adjacentLocation(dir);
                int newRubble = rc.senseRubble(adj);
                int newF = (int)(newRubble*0.5);
                if(closeToEnemyArchon) {
                    f -= 10*(GeneralManager.getSqDistance(currLoc, enemyArchonLoc) - GeneralManager.getSqDistance(currLoc, enemyArchonLoc) - GeneralManager.getSqDistance(currLoc, enemyArchonLoc));
                }

                MapLocation[] adjToAdj = rc.getAllLocationsWithinRadiusSquared(adj,2);

                // Account for resources, visited locations
                for(MapLocation adj2 : adjToAdj) {
                    newF -= rc.senseLead(adj2) + rc.senseGold(adj2)*5;
                    if(!visited[adj2.x][adj2.y]) newF -= 10;
                }

                // Account for enemies by adding to cost
                for(RobotInfo enemy : enemies) {
                    if(enemy.type.canAttack()) {
                        newF -= GeneralManager.getSqDistance(adj, enemy.location) - GeneralManager.getSqDistance(currLoc, enemy.location);;
                    }
                }

                if (visited[adj.x][adj.y]) newF += 100;

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
