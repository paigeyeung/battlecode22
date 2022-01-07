package w10;

import battlecode.common.*;

strictfp class MinerStrategy {
    /** Called by RobotPlayer */
    static void runMiner(RobotController rc) throws GameActionException {
        MapLocation myLocation = rc.getLocation();

        // Try to mine gold
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
            }
        }

        // Try to mine lead
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(myLocation.x + dx, myLocation.y + dy);
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Move randomly
        Direction dir = GeneralManager.getRandomDirection();
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
