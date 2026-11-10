# CombatLogger

CombatLogger tags players after player-versus-player combat and applies configured restrictions and logout punishments.

## Compatibility

The rework targets a single Java 8 bytecode artifact with the Spigot 1.8.8 API as its compile-time baseline. This is intended to preserve compatibility with legacy 1.8.8 servers while avoiding newer Bukkit API calls that would prevent the plugin from loading on older versions.

The server must still run on the Java version required by its Minecraft release. Java 8 bytecode does not mean modern Minecraft servers can run on Java 8.

The Java 8 build is checked by GitHub Actions. Runtime testing against each supported server version and implementation (Spigot, Paper, and forks) is still required before declaring the entire range fully verified.

## Optional integrations

All integrations are optional:

- **Vault** — economy punishments and permission-based punishment profiles.
- **WorldGuard** — configured protected-region entry restrictions; compatibility is handled reflectively for WorldGuard 6 and 7.
- **FactionsUUID** — Factions flight-command handling for the supported MassiveCraft API.
- **PlaceholderAPI** — external placeholders and CombatLogger placeholders.

The plugin can run without these integrations. Features requiring a missing integration are disabled or fall back to default behavior.

## Combat tagging rules

- Only player-versus-player damage can create combat tags.
- Player-fired projectiles count as player attacks.
- Non-player attackers and non-player victims are ignored.
- Self-damage is rejected by comparing player UUIDs.
- Cancelled damage events do not create tags.
- Damage in configured blacklisted worlds does not create tags.

## Build

Requirements: JDK 8 and Maven.

```bash
mvn clean verify
```

The resulting plugin JAR is written to `target/`.

## Configuration

- `config.yml` controls combat duration, messages, action bar, flight, gamemode, protected regions, blacklisted worlds, and blocked commands.
- `punishments.yml` controls default and permission-specific logout punishments.

Back up existing configuration files before upgrading. Newer releases may add configuration options; existing settings should be reviewed after updating.

## Development

The active compatibility rework is maintained on the `rework` branch. The `master` branch is kept separate until the rework is reviewed and runtime-tested.
