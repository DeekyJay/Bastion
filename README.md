# Bastion

A comprehensive tower defense and wave-based survival Minecraft plugin that transforms villages into strategic battlegrounds.

## Overview

Bastion is a Java-based Minecraft plugin that implements an immersive tower defense game system within the Minecraft world. Players must defend villages against waves of hostile mobs while managing resources, building defenses, and upgrading their capabilities through a sophisticated economy system.

## Features

### 🏰 Core Game Systems
- **Wave Management**: Progressive difficulty waves with customizable mob spawning patterns
- **Game State Management**: Robust state handling for multiplayer sessions
- **Village Defense**: Protect and manage villages with strategic placement mechanics
- **Barrier System**: Dynamic barrier placement and management for tactical defense

### 🎮 Game Mechanics
- **Advanced Mob AI**: Intelligent mob behavior and pathfinding systems
- **Statistics Tracking**: Comprehensive player and game statistics
- **User Interface**: Intuitive in-game UI for managing defenses and resources
- **Progressive Difficulty**: Scaling challenge system that adapts to player progress

### 💰 Economy & Progression
- **Loot System**: Reward-based loot distribution for successful defense
- **Trading Mechanics**: Village-based trading system for resource management
- **Upgrade System**: Equipment and defense upgrade progression
- **Resource Management**: Strategic resource allocation and planning

### 🛠️ Technical Features
- **Docker Support**: Containerized deployment for easy server management
- **Gradle Build System**: Modern Java build tooling
- **Configurable Settings**: Extensive configuration options via YAML files
- **Plugin Integration**: Seamless integration with existing Minecraft server ecosystems

## Installation

### Prerequisites
- Minecraft Server (Bukkit/Spigot/Paper) compatible with the plugin
- Java 8 or higher
- Gradle (for building from source)
- JDK 21 (required for building from source)

### Server Installation
1. Download the latest plugin JAR from releases
2. Place the JAR file in your server's `plugins/` directory
3. Start/restart your Minecraft server
4. Configure the plugin using the generated config files in `plugins/Bastion/`

### Configuration
The plugin generates several configuration files:
- `config.yml` - Main plugin configuration
- `stats.yml` - Statistics and tracking settings

Customize these files according to your server's needs and restart the server to apply changes.

## Usage

### Starting a Game
1. Players join the server and navigate to a designated village area
2. Use the plugin commands to initiate a wave defense session
3. Manage resources and place barriers strategically
4. Survive progressively challenging waves of mobs

### Commands
The plugin provides various commands for game management and player interaction. Refer to the in-game help system or plugin documentation for detailed command usage.

### Game Flow
1. **Preparation Phase**: Set up defenses and allocate resources
2. **Wave Phase**: Defend against incoming mob waves
3. **Intermission**: Upgrade equipment and repair defenses
4. **Progression**: Face increasingly difficult challenges

## Building from Source

### Requirements
- Java Development Kit (JDK) 21
- Gradle build system

### Build Instructions

#### Docker Build (All Platforms)
Docker provides a consistent cross-platform build environment that works on Windows, Mac, and Linux:

```bash
# Clone the repository
git clone <repository-url>
cd Bastion

# Build and run with Docker Compose (quick start)
docker-compose up -d

# View logs
docker-compose logs -f
```

**Separate Build and Run Commands**

For more control over the build and deployment process, you can build and run components separately:

```bash
# Build the plugin only
docker-compose run build

# Run the server after building
docker-compose up mc-dev -d

# Or run all services after building
docker-compose up -d
```

**Development Workflow**

When actively developing the plugin, use this workflow for faster iteration:

1. **Initial Setup**: Start the server for the first time
   ```bash
   docker-compose up -d
   ```

2. **Plugin Development**: When you make changes to the plugin code
   ```bash
   # Rebuild just the plugin
   docker-compose run build

   # Restart to load the new plugin
   docker-compose restart mc-dev
   ```

3. **Hot Reload**: If the server is running and you've rebuilt the plugin
   ```bash
   # Connect to your Minecraft server and use:
   /reload confirm
   ```

4. **Getting Started with Bastion**: After reloading, initialize the game
   ```bash
   # In Minecraft chat, run these commands to get started:
   /bastion findvillage    # Locate or create a village for defense
   /bastion barrier        # Set up initial barriers
   /bastion start          # Begin the wave defense game
   ```

**Troubleshooting Development**
- If changes don't appear after rebuilding, ensure you've restarted the mc-dev container
- Use `/reload confirm` only when the plugin JAR has been updated in the container
- Check container logs with `docker-compose logs mc-dev` for any plugin loading errors

#### Native Build

**Windows**
For Windows development, we recommend using VS Code with Gradle integration:

1. Install the Gradle for Java extension in VS Code
2. Open the project folder in VS Code
3. Use the Gradle panel to run build tasks
4. The compiled JAR will be available in `build/libs/`

**Mac/Linux**
```bash
# Clone the repository
git clone <repository-url>
cd Bastion

# Build the plugin
./gradlew build

# The compiled JAR will be available in build/libs/
```

## Project Structure

```
src/main/java/city/emerald/bastion/
├── Bastion.java              # Main plugin class
├── BarrierManager.java       # Barrier placement and management
├── VillageManager.java       # Village system management
├── economy/                  # Economy system components
│   ├── LootManager.java      # Loot distribution system
│   ├── TradeManager.java     # Trading mechanics
│   └── UpgradeManager.java   # Equipment upgrades
├── game/                     # Core game mechanics
│   ├── GameStateManager.java # Game state handling
│   ├── StatsManager.java     # Statistics tracking
│   └── UIManager.java        # User interface management
└── wave/                     # Wave system components
    ├── MobAI.java           # Mob artificial intelligence
    ├── MobSpawnManager.java # Mob spawning logic
    └── WaveManager.java     # Wave progression system
```

## Documentation

For detailed information about the plugin's design and implementation:

- **[DESIGN.md](DESIGN.md)** - Comprehensive design documentation and architecture overview
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Technical implementation details and development guidelines

## Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests to help improve the plugin.

### Development Guidelines
1. Follow existing code style and conventions
2. Include appropriate documentation for new features
3. Test thoroughly before submitting changes
4. Update relevant documentation files

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, bug reports, or feature requests, please use the project's issue tracker or community forums.

---

*Transform your Minecraft server into an epic tower defense battlefield with Bastion!*