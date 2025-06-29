# Code Change Workflow for Bastion Minecraft Plugin

This file establishes the mandatory workflow to follow when any code changes are made to the Bastion Minecraft plugin project. This ensures proper build processes and server management after modifications.

## When This Rule Applies

This workflow **MUST** be followed whenever:
- Java source code files (`.java`) are modified
- Configuration files (`config.yml`, `stats.yml`, `plugin.yml`) are changed
- Build configuration files (`build.gradle`, `gradle.properties`) are updated
- Any other plugin-related files are modified

## Required Build Step

### Mandatory Build After Code Changes
```bash
# ALWAYS run this command after making any code changes
docker-compose run build
```

**This step is non-negotiable** - the plugin must be rebuilt to reflect code changes in the running environment.

## Server Status Validation

After building, **ALWAYS** check the current server state before proceeding:

```bash
# Check if the Minecraft server is currently running
docker-compose ps

# Get recent server logs to verify current state
docker-compose logs --tail=100 mc-dev
```

**Interpreting `docker-compose ps` output:**
- If `mc-dev` shows `State: Up` - Server is running
- If `mc-dev` shows `State: Exit` or is missing - Server is not running

## Server Management Decision Flow

Based on the server status validation, follow this decision tree:

### Option 1: Server Not Running
If [`docker-compose ps`](docker-compose.yml) shows the server is not running:

```bash
# Start the server with the newly built plugin
docker-compose up mc-dev -d

# Check server startup logs
docker-compose logs mc-dev
```

### Option 2: Server Already Running
If [`docker-compose ps`](docker-compose.yml) shows the server is running, choose one of these approaches:

#### Hot Reload (Preferred for Quick Testing)
```bash
# In Minecraft server console or in-game chat
/reload confirm
```

**Benefits:**
- Faster than server restart
- Maintains player connections
- Good for testing small changes

**Limitations:**
- May not fully reload all plugin components
- Can cause issues with complex state changes

#### Full Server Restart (Recommended for Major Changes)
```bash
# Restart the server to ensure clean plugin loading
docker-compose restart mc-dev

# Check restart logs
docker-compose logs mc-dev
```

**Benefits:**
- Guarantees complete plugin reload
- Clears all plugin state
- More reliable for major changes

## Decision Guide

### When to Use Hot Reload (`/reload confirm`)
- Minor bug fixes
- Configuration changes
- Small feature additions
- Quick iterative testing

### When to Use Server Restart
- Major code restructuring
- New command additions
- Database/storage changes
- Plugin initialization changes
- When hot reload doesn't work as expected

## Complete Workflow Example

```bash
# 1. Make code changes to Java files
# (edit source files)

# 2. MANDATORY: Rebuild the plugin
docker-compose run build

# 3. MANDATORY: Check server status and recent logs
docker-compose ps
docker-compose logs --tail=100 mc-dev

# 4a. If server not running:
docker-compose up mc-dev -d

# 4b. If server running - choose reload method:
# Quick testing:
# /reload confirm (in server console/chat)

# OR comprehensive reload:
docker-compose restart mc-dev

# 5. MANDATORY: Validate changes took effect
docker-compose logs --tail=100 mc-dev

# 6. Check for any issues in recent logs
docker-compose logs mc-dev
```

## Validation Steps

After following the workflow, verify the changes took effect using these **mandatory validation commands**:

### Check Recent Server Activity
```bash
# Get the most recent 100 lines of server logs
docker-compose logs --tail=100 mc-dev
```

**Look for:**
- The success signal: `Done (xx.xxxs)! For help, type "help"` with no exceptions thrown
- Plugin reload/restart messages
- Any error messages or stack traces
- Confirmation that Bastion plugin loaded successfully

**When you see the success signal with no exceptions:**
- The server is ready for user testing
- Recommend the user log onto the server to test functionality

### Check Plugin Loading
```bash
# View server logs for plugin load confirmation
docker-compose logs mc-dev | grep -i bastion
```

### Verify Server Status
```bash
# Confirm server is running properly
docker-compose ps
```

**Expected output:** `mc-dev` service should show `State: Up`

### Test Functionality
```bash
# In Minecraft server console or in-game
/bastion help
```

### Check for Recent Errors
```bash
# Check recent logs for any errors
docker-compose logs mc-dev
```

**Validation Checklist:**
- [ ] Server status shows as "Up" in [`docker-compose ps`](docker-compose.yml)
- [ ] Recent logs show the success signal: `Done (xx.xxxs)! For help, type "help"` with no exceptions
- [ ] Bastion plugin appears in logs as loaded
- [ ] No critical errors in recent logs
- [ ] User can log onto server for testing
- [ ] `/bastion help` command responds correctly

## Common Mistakes to Avoid

1. **Skipping the build step** - Changes won't appear without rebuilding
2. **Using Gradle directly** - Always use Docker Compose for consistency
3. **Not monitoring logs** - Miss important error messages
4. **Hot reloading complex changes** - Use server restart for major modifications

## Integration with Other Rules

This workflow integrates with [`available-commands.md`](.roo/rules/available-commands.md):
- Uses Docker-based commands as specified
- Follows the established command priority
- Maintains consistency with existing development practices

## Enforcement

**All modes working with code changes must:**
1. Execute [`docker-compose run build`](docker-compose.yml) after any code modification
2. Check server status using [`docker-compose ps`](docker-compose.yml)
3. Review recent logs using [`docker-compose logs --tail=100 mc-dev`](docker-compose.yml)
4. Ask the user about their preferred server management approach
5. Provide specific commands based on the server state
6. Execute validation steps to verify changes took effect
7. Suggest monitoring steps for ongoing verification

This ensures consistent, reliable development workflows across all project interactions.