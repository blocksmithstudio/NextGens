# Change workflow

Follow the owner's current instructions, machine shared memory rules and reviewed Blocksmith Studio rules. Continue in the existing checkout and current branch. Create or switch branches or worktrees only when the owner explicitly requests it. Never use `codex/` as a branch prefix.

## Normal change flow

1. Read applicable instructions and shared project memory; check Git status and preserve existing edits.
2. Implement the requested change in the current checkout and branch.
3. Run the production Maven verification build and the preserved external regression suite against this checkout.
4. Check the primary shaded `target/NextGens-*.jar`, its metadata and checksum. Use a separate private test server for relevant runtime checks.
5. Update `CHANGELOG.md` with concise customer-facing changes and save important verification results and limits in private shared memory.
6. Report the result and remaining verification limits. Commit, push, open a pull request, merge, tag or publish only when explicitly requested.

## Prompt template

```text
Read AGENTS.md and the applicable machine shared memory rules. Continue in this
checkout on the current branch and preserve existing edits. Do not create or
switch branches or worktrees unless I explicitly request it. Never use codex/
as a branch prefix.

Requested behavior:
<describe the requested behavior and configuration contract>

Implement the change, run the production build and external regression suite,
update the changelog, and report relevant verification and limits. Do not commit,
push, open a pull request, merge, tag or release unless explicitly requested.
```

## Getting the test JAR

- Use the primary shaded JAR from `target/`, excluding `original-*` helper files.
- After an authorized push, the **Build plugin JAR** workflow uploads a single artifact named `NextGens-test-<run number>-<commit>` containing `NextGens.jar`.
- Record the tested source revision and JAR checksum alongside runtime verification.
