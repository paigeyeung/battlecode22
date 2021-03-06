package m2w1;

import battlecode.common.*;

strictfp class DebugManager {
    static String botName = "m2w1";
    static void log(String string) {
        System.out.println(botName + " - " + RobotPlayer.rc.getRoundNum() + " - " + string);
    }

    static byte archonMismatchTurns = 0;

    static void sanityCheck() {
        if (ArchonTrackerManager.receivedArchonTrackers) {
            int numAllyArchonsAlive = 0;
            for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                if (ArchonTrackerManager.allyArchonTrackers[i].alive) {
                    numAllyArchonsAlive++;
                }
            }
            if (numAllyArchonsAlive != RobotPlayer.rc.getArchonCount()) {
                if (archonMismatchTurns >= 2) {
                    log("SOMETHING WENT WRONG: Archon count mismatch " + numAllyArchonsAlive + " and " + RobotPlayer.rc.getArchonCount());
                }
                else {
                    archonMismatchTurns++;
                }
            }
            else {
                archonMismatchTurns = 0;
            }
        }
    }
}
