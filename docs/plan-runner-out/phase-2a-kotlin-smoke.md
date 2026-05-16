# Phase 2a Kotlin Conversion Smoke Test

**Date:** 2026-05-16 (current session)

## Results on 1.21.4

| Task | Result |
|---|---|
| `:1.21.4:build` | FAIL |
| `:1.21.4:test` | FAIL (0/7 tests — did not reach test phase) |
| `:1.21.4:runGametest` | FAIL (did not reach gametest phase) |

## Output snippets

### :1.21.4:build
```
Running Stonecutter 0.6

FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring project ':1.21.4'.
> Could not resolve all artifacts for configuration ':1.21.4:classpath'.
   > Could not resolve net.fabricmc:fabric-loom:1.10-SNAPSHOT.
     Required by:
         project :1.21.4 > fabric-loom:fabric-loom.gradle.plugin:1.10-SNAPSHOT:20250323.164030-5
      > Plugin net.fabricmc:fabric-loom:1.10-SNAPSHOT:20250323.164030-5 requires at least Gradle 8.12. This build uses Gradle 8.10.

* Try:
> Upgrade to at least Gradle 8.12. See the instructions at https://docs.gradle.org/8.10/userguide/upgrading_version_8.html#sub:updating-gradle.
> Downgrade plugin net.fabricmc:fabric-loom:1.10-SNAPSHOT:20250323.164030-5 to an older version compatible with Gradle 8.10.
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 2s
```

### :1.21.4:test
```
Running Stonecutter 0.6

FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring project ':1.21.4'.
> Could not resolve all artifacts for configuration ':1.21.4:classpath'.
   > Could not resolve net.fabricmc:fabric-loom:1.10-SNAPSHOT.
     Required by:
         project :1.21.4 > fabric-loom:fabric-loom.gradle.plugin:1.10-SNAPSHOT:20250323.164030-5
      > Plugin net.fabricmc:fabric-loom:1.10-SNAPSHOT:20250323.164030-5 requires at least Gradle 8.12. This build uses Gradle 8.10.

* Try:
> Upgrade to at least Gradle 8.12. See the instructions at https://docs.gradle.org/8.10/userguide/upgrading_version_8.html#sub:updating-gradle.
> Downgrade plugin net.fabricmc:fabric-loom:1.10-SNAPSHOT:20250323.164030-5 to an older version compatible with Gradle 8.10.
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 767ms
```

### :1.21.4:runGametest
```
Running Stonecutter 0.6

FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring project ':1.21.4'.
> Could not resolve all artifacts for configuration ':1.21.4:classpath'.
   > Could not resolve net.fabricmc:fabric-loom:1.10-SNAPSHOT:20250323.164030-5 requires at least Gradle 8.12. This build uses Gradle 8.10.

* Try:
> Upgrade to at least Gradle 8.12. See the instructions at https://docs.gradle.org/8.10/userguide/upgrading_version_8.html#sub:updating-gradle.
> Downgrade plugin net.fabricmc:fabric-loom:1.10-SNAPSHOT:20250323.164030-5 to an older version compatible with Gradle 8.10.
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 10s
```

## Notes

All three tasks failed at the Gradle configuration phase — no Kotlin source compilation or tests were attempted. The root cause is a version mismatch: `gradle/wrapper/gradle-wrapper.properties` pins the project to Gradle 8.10, but `fabric-loom:1.10-SNAPSHOT` (snapshot dated 2025-03-23) requires at least Gradle 8.12. The fix is to update `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` to reference `gradle-8.12-bin.zip` (or newer), which is outside this task's owned files. No code-correctness issues with the Kotlin conversion itself could be verified until the toolchain version constraint is resolved.
