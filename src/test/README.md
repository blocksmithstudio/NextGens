# Regression checks

Run `bash scripts/check-regressions.sh` from the repository root with the JDK specified in `pom.xml`. Maven options and goals can be passed to the script. CI runs `bash scripts/check-regressions.sh clean` to verify a clean build with the same checks. It resolves the dependency classpath and runs the standalone hologram-provider and pending-refund scripts plus all checks below; no Minecraft server or live database is contacted.

The checks cover cancelled and rejected sales, stale generator actions, repairs, explicit permission denials, timers, menu pagination, startup loading, pending generator protection, delayed scheduler callbacks and shutdown cleanup when final writes or saves fail. Production classes are exercised with isolated Bukkit, economy and scheduler fixtures. Startup checks use a disposable SQLite database under `target`.

A live Spigot/Paper/Folia server with the supported integration plugins is still needed to verify gameplay, scheduling ownership and plugin interaction ordering. Reflection and `Unsafe` are confined to the test fixtures; their compiler warnings do not affect the plugin artifact.
