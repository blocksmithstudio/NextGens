# Generator holograms

FancyHolograms is optional. DecentHolograms works on its own. To select it,
set this in `config.yml` and restart the server:

```yaml
holograms:
  type: DecentHolograms
```

NextGens uses the configured provider when its plugin is installed and enabled.
Otherwise it uses the first available registered provider. Built-in providers
are discovered in this order: DecentHolograms, FancyHolograms, HolographicDisplays.
Selection stops at the first usable provider instead of loading the rest.
Missing or broken providers are skipped. If none is available, NextGens logs a
warning and continues running with generator holograms disabled. Provider
registration never disables NextGens or other server plugins.

Supported configuration values are `DecentHolograms`, `FancyHologram` (also
`FancyHolograms`), and `HolographicDisplays`. The FancyHolograms integration
uses `de.oliver:FancyHolograms:2.9.1`, the version shown in the
[official API getting-started guide](https://fancyinnovations.com/docs/minecraft-plugins/fancyholograms/api/getting-started)
when checked on October 18, 2026. No dependency upgrade is needed. Only the
selected provider needs to be installed. Diagnose a missing API class using
the actual loaded plugin and API, rather than inferring compatibility from a
plugin version string alone.

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
substitutes to exercise selection, deterministic fallback, missing dependency
behavior, and nonfatal registration. A clean verification build, the selection
regression checks, and GitHub CI are the acceptance checks for this change;
in-game testing is not required. These checks do not claim live rendering was
tested.
