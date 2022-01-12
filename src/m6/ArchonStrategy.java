package m6;

import battlecode.common.*;
import jdk.nashorn.internal.runtime.Debug;

strictfp class ArchonStrategy {
    static int mySharedArrayIndex = -1;
    static boolean mySharedArrayToggle = true;
    static boolean[][] visited = null;

    static int droidsBuilt = 0;
    static int minersBuilt = 0;
    static int buildersBuilt = 0;
    static int soldiersBuilt = 0;

    static MapLocation nearestLeadLocation;

    /** Build a droid */
    static void archonBuild(RobotType robotType, Direction buildDirection) throws GameActionException {
        RobotPlayer.rc.buildRobot(robotType, buildDirection);
        switch (robotType) {
            case MINER: minersBuilt++; break;
            case BUILDER: buildersBuilt++; break;
            case SOLDIER: soldiersBuilt++; break;
        }
        droidsBuilt++;
    }

    /** Try to build a droid, returns boolean if successful */
    static boolean archonTryBuild(RobotType robotType) throws GameActionException {
        MapLocation myLocation = RobotPlayer.rc.getLocation();
        Direction preferredDirection = null;
        if (robotType == RobotType.MINER) {
            if (nearestLeadLocation != null) {
                preferredDirection = myLocation.directionTo(nearestLeadLocation);
            }
        }
        else if (robotType == RobotType.SOLDIER) {
            preferredDirection = myLocation.directionTo(ArchonTrackerManager.getNearestEnemyArchonGuessLocation(myLocation));
        }
        Direction buildDirection = GeneralManager.getBuildDirection(robotType, preferredDirection);
        if (buildDirection != null) {
            archonBuild(robotType, buildDirection);
            return true;
        }
        return false;
    }

    static boolean archonTryMove() throws GameActionException {
        MapLocation locFarthestFromEnemies = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.farthestArchonIndex].location;

        if(ArchonResourceManager.findArchonFarthestFromEnemies(true) != -1)
            locFarthestFromEnemies = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.findArchonFarthestFromEnemies(true)].location;

        if (!RobotPlayer.rc.getMode().canMove) {
            if (RobotPlayer.rc.canTransform() &&
                    RobotPlayer.rc.getLocation().distanceSquaredTo(locFarthestFromEnemies) >= ArchonResourceManager.MAX_DISTANCE_TO_NEARBY_ALLY_ARCHON) {
                RobotPlayer.rc.transform();
                return true;
            }
            return false;
        }

        if (RobotPlayer.rc.getLocation().distanceSquaredTo(locFarthestFromEnemies) < ArchonResourceManager.MAX_DISTANCE_TO_NEARBY_ALLY_ARCHON) {
            if (RobotPlayer.rc.getMode().canMove && RobotPlayer.rc.canTransform()) {
                RobotPlayer.rc.transform();
                return true;
            }
            return false;
        }

        Direction nextDir = getNextArchonDir(locFarthestFromEnemies);
        boolean moved = GeneralManager.tryMove(nextDir,false);

        if (moved) {
            ArchonTrackerManager.setAllyArchonLocation(mySharedArrayIndex, RobotPlayer.rc.getLocation());
        }

        return moved;
    }

    static Direction getNextArchonDir(MapLocation dest) throws GameActionException {
        MapLocation myLoc = RobotPlayer.rc.getLocation();

        if(myLoc.equals(dest) || dest == null) return null;

        Direction movementDir = null;
//        int minDist = myLoc.distanceSquaredTo(dest);
        int f = Integer.MAX_VALUE;

        for(Direction dir : GeneralManager.DIRECTIONS) {
            if(RobotPlayer.rc.canMove(dir)) {
                MapLocation adj = RobotPlayer.rc.adjacentLocation(dir);
                int newDist = adj.distanceSquaredTo(dest);
                int newRubble = RobotPlayer.rc.senseRubble(adj);
                int newF = newDist + newRubble;
                if(visited[adj.x][adj.y]) newF += 100;

                if(newF < f) {
                    f = newF;
                    movementDir = dir;
                    visited[adj.x][adj.y] = true;
                }
                else if(newF == f){
                    if(((int)Math.random()*2)==0) {
                        f = newF;
                        movementDir = dir;
                        visited[adj.x][adj.y] = true;
                    }
                }
            }
        }
        return movementDir;
    }

    static boolean archonTryRepair() throws GameActionException {
        RobotInfo[] actionableAllies = RobotPlayer.rc.senseNearbyRobots(RobotPlayer.rc.getType().actionRadiusSquared, RobotPlayer.rc.getTeam());
        for (int i = 0; i < actionableAllies.length; i++) {
            RobotInfo allyRobot = actionableAllies[i];
            if (allyRobot.getHealth() < allyRobot.getType().getMaxHealth(allyRobot.getLevel())) {
                if (RobotPlayer.rc.canRepair(allyRobot.location)) {
                    RobotPlayer.rc.repair(allyRobot.location);
                    return true;
                }
            }
        }
        return false;
    }

    /** Called by RobotPlayer */
    static void runArchon() throws GameActionException {
        MapLocation myLocation = RobotPlayer.rc.getLocation();

        if (visited == null) {
            visited = new boolean[GeneralManager.mapWidth + 1][GeneralManager.mapHeight + 1];
        }

        // First turn initializations
        if (GeneralManager.turnsAlive == 1) {
            // Broadcast my location in shared array indices 0-3 and instantiate enemy in indices 4-7
            // Find first empty array element
            mySharedArrayIndex = 0;
            while (mySharedArrayIndex <= 3) {
                int element = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + mySharedArrayIndex);
                if (element == 0) {
                    break;
                }
                mySharedArrayIndex++;
            }
            if (mySharedArrayIndex == 4) {
                DebugManager.log("SOMETHING WENT WRONG: Archon did not find empty array element");
            }
            else {
                ArchonTrackerManager.updateGlobalAllyArchonTrackerFirstTime(mySharedArrayIndex, true, myLocation, mySharedArrayToggle);
                ArchonTrackerManager.updateGlobalEnemyArchonTrackerFirstTime(mySharedArrayIndex, true, myLocation, false, 0, false);
            }

            // Initialize resource manager
            ArchonResourceManager.initializeTurn1();

            // Get nearest lead location
            nearestLeadLocation = GeneralManager.getNearestLeadLocation();
            if (nearestLeadLocation == null) {
                DebugManager.log("SOMETHING WENT WRONG: Nearest lead not found!");
            }

            // Build a miner
            archonTryBuild(RobotType.MINER);

            // Finish turn
            return;
        }

        // Turn 2 initializations
        if (GeneralManager.turnsAlive == 2) {
            // Initialize resource manager
            ArchonResourceManager.initializeTurn2();
        }

        // Toggle bit in shared array ALLY_ARCHON_TRACKERS_INDEX to show alive
        mySharedArrayToggle = !mySharedArrayToggle;
        ArchonTrackerManager.setAllyArchonToggle(mySharedArrayIndex, mySharedArrayToggle);
        ArchonTrackerManager.updateGlobalAllyArchonTracker(mySharedArrayIndex);

        // If first alive Archon, write to shared array ARCHON_RESOURCE_MANAGER_INDEX
        // Reset the array indices except cooldowns last turn, then write lead and gold
        if (mySharedArrayIndex == ArchonTrackerManager.getFirstAliveAllyArchon()) {
            int encodedResourceManager0 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX);
            encodedResourceManager0 = encodedResourceManager0 & 0xF;
            int lead = RobotPlayer.rc.getTeamLeadAmount(RobotPlayer.rc.getTeam());
            if (lead > 0xFFF) {
                lead = 0xFFF;
            }
            encodedResourceManager0 = encodedResourceManager0 | lead << 4;

            int encodedResourceManager1 = 0;
            int gold = RobotPlayer.rc.getTeamGoldAmount(RobotPlayer.rc.getTeam());
            if (gold > 0xFFF) {
                gold = 0xFFF;
            }
            encodedResourceManager1 = encodedResourceManager1 | gold << 4;

            RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX, encodedResourceManager0);
            RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1, encodedResourceManager1);
        }

        if (RobotPlayer.rc.getMode() == RobotMode.PORTABLE) {
            // If we're portable, don't try to do anything else
            archonTryMove();
        }
        else {
            // Get and perform action from ArchonResourceManager
            ArchonResourceManager.computeArchonActions();
            ArchonResourceManager.ARCHON_ACTIONS action = ArchonResourceManager.getArchonAction(mySharedArrayIndex);
            if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_MINER) {
                archonTryBuild(RobotType.MINER);
            }
            else if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_BUILDER) {
                archonTryBuild(RobotType.BUILDER);
            }
            else if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_SOLDIER) {
                archonTryBuild(RobotType.SOLDIER);
            }
            else if (action == ArchonResourceManager.ARCHON_ACTIONS.MOVE) {
                archonTryMove();
            }
            else {
                archonTryRepair();
            }
        }

        // Write to shared array ARCHON_RESOURCE_MANAGER_INDEX
        int encodedResourceManager0 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX);
        int encodedResourceManager1 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1);
        boolean onCooldown = RobotPlayer.rc.getActionCooldownTurns() > 10 || RobotPlayer.rc.getMode() == RobotMode.PORTABLE;
        encodedResourceManager1 = encodedResourceManager1 | ((onCooldown ? 1 : 0) << mySharedArrayIndex);
        // If last alive Archon, copy cooldowns last turn to this turn
        if (mySharedArrayIndex == ArchonTrackerManager.getLastAliveAllyArchon()) {
            encodedResourceManager0 = encodedResourceManager0 & 0xFFF0;
            encodedResourceManager0 = encodedResourceManager0 | (encodedResourceManager1 & 0xF);
            RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX, encodedResourceManager0);
        }
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1, encodedResourceManager1);
    }
}
