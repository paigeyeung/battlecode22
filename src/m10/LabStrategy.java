package m10;

import battlecode.common.*;

strictfp class LabStrategy {
    /** Called by RobotPlayer */
    static void runLab() throws GameActionException {
        if(RobotPlayer.rc.canTransmute()) RobotPlayer.rc.transmute();

        RobotPlayer.rc.writeSharedArray(CommunicationManager.BUILDING_INFO,
                ((1 << 2) | (RobotPlayer.rc.getRoundNum() % 4)));
    }

    static boolean isLab() throws GameActionException {
        return ((RobotPlayer.rc.readSharedArray(CommunicationManager.BUILDING_INFO) >>> 2) & 0x1) == 1;
    }

    static boolean isBuilderBuildingLab() throws GameActionException {
        return ((RobotPlayer.rc.readSharedArray(CommunicationManager.BUILDING_INFO) >>> 5) & 0x1) == 1;
    }

    static void setBuilderBuildingLab(boolean building) throws GameActionException {
        int buildingInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.BUILDING_INFO);
        int b = building ? 1 : 0;
        RobotPlayer.rc.writeSharedArray(CommunicationManager.BUILDING_INFO,
                ((buildingInfo >>> 6) << 6)  | (b << 5) | (RobotPlayer.rc.getRoundNum() % 4) << 3 |
                        (buildingInfo & 0x7));
    }

    static void setLabAlive() throws GameActionException {
        int buildingInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.BUILDING_INFO);
        RobotPlayer.rc.writeSharedArray(CommunicationManager.BUILDING_INFO,
                ((buildingInfo >>> 3) << 3) | 1 | (RobotPlayer.rc.getRoundNum() % 4));
    }
}
