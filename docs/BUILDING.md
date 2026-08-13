# Building IDCraft

You need JDK 25 and a SQL database (PostgreSQL, MySQL, or MariaDB). Redis is only required if you enable sessions or Redis messaging.

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
| `cache-local` | In-process cache |
| `minestom-shulker-impl` / `velocity-shulker-impl` | Optional [Shulker](https://github.com/OXipro/Shulker) integration |

Default `config.yml` files live in each runnable module (`velocity-plugin` and `minestom-server`).
