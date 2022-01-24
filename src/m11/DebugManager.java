package m11;

import battlecode.common.*;

strictfp class DebugManager {
    static String botName = "m11";
    static void log(String string) {
        System.out.println(botName + " - " + RobotPlayer.rc.getRoundNum() + " - " + string);
        RobotPlayer.rc.setIndicatorString(string);
    }

    static int archonMismatchTurns = 0;

    /** Color code
     * Miner
     * - Light blue dot is lead resource location
     * - Green dot is gold resource location
     * - Green line is successful get miner where to go
     * - Red line is unsuccessful get miner where to go because insufficient score
     * Soldier
     * - Red line is chase visible enemy
     * - Orange line is move towards target enemy Archon
     * - Medium blue line is scout direction
     * - Light blue line is storedAttackDirection
     * - Green line is encircle nearest ally Archon
     * Sage
     * - Green line is encircle nearest ally Archon
     * - Orange line is move towards target enemy Archon
     * - Medium blue line is move for charge
     * - Light blue line is move for fury
     */
    static final boolean drawMinerLines = true;
    static final boolean drawSoldierLines = false;
    static final boolean drawSageLines = true;

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
        if (!drawMinerLines) return;
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
