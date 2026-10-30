# NextGens repository instructions

## Project context

- NextGens is a public Minecraft Spigot/Paper plugin. Build with JDK 25 and Maven. The `java.version` property controls emitted bytecode; keep it at 21 so older supported server remappers can read the plugin and its bundled libraries.
- The build uses Maven Shade. The distributable plugin is the versioned `target/NextGens-*.jar`; never deliver `target/original-NextGens-*.jar`.
- The repository includes optional integrations with several other plugins. Preserve existing hooks and their provided/soft-dependency behavior unless the task explicitly changes them.
- `plugin.yml` declares Folia support. Use the existing MDLib `ExecutorManager` patterns for scheduled or asynchronous work instead of introducing unsafe Bukkit scheduler or blocking main-thread work.

## Working agreements

- Start every task by checking `git status`, the current branch, and the diff against the requested base branch. Preserve all pre-existing changes.
- Treat the base branch selected or named by the user as controlling. Do not silently switch, rebase, or retarget work between `master`, `moises`, or another branch.
- Work in the existing checkout and current branch. Create or switch branches or worktrees only when the owner explicitly requests it. Never use `codex/` as a branch prefix.
- Follow the owner's current instructions, machine shared memory rules and reviewed Blocksmith Studio rules. Repository workflow examples do not override them.
- Inspect representative nearby code before editing and follow its naming, formatting, command, configuration, and manager/listener patterns.
- Make the narrowest change that satisfies the requested behavior. Do not refactor unrelated code or update unrelated dependencies.
- When behavior is configurable, keep the bundled YAML defaults, config loading/reload behavior, messages, placeholders, and documentation consistent.
- Do not commit, push, open or merge a pull request, tag a release, publish an artifact, or rewrite history unless the user explicitly asks for that action.
- Commit messages must follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/): `<type>[optional scope]: <description>` (for example, `fix(generators): prevent duplicate refunds`). Never use `codex/` as a commit-message prefix; branch names and commit messages are separate. This convention does not authorize committing or pushing.
- Never force-push.

## Build and verification

- On Windows PowerShell, build with `./mvnw.cmd -B -ntp verify`.
- On Linux or macOS, build with `./mvnw -B -ntp verify`.
- Run the preserved regression suite from the owner-designated private external verification workspace against this checkout. Keep all test sources, runners, fixtures, reports, dependencies and test configuration outside the product. CI builds and inspects the plugin artifact; private regressions run separately. These checks do not replace live Minecraft, Folia or third-party plugin integration testing.
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

## Compatibility and change records

- Preserve the Minecraft 1.21 minimum API while supporting newer releases. `api-version` is the minimum server API, not the compile target.
- Check older server bytecode readers separately from the server JVM; Java 25 cannot update a bundled ASM reader.
- Reuse server-provided JDBC drivers; do not bundle SQLite or MySQL drivers.
- Update `CHANGELOG.md` for product changes and record actual compatibility evidence in the private verification workspace.
