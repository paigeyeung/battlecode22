package w10;

import battlecode.common.*;

strictfp class ArchonStrategy {
    static int mySharedArrayIndex = -1;
    static boolean mySharedArrayToggle = true;

    static int droidsBuilt = 0;
    static int minersBuilt = 0;
    static int buildersBuilt = 0;
    static int soldiersBuilt = 0;

    /** Build a droid */
    static void archonBuild(RobotController rc, RobotType robotType, Direction buildDirection) throws GameActionException {
        rc.buildRobot(robotType, buildDirection);
        switch (robotType) {
            case MINER: minersBuilt++; break;
            case BUILDER: buildersBuilt++; break;
            case SOLDIER: soldiersBuilt++; break;
        }
        droidsBuilt++;
    }

    /** Try to build a droid, returns boolean if successful */
    static boolean archonTryBuild(RobotController rc, RobotType robotType, Direction preferredDirection) throws GameActionException {
        Direction buildDirection = GeneralManager.getBuildDirection(rc, robotType, preferredDirection);
        if (buildDirection != null) {
            archonBuild(rc, robotType, buildDirection);
            return true;
        }
        return false;
    }

    static ArchonTrackerManager.AllyArchonTracker makeMyArchonTracker(RobotController rc) {
        return new ArchonTrackerManager.AllyArchonTracker(true, rc.getLocation(), mySharedArrayToggle);
    }

    /** Called by RobotPlayer */
    static void runArchon(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        // First turn initializations
        if (GeneralManager.turnsAlive == 1) {
            // Broadcast my location in shared array indicies 0-3 and instantiate enemy in indicies 4-7
            // Find first empty array element
            mySharedArrayIndex = 0;
            while (mySharedArrayIndex <= 3) {
                int element = rc.readSharedArray(mySharedArrayIndex);
                if (element == 0) {
                    break;
                }
                mySharedArrayIndex++;
            }
            if (mySharedArrayIndex == 4) {
                DebugManager.log(rc, "SOMETHING WENT WRONG: Archon did not find empty array element");
            }
            else {
                int encodedMyArchonTracker = ArchonTrackerManager.encodeAllyArchonTracker(makeMyArchonTracker(rc));
                rc.writeSharedArray(mySharedArrayIndex, encodedMyArchonTracker);
                DebugManager.log(rc, "Broadcasted my Archon location " + myLocation + " as " + encodedMyArchonTracker);

                int encodedEnemyArchonTracker = ArchonTrackerManager.encodeEnemyArchonTracker(true, false, myLocation);
                rc.writeSharedArray(mySharedArrayIndex + 4, encodedEnemyArchonTracker);
                DebugManager.log(rc, "Broadcasted enemy Archon as " + encodedEnemyArchonTracker);
            }

            // Initialize resource manager
            ArchonResourceManager.initializeTurn1(rc);

            // Build a miner
            archonTryBuild(rc, RobotType.MINER, null);

            // Finish turn
            return;
        }

        // Turn 2 initializations
        if (GeneralManager.turnsAlive == 2) {
            // Initialize resource manager
            ArchonResourceManager.initializeTurn2(rc);
        }

        // Toggle bit in shared array to show alive
        mySharedArrayToggle = !mySharedArrayToggle;
        int encodedMyArchonTracker = ArchonTrackerManager.encodeAllyArchonTracker(makeMyArchonTracker(rc));
        rc.writeSharedArray(mySharedArrayIndex, encodedMyArchonTracker);

        // Get and perform action from ArchonResourceManager
        ArchonResourceManager.computeArchonActions(rc);
        ArchonResourceManager.ARCHON_ACTIONS action = ArchonResourceManager.getArchonAction(mySharedArrayIndex);
        if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_MINER) {
            archonTryBuild(rc, RobotType.MINER, null);
        }
        else if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_BUILDER) {
            archonTryBuild(rc, RobotType.BUILDER, null);
        }
        else if (action == ArchonResourceManager.ARCHON_ACTIONS.BUILD_SOLDIER) {
            archonTryBuild(rc, RobotType.SOLDIER, null);
        }

        // Write to shared array indicies 8-9 for ArchonResourceManager for next turn
        int encodedIndex8 = rc.readSharedArray(8);
        int encodedIndex9Original = rc.readSharedArray(9);
        int encodedIndex9 = encodedIndex9Original;
        // If first alive Archon, reset the elements except cooldowns last turn
        if (mySharedArrayIndex == ArchonTrackerManager.getFirstAliveAllyArchon()) {
            encodedIndex8 = encodedIndex8 & 0xF;
            encodedIndex9 = 0;
        }
        boolean onCooldown = rc.getActionCooldownTurns() > 10;
        encodedIndex9 = encodedIndex9 | (onCooldown ? 1 : 0) << mySharedArrayIndex;
        // If last alive Archon, write lead and gold, and copy cooldowns last turn to this turn
        if (mySharedArrayIndex == ArchonTrackerManager.getLastAliveAllyArchon()) {
            int lead = rc.getTeamLeadAmount(rc.getTeam());
            if (lead > 0xFFF) {
                lead = 0xFFF;
            }
            encodedIndex8 = encodedIndex8 | lead << 4;

            int gold = rc.getTeamGoldAmount(rc.getTeam());
            if (gold > 0xFFF) {
                gold = 0xFFF;
            }
            encodedIndex9 = encodedIndex9 | gold << 4;

            encodedIndex8 = encodedIndex8 & 0xFFF0;
            encodedIndex8 = encodedIndex8 | (encodedIndex9Original & 0xF);
            rc.writeSharedArray(8, encodedIndex8);
        }
        rc.writeSharedArray(9, encodedIndex9);

//        // Start game by building miners
//        if (minersBuilt < 3) {
//            archonTryBuild(rc, RobotType.MINER, null);
//        }
//        else {
//            // If there are combat enemies nearby, build soldiers and repair soldiers
//            // Otherwise, build droids
//            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
//            if (visibleEnemies.length > 0) {
//                // Combat enemies nearby
//                if (archonTryBuild(rc, RobotType.SOLDIER, rc.getLocation().directionTo(visibleEnemies[0].location))) {
//                }
//                else {
//                    // Repair soldier
//                    RobotInfo[] actionableAllies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
//                    for (int i = 0; i < actionableAllies.length; i++) {
//                        RobotInfo allyRobot = actionableAllies[i];
//                        if (allyRobot.type == RobotType.SOLDIER && allyRobot.getHealth() < allyRobot.getType().getMaxHealth(allyRobot.getLevel())) {
//                            if (rc.canRepair(allyRobot.location)) {
//                                rc.repair(allyRobot.location);
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
