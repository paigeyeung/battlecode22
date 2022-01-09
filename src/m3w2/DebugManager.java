package m3w2;

strictfp class DebugManager {
    static String botName = "m3w2";
    static void log(String string) {
        System.out.println(botName + " - " + RobotPlayer.rc.getRoundNum() + " - " + string);
        RobotPlayer.rc.setIndicatorString(string);
    }

    static int archonMismatchTurns = 0;
    static int soldierStationaryTurns = 0;
    static MapLocation soldierLastLocation = null;
    static boolean soldierDebug = false;

    static void runDebug() {
        // Check ally Archon count
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

        // If Soldier, enable debug if stuck
        if (!soldierDebug && RobotPlayer.rc.getType() == RobotType.SOLDIER) {
            if (soldierLastLocation != null && soldierLastLocation.distanceSquaredTo(new MapLocation(2, 2)) <= 2) {
                if (RobotPlayer.rc.getLocation().equals(soldierLastLocation)) {
                    soldierStationaryTurns++;
                }
                else {
                    soldierStationaryTurns = 0;
                }
                if (soldierStationaryTurns > 3) {
                    soldierDebug = true;
                }
                else {
                    soldierDebug = false;
                }
            }
            soldierLastLocation = RobotPlayer.rc.getLocation();
        }
    }
}
