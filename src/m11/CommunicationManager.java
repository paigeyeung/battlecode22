package m11;

strictfp class CommunicationManager {
    /** ALLY_ARCHON_TRACKERS_INDEX
     * Index 0 to 3:
     * First 6 bits stores x location and second 6 bits stores y location
     * Second to last bit stores whether unit is alive or not
     * Last bit is toggled by ally Archon every turn to show it's alive
     * x | x | x | x | x | x | y | y | y | y | y | y | alive | toggle
     */
    static final int ALLY_ARCHON_TRACKERS_INDEX = 0; // 0-3

    /** ENEMY_ARCHON_TRACKERS_INDEX
     * Index 0 to 3:
     * First 6 bits stores x location and second 6 bits stores y location
     * Second to last bit stores whether unit is alive or not
     * Last bit stores whether unit has been seen at least once
     * x | x | x | x | x | x | y | y | y | y | y | y | alive | seen
     */
    static final int ENEMY_ARCHON_TRACKERS_INDEX = 4; // 4-7

    /** ARCHON_RESOURCE_MANAGER_INDEX
     * Index 0:
     * First 12 bits store lead amount at start of turn
     * Last 4 bits store whether Archons 0-3 on cooldown last turn
     * lead... | 3 on cooldown | 2 on cooldown | 1 on cooldown | 0 on cooldown
     * Index 1:
     * First 12 bits store gold amount at start of turn
     * Last 4 bits store whether Archons 0-3 on cooldown this turn
     * gold... | 3 on cooldown | 2 on cooldown | 1 on cooldown | 0 on cooldown
     */
    static final int ARCHON_RESOURCE_MANAGER_INDEX = 8; // 8-9

    /** ALLY_ARCHON_ADDITIONAL_INFO
     * Last 4 bits store if a unit near ally Archons 0-3 have seen an enemy
     * Next 4 store if ally Archons 0-3 are moving
     * 8 unused | 3 moving | 2 moving | 1 moving | 0 moving | 3 seen enemy | 2 seen enemy | 1 seen enemy | 0 seen enemy
     */
    static final int ALLY_ARCHON_ADDITIONAL_INFO = 10; // 10

    /** ENEMY_ARCHON_ADDITIONAL_INFO2
     * First 4 bits store if enemy Archon 0-3 guess location is manually overridden
     * Second 8 bits store guess location index of enemy Archons 0-3
     * Last 4 bits unused
     * 3 overridden | 1 overridden | 2 overridden | 0 overridden | 3 guess location 2 | 3 guess location 1 | 2 guess location 2 | 2 guess location 1 | 1 guess location 2 | 1 guess location 1 | 0 guess location 2 | 0 guess location 1 | unused...
     */
    static final int ENEMY_ARCHON_ADDITIONAL_INFO2 = 11; // 11

    /**
     * Saved enemy combat score from a robot with enemy Archon in vision range
     * 8 unused | 8 combat score
     */
    static final int SAVED_ENEMY_COMBAT_SCORE = 12;

    /**
     * Number of soldiers scouting (combine with above)
     */
    static final int SCOUT_COUNT = 13;

    /**
     * Enemy combat score at ally archons
     * First 7 bits store combat score of 1st ally archon
     * Next 7 bits store combat score of next ally archon
     * Same for next array index
     */
    static final int ALLY_ARCHON_ENEMY_COMBAT_SCORE = 14; // 14, 15

    /** ENEMY_ARCHON_ADDITIONAL_INFO
     * First 4 bits store enemy Archon 3 guess locations that are invalid
     * Next 4 bits store enemy Archon 2...
     * 3 guess location 3 invalid | 3 guess location 2 invalid | ...
     */
    static final int ENEMY_ARCHON_ADDITIONAL_INFO = 16; // 16

    /** BUILDING_INFO
     * Info for buildings other than archons
     * unused | 0/1 builder building lab | 0/1 is there a lab? | Last 2 bits round number where lab last acted mod 4
     */
    static final int BUILDING_INFO = 17;

    /** RESOURCE_LOCATIONS_INDEX
     * Index 0 to RESOURCE_LOCATIONS_NUM_ELEMENTS:
     * First 6 bits stores x location and second 6 bits stores y location
     * Third 1 bit stores whether it's gold (1) or lead (0)
     * Last 3 bits stores need miners score (0 is unused, 1-7 is how much miners are needed)
     * x | x | x | x | x | x | y | y | y | y | y | y | gold or lead | score | score | score
     */
    static final int RESOURCE_LOCATIONS_INDEX = 56; // 56-63
    static final int RESOURCE_LOCATIONS_NUM_ELEMENTS = 8;
}
