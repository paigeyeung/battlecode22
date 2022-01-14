package w9;

import battlecode.common.*;

public class DebugManager {
    static String botName = "w9";
    static void log(RobotController rc, String string) {
        System.out.println(botName + " - " + (rc == null ? "null" : rc.getRoundNum()) + " - " + string);
    }

    static byte archonMismatchTurns = 0;

    static void sanityCheck(RobotController rc) {
        if (ArchonTrackerManager.receivedArchonTrackers) {
            int numAllyArchonsAlive = 0;
            for (int i = 0; i < ArchonTrackerManager.allyArchonTrackers.length; i++) {
                if (ArchonTrackerManager.allyArchonTrackers[i].alive) {
                    numAllyArchonsAlive++;
                }
            }
            if (numAllyArchonsAlive != rc.getArchonCount()) {
                if (archonMismatchTurns >= 2) {
                    log(rc, "SOMETHING WENT WRONG: Archon count mismatch " + numAllyArchonsAlive + " and " + rc.getArchonCount());
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
