package m3w1;

import battlecode.common.*;

import java.util.ArrayList;

strictfp class ArchonTrackerManager {
    static class ArchonTracker {
        int index;
        boolean alive;

        ArchonTracker(int _index, boolean _alive) {
            index = _index;
            alive = _alive;
        }
    }

    static class AllyArchonTracker extends ArchonTracker {
        MapLocation location;
        boolean toggle;

        AllyArchonTracker(int _index, boolean _alive, MapLocation _location, boolean _toggle) {
            super(_index, _alive);
            location = _location;
            toggle = _toggle;
//            DebugManager.log("AllyArchonTracker constructor index: " + index + ", alive: " + alive + ", location: " + location + ", toggle: " + toggle);
        }

        boolean isEqualTo(AllyArchonTracker other) {
            return alive == other.alive && location.equals(other.location) && toggle == other.toggle;
        }

        void update(boolean _alive, MapLocation _location, boolean _toggle) {
            alive = _alive;
            location = _location;
            toggle = _toggle;
        }
    }

    static class EnemyArchonTracker extends ArchonTracker {
        MapLocation correspondingAllyArchonStartingLocation;
        boolean seen;
        ArrayList<MapLocation> guessLocations;
        int guessLocation; // This can't ever be greater than 4, otherwise shared array breaks

        EnemyArchonTracker(int _index, boolean _alive, MapLocation _correspondingAllyArchonStartingLocation, boolean _seen, AllyArchonTracker[] _allyArchonTrackers) {
            super(_index, _alive);
            correspondingAllyArchonStartingLocation = _correspondingAllyArchonStartingLocation;
            seen = _seen;

            guessLocations = new ArrayList<>();
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    MapLocation location = GeneralManager.getOppositeLocation(correspondingAllyArchonStartingLocation, i == 0, j == 0);
                    boolean isValidGuess = true;
                    for (MapLocation previousGuessLocation : guessLocations) {
                        if (location.distanceSquaredTo(previousGuessLocation) <= 2) {
                            isValidGuess = false;
                            break;
                        }
                    }
                    if (isValidGuess) {
                        for (int k = 0; k < _allyArchonTrackers.length; k++) {
                            if (location.distanceSquaredTo(_allyArchonTrackers[k].location) <= 2) {
                                isValidGuess = false;
                                break;
                            }
                        }
                    }
                    if (isValidGuess) {
                        guessLocations.add(location);
//                        DebugManager.log("Add guess location " + location);
                    }
                }
            }
            guessLocation = 0;

//            DebugManager.log("EnemyArchonTracker constructor index: " + index + ", alive: " + alive + ", correspondingAllyArchonStartingLocation: " + correspondingAllyArchonStartingLocation + ", seen: " + seen + ", guessLocation: " + guessLocation + ", guessLocations: " + guessLocations);
        }

        MapLocation getGuessLocation() {
            return guessLocations.get(guessLocation);
        }

        boolean goToNextGuessLocation() {
            guessLocation++;
            if (guessLocation >= guessLocations.size()) {
                DebugManager.log("Enemy Archon tracker ran out of guess locations!");
                return false;
            }
            DebugManager.log("Enemy Archon tracker new guess location: " + getGuessLocation());
            return true;
        }

        boolean isEqualTo(EnemyArchonTracker other) {
            return alive == other.alive && getGuessLocation().equals(other.guessLocation) && seen == other.seen;
        }

        void update(boolean _alive, MapLocation _correspondingAllyArchonStartingLocation, boolean _seen) {
            alive = _alive;
            correspondingAllyArchonStartingLocation = _correspondingAllyArchonStartingLocation;
            seen = _seen;
        }
    }

    static boolean receivedArchonTrackers = false;
    static AllyArchonTracker[] allyArchonTrackers;
    static EnemyArchonTracker[] enemyArchonTrackers;
    static int myStartingArchonIndex = -1; // This should only be for droids

    static int encodeAllyArchonTracker(AllyArchonTracker allyArchonTracker) {
        return allyArchonTracker.location.x << 8 | allyArchonTracker.location.y << 2 | (allyArchonTracker.alive ? 1 : 0) << 1 | (allyArchonTracker.toggle ? 1 : 0);
    }
    static int encodeAllyArchonTracker(boolean alive, boolean toggle, MapLocation location) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (toggle ? 1 : 0);
    }
    static int encodeEnemyArchonTracker(EnemyArchonTracker enemyArchonTracker) {
        return enemyArchonTracker.correspondingAllyArchonStartingLocation.x << 8 | enemyArchonTracker.correspondingAllyArchonStartingLocation.y << 2 | (enemyArchonTracker.alive ? 1 : 0) << 1 | (enemyArchonTracker.seen ? 1 : 0);
    }
    static int encodeEnemyArchonTracker(boolean alive, boolean seen, MapLocation location) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (seen ? 1 : 0);
    }
