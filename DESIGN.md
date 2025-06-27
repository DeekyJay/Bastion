# Bastion Plugin Design Document

## 1. Overview

### Core Concept
Bastion is a Minecraft plugin that transforms villages into defensive battlegrounds where players must protect villagers from increasingly challenging waves of hostile mobs.

### Primary Objectives
- Create an engaging wave-based defense experience
- Encourage player cooperation and strategic resource management
- Provide progressive difficulty scaling that maintains challenge
- Integrate with vanilla Minecraft mechanics for natural gameplay feel
- Prevent trivial defensive strategies through counter-mechanics

### Target Experience
Players work together to defend a village within a confined space, utilizing vanilla Minecraft progression systems including villager trading, enchanting, farming, building defenses, and breeding villagers. Resource management focuses on mob drops rather than mining, creating a self-contained survival experience while facing increasingly difficult waves of enemies.

## 2. Game Mechanics

### Wave System

#### Progressive Difficulty
- Difficulty increases through higher mob counts and faster spawn rates
- Mob health and damage remain at vanilla levels throughout all waves
- Each wave has an increasing maximum mob count limit
- Spawn rate increases with each wave number
- Boss waves every 10 rounds featuring unique enemies
- Wave completion occurs when sunrise arrives OR maximum mob count is reached
- At sunrise, ALL remaining mobs are automatically killed (burned/eliminated)
- Mobs killed by sunrise mechanics DO NOT drop loot
- Sunrise wave completion counts as UNSUCCESSFUL - wave must be repeated without incrementing wave count
- Game ends immediately when ALL villagers are killed

#### Player Count Scaling
- Base mob count = 10 + (2 × number_of_players)
- Maximum mob count increases with wave number
- Spawn rate scales with both wave number and player count
- Resource drops scale proportionally to player-killed mobs only

#### Trickle-Spawn System
- Mobs spawn in smaller groups rather than all at once
- Spawn frequency increases with wave number
- Spawn locations randomized within valid areas outside village center
- Visual and sound effects signal incoming spawns

### Combat & Enemies

#### Mob Types and Variations
- Standard Mobs (Waves 1-9):
  - Zombies: Basic melee attackers
  - Skeletons: Ranged attackers
  - Spiders: Fast-moving flankers
  - Creepers: Explosive threats with obstruction-triggered detonation
- Elite Variants (Every 5th wave):
  - Enhanced versions with special abilities
  - Unique visual identifiers
  - Same health/damage as standard variants
- Boss Mobs (Every 10th wave):
  - Custom-named with unique abilities
  - Vanilla health pools
  - Special drop tables
  - Flying mobs: Phantoms, Blazes, Ghasts capable of targeting elevated positions

#### Enhanced Pathfinding Behavior
- Mobs have significantly larger aggro range for detecting targets
- Mobs actively pathfind to both players and villagers from greater distances  
- Prioritize targeting based on proximity and threat level
- Group coordination for elite mobs
- Tactical retreats for ranged units
- Special Creeper Behavior: When pathfinding is obstructed to target player or villager, creeper will explode immediately

#### Anti-Exploit Mechanics
- Buried/Barricaded Villagers: Creepers specifically target and explode when close to villagers blocked in holes or buildings
- Elevated Villagers: Flying boss mobs (Phantoms, Blazes, Ghasts) can target villagers placed at build height
- Unreachable Villager Detection: When villagers cannot be reached by normal pathfinding, spawn probabilities increase:
  - Creeper spawn rate increases significantly
  - Lightning strike frequency increases during boss waves
  - Flying mob spawn rate increases
- These mechanics prevent trivial defensive strategies that remove challenge from the game

#### Boss Wave Environmental Hazards
- Lightning strikes occur randomly during boss waves (every 10th wave)
- Lightning strikes utilize vanilla Minecraft lightning mechanics:
  - Converts villagers into witches via natural lightning strike (removing them from protection objectives)
  - Transforms creepers into charged creepers via natural lightning strike (increased explosion damage)
  - Damages players caught in natural lightning strikes (standard lightning damage)
- Lightning targeting system ensures strikes hit players, villagers, and creepers within the barrier
- Lightning adds additional challenge and unpredictability to boss encounters

#### Special Drop Tables
- Common drops: XP orbs, basic materials, food items (only from player-killed mobs)
- Rare drops: Mining materials (emeralds, diamonds, obsidian), adventure items (potions, leather, paper)
- Equipment drops: Armor pieces, weapons, tools with varying durability and enchantments
- Crafting drops: items needed for crafting normally obtained in the Nether or through mining (redstone, nether wart, diamonds)
- Elite drops: Higher-tier enchanted items, rare materials (only from player-killed mobs)
- Boss drops: Guaranteed high-value items including rare enchanted gear (only from player-killed mobs)
- Drop quality and rarity increase progressively with wave number
- Mobs killed by sunrise mechanics provide NO drops or XP
- Mob drops designed to reduce mining necessity while maintaining resource variety

### Village System

#### Village Selection/Initialization
- 80-block radius play area
- 3D dome barrier boundaries
- Central spawn point placement
- Villager registration system

#### Barrier Mechanics
- Defines configurable dome-shaped play area
- Prevents entry/exit of all entities with spherical collision detection
- 3D dome particle visualization with underground visibility
- Clear boundary markers for players throughout the dome structure

#### Enhanced Villager Properties
- Improved pathfinding for escape behavior
- Cannot leave barrier area
- Panic state triggers running from nearby mobs
- Health regeneration between waves
- Full vanilla trading mechanics preserved
- Vanilla breeding capabilities utilized (no custom breeding mechanics required)
- Trade progression unlocked through wave advancement

### Economy & Progression

