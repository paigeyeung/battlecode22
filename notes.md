Team A is red, Team B is blue

Ideas
- Rush enemy Archons for the win
    - Sacrifice Archon to rush
- Fortify and play defense
    - If I have more Archons than enemy, transition to defensive
- Scouting enemy strategy
- Cannon rush
    - Builder builds watchtowers around enemy base
    - Builder builds watchtowers in unoccupied location and watchtowers move to enemy base
- Play defensive on long maps and rush on short maps
- Gather data of tournament map?
- Each my unit toggles a bit every turn it's alive, stopped toggling means it's dead?
- Remove seeded randomness?
- Have some soldiers encircling my Archons at all times (move them out in that direction?)

w changelog
- Archon
    - example
        - Randomly (50% / 50%) build a miner or a soldier in a random direction
    - w1
        - Track number of units built in static variables
    - w2
        - Build miner or soldier to maintain 50% / 50% balance
        - Track ally Archon locations in shared array
        - Guess enemy Archon locations in shared array
    - w4
        - If there are hostile enemies in vision range, stop producing miners
            - If can build soldier, build solder
            - Otherwise, if there is a damaged soldier, repair it
        - Try to build in direction of enemy
    - w5
        - Build miner / builder / soldier in 30% / 10% / 60%
        - If turn above 500, don't spend lead if below 200
    - w6
        - Build minder / solder in 30% / 70%
    - w9
        - Toggle last bit in shared array to show alive and check for death of other Archons
    - w10
        - Start game by building 1 miner
        - Archons each have roles and builds are split evenly between them
    - future
        - Not guarenteed to execute chosen action if all surrounding squares blocked
        - Determine number of miners each Archon produces based on amount of lead in vision
            - Can deduce number of miners mining lead this turn by tracking all expenses and calculating change in lead per turn
- Miner
    - example
        - Try to mine gold and lead if there’s any around us
        - Otherwise, move in a random direction
    - w1
        - Slight adjustment to always mine gold before mining lead
    - future
        - Try mine gold, then try move towards gold, then try mine lead, then try move towards lead
- Builder
    - w5
        - Try to repair prototype building
        - Try to repair damaged building
        - Try to build watchtower every 200 turns
        - Move randomly
- Soldier
    - example
        - Try to attack enemy if there’s any around us
        - Otherwise, move in a random direction
    - w1
        - If no enemy is nearby, move towards enemy Archon
        - If enemy is nearby, move towards ally Archon
        - If path is blocked, move randomly
    - w2
        - Pick nearest Archons to move towards
    - w3
        - If hostile enemy is nearby, move towards my Archon
        - If peaceful enemy is nearby, move towards enemy
    - w6
        - Smart selection of attack target
    - w7
        - Locally evaluate combat for decision making
    - future
        - Use shared array for combat decision making
- Sage
- Laboratory
- Watchtower
    - w5
        - Try to attack enemy
- Any unit
    - example
        - Track turn count in static variable
    - w1
        - Track starting location in static variable
    - w2
        - Read Archon locations from shared array
        - Droids help update Archons seen and alive in shared array
    - w6
        - Track starting Archon in static variable
    - w8
        - Refactor into classes
    - w9
        - Continued refactor, added Debug Manager
    - future
        - Search for enemy Archons in radius if it is missing
        - Super high movement penalty for blocking off the last surrounding square of an Archon, or not moving away from a crowded Archon
- Shared array
    - w2
        - Indicies 0-3 store ally Archon locations, 4-7 store enemy Archon locations
            - Max map width and height are 60
            - 0 to 64 can be stored in 6 bits
            - Each 16 bit int stores x location in first 6 bits and y location in second 6 bits
            - Second to last bit is whether unit is alive or not
            - Last bit is whether unit has been seen at least once
            - x | x | x | x | x | x | y | y | y | y | y | y | alive | seen
    - w9
        - Indicies 0-3 store ally Archon locations
            - Last bit is toggled by ally Archon every turn to show it's alive
            - x | x | x | x | x | x | y | y | y | y | y | y | alive | toggle
    - w10
        - Indicies 8-9 store values for Archon resource manager
            - Index 8
                - First 12 bits store lead amount at start of turn
                - Last 4 bits store whether Archons 0-3 on cooldown last turn
                - lead... | 3 on cooldown | 2 on cooldown | 1 on cooldown | 0 on cooldown
            - Index 9
                - First 12 bits store gold amount at start of turn
                - Last 4 bits store whether Archons 0-3 on cooldown this turn
                - gold... | 3 on cooldown | 2 on cooldown | 1 on cooldown | 0 on cooldown
    - future
        - Indicies ?-? could store multiples of 5 so can store larger max values
        - Index ? stores combat scores
            - First 6 bits is ally highest combat score
            - Second 6 bits is enemy highest combat score
            - Last 4 bits are last turn's combat decision 

