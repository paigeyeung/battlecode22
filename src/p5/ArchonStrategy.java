package p5;

import battlecode.common.*;

strictfp class ArchonStrategy {
    static boolean archonBroadcastedLocation = false;
    static int archonTotalBuilt = 0;
    static int archonMinersBuilt = 0;
    static int archonBuildersBuilt = 0;
    static int archonSoldiersBuilt = 0;
    static void archonBuild(RobotController rc, RobotType robotType, Direction buildDirection) throws GameActionException {
        rc.buildRobot(robotType, buildDirection);
        switch (robotType) {
            case MINER: archonMinersBuilt++; break;
            case BUILDER: archonBuildersBuilt++; break;
            case SOLDIER: archonSoldiersBuilt++; break;
        }
        archonTotalBuilt++;
    }
    static boolean archonTryBuild(RobotController rc, RobotType robotType, Direction preferredDirection) throws GameActionException {
        Direction buildDirection = GeneralManager.getBuildDirection(rc, robotType, preferredDirection);
        if (buildDirection != null) {
            archonBuild(rc, robotType, buildDirection);
            return true;
        }
        return false;
    }
    static void runArchon(RobotController rc) throws GameActionException {
        // Broadcast my location in shared array indicies 0-3 and instantiate enemy in indicies 4-7
        if (!archonBroadcastedLocation) {
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
                int encodedMyArchonTracker = ArchonTrackerManager.encodeMyArchonTracker(new ArchonTrackerManager.MyArchonTracker(true, myLocation));
                rc.writeSharedArray(i, encodedMyArchonTracker);
                System.out.println("Broadcasted my Archon location " + myLocation + " as " + encodedMyArchonTracker);

                int encodedEnemyArchonTracker = ArchonTrackerManager.encodeEnemyArchonTracker(true, false, myLocation);
                rc.writeSharedArray(i + 4, encodedEnemyArchonTracker);
                System.out.println("Broadcasted enemy Archon as " + encodedEnemyArchonTracker);

                archonBroadcastedLocation = true;
            }
        }

        // If there are hostile enemies nearby, build soldiers and repair soldiers
        // Otherwise, build droids
        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        if (visibleEnemies.length > 0) {
            // Hostile enemies nearby
            if (archonTryBuild(rc, RobotType.SOLDIER, rc.getLocation().directionTo(visibleEnemies[0].location))) {
            }
            else {
                // Repair soldier
                RobotInfo[] actionableMine = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
                for (int i = 0; i < actionableMine.length; i++) {
                    RobotInfo myRobot = actionableMine[i];
                    if (myRobot.type == RobotType.SOLDIER && myRobot.getHealth() < myRobot.getType().getMaxHealth(myRobot.getLevel())) {
                        if (rc.canRepair(myRobot.location)) {
                            rc.repair(myRobot.location);
                        }
                    }
                }
            }
        }
        else {
            // No hostile enemies nearby
            if (archonMinersBuilt < archonTotalBuilt * 0.3) {
                archonTryBuild(rc, RobotType.MINER, null);
            }
            else {
                archonTryBuild(rc, RobotType.SOLDIER, null);
            }
        }
    }
}
