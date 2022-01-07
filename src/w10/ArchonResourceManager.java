package w10;

import battlecode.common.*;
import scala.collection.concurrent.Debug;

strictfp class ArchonResourceManager {
    enum ARCHON_ROLES {
        OFFENSIVE,
        NONOFFENSIVE,
        DO_NOTHING
    }

    enum ARCHON_ACTIONS {
        BUILD_MINER,
        BUILD_BUILDER,
        BUILD_SOLDIER,
        REPAIR,
        DO_NOTHING
    }

    static class ArchonModel {
        int index;
        boolean alive;
        boolean onCooldown;
        int nearestEnemyArchonIndex;
        int nearestEnemyArchonDistanceSquared;

        ARCHON_ROLES archonRole;
        ARCHON_ACTIONS archonAction;

        int droidsBuilt = 0;
        int minersBuilt = 0;
        int buildersBuilt = 0;
        int soldiersBuilt = 0;

        ArchonModel(int _myIndex) {
            index = _myIndex;
            alive = true;

            // Each Archon builds a miner on turn 1, before this runs
            minersBuilt++;
            droidsBuilt++;

            updateNearestEnemyArchon();
        }

        void updateNearestEnemyArchon() {
            ArchonTrackerManager.EnemyArchonTracker nearestEnemyArchon = ArchonTrackerManager.getNearestEnemyArchon(ArchonTrackerManager.allyArchonTrackers[index].location);
            nearestEnemyArchonIndex = ArchonTrackerManager.getEnemyArchonIndex(nearestEnemyArchon);
            nearestEnemyArchonDistanceSquared = ArchonTrackerManager.allyArchonTrackers[index].location.distanceSquaredTo(nearestEnemyArchon.guessLocation);
        }

        void setActionBuildMiner() {
            archonAction = ARCHON_ACTIONS.BUILD_MINER;
            onCooldown = true;
            minersBuilt++;
            droidsBuilt++;
        }
        void setActionBuildBuilder() {
            archonAction = ARCHON_ACTIONS.BUILD_BUILDER;
            onCooldown = true;
            buildersBuilt++;
            droidsBuilt++;
        }
        void setActionBuildSoldier() {
            archonAction = ARCHON_ACTIONS.BUILD_SOLDIER;
            onCooldown = true;
            soldiersBuilt++;
            droidsBuilt++;
        }
        void setActionDoNothing() {
            archonAction = ARCHON_ACTIONS.DO_NOTHING;
        }
    }

    static ArchonModel[] allyArchonModels;

    /** Initialize, should always be called on turn 1 */
    static void initializeTurn1(RobotController rc) {
        // Could broadcast amount of visible lead here
    }

    /** Initialize, should always be called on turn 2 */
    static void initializeTurn2(RobotController rc) {
        allyArchonModels = new ArchonModel[ArchonTrackerManager.allyArchonTrackers.length];
        for (int i = 0; i < allyArchonModels.length; i++) {
            allyArchonModels[i] = new ArchonModel(i);
        }

        computeArchonRoles(rc);
    }

    static void setArchonDead(RobotController rc, int index) {
        allyArchonModels[index].alive = false;
        computeArchonRoles(rc);
    }

    /** Compute Archon roles */
    static void computeArchonRoles(RobotController rc) {
        int closestToEnemyDistanceSquared = 10000;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            if (allyArchonModels[i].nearestEnemyArchonDistanceSquared < closestToEnemyDistanceSquared) {
                closestToEnemyDistanceSquared = allyArchonModels[i].nearestEnemyArchonDistanceSquared;
            }
        }

        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            if (allyArchonModels[i].nearestEnemyArchonDistanceSquared - closestToEnemyDistanceSquared <= 10) {
                allyArchonModels[i].archonRole = ARCHON_ROLES.OFFENSIVE;
            }
            else {
                allyArchonModels[i].archonRole = ARCHON_ROLES.NONOFFENSIVE;
            }
            DebugManager.log(rc, "Archon at location " + ArchonTrackerManager.allyArchonTrackers[i].location + " allyArchonModels[" + i + "].archonRole: " + allyArchonModels[i].archonRole);
        }
    }

    /** Compute Archon actions */
    static void computeArchonActions(RobotController rc) throws GameActionException {
        int turn = rc.getRoundNum();
        int totalDroidsBuilt = 0;
        int totalMinersBuilt = 0;
        int totalBuildersBuilt = 0;
        int totalSoldiersBuilt = 0;
        // We include dead ally Archon numbers in this calculation
        for (int i = 0; i < allyArchonModels.length; i++) {
            totalDroidsBuilt += allyArchonModels[i].droidsBuilt;
            totalMinersBuilt += allyArchonModels[i].minersBuilt;
            totalBuildersBuilt += allyArchonModels[i].buildersBuilt;
            totalSoldiersBuilt += allyArchonModels[i].soldiersBuilt;

            allyArchonModels[i].onCooldown = false;
        }

        // Read from shared array indicies 8-9
        int encodedIndex8 = rc.readSharedArray(8);
        int encodedIndex9 = rc.readSharedArray(9);
        int lead = encodedIndex8 >>> 4;
        int gold = encodedIndex9 >>> 4;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if (!allyArchonModels[i].alive) {
                continue;
            }
            allyArchonModels[i].onCooldown = ((encodedIndex8 >>> i) & 0x1) == 1;
            allyArchonModels[i].setActionDoNothing();
        }
