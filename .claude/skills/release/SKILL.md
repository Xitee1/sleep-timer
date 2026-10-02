---
name: release
description: Cut a new SleepTimer release end to end — version bump, F-Droid changelogs, release PR, git tag and GitHub release. Only run when the user explicitly asks to create, cut or publish a release.
argument-hint: "[X.Y.Z | major | minor | patch]"
disable-model-invocation: true
---

# Cut a SleepTimer release

Requested version argument: `$ARGUMENTS` (empty means: propose one in step 2 and ask).

Follow the steps in order. Steps 1–5 are preparation and run without further questions once the version is settled. Step 6 (merge + tag push) publishes the release and triggers the F-Droid pickup, so it needs the user's explicit go at the review checkpoint in step 5.

## Current state (collected when the skill was invoked)

- Fetch: !`git fetch -q --tags --prune origin 2>&1 | tail -1; echo done`
- Branch: !`git status -sb 2>/dev/null | head -1 || true`
- Latest tag on origin/main: !`git describe --tags --abbrev=0 --match 'v*' origin/main 2>/dev/null || echo none`
- version.properties: !`grep -E '^version(Name|Code)=' app/version.properties | tr '\n' ' ' || true`
- Existing changelogs: !`ls fastlane/metadata/android/en-US/changelogs/ 2>/dev/null | tr '\n' ' ' || true`

Merged PRs on origin/main since the latest tag (merge subject, then PR title):

!`T=$(git describe --tags --abbrev=0 --match 'v*' origin/main 2>/dev/null); git log --merges --format='%s%n    %b' "$T"..origin/main 2>/dev/null || true`

Non-merge commits on origin/main since the latest tag:

!`T=$(git describe --tags --abbrev=0 --match 'v*' origin/main 2>/dev/null); git log --oneline --no-merges "$T"..origin/main 2>/dev/null || true`

## 1. Preconditions

Stop and tell the user if any of these fail — do not work around them.

1. `gh auth status` succeeds (needed for the PR, the workflow watch and the release edit).
2. The working tree is clean. Switch to `main` and fast-forward it: `git checkout main && git pull --ff-only`. Unmerged feature branches are not part of a release; the user must merge them first.
3. There is at least one commit on `origin/main` since the latest tag.
4. The release flow is PR-based: every previous tag sits on a `Merge pull request` commit on `main`. Do not commit directly to `main`.

## 2. Determine the version

- `X.Y.Z` or `vX.Y.Z` given → use it. `major` / `minor` / `patch` given → bump the latest tag accordingly.
- Nothing given → propose one from the commits above: any `feat` commit or PR → minor bump, otherwise patch bump. Ask the user to confirm the version before writing anything.
- The version must be plain SemVer (axion-release fails the build on anything else, e.g. `1.3.0.1`), greater than the latest tag, and `vX.Y.Z` must not exist yet (`git tag -l vX.Y.Z` prints nothing).
- `versionCode = major*100000 + minor*1000 + patch*10` (e.g. `1.3.0` → `103000`). The release workflow recomputes this from the tag and compares.

## 3. Prepare the release commit on a branch

1. `git checkout -b chore/release-vX.Y.Z`
2. Bump `app/version.properties` (only the two value lines, keep the comments):
   `sed -i 's/^versionName=.*/versionName=X.Y.Z/; s/^versionCode=.*/versionCode=CODE/' app/version.properties`
3. Write the changelogs `fastlane/metadata/android/en-US/changelogs/CODE.txt` and `fastlane/metadata/android/de-DE/changelogs/CODE.txt` — see the rules below.
4. Run the same check the release workflow runs. This mirrors the `Verify release metadata matches tag` step in `.github/workflows/release.yml`; if that step changes, update this snippet:

   ```bash
   VER=X.Y.Z
   IFS=. read -r MA MI PA <<< "${VER%%-*}"
   CODE=$((MA * 100000 + MI * 1000 + PA * 10))
   grep -qx "versionName=$VER"  app/version.properties && echo "versionName OK"
   grep -qx "versionCode=$CODE" app/version.properties && echo "versionCode OK"
   for LOCALE in en-US de-DE; do
     FILE="fastlane/metadata/android/$LOCALE/changelogs/$CODE.txt"
     test -f "$FILE" || { echo "Missing F-Droid changelog: $FILE"; exit 1; }
     test "$(wc -c < "$FILE")" -le 500 || { echo "Changelog over 500 chars: $FILE"; exit 1; }
     echo "$FILE: $(wc -c < "$FILE") bytes OK"
   done
   ```

### Changelog rules

