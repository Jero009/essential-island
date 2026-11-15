# SmartIsland — Local Setup Notes

Setup date: 2026-11-15. Local-only preparation for the "Nothing-styled dynamic island" project.
Nothing has been forked, pushed, published, or released. GPL-3.0 LICENSE untouched.

## Source

- Cloned from: https://github.com/agupta07505/SmartIsland.git (full upstream history kept)
- Upstream commit: `149efd1fde40416b93116ee7c958c2793a5d4459` (merge of PR #117 from dev, 2026-10-19)
- App at this commit: `com.agupta07505.smartisland`, versionName 7.6.0, versionCode 9, minSdk 26, target/compileSdk 36

## Toolchain

- JDK: 21 (`/usr/lib/jvm/java-21-openjdk-amd64`), pinned user-level via `~/.gradle/gradle.properties`
  `org.gradle.java.home`. (README says JDK 17; 21 is within Gradle 9.7.1/AGP 9.1.1 support and is
  what this machine has. System default java 25 is not used.)
- Gradle: 9.7.1 (wrapper), AGP 9.1.1, Kotlin 2.3.21, Hilt 2.60.1
- Android SDK: `~/android-sdk` (platforms android-35 + android-36 installed), declared in
  `local.properties` (`sdk.dir`, gitignored)

## Signing

- Keystore: `keystore/island.jks` — PKCS12, RSA 4096, alias `island`,
  subject `CN=Jernej Peter, O=Jero009, C=SI`, valid 10950 days (~30 years)
- Config: `keystore/keystore.properties` (chmod 600; storeFile/storePassword/keyAlias/keyPassword).
  Single random 24-byte-hex password is used for both store and key — PKCS12 effectively has one
  password, and AGP signing fails with "Given final block not properly padded" when keypass
  differs from storepass.
- Cert SHA-256 (public): `4E:6E:D0:D6:55:68:F7:54:84:C0:33:86:DA:5E:D0:88:9F:39:6E:28:20:2F:06:C5:B0:0D:9C:D7:12:75:7A:A7`
- Public cert: `keystore/island-release.crt`
- Backups: `~/keystore-backups/island/` (dir mode 700) and `docker-vm:~/keystores/island/` (scp OK,
  no Tailscale stall this time — unlike autoplaner)
- Passwords are never stored anywhere else; keystore dir is gitignored (see below)

## Gradle signing wiring (local change, uncommitted)

`app/build.gradle.kts` upstream reads signing config from env vars
(`SIGNING_STORE_FILE` / `SIGNING_STORE_PASSWORD` / `SIGNING_KEY_ALIAS` / `SIGNING_KEY_PASSWORD`).
Adapted: it now loads `keystore/keystore.properties` via `rootProject.file(...)` first, falls back
to the upstream env vars (so upstream CI workflows keep working), and otherwise the release build
stays unsigned without failing. Plus one `import java.util.Properties` at the top of the script
(in Gradle Kotlin DSL the `java` extension accessor shadows the `java` package, so a fully
qualified `java.util.Properties` inline does not compile).

## Build commands that work

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd ~/projects/island
./gradlew assembleDebug          # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # -> app/build/outputs/apk/release/app-release.apk (signed)
./gradlew testDebugUnitTest      # run separately from lint, see quirks
./gradlew lintDebug
~/android-sdk/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

First clean build ~7 min (dependency download); incremental much faster.

## Verification results (2026-11-15)

- assembleDebug: BUILD SUCCESSFUL (6m 41s first run)
- assembleRelease (signed): BUILD SUCCESSFUL; output `app-release.apk`
- testDebugUnitTest: 1774 tests, 0 failures, 0 errors, 0 skipped (174 test classes)
- lintDebug: BUILD SUCCESSFUL (passes under upstream `warningsAsErrors = true`)
- Unsigned fallback: with `keystore/keystore.properties` removed, `packageRelease` still
  succeeds and emits `app-release-unsigned.apk`; properties restored afterwards, signed APK
  rebuilt and re-verified

## Release APK verification (this setup)

- `app-release.apk` SHA-256: `5ac9aca026c73100e3af776770daf6712744a121ccaa5d849f4d5a96b28483ef`
- apksigner: signer cert CN=Jernej Peter, O=Jero009, C=SI — digest matches keystore cert above
- aapt: package com.agupta07505.smartisland, versionCode 9, versionName 7.6.0, minSdk 26, target 36

## Upstream quirks found

1. `gradlew` is committed without the executable bit (chmod +x applied locally; shows as a mode
   change in git).
2. Release build uses R8 minify + resource shrink with `lint { warningsAsErrors = true }` —
   lint strictness is upstream policy.
3. Upstream README says "JDK 17"; Gradle 9.7.1 + AGP 9.1.1 also run on JDK 21 (verified — builds
   and tests pass). JDK 25 (system default) is not supported by this toolchain.
4. Upstream CI signing is env-var based; the local keystore.properties layer was added on top
   without removing the env-var path.
5. Compilation emits a number of upstream deprecation warnings (MediaSession/getParcelableArray,
   Shizuku unsafeCheckOpNoThrow, etc.) — pre-existing, harmless.
6. This box has 7.2 GB RAM; running `testDebugUnitTest lintDebug` in ONE Gradle invocation
   OOM-killed the daemon during lintAnalyzeDebug (daemon: -Xmx3g per upstream gradle.properties).
   Run tests and lint as separate invocations.
7. PKCS12 + AGP: generating the keystore with keyPassword != storePassword fails at packageRelease
   ("Given final block not properly padded"). Use one password for both (what this keystore does).
   keytool silently accepts mismatched passwords at generation time, so this only surfaces at
   signing.

## Git hygiene

- `.gitignore`: upstream already covers `local.properties`, `*.jks`, `keystore.properties`;
  `keystore/` added so the public cert is excluded too. `git check-ignore` confirms all four
  keystore paths + local.properties are ignored.
- Local modifications (uncommitted, intentional): `.gitignore`, `app/build.gradle.kts`,
  `gradlew` (mode), `local.properties` (ignored), `keystore/` (ignored).
