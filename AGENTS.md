# Repository Instructions

## Gradle And JDK

When running Gradle commands in this repository, prefer Android Studio's bundled JBR instead of the system JDK.

Use this prefix for verification commands:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

Apply the same `JAVA_HOME` prefix to other `./gradlew` commands unless the user explicitly asks for a different JDK. This avoids Android Gradle `jlink` failures seen with the system Temurin JDK 26.

Do not commit machine-local JDK configuration such as `org.gradle.java.home` in repository files. If a local persistent override is needed, put it in the user's global `~/.gradle/gradle.properties`.