- One bullet per user-visible change, derived from the merged PRs and commits since the last tag. Written for end users, not developers: say what changed for them, not which class moved. Add "(requires Shizuku)" / "(Shizuku benötigt)" to features that only work with Shizuku.
- Same bullets in both files, same order, same facts. The German file is a translation, not a rewrite.
- Fixed prefixes — use these words and no synonyms:

  | en-US          | de-DE          | use for                                                                                                   |
  |----------------|----------------|-----------------------------------------------------------------------------------------------------------|
  | `* New: `      | `* Neu: `      | new feature or setting                                                                                    |
  | `* Fixed: `    | `* Behoben: `  | bug fix                                                                                                   |
  | `* Polish: `   | `* Politur: `  | visual or UX refinement without new function                                                              |
  | `* Internal: ` | `* Intern: `   | build or metadata changes; end with "No user-facing changes." / "Keine sichtbaren Änderungen." when that is all |

- The limit is 500 **bytes** (`wc -c`), not characters — German umlauts and „“ count as two or three bytes. F-Droid shows only the current version's file.
- Read the previous files in the same directory first and match their tone.

## 4. Commit, push, open the PR

- Commit message: `chore(release): bump to X.Y.Z and add F-Droid changelogs (CODE)` plus a short body naming the included changes.
- `git push -u origin chore/release-vX.Y.Z`
- `gh pr create --base main --title "<same as the commit subject>"` with a body that lists the included PRs (`#nn — summary`), the three changed files, and that the local metadata check passes.
- Wait for CI: `gh pr checks <n> --watch`. The `Build & test` check must pass before step 6.

## 5. Review checkpoint — STOP here and wait for the user's go

Show the user in the reply:

- the PR link,
- both changelog texts verbatim in code blocks,
- the proposed GitHub release title in the house pattern `[vX.Y.Z] <three to five words summarising the release>` (see `gh release list` for previous titles),
- the remaining steps 6–8 in one line each.

Do not merge, tag or push a tag until the user answers with an explicit go. If they ask for wording changes, amend the PR and show the result again.

## 6. Merge, tag, push the tag

```bash
gh pr merge <n> --merge --delete-branch     # merge commit, never squash or rebase — tags sit on merge commits
git checkout main && git pull --ff-only
git tag -a vX.Y.Z -m "vX.Y.Z"               # on the merge commit
git push origin vX.Y.Z                      # triggers .github/workflows/release.yml
```

## 7. Watch the release workflow

```bash
gh run list --workflow release.yml --limit 1          # status "waiting" for the first seconds is normal: the job uses the `release` environment
gh run watch <run-id> --exit-status --interval 20     # about 3 minutes; use a Bash timeout of 600000 ms
```

The job verifies the metadata, builds the signed APK, uploads `SleepTimer-vX.Y.Z.apk` and creates the GitHub release with auto-generated notes. If it fails: fix on `main` through a PR, then `git tag -d vX.Y.Z && git push origin :refs/tags/vX.Y.Z`, re-tag the new merge commit and push again. A hotfix after a successful release is the next patch tag, never a fourth version component.

## 8. Finalize the GitHub release

The generated notes contain only a `## What's Changed` list and a `**Full Changelog**` link, and the title is the bare tag. Bring both into the house format:

```bash
gh release view vX.Y.Z --json name,body,assets        # read the generated lines first
gh release edit vX.Y.Z --title "[vX.Y.Z] <summary>" --notes-file - <<'EOF'
## Features
* <generated line of each feature PR: "<PR title> by @user in <url>">

## Fixes
* <generated line of each fix PR>

## What's Changed
* <generated line of the release-prep PR and any docs or chore PRs>

**Full Changelog**: https://github.com/Xitee1/sleep-timer/compare/vPREV...vX.Y.Z
EOF
gh release list --limit 2                              # confirms the "Latest" marker; `gh release view` has no isLatest field
```

Omit a section that would be empty. Keep the generated `by @user in <url>` lines as they are so GitHub keeps linking the PRs; a short clarifying suffix such as "(requires Shizuku)" is fine.

## 9. Report

Tell the user: release URL, tag commit, APK asset name, that F-Droid picks the version up automatically from `app/version.properties` on the tag (nothing to do), and whether CI and the workflow passed. Report failures with their output.

## Gotchas

- `app/version.properties` is not read by Gradle; axion-release derives the real version from the tag. The file exists only so F-Droid `checkupdates` and the workflow's metadata check can read it — it still must match the tag exactly.
- No `.github/release.yml` exists, so GitHub's generated notes are not categorised; the Features/Fixes split in step 8 is manual.
- PRs in this repo carry no labels; categorise by the `feat` / `fix` / `chore` / `docs` prefix of the PR title or commit subject.
