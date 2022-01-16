package m8;

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
        int guessLocation = -1; // This can't ever be greater than 4, otherwise shared array breaks. Also setting to -1 to force proper initialization in constructor
        boolean missing;
        boolean guessLocationOverridden;
        MapLocation overriddenGuessLocation;

        EnemyArchonTracker(int _index, boolean _alive, MapLocation location, boolean _seen, int _guessLocation, boolean _guessLocationOverrridden, AllyArchonTracker[] _allyArchonTrackers) {
            super(_index, _alive);
            correspondingAllyArchonStartingLocation = location;
            seen = _seen;

            guessLocations = new ArrayList<>();
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    // If we're about to not apply a mirror on any axis
                    if (i == 1 && j == 1) {
                        continue;
                    }

                    MapLocation oppositeLocation = GeneralManager.getOppositeLocation(correspondingAllyArchonStartingLocation, i == 0, j == 0);
                    // Guess locations are always valid, because we need to always have exactly 3 guesses
//                    boolean isValidGuess = true;
//                    for (MapLocation previousGuessLocation : guessLocations) {
//                        if (oppositeLocation.distanceSquaredTo(previousGuessLocation) <= 2) {
//                            isValidGuess = false;
//                            break;
//                        }
//                    }

                    // This flexible check no longer works because ally Archons can move, used fixed check instead
//                    if (isValidGuess) {
//                        for (int k = 0; k < _allyArchonTrackers.length; k++) {
//                            if (oppositeLocation.distanceSquaredTo(_allyArchonTrackers[k].location) <= 2) {
//                                isValidGuess = false;
//                                break;
//                            }
//                        }
//                    }

//                    if (isValidGuess) {
//                    guessLocations.add(oppositeLocation);
//                        DebugManager.log("Add guess location " + location);
//                    }
                    guessLocations.add(oppositeLocation);
                }
            }
//            DebugManager.log("Enemy Archon tracker " + index + " guess locations: " + guessLocations);
            if (guessLocations.size() != 3) {
                DebugManager.log("SOMETHING WENT WRONG: There are " + guessLocations.size() + " guess locations when there should be exactly 3!");
            }

            updateGuessLocation(_guessLocation);
            guessLocationOverridden = _guessLocationOverrridden;
            if (guessLocationOverridden) {
                overriddenGuessLocation = correspondingAllyArchonStartingLocation;
                correspondingAllyArchonStartingLocation = null;
            }

//            DebugManager.log("EnemyArchonTracker constructor index: " + index + ", alive: " + alive + ", correspondingAllyArchonStartingLocation: " + correspondingAllyArchonStartingLocation + ", seen: " + seen + ", guessLocation: " + guessLocation + ", guessLocations: " + guessLocations);
        }

        MapLocation getGuessLocation() {
            if (guessLocationOverridden) {
                return overriddenGuessLocation;
            }
            if (missing) {
                return null;
            }
            if (guessLocation >= guessLocations.size()) {
                DebugManager.log("SOMETHING WENT WRONG: guessLocation is " + guessLocation + " but guessLocations.size() is " + guessLocations.size());
            }
            return guessLocations.get(guessLocation);
        }

        void goToNextGuessLocation() {
            updateGuessLocation(guessLocation + 1);
            DebugManager.log("Enemy Archon tracker " + index + " new guess location: " + getGuessLocation());
        }

        void updateGuessLocation(int _guessLocation) {
            if (guessLocation == _guessLocation) {
                return;
            }

//            int debugOldGuessLocation = guessLocation;
            guessLocation = _guessLocation;
//            DebugManager.log("Enemy Archon tracker " + index + " updated guess location from " + debugOldGuessLocation + " to " + guessLocation);
            if (guessLocation == guessLocations.size()) {
//                DebugManager.log("Enemy Archon tracker " + index + " ran out of guess locations!");
                missing = true;
            }
            else {
                missing = false;
            }
        }

        void update(boolean _alive, MapLocation location, boolean _seen, int _guessLocation, boolean _guessLocationOverridden) {
            alive = _alive;
            updateGuessLocation(_guessLocation);
            guessLocationOverridden = _guessLocationOverridden;
            if (_guessLocationOverridden) {
                overriddenGuessLocation = location;
            }
            else {
                correspondingAllyArchonStartingLocation = location;
            }
            seen = _seen;
        }
    }

    static boolean receivedArchonTrackers = false;
    static AllyArchonTracker[] allyArchonTrackers;
    static EnemyArchonTracker[] enemyArchonTrackers;
    static int myStartingArchonIndex = -1; // This should only be for droids

