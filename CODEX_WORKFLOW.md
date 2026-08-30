# Codex change workflow

This repository uses short-lived branches, reproducible Maven builds, downloadable test JARs, and a human-controlled merge into the target branch.

## Normal change flow

1. Start a new Codex task for NextGens and choose **Worktree**.
2. Select the correct starting branch:
   - Use `master` for a normal public release change.
   - Use `moises` or another named branch only for a change intended specifically for that line.
3. Ask Codex to create or prepare a short-lived branch such as `codex/fix-generator-duplication` or `codex/feat-custom-message`.
4. Let Codex implement the change and run `./mvnw.cmd -B -ntp verify`.
5. Test the primary `target/NextGens-*.jar` on a separate Minecraft test server. Do not use `original-NextGens-*.jar`.
6. If testing fails, give the exact error, log, configuration, and reproduction steps to the same Codex task so it can update the same branch.
7. When testing passes, ask Codex to run `/review` against the target branch and show the final diff, build result, JAR checksum, and proposed commit message.
8. Explicitly approve any commit, push, or pull-request action. Merge the pull request only after the checks pass and you are satisfied with the test JAR.

## Prompt template

```text
Start from the branch I selected and work in an isolated worktree. Prepare a
short-lived codex/<type>-<slug> branch for this change.

Requested behavior:
<describe the exact player/admin behavior and configuration contract>

Inspect the existing implementation and nearby coding style first. Preserve
unrelated behavior and existing configuration compatibility. Run the full
Maven verification build, identify the primary shaded NextGens JAR, and report
its path, size, and SHA-256 checksum. Show me the final diff and anything that
still requires Minecraft server testing. Do not commit, push, open a pull
request, merge, tag, or release unless I explicitly approve it.
```

## Getting the test JAR

- Before pushing, Codex can provide the locally built primary JAR from `target/`.
- After a branch is pushed, the **Build test JAR** GitHub Actions run uploads a single artifact named `NextGens-test-<run number>-<commit>`. Download `NextGens.jar` from that run and test that exact file.
- Record the tested commit and JAR checksum in the pull request so the merged code can be tied back to the binary that passed testing.

## First workflow test

After these setup files are committed, start a Worktree task from the intended base branch and use this safe prompt:

```text
Read AGENTS.md and inspect NextGens without changing product behavior. Run the
full verification build, identify the distributable JAR, verify that plugin.yml
is inside it, and report the branch, build result, artifact size, and SHA-256.
Do not commit or push anything.
```

That test validates repository instructions, worktree isolation, Maven Wrapper operation, shaded-JAR selection, and the human approval boundary.