//        DebugManager.log(rc, "I'm " + ArchonStrategy.mySharedArrayIndex + " at " + ArchonTrackerManager.allyArchonTrackers[ArchonStrategy.mySharedArrayIndex].location);
//        DebugManager.log(rc, "lead = " + lead + ", gold = " + gold);
        while (true) {
            // Maintain 30% proportion of miners
            if (totalMinersBuilt < totalDroidsBuilt * 0.3) {
                if (lead < 50) {
                    break;
                }
                int chosenArchonIndex = findArchonWithFewestMinersBuilt(true);
//                DebugManager.log(rc, "allyArchonModels[0].minersBuilt = " + allyArchonModels[0].minersBuilt + ", allyArchonModels[0].alive = " + allyArchonModels[0].alive + ", allyArchonModels[0].onCooldown = " + allyArchonModels[0].onCooldown);
//                DebugManager.log(rc, "allyArchonModels[1].minersBuilt = " + allyArchonModels[1].minersBuilt + ", allyArchonModels[1].alive = " + allyArchonModels[1].alive + ", allyArchonModels[1].onCooldown = " + allyArchonModels[1].onCooldown);
//                DebugManager.log(rc, "allyArchonModels[2].minersBuilt = " + allyArchonModels[2].minersBuilt + ", allyArchonModels[2].alive = " + allyArchonModels[2].alive + ", allyArchonModels[2].onCooldown = " + allyArchonModels[2].onCooldown);
//                DebugManager.log(rc, "allyArchonModels[3].minersBuilt = " + allyArchonModels[3].minersBuilt + ", allyArchonModels[3].alive = " + allyArchonModels[3].alive + ", allyArchonModels[3].onCooldown = " + allyArchonModels[3].onCooldown);
//                DebugManager.log(rc, "allyArchonModels[" + chosenArchonIndex + "].setActionBuildMiner();");
                if (chosenArchonIndex == -1) {
                    break;
                }
                allyArchonModels[chosenArchonIndex].setActionBuildMiner();
                lead -= 50;
                continue;
            }

            // Otherwise, build soldiers
            if (lead < 75) {
                break;
            }
            int chosenArchonIndex = findArchonWithFewestSoldiersBuilt(true, true);
//            DebugManager.log(rc, "allyArchonModels[2].soldiersBuilt = " + allyArchonModels[2].soldiersBuilt + ", allyArchonModels[2].alive = " + allyArchonModels[2].alive + ", allyArchonModels[2].onCooldown = " + allyArchonModels[2].onCooldown);
//            DebugManager.log(rc, "allyArchonModels[3].soldiersBuilt = " + allyArchonModels[3].soldiersBuilt + ", allyArchonModels[3].alive = " + allyArchonModels[3].alive + ", allyArchonModels[3].onCooldown = " + allyArchonModels[3].onCooldown);
//            DebugManager.log(rc, "allyArchonModels[" + chosenArchonIndex + "].setActionBuildSoldier();");
            if (chosenArchonIndex == -1) {
                break;
            }
            allyArchonModels[chosenArchonIndex].setActionBuildSoldier();
            lead -= 75;
            continue;
        }
    }

    /** Get action for specific Archon */
    static ARCHON_ACTIONS getArchonAction(int index) {
        return allyArchonModels[index].archonAction;
    }

    /** Helper functions to find Archons that meet criteria, returns -1 if no match is found */
    static int findArchonWithFewestDroidsBuilt(boolean ableToBuild) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].droidsBuilt < allyArchonModels[fewestIndex].droidsBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestMinersBuilt(boolean ableToBuild) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].minersBuilt < allyArchonModels[fewestIndex].minersBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestBuildersBuilt(boolean ableToBuild) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].buildersBuilt < allyArchonModels[fewestIndex].buildersBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
    static int findArchonWithFewestSoldiersBuilt(boolean ableToBuild, boolean offensiveArchon) {
        int fewestIndex = -1;
        for (int i = 0; i < allyArchonModels.length; i++) {
            if ((fewestIndex == -1 || allyArchonModels[i].soldiersBuilt < allyArchonModels[fewestIndex].soldiersBuilt)
                && (!ableToBuild || (allyArchonModels[i].alive && !allyArchonModels[i].onCooldown))
                && (!offensiveArchon || allyArchonModels[i].archonRole == ARCHON_ROLES.OFFENSIVE)) {
                fewestIndex = i;
            }
        }
        return fewestIndex;
    }
}
