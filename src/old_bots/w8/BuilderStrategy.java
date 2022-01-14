package w8;

import battlecode.common.*;

import java.util.Arrays;

strictfp class BuilderStrategy {
    static int totalBuildingsBuilt = 0;
    static int watchtowersBuilt = 0;

    /** Build a building */
    static void builderBuild(RobotController rc, RobotType robotType, Direction buildDirection) throws GameActionException {
        rc.buildRobot(robotType, buildDirection);
        switch (robotType) {
            case WATCHTOWER: watchtowersBuilt++; break;
        }
        totalBuildingsBuilt++;
    }

    /** Try to build a building, returns boolean if successful */
    static boolean builderTryBuild(RobotController rc, RobotType robotType, Direction preferredDirection) throws GameActionException {
        Direction buildDirection = GeneralManager.getBuildDirection(rc, robotType, preferredDirection);
        if (buildDirection != null) {
            builderBuild(rc, robotType, buildDirection);
            return true;
        }
        return false;
    }

    /** Called by RobotPlayer */
    static void runBuilder(RobotController rc) throws GameActionException {
        // Try to repair prototype building
        RobotInfo[] actionableAllies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
        for (int i = 0; i < actionableAllies.length; i++) {
            RobotInfo allyRobot = actionableAllies[i];
            if (Arrays.asList(GeneralManager.BUILDINGS).contains(allyRobot.getType())) {
                if (allyRobot.getMode() == RobotMode.PROTOTYPE) {
                    if (rc.canRepair(allyRobot.location)) {
                        rc.repair(allyRobot.location);
                    }
                }
            }
        }

        // Try to repair damaged building
        for (int i = 0; i < actionableAllies.length; i++) {
            RobotInfo allyRobot = actionableAllies[i];
            if (Arrays.asList(GeneralManager.BUILDINGS).contains(allyRobot.getType())) {
                if (allyRobot.getHealth() < allyRobot.getType().getMaxHealth(allyRobot.getLevel())) {
                    if (rc.canRepair(allyRobot.location)) {
                        rc.repair(allyRobot.location);
                    }
                }
            }
        }

        // Try to build watchtower
        if (GeneralManager.turnCount > (watchtowersBuilt + 1) * 200) {
            builderTryBuild(rc, RobotType.WATCHTOWER, null);
        }

        // Move randomly
        if (actionableAllies.length == 0 || actionableAllies.length > 5) {
            Direction dir = GeneralManager.getRandomDirection();
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }
}