#### Vanilla Minecraft Integration
- Full compatibility with vanilla enchanting mechanics
- Standard villager trading system with profession-based trades
- Farming and food production capabilities
- Vanilla villager breeding system (no custom modifications needed)
- Building and defensive construction options
- Crafting system utilizing mob-dropped materials

#### Trading System
- Standard villager trading mechanics with all professions
- Enhanced trade options unlocked by wave progression
- Special trades from rescued villagers
- Trading post protection zones
- XP acquisition through trading for enchanting progression

#### Resource Acquisition Methods
- Primary: Mob drops (weapons, armor, materials, rare items)
- Secondary: Farming (food, breeding materials, basic resources)
- Tertiary: Trading with villagers (XP, specialized items)
- Limited: Mining (possible but not necessary for progression)
- Enhanced mob drops replace traditional mining/adventuring rewards

#### Item Acquisition
- Mob drops: Enhanced loot tables including mining materials (emeralds, diamonds, obsidian)
- Adventure items: Potions, leather, paper, enchanted books
- Equipment: Armor, weapons, tools with progressive quality
- XP from kills, wave completion, and villager trading
- Special rewards from boss waves
- Farming: Food, breeding materials, basic crafting components

#### Equipment Progression
- Standard Minecraft enchanting system using XP from multiple sources
- Materials gathered from enhanced mob drops rather than mining
- Strategic choices between personal gear, village improvements, and defenses
- Enhanced crafting recipes utilizing rare mob-dropped materials
- Building defensive structures using dropped and farmed materials

### Scoring System

#### Performance Tracking
- Player-killed mob count: Primary scoring metric tracking mobs eliminated by players
- Player death count: Negative scoring factor tracking player casualties
- Wave progression: Bonus points for reaching higher waves
- Villager survival rate: Bonus multiplier based on villagers protected
- Sunrise kills are excluded from scoring (no points awarded)

#### Score Calculation
- Base points per player-killed mob (varies by mob type)
- Death penalty: Points deducted per player death
- Wave completion bonus: Escalating rewards for higher waves reached
- Villager protection multiplier: Score multiplied by percentage of villagers alive

#### Game Over Display
- Game ends when all villagers are killed
- Final statistics displayed for all players:
  - Completed wave count (successful waves only)
  - Individual kill/death counts per player
  - Fun stats for each player. Using the vanilla stats, find the highest stat for each player that is also higher that the other players. There will be one distinct stat for each player, where they had 'the most'. Then find the player with the _lowest_ value for each of those stats. E.g. "most blocks placed by Foo, least blocks placed by Bar"
  - Total score and ranking
  - Villager survival statistics

## 3. Technical Requirements

### Core Systems
1. Wave Management System
   - Wave state tracking with day/night cycle integration
   - Mob spawning coordination with increasing spawn rates
   - Maximum mob count tracking per wave
   - Sunrise mob elimination mechanics (unsuccessful wave completion)
   - Wave repetition system for sunrise-failed waves
   - Victory/defeat conditions based on time, mob limits, or villager survival
   - Game termination when all villagers are killed with final statistics display
   - Player respawn handling

2. Boundary System
   - Configurable dome barrier implementation (default 80-block radius)
   - 3D spherical collision detection for entity containment
   - Enhanced particle visualization system with underground visibility
   - Spawn point management

3. Mob Enhancement System
   - Enhanced aggro range implementation
   - Advanced pathfinding AI modifications
   - Creeper obstruction detection and explosion triggers
   - Anti-exploit targeting system for buried/barricaded villagers
   - Flying mob AI for targeting elevated positions
   - Unreachable villager detection system
   - Dynamic spawn probability adjustment (creepers, lightning, flying mobs)
   - Enhanced drop table management with rare item integration
   - Progressive drop quality scaling with wave difficulty
   - Drop differentiation (player-killed vs sunrise-killed)

4. Environmental Hazard System
   - Lightning generation during boss waves
   - Lightning targeting system for players, villagers, and creepers
   - Integration with vanilla lightning transformation mechanics

5. Player Management
   - Death/respawn handling with death count tracking
   - Progress tracking and scoring system
   - Inventory management between waves
   - XP and enchanting progression tracking

6. Villager Enhancement
   - Custom AI implementation with vanilla trading preservation
   - Breeding mechanics and population management
   - Trade progression unlocked by wave advancement
   - Protection mechanics
   - Villager death tracking for game termination conditions

6. Scoring and Statistics System
   - Player-killed mob count tracking
   - Player death count tracking
   - Wave progression statistics
   - Villager survival rate calculation
   - Score calculation and leaderboards

7. Vanilla Integration Systems
   - Enhanced mob drop tables with rare item generation
   - Farming and breeding mechanics preservation
   - Enchanting system integration with multiple XP sources
   - Building and crafting system support
   - Villager profession and trading system maintenance

### Important Considerations
- Performance optimization for enhanced mob AI and larger aggro ranges
- Scalability for multiple concurrent games
- Accurate day/night cycle tracking for sunrise mechanics
- Efficient mob kill tracking and differentiation (player vs sunrise kills)
- Balanced rare drop rates to maintain gameplay flow without trivializing progression
- Preservation of vanilla mechanics while enhancing mob behavior
- Anti-exploit system implementation without breaking legitimate defensive strategies
- Lightning strike timing and frequency balance during boss waves
- Lightning targeting system to ensure proper entity selection
- Vanilla lightning effect integration and tracking
- Game termination detection and handling when all villagers are eliminated
- Data persistence for scoring and long-term progression
- Proper cleanup on plugin disable
- Efficient entity tracking with larger detection ranges
- Integration with vanilla villager AI and trading without conflicts

### Plugin Dependencies
- Spigot/Paper API (1.21+)
- No additional plugin dependencies required