package m2_c;

import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        GeneralManager.startingLocation = rc.getLocation();
        GeneralManager.mapWidth = rc.getMapWidth();
        GeneralManager.mapHeight = rc.getMapHeight();

        while (true) {
            GeneralManager.turnsAlive++;

            try {
                AllUnitStrategy.runAllEarly(rc);
                switch (rc.getType()) {
                    case ARCHON:     ArchonStrategy.runArchon(rc); break;
                    case MINER:      MinerStrategy.runMiner(rc); break;
                    case BUILDER:    BuilderStrategy.runBuilder(rc); break;
                    case SOLDIER:    SoldierStrategy.runSoldier(rc); break;
                    case SAGE:       break;
                    case LABORATORY: break;
                    case WATCHTOWER: WatchtowerStrategy.runWatchtower(rc); break;
                }
                AllUnitStrategy.runAllLate(rc);
                DebugManager.sanityCheck(rc);
            } catch (GameActionException e) {
                DebugManager.log(rc, rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                DebugManager.log(rc, rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