//    static int encodeAllyArchonTracker(AllyArchonTracker allyArchonTracker) {
//        return allyArchonTracker.location.x << 8 | allyArchonTracker.location.y << 2 | (allyArchonTracker.alive ? 1 : 0) << 1 | (allyArchonTracker.toggle ? 1 : 0);
//    }
    static int encodeAllyArchonTracker(boolean alive, MapLocation location, boolean toggle) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (toggle ? 1 : 0);
    }
//    static int encodeEnemyArchonTracker(EnemyArchonTracker enemyArchonTracker) {
//        return enemyArchonTracker.correspondingAllyArchonStartingLocation.x << 8 | enemyArchonTracker.correspondingAllyArchonStartingLocation.y << 2 | (enemyArchonTracker.alive ? 1 : 0) << 1 | (enemyArchonTracker.seen ? 1 : 0);
//    }
    static int encodeEnemyArchonTracker(boolean alive, MapLocation location, boolean seen) {
        return location.x << 8 | location.y << 2 | (alive ? 1 : 0) << 1 | (seen ? 1 : 0);
    }
//    static AllyArchonTracker decodeAllyArchonTracker(int index, int encoded) {
//        return new AllyArchonTracker(index, ((encoded >>> 1) & 0x1) == 1, new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F), (encoded & 0x1) == 1);
//    }
//    static EnemyArchonTracker decodeEnemyArchonTracker(int index, int encoded) {
//        return new EnemyArchonTracker(index, ((encoded >>> 1) & 0x1) == 1, new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F), (encoded & 0x1) == 1, allyArchonTrackers);
//    }

    /** Update global functions that take local trackers and updates shared array */
    static void updateGlobalAllyArchonTrackerFirstTime(int index, boolean alive, MapLocation location, boolean toggle) throws GameActionException {
        DebugManager.log("First time update global ally Archon tracker index: " + index + ", alive: " + alive + ", toggle: " + toggle + ", location: " + location);
        updateGlobalAllyArchonTracker(index, alive, location, toggle);
    }
    static void updateGlobalAllyArchonTracker(int index) throws GameActionException {
        updateGlobalAllyArchonTracker(index, allyArchonTrackers[index].alive, allyArchonTrackers[index].location, allyArchonTrackers[index].toggle);
    }
    static void updateGlobalAllyArchonTracker(int index, boolean alive, MapLocation location, boolean toggle) throws GameActionException {
        int encoded = encodeAllyArchonTracker(alive, location, toggle);
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ALLY_ARCHON_TRACKERS_INDEX + index, encoded);
    }
    static void updateGlobalEnemyArchonTrackerFirstTime(int index, boolean alive, MapLocation location, boolean seen, int guessLocation, boolean guessLocationOverridden) throws GameActionException {
        DebugManager.log("First time update global enemy Archon tracker index: " + index + ", alive: " + alive + ", seen: " + seen + ", location: " + location);
        updateGlobalEnemyArchonTracker(index, alive, location, seen, guessLocation, guessLocationOverridden);
    }
    static void updateGlobalEnemyArchonTracker(int index) throws GameActionException {
        MapLocation location = enemyArchonTrackers[index].correspondingAllyArchonStartingLocation;
        if (enemyArchonTrackers[index].guessLocationOverridden) {
            location = enemyArchonTrackers[index].overriddenGuessLocation;
        }
        updateGlobalEnemyArchonTracker(index, enemyArchonTrackers[index].alive, location, enemyArchonTrackers[index].seen, enemyArchonTrackers[index].guessLocation, enemyArchonTrackers[index].guessLocationOverridden);
    }
    static void updateGlobalEnemyArchonTracker(int index, boolean alive, MapLocation location, boolean seen, int guessLocation, boolean guessLocationOverridden) throws GameActionException {
        int encoded = encodeEnemyArchonTracker(alive, location, seen);
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + index, encoded);

        int encoded2 = RobotPlayer.rc.readSharedArray(CommunicationManager.ENEMY_ARCHON_ADDITIONAL_INFO);
        encoded2 = (encoded2 & (~(3 << (4 + index * 2)))) | (guessLocation << (4 + index * 2));
        encoded2 = (encoded2 & (~(1 << (12 + index)))) | ((guessLocationOverridden ? 1 : 0) << (12 + index));
        RobotPlayer.rc.writeSharedArray(CommunicationManager.ENEMY_ARCHON_ADDITIONAL_INFO, encoded2);
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

        if (GeneralManager.myType == RobotType.ARCHON && GeneralManager.turnsAlive > 2) {
            ArchonResourceManager.setArchonAlive(index, alive);
        }
    }
    static void decodeAndUpdateLocalEnemyArchonTracker(int index, boolean firstTime) throws GameActionException {
        int encoded = RobotPlayer.rc.readSharedArray(CommunicationManager.ENEMY_ARCHON_TRACKERS_INDEX + index);
        boolean alive = ((encoded >>> 1) & 0x1) == 1;
        MapLocation location = new MapLocation((encoded >>> 8) & 0x3F, (encoded >>> 2) & 0x3F);
        boolean seen = (encoded & 0x1) == 1;

        int encoded2 = RobotPlayer.rc.readSharedArray(CommunicationManager.ENEMY_ARCHON_ADDITIONAL_INFO);
        int guessLocation = (encoded2 >>> (4 + index * 2)) & 0x3;
        boolean guessLocationOverridden = ((encoded2 >>> (12 + index)) & 0x1) == 1;
        if (firstTime) {
            enemyArchonTrackers[index] = new EnemyArchonTracker(index, alive, location, seen, guessLocation, guessLocationOverridden, allyArchonTrackers);
        }
        else {
            enemyArchonTrackers[index].update(alive, location, seen, guessLocation, guessLocationOverridden);
        }
    }

    /** Set functions set both local and global */
    static void setAllyArchonAlive(int index, boolean alive) throws GameActionException {
        DebugManager.log("Set ally Archon " + index + " alive " + alive);
        allyArchonTrackers[index].alive = alive;
        updateGlobalAllyArchonTracker(index);

        if (GeneralManager.myType == RobotType.ARCHON) {
            ArchonResourceManager.setArchonAlive(index, alive);
        }
    }
    static void setAllyArchonLocation(int index, MapLocation location) throws GameActionException {
        DebugManager.log("Set ally Archon " + index + " location " + location);
        allyArchonTrackers[index].location = location;
        updateGlobalAllyArchonTracker(index);
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
    static void setEnemyArchonAlive(MapLocation location, boolean alive) throws GameActionException {
        int index = -1;
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if ((!enemyArchonTrackers[i].guessLocationOverridden && enemyArchonTrackers[i].seen && enemyArchonTrackers[i].getGuessLocation().equals(location))
                || (enemyArchonTrackers[i].guessLocationOverridden && enemyArchonTrackers[i].overriddenGuessLocation.equals(location))) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            DebugManager.log("SOMETHING WENT WRONG: Didn't find enemy Archon at location " + location + " while trying to set alive " + alive);
            return;
        }
        setEnemyArchonAlive(index, alive);
    }
    static void setEnemyArchonSeen(int index, boolean seen) throws GameActionException {
        DebugManager.log("Set enemy Archon " + index + " seen " + seen);
        enemyArchonTrackers[index].seen = seen;
        updateGlobalEnemyArchonTracker(index);
    }
    static void goToEnemyArchonNextGuessLocation(int index) throws GameActionException {
        DebugManager.log("Go to enemy Archon " + index + " next guess location");
        enemyArchonTrackers[index].goToNextGuessLocation();
        updateGlobalEnemyArchonTracker(index);
    }
    static void setEnemyArchonMissing(int index) throws GameActionException {
        DebugManager.log("Set enemy Archon " + index + " missing");
        enemyArchonTrackers[index].seen = false;
        enemyArchonTrackers[index].updateGuessLocation(3);
        updateGlobalEnemyArchonTracker(index);
    }
    static void foundEnemyArchon(MapLocation location) throws GameActionException {
        DebugManager.log("Found enemy Archon at " + location);
        // Check for missing enemy Archons
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (!enemyArchonTrackers[i].alive) {
                continue;
            }
            if (enemyArchonTrackers[i].missing) {
                DebugManager.log("Decided to find missing enemy Archon " + i);
                enemyArchonTrackers[i].update(true, location, true, 0, true);
                updateGlobalEnemyArchonTracker(i);
                return;
            }
        }
        // Check for unseen enemy Archons
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (!enemyArchonTrackers[i].alive) {
                continue;
            }
            if (!enemyArchonTrackers[i].seen) {
                DebugManager.log("Decided to find unseen enemy Archon " + i);
                enemyArchonTrackers[i].update(true, location, true, 0, true);
                updateGlobalEnemyArchonTracker(i);
                return;
            }
        }
        // No missing or unseen enemy Archons, so some enemy Archon relocated since we've last seen it
        // Check for enemy Archons without overridden locations
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (!enemyArchonTrackers[i].alive) {
                continue;
            }
            if (!enemyArchonTrackers[i].guessLocationOverridden) {
                DebugManager.log("Decided to find un-guess-location-overridden enemy Archon " + i);
                enemyArchonTrackers[i].update(true, location, true, 0, true);
                updateGlobalEnemyArchonTracker(i);
                return;
            }
        }
        // Check for any enemy Archons
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (!enemyArchonTrackers[i].alive) {
                continue;
            }
            DebugManager.log("Decided to find normal enemy Archon " + i);
            enemyArchonTrackers[i].update(true, location, true, 0, true);
            updateGlobalEnemyArchonTracker(i);
            return;
        }
        DebugManager.log("SOMETHING WENT WRONG: Does this mean no enemy Archons are alive?");
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
            if (enemyArchonTrackers[i].alive && !enemyArchonTrackers[i].missing &&
                    (nearest == -1 ||
                            ((fromLocation != null &&
                                    enemyArchonTrackers[i].getGuessLocation() != null
                                    && enemyArchonTrackers[nearest].getGuessLocation() != null) &&
                                    enemyArchonTrackers[i].getGuessLocation().distanceSquaredTo(fromLocation) < enemyArchonTrackers[nearest].getGuessLocation().distanceSquaredTo(fromLocation)))) {
                nearest = i;
            }
        }
        return nearest;
    }
    static int getFarthestEnemyArchon(MapLocation fromLocation) {
        int farthest = -1;
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (enemyArchonTrackers[i].alive && !enemyArchonTrackers[i].missing &&
                    (farthest == -1 ||
                            ((fromLocation != null &&
                                    enemyArchonTrackers[i].getGuessLocation() != null
                                    && enemyArchonTrackers[farthest].getGuessLocation() != null) &&
                                    enemyArchonTrackers[i].getGuessLocation().distanceSquaredTo(fromLocation) > enemyArchonTrackers[farthest].getGuessLocation().distanceSquaredTo(fromLocation)))) {
                farthest = i;
            }
        }
        return farthest;
    }
    static int getCentralEnemyArchon() {
//        int xSum = 0, ySum = 0, numArchons = 0;
        int index = -1;
//        for (int i = 0; i < enemyArchonTrackers.length; i++) {
//            if (enemyArchonTrackers[i].getGuessLocation() != null) {
//                xSum += enemyArchonTrackers[i].getGuessLocation().x;
//                ySum += enemyArchonTrackers[i].getGuessLocation().y;
//                numArchons++;
//            }
//        }
//
//        if(numArchons == 0) return index;

        int minDist = Integer.MAX_VALUE;
        MapLocation centerLoc = GeneralManager.getMapCenter();
                //new MapLocation(xSum/numArchons,ySum/numArchons);
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (enemyArchonTrackers[i].alive && enemyArchonTrackers[i].getGuessLocation() != null) {
                int dist = enemyArchonTrackers[i].getGuessLocation().distanceSquaredTo(centerLoc);
                if(dist < minDist) {
                    minDist = dist;
                    index = i;
                }
            }
        }
        return index;
    }
    static MapLocation getNearestAllyArchonLocation(MapLocation fromLocation) {
        int nearest = getNearestAllyArchon(fromLocation);
        if (nearest == -1) {
            DebugManager.log("SOMETHING WENT WRONG: Nearest ally Archon not found");
            return null;
        }
        return allyArchonTrackers[nearest].location;
    }
    static MapLocation getNearestEnemyArchonGuessLocation(MapLocation fromLocation) {
        int nearest = getNearestEnemyArchon(fromLocation);
        if (nearest == -1) {
//            DebugManager.log("Nearest enemy Archon not found");
            return null;
        }
        return enemyArchonTrackers[nearest].getGuessLocation();
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

    static int numEnemyArchonMissing() {
        int num = 0;
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (!enemyArchonTrackers[i].alive) {
                continue;
            }
            if (enemyArchonTrackers[i].missing) {
                num++;
            }
        }
        return num;
    }

    static boolean existsEnemyArchonAtLocation(MapLocation location) {
        for (int i = 0; i < enemyArchonTrackers.length; i++) {
            if (!enemyArchonTrackers[i].alive || enemyArchonTrackers[i].missing) {
                continue;
            }
            if (location.equals(enemyArchonTrackers[i].getGuessLocation())) {
                return true;
            }
        }
        return false;
    }

    static int getEnemyCombatScoreAtArchon(int index) throws GameActionException {
        return ((RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ENEMY_COMBAT_SCORE+index/2) >>> (7 * (index % 2))) & 0x7F);
    }

    static boolean isMovingArchon(int index) throws GameActionException {
        int encodedAllyArchonAdditionalInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO);
        if (!allyArchonTrackers[index].alive) return false;
        return ((encodedAllyArchonAdditionalInfo >>> (4 + index)) & 0x1) == 1;
    }

    static void setMoving(int index, boolean moving) throws GameActionException {
        int encodedAllyArchonAdditionalInfo = RobotPlayer.rc.readSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO);

        int m = moving ? 1 : 0;
        if(((encodedAllyArchonAdditionalInfo >>> (4 + index)) & 0x1) == m) return;

        RobotPlayer.rc.writeSharedArray(CommunicationManager.ALLY_ARCHON_ADDITIONAL_INFO,
                (encodedAllyArchonAdditionalInfo >>> (4 + index + 1)) |
                m | (encodedAllyArchonAdditionalInfo & ((int)(Math.pow(2, 4 + index))-1)));
    }

    static int numArchonsMoving() throws GameActionException {
        int count = 0;
        for(int i = allyArchonTrackers.length - 1; i >= 0; i--) {
            if(isMovingArchon(i)) {
                count++;
            }
        }
        return count;
    }
}
