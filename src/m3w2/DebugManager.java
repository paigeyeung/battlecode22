package m3w2;

strictfp class DebugManager {
    static String botName = "m3w2";
    static void log(String string) {
        System.out.println(botName + " - " + RobotPlayer.rc.getRoundNum() + " - " + string);
        RobotPlayer.rc.setIndicatorString(string);
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
