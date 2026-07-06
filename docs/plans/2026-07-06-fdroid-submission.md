# F-Droid submission

Reference notes for publishing SleepTimer (`dev.xitee.sleeptimer`) on F-Droid.
Verified against live F-Droid docs and precedent recipes on 2026-07-06.

## What lives where

- **This repo** carries everything F-Droid reads from source: `LICENSE` (GPL-3.0),
  `fastlane/metadata/android/{en-US,de-DE}/` (title, descriptions, icon, screenshots,
  per-`versionCode` changelogs), `app/version.properties` (version literals for the
  update checker), and the `-PdisableSigning` build path in `app/build.gradle.kts`.
- **The fdroiddata fork** carries the build recipe below. It is **not** committed to
  this repo — F-Droid lint rejects a recipe that duplicates the fastlane
  `Summary`/`Description`, and the recipe references paths inside this repo by URL.

## The recipe — `metadata/dev.xitee.sleeptimer.yml`

Add this file in a fork of https://gitlab.com/fdroid/fdroiddata (not here):

```yaml
Categories:
  - Timer
  - Multimedia
License: GPL-3.0-or-later
AuthorName: Xitee
SourceCode: https://github.com/Xitee1/sleep-timer
IssueTracker: https://github.com/Xitee1/sleep-timer/issues
Changelog: https://github.com/Xitee1/sleep-timer/releases
Donate: https://ko-fi.com/xitee165479

AutoName: SleepTimer

RepoType: git
Repo: https://github.com/Xitee1/sleep-timer.git

Builds:
  - versionName: 1.1.1
    versionCode: 101010
    commit: v1.1.1
    subdir: app
    gradle:
      - yes
    gradleprops:
      - disableSigning=true

AutoUpdateMode: Version
UpdateCheckMode: Tags ^v\d+\.\d+\.\d+$
UpdateCheckData: app/version.properties|versionCode=(\d+)|.|versionName=(.+)
CurrentVersion: 1.1.1
CurrentVersionCode: 101010
```

Notes:
- No `Summary:`/`Description:` — the fastlane metadata in this repo supplies them.
- No `AntiFeatures:` — Shizuku is an *optional* Apache-2.0 dependency from Maven Central
  (no `NonFreeDep`; precedent: the Hail recipe, which also has optional Shizuku), and the
  app has no `INTERNET` permission and no trackers.
- `UpdateCheckData` reads `app/version.properties` because the Gradle build derives
  `versionCode`/`versionName` dynamically from the git tag (axion-release), which the
  static update checker cannot parse. The `.` reuses the same file for the versionName
  regex. The `Tags` regex excludes the old `v0.0.1-beta` tag.
- After the first release, the checkupdates bot appends new build entries automatically
  (copying `gradleprops`), so future releases need no recipe edits.

## Reproducible-builds probe (decide before opening the MR)

Reproducible builds are a one-way door: if the F-Droid build is bit-identical to the
signed GitHub release APK, F-Droid publishes *your* signature (users can cross-update
between GitHub and F-Droid). If not enabled at inclusion time, F-Droid signs with its own
key and you cannot switch later. Test first:

```sh
# at the v1.1.1 tag, with JDK 17:
./gradlew :app:assembleRelease -PdisableSigning
pip install apksigcopier
apksigcopier compare --unsigned \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  SleepTimer-v1.1.1.apk        # the asset from the GitHub v1.1.1 release
```

If it reports the APKs match, add to the build entry:

```yaml
    binary: https://github.com/Xitee1/sleep-timer/releases/download/v%v/SleepTimer-v%v.apk
```

and top-level (get the hash from `apksigner verify --print-certs SleepTimer-v1.1.1.apk`):

```yaml
AllowedAPKSigningKeys: <sha256 of the signing cert>
```

If it does not match (R8/JDK nondeterminism is the usual cause), skip these fields and
write "No, I don't want this." in the MR reproducibility question.

## Local validation

```sh
python3 -m venv ~/venvs/fdroid && ~/venvs/fdroid/bin/pip install fdroidserver
git clone https://gitlab.com/<your-gitlab-user>/fdroiddata.git && cd fdroiddata
git switch -c dev.xitee.sleeptimer          # branch name = applicationId (convention)
# add metadata/dev.xitee.sleeptimer.yml, then:
fdroid readmeta && fdroid rewritemeta dev.xitee.sleeptimer
fdroid lint dev.xitee.sleeptimer            # must be clean
fdroid checkupdates dev.xitee.sleeptimer    # only after v1.1.1 is tagged
fdroid build -v -l dev.xitee.sleeptimer     # optional locally (needs ANDROID_HOME); fork CI runs it too
```

## Submission MR

1. Fork fdroiddata (public fork, unprotected branch `dev.xitee.sleeptimer`), push.
2. Wait for the fork's GitLab pipelines to go green (they run lint + a full `fdroid build`).
3. Open an MR against fdroiddata `master` with the **App inclusion** template, title
   `New app: SleepTimer`, squash enabled.
4. In the description: state you are the upstream author (RFP issue not needed), that there
   are no anti-features (optional FLOSS Shizuku, no `INTERNET` permission, no trackers), and
   your reproducible-builds decision.

Review typically takes days to a few weeks; after merge the app appears on f-droid.org
within about a week.

## After publication

- Replace the `_Submission pending._` line in `README.md` with the F-Droid badge linking
  `https://f-droid.org/packages/dev.xitee.sleeptimer`.
- Optional: add `fastlane/metadata/android/en-US/images/featureGraphic.png` (1024×500) for
  a nicer "Latest" listing card.
