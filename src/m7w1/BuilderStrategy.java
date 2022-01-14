package m7w1;

import battlecode.common.*;

import java.util.Arrays;

strictfp class BuilderStrategy {
    static int totalBuildingsBuilt = 0;
    static int watchtowersBuilt = 0;

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
        // Try to repair prototype building
        RobotInfo[] actionableAllies = RobotPlayer.rc.senseNearbyRobots(GeneralManager.myType.actionRadiusSquared, RobotPlayer.rc.getTeam());
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
            }
        }

        // Try to build watchtower
        if (GeneralManager.turnsAlive > (watchtowersBuilt + 1) * 200) {
            builderTryBuild(RobotType.WATCHTOWER, null);
        }

        // Move randomly
        if (actionableAllies.length == 0 || actionableAllies.length > 5) {
            Direction dir = GeneralManager.getRandomDirection();
            if (RobotPlayer.rc.canMove(dir)) {
                RobotPlayer.rc.move(dir);
                GeneralManager.myLocation = RobotPlayer.rc.getLocation();
            }
        }
    }
}
