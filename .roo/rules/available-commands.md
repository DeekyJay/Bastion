# Available Commands for Bastion Minecraft Plugin

This file documents the proper commands for building and running the Bastion Minecraft plugin project. **Use Docker-based commands instead of Gradle commands** for consistent cross-platform development.

## Project Build Commands

### Quick Start Build & Run
```bash
# Build and start everything (plugin + server)
docker-compose up -d

# View real-time logs
docker-compose logs -f
```

### Separate Build Commands
```bash
# Build the plugin only (without starting server)
docker-compose run build

# Run the server after building
docker-compose up mc-dev -d

# Run all services after building
docker-compose up -d
```

## Server Management Commands

### Server Control
```bash
# Start the Minecraft server (after building)
docker-compose up mc-dev -d

# Stop the server
docker-compose down

# Restart the server (useful after plugin changes)
docker-compose restart mc-dev

# Stop specific service
docker-compose stop mc-dev
```

### Server Logs and Monitoring
```bash
# View server logs (follow mode)
docker-compose logs -f mc-dev

# View all service logs
docker-compose logs -f

# View recent logs (without follow)
docker-compose logs mc-dev
```

## Development Workflow Commands

### Initial Setup
```bash
# First time setup - builds plugin and starts server
docker-compose up -d
```

### Plugin Development Iteration
```bash
# 1. Rebuild plugin after code changes
docker-compose run build

# 2. Restart server to load new plugin
docker-compose restart mc-dev

# 3. Alternative: Hot reload (in Minecraft server console/chat)
/reload confirm
```

### Development Status Checks
```bash
# Check running containers
docker-compose ps

# Check container status
docker-compose top
```

## In-Game Commands

### Bastion Plugin Commands
```bash
# Initialize and start the game
/bastion findvillage    # Locate or create a village for defense
/bastion barrier        # Set up initial barriers
/bastion start          # Begin the wave defense game
```

### Server Management (In-Game)
```bash
# Hot reload plugin (after rebuilding)
/reload confirm
```

## Troubleshooting Commands

### Container and Log Inspection
```bash
# Check for plugin loading errors
docker-compose logs mc-dev

# View detailed container information
docker-compose ps -a

# Access container shell (if needed)
docker-compose exec mc-dev /bin/bash
```

### Common Issues
```bash
# If changes don't appear after rebuilding:
docker-compose restart mc-dev

# If server won't start:
docker-compose down
docker-compose up -d

# Clean rebuild (if needed):
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## File Locations

### Plugin Output
- Built plugin JAR: Available in container after `docker-compose run build`
- Server logs: Available via `docker-compose logs mc-dev`

### Configuration Files
- `config.yml` - Main plugin configuration
- `stats.yml` - Statistics and tracking settings
- `docker-compose.yml` - Container orchestration

## Best Practices

1. **Always use Docker commands** instead of `./gradlew` for building
2. **Use `docker-compose run build`** for plugin compilation
3. **Use `docker-compose restart mc-dev`** after plugin changes
4. **Use `docker-compose logs -f mc-dev`** to monitor server behavior
5. **Use `/reload confirm`** only after rebuilding the plugin JAR

## Command Priority

When suggesting commands to users:

1. **Primary**: Docker Compose commands for build/run/restart
2. **Secondary**: In-game Minecraft commands for plugin functionality
3. **Avoid**: Direct Gradle commands (`./gradlew`) - use Docker instead

This ensures consistent, cross-platform development experience regardless of the user's operating system.