p changelog
- Pathfinding
    - Long distance general purpose pathfinding algorithm
    - Individual short distance algorithms for each unit
         - Miners
             - Highest priority is to avoid enemy hostile units
             - Mine gold if possible
             - Move towards gold if possible and visible
             - Mine lead if possible
             - Move towards lead if possible and visible
             - Search unexplored areas for lead
         - Soldiers + sages
             - later
    - p3
      - Miners move based on combination of factors
    - p4
      - Combined p3 with w6, added soldier movement
    - p5
      - Refining p4
    - p6
      - Miners share locations with high resource density
      - Target locations - split between high-resource-density locations, edges of map, archon that produced it

w and p merged into m1

m1w changelog
- Archon
    - m1w1
        - Build miners in direction of lead and build soldiers in direction of nearest enemy Archon
        - Meta adaptation: Build miners until first see enemy
    - future
        - If low on lead income per turn, increase proportion of miners built
- Shared array
    - m1w1
        - Index 10 stores general strategy info
            - Last 4 bits store if a unit near Archons 0-3 have seen an enemy
            - unused... | 3 seen enemy | 2 seen enemy | 1 seen enemy | 0 seen enemy

m1p changelog
- Miner
    - Cost to move is higher when there are already resources available
    - Cost to move is lower when miner is close to a friendly Archon
- Soldier
    - Moves when there are other friendly soldiers in range
    - Cost to move is higher when enemies are in range

m1p and m1w merged into m2

m2w changelog
- General
    - m2w1
        - Make manager for shared array indicies
        - Optimize static variables

m2w became m3

m3w changelog
- All units
    - m3w1
        - If in range of enemy Archon and still don't see it, go to next guess location
        - If there is no next guess location, mark it as missing
        - If all enemy Archons are missing, go to center of map
    - m3w2
        - Ability to rediscover enemy Archon if previously missing and now seen
- Shared array
    - m3w1
        - Index 10 stores general strategy info
            - First 4 bits unused
            - Second 8 bits store guess location index of enemy Archons 0-3
            - Last 4 bits store if a unit near ally Archons 0-3 have seen an enemy
            - unused | unused | unused | unused | 3 guess location 2 | 3 guess location 1 | 2 guess location 2 | 2 guess location 1 | 1 guess location 2 | 1 guess location 1 | 0 guess location 2 | 0 guess location 1 | 3 seen enemy | 2 seen enemy | 1 seen enemy | 0 seen enemy
    - m3w2
        - Index 10 stores general strategy info
            - First 4 bits store if enemy Archon 0-3 guess location is manually overridden
            - Second 8 bits store guess location index of enemy Archons 0-3
            - Last 4 bits store if a unit near ally Archons 0-3 have seen an enemy
            - 3 overridden | 1 overridden | 2 overridden | 0 overridden | 3 guess location 2 | 3 guess location 1 | 2 guess location 2 | 2 guess location 1 | 1 guess location 2 | 1 guess location 1 | 0 guess location 2 | 0 guess location 1 | 3 seen enemy | 2 seen enemy | 1 seen enemy | 0 seen enemy

m3w and m2p merged into m4

m4w changelog
- Miner
    - m4w1
        - Don't deplete lead if closer to ally Archon than Enemy Archon

m4p changelog
- Archon
    - m4p1
        - Move ally Archons

m4w1 and m4p1 merged into m5

m5 became m6

m6 changelog
- Miner
    - Deplete lead if more enemies than allies nearby
- Shared array
    - Split index 10 general strategy into ALLY_ARCHON_ADDITIONAL_INFO and ENEMY_ARCHON_ADDITIONAL_INFO
    - ALLY_ARCHON_ADDITIONAL_INFO
        - Last 4 bits store if a unit near ally Archons 0-3 have seen an enemy
        - unused... | 3 seen enemy | 2 seen enemy | 1 seen enemy | 0 seen enemy
    - ENEMY_ARCHON_ADDITIONAL_INFO 
        - First 4 bits store if enemy Archon 0-3 guess location is manually overridden
        - Second 8 bits store guess location index of enemy Archons 0-3
        - Last 4 bits unused
        - 3 overridden | 1 overridden | 2 overridden | 0 overridden | 3 guess location 2 | 3 guess location 1 | 2 guess location 2 | 2 guess location 1 | 1 guess location 2 | 1 guess location 1 | 0 guess location 2 | 0 guess location 1 | unused...

- future
  - Call rc.getLocation once per turn, rc.getType once per game