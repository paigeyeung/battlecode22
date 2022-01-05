Team A is red, Team B is blue

Ideas

- Rush enemy Archons for the win
    - Sacrifice Archon to rush
- Fortify and play defense
    - If I have more Archons than enemy, transition to defensive
- Scouting enemy strategy
- Gather data of tournament map

William's Changelog

- Archon
    - example
        - Randomly (50% / 50%) build a miner or a soldier in a random direction
    - w1
        - Track number of units built in static variables
    - w2
        - Build miner or soldier to maintain 50% / 50% balance
        - Track my Archon locations in shared array
        - Guess enemy Archon locations in shared array
    - w4
        - If there are hostile enemies in vision range, stop producing miners
            - If can build soldier, build solder
            - Otherwise, if there is a damaged soldier, repair it
        - Try to build in direction of enemy
- Miner
    - example
        - Try to mine gold and lead if there’s any around us
        - Otherwise, move in a random direction
    - w1
        - Slight adjustment to always mine gold before mining lead
    - future
        - Try mine gold, then try move towards gold, then try mine lead, then try move towards lead
- Builder
- Soldier
    - example
        - Try to attack enemy if there’s any around us
        - Otherwise, move in a random direction
    - w1
        - If no enemy is nearby, move towards enemy Archon
        - If enemy is nearby, move towards my Archon
        - If path is blocked, move randomly
    - w2
        - Pick nearest Archons to move towards
    - w3
        - If hostile enemy is nearby, move towards my Archon
        - If peaceful enemy is nearby, move towards enemy
    - future
        - Prioritize attack the lowest health enemy, multiplied by enemy threat score
- Sage
- Laboratory
- Watchtower
- Any unit
    - example
        - Track turn count in static variable
    - w1
        - Track starting location in static variable
    - w2
        - Read Archon locations from shared array
        - Droids help update Archons seen and alive in shared array
    - future
        - Search for enemy Archons in radius if it is missing
        - Remove seeded randomness
- Shared array
    - w2
        - Indicies 0-3 store my Archon locations, 4-7 store enemy Archon locations
            - Max map width and height are 60
            - 0 to 64 can be stored in 6 bits
            - Each 16 bit int stores x location in first 6 bits and y location in second 6 bits
            - Second to last bit is whether unit is alive or not
            - Last bit is whether unit has been seen at least once
            - x | x | x | x | x | x | y | y | y | y | y | y | alive | seen

Paige's Notes
- pathfinding
    - long distance general purpose pathfinding algorithm
    - individual short distance algorithms for each unit
         - miners
             - highest priority is to avoid enemy hostile units
             - mine gold if possible
             - move towards gold if possible and visible
             - mine lead if possible
             - move towards lead if possible and visible
             - search unexplored areas for lead
         - soldiers + sages
             - later