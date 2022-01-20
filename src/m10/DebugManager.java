package m10;

import battlecode.common.*;

strictfp class DebugManager {
    static String botName = "m10";
    static void log(String string) {
        System.out.println(botName + " - " + RobotPlayer.rc.getRoundNum() + " - " + string);
        RobotPlayer.rc.setIndicatorString(string);
    }

    static int archonMismatchTurns = 0;

    static boolean drawMinerLines = true;
    static boolean drawSoldierLines = true;

    static void sanityCheck() {
        if (ArchonTrackerManager.receivedArchonTrackers) {
            // Check ArchonTrackerManager consistent with RC
            int numAllyArchonsAlive = 0;
            for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                if (ArchonTrackerManager.allyArchonTrackers[i].alive) {
                    numAllyArchonsAlive++;
                }
            }
            if (numAllyArchonsAlive != RobotPlayer.rc.getArchonCount()) {
                if (archonMismatchTurns >= 2) {
                    log("SOMETHING WENT WRONG: Ally Archon count mismatch " + numAllyArchonsAlive + " and " + RobotPlayer.rc.getArchonCount());
                }
                else {
                    archonMismatchTurns++;
                }
            }
            else {
                archonMismatchTurns = 0;
            }

            if (GeneralManager.myType == RobotType.ARCHON) {
                // Check ArchonTrackerManager consistent with ArchonResourceManager
                for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                    if (ArchonTrackerManager.allyArchonTrackers[i].alive != ArchonResourceManager.allyArchonModels[i].alive) {
                        log("SOMETHING WENT WRONG: Ally Archon " + i + " ArchonTrackerManager alive: " + ArchonTrackerManager.allyArchonTrackers[i].alive + " and ArchonResourceManager alive: " + ArchonResourceManager.allyArchonModels[i].alive);
                    }
                }
            }
        }
    }

    static void drawResourceLocations() throws GameActionException {
        ResourceLocationsManager.ResourceLocation[] resourceLocations = ResourceLocationsManager.readResourceLocations();
        for (ResourceLocationsManager.ResourceLocation resourceLocation : resourceLocations) {
            if (!resourceLocation.isUsed) {
                continue;
            }
            if (resourceLocation.isGold) {
                RobotPlayer.rc.setIndicatorDot(resourceLocation.location, 0, 255, 0);
            }
            else {
                RobotPlayer.rc.setIndicatorDot(resourceLocation.location, 0, 255, 255);
            }
        }
    }
}
