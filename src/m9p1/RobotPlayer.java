package m9p1;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    public static void run(RobotController _rc) throws GameActionException {
        rc = _rc;

        // Once per game statics
        GeneralManager.myType = rc.getType();
        GeneralManager.startingLocation = rc.getLocation();
        GeneralManager.mapWidth = rc.getMapWidth();
        GeneralManager.mapHeight = rc.getMapHeight();

        while (true) {
            // Once per turn statics
            GeneralManager.turnsAlive++;
            GeneralManager.myLocation = rc.getLocation();

            try {
                AllUnitStrategy.runAllEarly();
                switch (rc.getType()) {
                    case ARCHON:     ArchonStrategy.runArchon(); break;
                    case MINER:      MinerStrategy.runMiner(); break;
                    case BUILDER:    BuilderStrategy.runBuilder(); break;
                    case SOLDIER:    SoldierStrategy.runSoldier(); break;
                    case SAGE:       SageStrategy.runSage(); break;
                    case LABORATORY: break;
                    case WATCHTOWER: WatchtowerStrategy.runWatchtower(); break;
                }
                AllUnitStrategy.runAllLate();
                DebugManager.sanityCheck();
            } catch (GameActionException e) {
                DebugManager.log(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                DebugManager.log(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
