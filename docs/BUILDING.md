# Building IDCraft

You need JDK 25 and an internet connection (for all the deps). <p>

# Dependencies

IDCraft implements many of my custom libraries. You probably need to build/install all of them:

- [CSSDB API](https://github.com/OXipro/cssdb-api) (handles all the player settings database, ConfigLang depends on it)
- [CMU Config Lang](https://github.com/OXipro/cmu-config-lang) (manages all the languages & config)
- [OXipro's Shulker Fork](https://github.com/OXipro/Shulker) (handles autodetection and configuration when running in a shulker k8s cluster)

# Build Process

```bash
./gradlew shadowJar
```

Artifacts:

- `velocity-plugin/build/libs/idcraft-velocity-plugin.jar` on every Velocity proxy
- `minestom-server/build/libs/idcraft-minestom-server.jar` as one or more auth servers

## Modules

| Module | Role |
| --- | --- |
| `api` | Shared contracts (auth, accounts, messaging, sessions) |
| `core` | Auth manager, Mojang lookup, password hashing, config defaults |
| `velocity-plugin` | Proxy: login decision, sessions, forwarding, auth-server load balancing |
| `minestom-server` | Auth server: dialogs, commands, world, factors |
| `db-postgres` / `db-mysql` | SQL repositories |
| `redis-provider` | Session cache and Redis messaging |
| `minestom-shulker-impl` / `velocity-shulker-impl` | Optional [Shulker](https://github.com/OXipro/Shulker) integration |

Default `config.yml` files live in each runnable module (`velocity-plugin` and `minestom-server`).
