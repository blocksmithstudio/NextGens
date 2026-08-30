# NextGens repository instructions

## Project context

- NextGens is a public Minecraft Spigot/Paper plugin. The `master` branch is built with Java 21 and Maven; treat the `java.version` in the checked-out branch's `pom.xml` as authoritative for branch-specific work.
- The build uses Maven Shade. The distributable plugin is the versioned `target/NextGens-*.jar`; never deliver `target/original-NextGens-*.jar`.
- The repository includes optional integrations with several other plugins. Preserve existing hooks and their provided/soft-dependency behavior unless the task explicitly changes them.
- `plugin.yml` declares Folia support. Use the existing MDLib `ExecutorManager` patterns for scheduled or asynchronous work instead of introducing unsafe Bukkit scheduler or blocking main-thread work.

## Working agreements

- Start every task by checking `git status`, the current branch, and the diff against the requested base branch. Preserve all pre-existing changes.
- Treat the base branch selected or named by the user as controlling. Do not silently switch, rebase, or retarget work between `master`, `moises`, or another branch.
- For a new public change that is intended for the normal release line, use a short-lived `codex/<type>-<slug>` branch or Codex worktree based on `master`. Continue branch-specific fixes from the branch the user names.
- Inspect representative nearby code before editing and follow its naming, formatting, command, configuration, and manager/listener patterns.
- Make the narrowest change that satisfies the requested behavior. Do not refactor unrelated code or update unrelated dependencies.
- When behavior is configurable, keep the bundled YAML defaults, config loading/reload behavior, messages, placeholders, and documentation consistent.
- Do not commit, push, open or merge a pull request, tag a release, publish an artifact, or rewrite history unless the user explicitly asks for that action.
- Never force-push.

## Build and verification

- On Windows PowerShell, build with `./mvnw.cmd -B -ntp verify`.
- On Linux or macOS, build with `./mvnw -B -ntp verify`.
- The project currently has no automated test sources. A successful Maven build proves compilation and packaging, not Minecraft, Folia, database, hook, or gameplay behavior.
- After a code or resource change:
  1. Run the Maven verification build.
  2. Confirm exactly one primary versioned JAR exists after excluding `original-*` and `*-shaded.jar` helper files.
  3. Confirm the distributable JAR contains `plugin.yml`.
  4. Run `git diff --check`, inspect the complete diff, and report the final `git status`.
- Report Maven model, repository metadata, and shade overlap warnings separately from build failures. Do not modify dependencies merely to silence an unrelated warning.
- When asked for a test JAR, provide the primary shaded artifact and state its filename, size, and SHA-256 checksum. Do not claim server or gameplay verification unless it was actually performed.

## Review priorities

- Look first for item or generator duplication, data loss, unsafe async Bukkit access, reload inconsistencies, permission bypasses, incompatible hook behavior, and configuration migrations that break existing servers.
- Verify that commands enforce their intended permissions and that player-facing behavior still works for existing configuration files.
- Keep public API and persistent-data changes backward compatible unless a breaking change is explicitly requested and documented.
