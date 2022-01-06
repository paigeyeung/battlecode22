package w8;

import battlecode.common.*;

strictfp class ArchonStrategy {
    static boolean broadcastedLocation = false;

    static int totalDroidsBuilt = 0;
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
        totalDroidsBuilt++;
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

    /** Called by RobotPlayer */
    static void runArchon(RobotController rc) throws GameActionException {
        // Broadcast my location in shared array indicies 0-3 and instantiate enemy in indicies 4-7
        if (!broadcastedLocation) {
            // Find first empty array element
            int i = 0;
            while (i <= 3) {
                int element = rc.readSharedArray(i);
                if (element == 0) {
                    break;
                }
                i++;
            }
            if (i == 4) {
                System.out.println("SOMETHING WENT WRONG: Archon did not find empty array element");
            }
            else {
                MapLocation myLocation = rc.getLocation();
                int encodedMyArchonTracker = ArchonTrackerManager.encodeAllyArchonTracker(new ArchonTrackerManager.AllyArchonTracker(true, myLocation));
                rc.writeSharedArray(i, encodedMyArchonTracker);
                System.out.println("Broadcasted my Archon location " + myLocation + " as " + encodedMyArchonTracker);

                int encodedEnemyArchonTracker = ArchonTrackerManager.encodeEnemyArchonTracker(true, false, myLocation);
                rc.writeSharedArray(i + 4, encodedEnemyArchonTracker);
                System.out.println("Broadcasted enemy Archon as " + encodedEnemyArchonTracker);

                broadcastedLocation = true;
            }
        }

        // If there are combat enemies nearby, build soldiers and repair soldiers
        // Otherwise, build droids
        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        if (visibleEnemies.length > 0) {
            // Combat enemies nearby
            if (archonTryBuild(rc, RobotType.SOLDIER, rc.getLocation().directionTo(visibleEnemies[0].location))) {
            }
            else {
                // Repair soldier
                RobotInfo[] actionableAllies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
                for (int i = 0; i < actionableAllies.length; i++) {
                    RobotInfo allyRobot = actionableAllies[i];
                    if (allyRobot.type == RobotType.SOLDIER && allyRobot.getHealth() < allyRobot.getType().getMaxHealth(allyRobot.getLevel())) {
                        if (rc.canRepair(allyRobot.location)) {
                            rc.repair(allyRobot.location);
                        }
                    }
                }
            }
        }
        else {
            // No combat enemies nearby
            if (minersBuilt < totalDroidsBuilt * 0.3) {
                archonTryBuild(rc, RobotType.MINER, null);
            }
            else {
                archonTryBuild(rc, RobotType.SOLDIER, null);
            }
        }
    }
}