//    static AllyArchonTracker decodeAllyArchonTracker(int index, int encoded) {
//        return new AllyArchonTracker(index, ((encoded >>> 1) & 0x1) == 1, new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F), (encoded & 0x1) == 1);
//    }
//    static EnemyArchonTracker decodeEnemyArchonTracker(int index, int encoded) {
//        return new EnemyArchonTracker(index, ((encoded >>> 1) & 0x1) == 1, new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F), (encoded & 0x1) == 1, allyArchonTrackers);
//    }

    /** Update global functions that take local trackers and updates shared array */
    static void updateGlobalAllyArchonTrackerFirstTime(int index, boolean alive, boolean toggle, MapLocation location) throws GameActionException {
        DebugManager.log("First time update global ally Archon tracker index: " + index + ", alive: " + alive + ", toggle: " + toggle + ", location: " + location);
        updateGlobalAllyArchonTracker(index, alive, toggle, location);
    }
    static void updateGlobalAllyArchonTracker(int index) throws GameActionException {
        updateGlobalAllyArchonTracker(index, allyArchonTrackers[index].alive, allyArchonTrackers[index].toggle, allyArchonTrackers[index].location);
    }
    static void updateGlobalAllyArchonTracker(int index, boolean alive, boolean toggle, MapLocation location) throws GameActionException {
        int encoded = encodeAllyArchonTracker(alive, toggle, location);
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + index, encoded);
    }
    static void updateGlobalEnemyArchonTrackerFirstTime(int index, boolean alive, boolean seen, MapLocation location) throws GameActionException {
        DebugManager.log("First time update global enemy Archon tracker index: " + index + ", alive: " + alive + ", seen: " + seen + ", location: " + location);
        updateGlobalEnemyArchonTracker(index, alive, seen, location);
    }
    static void updateGlobalEnemyArchonTracker(int index) throws GameActionException {
        updateGlobalEnemyArchonTracker(index, enemyArchonTrackers[index].alive, enemyArchonTrackers[index].seen, enemyArchonTrackers[index].correspondingAllyArchonStartingLocation);
    }
    static void updateGlobalEnemyArchonTracker(int index, boolean alive, boolean seen, MapLocation location) throws GameActionException {
        int encoded = encodeEnemyArchonTracker(alive, seen, location);
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + index, encoded);
    }

    static void goToEnemyArchonNextGuessLocation(int index) throws GameActionException {
        enemyArchonTrackers[index].goToNextGuessLocation();
        int encoded = RobotPlayer.rc.readSharedArray(CommunicationManager.GENERAL_STRATEGY_INDEX);
        encoded = (encoded & (~(3 << (4 + index * 2)))) | (enemyArchonTrackers[index].guessLocation << (4 + index * 2));
        RobotPlayer.rc.writeSharedArray(CommunicationManager.GENERAL_STRATEGY_INDEX, encoded);
    }

    /** Update local functions that read shared array and update local trackers */
    static void decodeAndUpdateLocalAllyArchonTracker(int index, boolean firstTime) throws GameActionException {
        int encoded = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + index);
        boolean alive = ((encoded >>> 1) & 0x1) == 1;
        MapLocation location = new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F);
        boolean toggle = (encoded & 0x1) == 1;
        if (firstTime) {
            allyArchonTrackers[index] = new AllyArchonTracker(index, alive, location, toggle);
        }
        else {
            allyArchonTrackers[index].update(alive, location, toggle);
        }
    }
    static void decodeAndUpdateLocalEnemyArchonTracker(int index, boolean firstTime) throws GameActionException {
        int encoded = RobotPlayer.rc.readSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + index);
        boolean alive = ((encoded >>> 1) & 0x1) == 1;
        MapLocation location = new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F);
        boolean seen = (encoded & 0x1) == 1;
        if (firstTime) {
            enemyArchonTrackers[index] = new EnemyArchonTracker(index, alive, location, seen, allyArchonTrackers);
        }
        else {
            enemyArchonTrackers[index].update(alive, location, seen);
        }
    }

    static void decodeAndUpdateLocalEnemyArchonGuessLocations() throws GameActionException {
        int encoded = RobotPlayer.rc.readSharedArray(CommunicationManager.GENERAL_STRATEGY_INDEX);
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            int guessLocation = (encoded >>> (4 + i * 2)) & 0x3;
            enemyArchonTrackers[i].guessLocation = guessLocation;
        }
    }

    /** Set functions set both local and global */
    static void setAllyArchonAlive(int index, boolean alive) throws GameActionException {
        DebugManager.log("Set ally Archon " + index + " alive " + alive);
        allyArchonTrackers[index].alive = alive;
        updateGlobalAllyArchonTracker(index);

        if (RobotPlayer.rc.getType() == RobotType.ARCHON) {
            ArchonResourceManager.setArchonDead(index);
        }
    }
    static void setAllyArchonToggle(int index, boolean toggle) throws GameActionException {
//        DebugManager.log("Set ally Archon " + index + " toggle " + toggle);
        allyArchonTrackers[index].toggle = toggle;
        updateGlobalAllyArchonTracker(index);
    }
    static void setEnemyArchonAlive(int index, boolean alive) throws GameActionException {
        DebugManager.log("Set enemy Archon " + index + " alive " + alive);
        enemyArchonTrackers[index].alive = alive;
        updateGlobalEnemyArchonTracker(index);
    }
    static void setEnemyArchonSeen(int index, boolean seen) throws GameActionException {
        DebugManager.log("Set enemy Archon " + index + " seen " + seen);
        enemyArchonTrackers[index].seen = seen;
        updateGlobalEnemyArchonTracker(index);
    }

    /** Get functions */
    static int getNearestAllyArchon(MapLocation fromLocation) {
        int nearest = -1;
        for (int i = 0; i < allyArchonTrackers.length; i++) {
            if (allyArchonTrackers[i].alive && (nearest == -1 || allyArchonTrackers[i].location.distanceSquaredTo(fromLocation) < allyArchonTrackers[nearest].location.distanceSquaredTo(fromLocation))) {
                nearest = i;
            }
        }
        return nearest;
    }
    static int getNearestEnemyArchon(MapLocation fromLocation) {
        int nearest = -1;
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (enemyArchonTrackers[i].alive && (nearest == -1 || enemyArchonTrackers[i].getGuessLocation().distanceSquaredTo(fromLocation) < enemyArchonTrackers[nearest].getGuessLocation().distanceSquaredTo(fromLocation))) {
                nearest = i;
            }
        }
        return nearest;
    }
    static MapLocation getNearestAllyArchonLocation(MapLocation fromLocation) {
        return allyArchonTrackers[getNearestAllyArchon(fromLocation)].location;
    }
    static MapLocation getNearestEnemyArchonGuessLocation(MapLocation fromLocation) {
        return enemyArchonTrackers[getNearestEnemyArchon(fromLocation)].getGuessLocation();
    }

    static int getFirstAliveAllyArchon() {
        for (int i = 0; i < allyArchonTrackers.length; i++) {
            if (allyArchonTrackers[i].alive) {
                return i;
            }
        }
        return -1;
    }
    static int getLastAliveAllyArchon() {
        int last = -1;
        for (int i = 0; i < allyArchonTrackers.length; i++) {
            if (allyArchonTrackers[i].alive) {
                last = i;
            }
        }
        return last;
    }
}
