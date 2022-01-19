package m9;

import battlecode.common.RobotType;

strictfp class DebugManager {
    static String botName = "m9";
    static void log(String string) {
//        System.out.println(botName + " - " + RobotPlayer.rc.getRoundNum() + " - " + string);
        RobotPlayer.rc.setIndicatorString(string);
    }

    static byte archonMismatchTurns = 0;

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
}
