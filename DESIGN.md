# Bastion Plugin Design Document

## 1. Overview

### Core Concept
Bastion is a Minecraft plugin that transforms villages into defensive battlegrounds where players must protect villagers from increasingly challenging waves of hostile mobs.

### Primary Objectives
- Create an engaging wave-based defense experience
- Encourage player cooperation and strategic resource management
- Provide progressive difficulty scaling that maintains challenge
- Integrate with vanilla Minecraft mechanics for natural gameplay feel

### Target Experience
Players work together to defend a village within a confined space, managing resources, upgrading their gear, and protecting villagers while facing increasingly difficult waves of enemies.

## 2. Game Mechanics

### Wave System

#### Progressive Difficulty
- Base difficulty multiplier: 1.5x per wave
- Mob stats scale exponentially:
  - Health: Base × 1.5^(wave_number)
  - Damage: Base × 1.5^(wave_number)
- Boss waves every 10 rounds featuring unique enemies
- Wave completion requires elimination of all spawned mobs

#### Player Count Scaling
- Base mob count = 10 + (2 × number_of_players)
- Mob health scales with player count: Base × (1 + 0.3 × number_of_players)
- Resource drops scale proportionally to maintain balance

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
  - Creepers: Explosive threats
- Elite Variants (Every 5th wave):
  - Enhanced versions with special abilities
  - Unique visual identifiers
- Boss Mobs (Every 10th wave):
  - Custom-named with unique abilities
  - Higher health pools
  - Special drop tables

#### Enhanced Pathfinding Behavior
- Mobs actively target both players and villagers
- Prioritize targeting based on proximity and threat level
- Group coordination for elite mobs
- Tactical retreats for ranged units

#### Special Drop Tables
- Common drops: XP orbs, basic materials
- Elite drops: Enchanted items, rare materials
- Boss drops: Guaranteed high-value items
- Progressive drop quality based on wave number

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

### Economy & Progression

#### Trading System
- Standard villager trading mechanics
- Enhanced trade options unlocked by wave progression
- Special trades from rescued villagers
- Trading post protection zones

#### Item Acquisition
- Mob drops (weapons, armor, materials)
- XP from kills and wave completion
- Special rewards from boss waves
- Trading with villagers

#### Equipment Progression
- Standard Minecraft enchanting system
- Materials gathered from mob drops
- Strategic choices between personal gear and village improvements
- Enhanced crafting recipes unlocked by progression

## 3. Technical Requirements

### Core Systems
1. Wave Management System
   - Wave state tracking
   - Mob spawning coordination
   - Victory/defeat conditions
   - Player respawn handling

2. Boundary System
   - Configurable dome barrier implementation (default 80-block radius)
   - 3D spherical collision detection for entity containment
   - Enhanced particle visualization system with underground visibility
   - Spawn point management

3. Mob Enhancement System
   - Custom mob properties
   - AI modifications
   - Drop table management

4. Player Management
   - Death/respawn handling
   - Progress tracking
   - Inventory management between waves

5. Villager Enhancement
   - Custom AI implementation
   - Trade management
   - Protection mechanics

### Important Considerations
- Performance optimization for mob AI
- Scalability for multiple concurrent games
- Data persistence for long-term progression
- Proper cleanup on plugin disable
- Efficient entity tracking

### Plugin Dependencies
- Spigot/Paper API (1.21+)
- No additional plugin dependencies required