# Generator holograms

FancyHolograms is optional. DecentHolograms works on its own. To select it,
set this in `config.yml` and restart the server:

```yaml
holograms:
  type: DecentHolograms
```

NextGens first loads the configured provider. If it is unavailable, NextGens
tries other installed, enabled providers. With no usable provider, generator
holograms are disabled and a warning is logged.

Supported configuration values are `DecentHolograms`, `FancyHologram` (also
`FancyHolograms`), and `HolographicDisplays`. The FancyHolograms integration
targets the 2.x API (compiled against 2.9.1). FancyHolograms 3 is not supported
by this integration; use DecentHolograms or a compatible FancyHolograms 2.x
release for your server. Only the selected provider needs to be installed.

The `NoClassDefFoundError: de/oliver/fancyholograms/api/data/HologramData`
report in issue #16 concerns NextGens 1.3.3. Current source isolates optional
provider classes and skips providers that fail to load. The issue #16 follow-up
also avoids loading unselected providers while the configured one is usable.
Check your server's NextGens version and the selected provider before installing
additional hologram plugins. Current source requires Java 25; upgrading from an
older binary also requires checking server and Java compatibility.

Run the dependency-free selection regression check with a JDK:

```sh
java scripts/HologramProviderCheck.java
```

This check compiles the real manager and provider interface against small API
substitutes to exercise selection, fallback, and missing dependency behavior.
It does not run a Minecraft server or prove hologram rendering. Before merging,
test the built JAR on a separate server with only DecentHolograms installed,
trigger a generator corruption hologram, repair the generator, and confirm the
hologram is removed without any FancyHolograms class errors.
