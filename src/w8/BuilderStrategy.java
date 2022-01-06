package w8;

import battlecode.common.*;

import java.util.Arrays;

strictfp class BuilderStrategy {
    /**
     * Run a single turn for a Builder.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static int builderTotalBuilt = 0;
    static int builderWatchtowersBuilt = 0;
    static void builderBuild(RobotController rc, RobotType robotType, Direction buildDirection) throws GameActionException {
        rc.buildRobot(robotType, buildDirection);
        switch (robotType) {
            case WATCHTOWER: builderWatchtowersBuilt++; break;
        }
        builderTotalBuilt++;
    }
    static boolean builderTryBuild(RobotController rc, RobotType robotType, Direction preferredDirection) throws GameActionException {
        Direction buildDirection = GeneralManager.getBuildDirection(rc, robotType, preferredDirection);
        if (buildDirection != null) {
            builderBuild(rc, robotType, buildDirection);
            return true;
        }
        return false;
    }
    static void runBuilder(RobotController rc) throws GameActionException {
        // Try to repair prototype building
        RobotInfo[] actionableMine = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
        for (int i = 0; i < actionableMine.length; i++) {
            RobotInfo myRobot = actionableMine[i];
            if (Arrays.asList(GeneralManager.BUILDINGS).contains(myRobot.getType())) {
                if (myRobot.getMode() == RobotMode.PROTOTYPE) {
                    if (rc.canRepair(myRobot.location)) {
                        rc.repair(myRobot.location);
                    }
                }
            }
        }

        // Try to repair damaged building
        for (int i = 0; i < actionableMine.length; i++) {
            RobotInfo myRobot = actionableMine[i];
            if (Arrays.asList(GeneralManager.BUILDINGS).contains(myRobot.getType())) {
                if (myRobot.getHealth() < myRobot.getType().getMaxHealth(myRobot.getLevel())) {
                    if (rc.canRepair(myRobot.location)) {
                        rc.repair(myRobot.location);
                    }
                }
            }
        }

        // Try to build watchtower
        if (GeneralManager.turnCount > (builderWatchtowersBuilt + 1) * 200) {
            builderTryBuild(rc, RobotType.WATCHTOWER, null);
        }

        // Move randomly
        if (actionableMine.length == 0 || actionableMine.length > 5) {
            Direction dir = GeneralManager.getRandomDirection();
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }
}
