package m6;

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

        CombatManager.COMBAT_DROID_ACTIONS action = CombatManager.getCombatDroidAction();
        if (action == CombatManager.COMBAT_DROID_ACTIONS.ATTACK) {
            if (CombatManager.tryAttack()) {
                // Try to attack
            }
            else {
                MapLocation visibleAttackTarget = CombatManager.getAttackTarget(GeneralManager.myType.visionRadiusSquared);
                if (visibleAttackTarget != null) {
                    // Move towards nearest visible enemy
                    GeneralManager.tryMove(getNextSoldierDir(visibleAttackTarget), false);
                }
                else {
                    MapLocation nearestEnemyArchonGuessLocation = ArchonTrackerManager.getNearestEnemyArchonGuessLocation(GeneralManager.myLocation);
                    if (nearestEnemyArchonGuessLocation != null) {
                        // If no enemies are visible, move towards nearest enemy Archon
                        GeneralManager.tryMove(getNextSoldierDir(nearestEnemyArchonGuessLocation), false);
                    }
                    else {
                        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
                        // Unless there is no known enemy Archon
                        // Then move towards the center of the map
//                        GeneralManager.tryMove(getNextSoldierDir(GeneralManager.getMapCenter()), true);
                        GeneralManager.tryMove(getSoldierDirToEncircle(nearestAllyArchonLocation,9), false);
                    }
                }
            }
        }
        else if (action == CombatManager.COMBAT_DROID_ACTIONS.RETREAT) {
            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
//            if (myLocation.distanceSquaredTo(nearestAllyArchonLocation) > 10) {
//                // Move towards nearest ally Archon
//                GeneralManager.tryMove(getNextSoldierDir(nearestAllyArchonLocation), true);
//            }
//            else {
//                action = CombatManager.COMBAT_DROID_ACTIONS.HOLD;
//            }
            GeneralManager.tryMove(getSoldierDirToEncircle(nearestAllyArchonLocation,4), true);
            if (CombatManager.tryAttack()) {
                // Try to attack
            }
        }
        if (action == CombatManager.COMBAT_DROID_ACTIONS.HOLD) {
            RobotInfo[] enemies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared);
            if(enemies.length > 0) {
                GeneralManager.tryMove(getNextSoldierDir(enemies[(int)(Math.random()*enemies.length)].location),false);
            }
            if (CombatManager.tryAttack()) {
                // Try to attack
            }
        }
    }

    static Direction getSoldierDirToEncircle(MapLocation loc, int rSq) throws GameActionException {
        double r = Math.sqrt(rSq);

        int xOffset = 1000, yOffset = 1000;

        while (!GeneralManager.onMap(loc.x + xOffset, loc.y + yOffset)){
            xOffset = (int) (Math.random() * ((int) r + 1)) * ((int) (Math.random() * 3) - 1);
            yOffset = (int) Math.ceil(Math.sqrt(rSq - xOffset * xOffset)) * ((int) (Math.random() * 3) - 1);
        }

        return getNextSoldierDir(new MapLocation(loc.x + xOffset, loc.y + yOffset));
    }

    static Direction getNextSoldierDir(MapLocation dest) throws GameActionException {
        MapLocation myLoc = RobotPlayer.rc.getLocation();
        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(RobotPlayer.rc.getLocation());

        if(myLoc.equals(dest)) return null;
        if(myLoc.distanceSquaredTo(dest) <= myLoc.distanceSquaredTo(nearestAllyArchonLocation))
            return GeneralManager.getDirToEncircle(dest,GeneralManager.myType.actionRadiusSquared);

        Direction movementDir = null;

        int f = Integer.MAX_VALUE;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int)Math.sqrt(newDist) * 2 + newRubble + 20*visitedTurns[adj.x][adj.y];

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj,2);

                for(MapLocation adj2 : adjToAdj) {
                    newF += 2*visitedTurns[adj2.x][adj2.y];
                }

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
            MapLocation adj = RobotPlayer.rc.adjacentLocation(movementDir);
            visitedTurns[adj.x][adj.y]++;
        }
        return movementDir;
    }
}
