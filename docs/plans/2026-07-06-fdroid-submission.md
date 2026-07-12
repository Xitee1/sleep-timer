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
    binary: https://github.com/Xitee1/sleep-timer/releases/download/v%v/SleepTimer-v%v.apk

AllowedAPKSigningKeys: 87e5fe65c58d5b8406d239d68e7a9d6d7f40245ee6c3a1f458e67094b0a67fb0

AutoUpdateMode: Version
UpdateCheckMode: Tags ^v\d+\.\d+\.\d+$
UpdateCheckData: app/version.properties|versionCode=(\d+)|.|versionName=(.+)
CurrentVersion: 1.1.1
CurrentVersionCode: 101010
```

This is the **reproducible-builds (your-key)** form: `binary:` points F-Droid at your
signed GitHub release APK as the reference, and `AllowedAPKSigningKeys` is the SHA-256 of
your release signing certificate (`CN=Xitee`, extracted from the v1.1.0 release APK — valid
as long as v1.1.1 is signed with the same keystore). F-Droid rebuilds from source, confirms
its build is byte-identical to your APK, and ships your APK's signature. **Only submit this
form if the probe below passes for v1.1.1.** If it does not, drop `binary:` and
`AllowedAPKSigningKeys:` and F-Droid signs with its own key (a one-way door — you cannot
switch to your signature later).

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

## Reproducible-builds probe — preliminary result (already run against v1.1.0)

Reproducible builds are a one-way door: if the F-Droid build is bit-identical to the
signed GitHub release APK, F-Droid ships *your* signature (users cross-update between
GitHub and F-Droid); if not enabled at inclusion time, F-Droid signs with its own key and
you cannot switch later.

**A preliminary probe was run on 2026-07-06 against the existing v1.1.0 release** (build the
`v1.1.0` tag from a clean clone, unsigned, and compare its zip entries by CRC against
`SleepTimer-v1.1.0.apk`). Result: **121 of 122 entries were already byte-identical** —
including all DEX, resources, and `resources.arsc`. The only mismatch was
`libdatastore_shared_counter.so` (the AndroidX DataStore native lib, 4 ABIs): GitHub Actions
**strips** it (host-dependent, via the runner's NDK), while a plain build leaves it as the
pristine bytes shipped in the `datastore-core` AAR. The unstripped bytes are fixed by the
dependency version, so they are identical on every machine.

**Fix applied in this repo** (`app/build.gradle.kts`): a `packaging { jniLibs {
keepDebugSymbols += "**/*.so" } }` block, which tells AGP not to strip native libs, so every
environment (GitHub Actions, F-Droid, local) packages the pristine AAR bytes. Verified: with
the block, the four `.so` CRCs equal the pristine AAR CRCs. This **must ship in the v1.1.1
tag** so the reference GitHub APK is also unstripped — with it in place, all 122 entries
match and the build is reproducible.

**Confirm on v1.1.1 before submitting** (definitive check, once v1.1.1 is tagged & released):

```sh
# at the v1.1.1 tag, JDK 17, ANDROID_HOME set:
./gradlew :app:assembleRelease -PdisableSigning
export PATH="$ANDROID_HOME/build-tools/<ver>:$PATH"   # for apksigner
pip install apksigcopier
apksigcopier compare SleepTimer-v1.1.1.apk \
  --unsigned app/build/outputs/apk/release/app-release-unsigned.apk
# apksigcopier 1.1.1 may error parsing the signing block; the robust fallback is a
# per-entry CRC diff of `unzip -v` on both APKs (ignoring META-INF signature files).
```

The definitive check is F-Droid's own CI on the submission MR (or a local
`fdroid build` in a fdroiddata checkout), which builds in F-Droid's controlled environment.
Given the preliminary result (only the now-fixed `.so` differed), it is very likely to pass.
If it unexpectedly fails, drop `binary:` + `AllowedAPKSigningKeys:` from the recipe and write
"No, I don't want this." in the MR reproducibility question.

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
