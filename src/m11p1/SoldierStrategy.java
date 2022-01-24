package m11p1;

import battlecode.common.*;

strictfp class SoldierStrategy {
    static boolean scouting = false;
    static MapLocation dest = null;
    static Direction storedAttackDirection = null;

    /** Called by RobotPlayer */
    static void runSoldier() throws GameActionException {
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
            if (CombatManager.tryAttack()) {
                // Try to attack
            }
            else {
                MapLocation visibleAttackTarget = CombatManager.getAttackTarget(GeneralManager.myType.visionRadiusSquared);
                if (visibleAttackTarget != null) {
                    // Move towards nearest visible enemy
                    if (DebugManager.drawSoldierLines) RobotPlayer.rc.setIndicatorLine(GeneralManager.myLocation, visibleAttackTarget, 255, 0, 0);
                    storedAttackDirection = getNextSoldierDir(visibleAttackTarget);
                    GeneralManager.tryMove(storedAttackDirection, false);
                }
                else {
                    MapLocation targetEnemyArchonGuessLocation = null;
                    int targetArchon = ArchonTrackerManager.getCentralEnemyArchon();
                    if (targetArchon != -1) {
                        targetEnemyArchonGuessLocation = ArchonTrackerManager.enemyArchonTrackers[targetArchon].getGuessLocation();
                    }
                    //change from getNearestEnemyArchonGuessLocation(GeneralManager.myLocation);
                    if (targetEnemyArchonGuessLocation != null) {
                        if (scouting) {
                            scouting = false;
                            int scoutCount = RobotPlayer.rc.readSharedArray(CommunicationManager.SCOUT_COUNT);
                            RobotPlayer.rc.writeSharedArray(CommunicationManager.SCOUT_COUNT, scoutCount - 1);
                        }

                        // If no enemies are visible, move towards nearest enemy Archon
                        if (DebugManager.drawSoldierLines) RobotPlayer.rc.setIndicatorLine(GeneralManager.myLocation, targetEnemyArchonGuessLocation, 255, 175, 0);
                        GeneralManager.tryMove(getNextSoldierDir(targetEnemyArchonGuessLocation), false);
                    }
                    else {
                        // Unless there is no known enemy Archon
                        // Then scout / retreat
                        int scoutCount = RobotPlayer.rc.readSharedArray(CommunicationManager.SCOUT_COUNT);

                        if (!scouting && scoutCount < RobotPlayer.rc.readSharedArray(CommunicationManager.SAVED_ENEMY_COMBAT_SCORE)/20) {
                            scouting = true;
                            if (scoutCount <= 0) RobotPlayer.rc.writeSharedArray(CommunicationManager.SCOUT_COUNT, 1);
                            else RobotPlayer.rc.writeSharedArray(CommunicationManager.SCOUT_COUNT, scoutCount + 1);
                        }
                        if (scouting) {
                            if (DebugManager.drawSoldierLines) RobotPlayer.rc.setIndicatorLine(GeneralManager.myLocation, GeneralManager.myLocation.add(getSoldierDirToScout()), 0, 100, 100);
                            GeneralManager.tryMove(getSoldierDirToScout(), false);
                        }
                        else if (storedAttackDirection != null) {
                            if (DebugManager.drawSoldierLines) RobotPlayer.rc.setIndicatorLine(GeneralManager.myLocation, GeneralManager.myLocation.add(storedAttackDirection), 0, 255, 255);
                            GeneralManager.tryMove(storedAttackDirection, false);
                        }
                        else {
                            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
                            if (DebugManager.drawSoldierLines) RobotPlayer.rc.setIndicatorLine(GeneralManager.myLocation, nearestAllyArchonLocation, 0, 255, 0);
                            GeneralManager.tryMove(getSoldierDirToEncircle(nearestAllyArchonLocation, 9), false);
                        }
                    }
                }
            }
        }
        else if (action == CombatManager.COMBAT_DROID_ACTIONS.RETREAT) {
            if(scouting) {
                scouting = false;
                int scoutCount = RobotPlayer.rc.readSharedArray(CommunicationManager.SCOUT_COUNT);
                RobotPlayer.rc.writeSharedArray(CommunicationManager.SCOUT_COUNT, scoutCount - 1);
            }

//            MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);
            MapLocation archonNeedingSoldiers = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.findArchonNeedingSoldiers()].location;

//            MapLocation retreatArchonLocation = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.findArchonNeedingSoldiers(false)].location;

            GeneralManager.tryMove(getSoldierDirToEncircle(archonNeedingSoldiers,4), false);
            if (CombatManager.tryAttack()) {
                // Try to attack
            }
        }
        if (action == CombatManager.COMBAT_DROID_ACTIONS.HOLD) {
            if(scouting) {
                scouting = false;
                int scoutCount = RobotPlayer.rc.readSharedArray(CommunicationManager.SCOUT_COUNT);
                RobotPlayer.rc.writeSharedArray(CommunicationManager.SCOUT_COUNT, scoutCount - 1);
            }

            RobotInfo[] enemies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.visionRadiusSquared);
            if(enemies.length > 0) {
                storedAttackDirection = getNextSoldierDir(enemies[(int)(Math.random()*enemies.length)].location);
                GeneralManager.tryMove(storedAttackDirection,false);

//                if(ArchonTrackerManager.getCentralEnemyArchon() == -1) {
//                    dest = GeneralManager.getFarthestLocationInDirection(storedAttackDirection);
//                }
            }
            else if (storedAttackDirection != null) {
                GeneralManager.tryMove(storedAttackDirection, false);
            }
            else {
                GeneralManager.tryMove(getSoldierDirToEncircle(ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation),
                        (int)CombatManager.evaluateLocalCombatScore(GeneralManager.myTeam, true)), false);
//                GeneralManager.tryMove(getSoldierDirToEncircle(GeneralManager.getMapCenter(), 10), false);
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

    static Direction getSoldierDirToScout() throws GameActionException {
        int f = Integer.MAX_VALUE;

        Direction movementDir = null;

        for (Direction dir : GeneralManager.DIRECTIONS) {
            if (RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int) (newRubble * 0.5) + 20*GeneralManager.visitedTurns[adj.x][adj.y];

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj, 2);

                // Higher cost to move to visited locations
                for (MapLocation adj2 : adjToAdj) {
                    newF += (int) (newRubble * 0.5) + 4*GeneralManager.visitedTurns[adj.x][adj.y];
                }

                int e = 0;

                // If cost smaller than previous smallest cost, move
                if (newF < f - e) {
                    f = newF;
                    movementDir = dir;
                } else if (newF <= f + e) {
                    if (((int) (Math.random() * 2) == 0)) {
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