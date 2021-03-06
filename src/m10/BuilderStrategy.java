package m10;

import battlecode.common.*;

import java.util.Arrays;

strictfp class BuilderStrategy {
    static int totalBuildingsBuilt = 0;
    static int watchtowersBuilt = 0;
    static boolean buildingLab = false;

    /** Build a building */
    static void builderBuild(RobotType robotType, Direction buildDirection) throws GameActionException {
        RobotPlayer.rc.buildRobot(robotType, buildDirection);
        switch (robotType) {
            case WATCHTOWER: watchtowersBuilt++; break;
        }
        totalBuildingsBuilt++;
    }

    /** Try to build a building, returns boolean if successful */
    static boolean builderTryBuild(RobotType robotType, Direction preferredDirection) throws GameActionException {
        Direction buildDirection = GeneralManager.getBuildDirection(robotType, preferredDirection);
        if (buildDirection != null) {
            builderBuild(robotType, buildDirection);
            return true;
        }
        return false;
    }

    /** Called by RobotPlayer */
    static void runBuilder() throws GameActionException {
        if (GeneralManager.visitedTurns == null) {
            GeneralManager.visitedTurns = new int[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
            for (int i = 0; i < GeneralManager.visitedTurns.length; i++) {
                for (int j = 0; j < GeneralManager.visitedTurns[i].length; j++) {
                    GeneralManager.visitedTurns[i][j] = 0;
                }
            }
        }

        // Try to repair prototype building
        RobotInfo[] actionableAllies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.actionRadiusSquared, GeneralManager.myTeam);

        for (int i = 0; i < actionableAllies.length; i++) {
            RobotInfo allyRobot = actionableAllies[i];
            if (Arrays.asList(GeneralManager.BUILDINGS).contains(allyRobot.getType())) {
                if (allyRobot.getMode() == RobotMode.PROTOTYPE) {
                    if (RobotPlayer.rc.canRepair(allyRobot.location)) {
                        RobotPlayer.rc.repair(allyRobot.location);
                    }
                }
            }
        }

        // Try to repair damaged building
        for (int i = 0; i < actionableAllies.length; i++) {
            RobotInfo allyRobot = actionableAllies[i];
            if (Arrays.asList(GeneralManager.BUILDINGS).contains(allyRobot.getType())) {
                if (allyRobot.getHealth() < allyRobot.getType().getMaxHealth(allyRobot.getLevel())) {
                    if (RobotPlayer.rc.canRepair(allyRobot.location)) {
                        RobotPlayer.rc.repair(allyRobot.location);
                    }
                }
                else if(RobotPlayer.rc.canMutate(allyRobot.location)) {
                    RobotPlayer.rc.mutate(allyRobot.location);
                }
            }
        }

        if((!LabStrategy.isLab() && !LabStrategy.isBuilderBuildingLab())
                || buildingLab) {
            MapLocation corner = GeneralManager.getNearestCorner(
                    ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation)
            );

            buildingLab = true;
            if(GeneralManager.myLocation.distanceSquaredTo(corner) <= 2) {
                if (RobotPlayer.rc.canBuildRobot(RobotType.LABORATORY, GeneralManager.myLocation.directionTo(corner))) {
                    RobotPlayer.rc.buildRobot(RobotType.LABORATORY, GeneralManager.myLocation.directionTo(corner));
                    LabStrategy.setLabAlive();
                    buildingLab = false;
                }
            }
            else {
                Direction nextBuildingDir = getNextBuilderDir(corner);
                if(nextBuildingDir != null && RobotPlayer.rc.canMove(nextBuildingDir)) {
                    MapLocation loc = RobotPlayer.rc.adjacentLocation(nextBuildingDir);
                    if (GeneralManager.visitedTurns[loc.x][loc.y] < 4)
                        GeneralManager.tryMove(nextBuildingDir, false);
                    else {
                        Direction minRubbleBuildDir = null;
                        int minRubble = Integer.MAX_VALUE;

                        for (MapLocation adj : RobotPlayer.rc.getAllLocationsWithinRadiusSquared(GeneralManager.myLocation,
                                2)) {
                            if (!adj.equals(GeneralManager.myLocation) && RobotPlayer.rc.senseRubble(adj) < minRubble) {
                                minRubbleBuildDir = GeneralManager.myLocation.directionTo(adj);
                            }
                        }
                        if (minRubbleBuildDir != null && RobotPlayer.rc.canBuildRobot(RobotType.LABORATORY, minRubbleBuildDir)) {
                            RobotPlayer.rc.buildRobot(RobotType.LABORATORY, minRubbleBuildDir);
                            LabStrategy.setLabAlive();
                            buildingLab = false;
                        }
                    }
                }
            }
            LabStrategy.setBuilderBuildingLab(buildingLab);
        }
        else {
            // Try to build watchtower
            if (GeneralManager.turnsAlive > (watchtowersBuilt + 1) * 200) {
                builderTryBuild(RobotType.WATCHTOWER, null);
            }

            if (actionableAllies.length == 0 || actionableAllies.length > 5) {
                MapLocation targetEnemyArchonGuessLocation = null;
                int centralEnemyArchon = ArchonTrackerManager.getCentralEnemyArchon();
                if (centralEnemyArchon != -1) {
                    targetEnemyArchonGuessLocation = ArchonTrackerManager.enemyArchonTrackers[centralEnemyArchon].getGuessLocation();
                }
                //change from getNearestEnemyArchonGuessLocation(GeneralManager.myLocation);
                if (targetEnemyArchonGuessLocation != null) {
                    // If no enemies are visible, move towards nearest enemy Archon
                    GeneralManager.tryMove(getNextBuilderDir(targetEnemyArchonGuessLocation), false);
                }
            }
        }
    }

    static Direction getNextBuilderDir(MapLocation dest) throws GameActionException {
        MapLocation nearestAllyArchonLocation = ArchonTrackerManager.getNearestAllyArchonLocation(GeneralManager.myLocation);

        if (GeneralManager.myLocation.equals(dest)) return null;
        if (GeneralManager.myLocation.distanceSquaredTo(dest) <= GeneralManager.myLocation.distanceSquaredTo(nearestAllyArchonLocation))
            return GeneralManager.getDirToEncircle(dest,GeneralManager.myType.actionRadiusSquared);

        Direction movementDir = null;

        int f = Integer.MAX_VALUE;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = (int)Math.sqrt(newDist) * 4 + newRubble + 20 * GeneralManager.visitedTurns[adj.x][adj.y];

                MapLocation[] adjToAdj = RobotPlayer.rc.getAllLocationsWithinRadiusSquared(adj,2);

                for(MapLocation adj2 : adjToAdj) {
                    newF += 2 * GeneralManager.visitedTurns[adj2.x][adj2.y];
                }

                if(newF < f) {
                    f = newF;
                    movementDir = dir;
                }
                else if(newF == f){
                    if(((int)Math.random() * 2) == 0) {
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
