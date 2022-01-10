package m5;

import battlecode.common.*;
import jdk.nashorn.internal.runtime.Debug;

strictfp class ArchonStrategy {
    static int mySharedArrayIndex = -1;
    static boolean mySharedArrayToggle = true;

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
        MapLocation locFarthestFromEnemies = ArchonTrackerManager.allyArchonTrackers[ArchonResourceManager.findArchonFarthestFromEnemies()].location;

        if (!RobotPlayer.rc.getMode().canMove) {
            if (RobotPlayer.rc.canTransform() && RobotPlayer.rc.getLocation().distanceSquaredTo(locFarthestFromEnemies) >= ArchonResourceManager.MAX_DISTANCE_TO_NEARBY_ALLY_ARCHON) {
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
//            ArchonTrackerManager.allyArchonTrackers[archonIndex].location = RobotPlayer.rc.adjacentLocation(nextDir);
//            ArchonTrackerManager.decodeAndUpdateLocalAllyArchonTracker(archonIndex,false);
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
        return movementDir;
    }

    /** Called by RobotPlayer */
    static void runArchon() throws GameActionException {
        MapLocation myLocation = RobotPlayer.rc.getLocation();

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

        // Toggle bit in shared array to show alive
        mySharedArrayToggle = !mySharedArrayToggle;
        ArchonTrackerManager.setAllyArchonToggle(mySharedArrayIndex, mySharedArrayToggle);
        ArchonTrackerManager.updateGlobalAllyArchonTracker(mySharedArrayIndex);

        // If first alive Archon, write to shared array indices 8-9 for ArchonResourceManager
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

        if (RobotPlayer.rc.getMode().canMove) {
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
        }

        // Write to shared array indicies 8-9 for ArchonResourceManager
        int encodedResourceManager0 = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX);
        int encodedResourceManager1Original = RobotPlayer.rc.readSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1);
        int encodedResourceManager1 = encodedResourceManager1Original;
        boolean onCooldown = RobotPlayer.rc.getActionCooldownTurns() > 10 || (RobotPlayer.rc.getMode().canMove && RobotPlayer.rc.getMovementCooldownTurns() > 10);
        encodedResourceManager1 = encodedResourceManager1 | (onCooldown ? 1 : 0) << mySharedArrayIndex;
        // If last alive Archon, copy cooldowns last turn to this turn
        if (mySharedArrayIndex == ArchonTrackerManager.getLastAliveAllyArchon()) {
            encodedResourceManager0 = encodedResourceManager0 & 0xFFF0;
            encodedResourceManager0 = encodedResourceManager0 | (encodedResourceManager1Original & 0xF);
            RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX, encodedResourceManager0);
        }
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ARCHON_RESOURCE_MANAGER_INDEX + 1, encodedResourceManager1);

//        // Start game by building miners
//        if (minersBuilt < 3) {
//            archonTryBuild(rc, RobotType.MINER, null);
//        }
//        else {
//            // If there are combat enemies nearby, build soldiers and repair soldiers
//            // Otherwise, build droids
//            RobotInfo[] visibleEnemies = RobotPlayer.rc.senseNearbyRobots(RobotPlayer.rc.getType().visionRadiusSquared, RobotPlayer.rc.getTeam().opponent());
//            if (visibleEnemies.length > 0) {
//                // Combat enemies nearby
//                if (archonTryBuild(rc, RobotType.SOLDIER, RobotPlayer.rc.getLocation().directionTo(visibleEnemies[0].location))) {
//                }
//                else {
//                    // Repair soldier
//                    RobotInfo[] actionableAllies = RobotPlayer.rc.senseNearbyRobots(RobotPlayer.rc.getType().actionRadiusSquared, RobotPlayer.rc.getTeam());
//                    for (int i = 0; i < actionableAllies.length; i++) {
//                        RobotInfo allyRobot = actionableAllies[i];
//                        if (allyRobot.type == RobotType.SOLDIER && allyRobot.getHealth() < allyRobot.getType().getMaxHealth(allyRobot.getLevel())) {
//                            if (RobotPlayer.rc.canRepair(allyRobot.location)) {
//                                RobotPlayer.rc.repair(allyRobot.location);
//                            }
//                        }
//                    }
//                }
//            }
//            else {
//                // No combat enemies nearby
//                if (minersBuilt < droidsBuilt * 0.3) {
//                    archonTryBuild(rc, RobotType.MINER, null);
//                }
//                else {
//                    archonTryBuild(rc, RobotType.SOLDIER, null);
//                }
//            }
//        }
    }
}
