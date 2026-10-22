#!/bin/sh
# Run after ./gradlew build --offline from the repository root.
set -eu
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
cat > "$OUT/classpath.gradle" <<'GRADLE'
gradle.projectsEvaluated {
    rootProject.tasks.register('printConfigCheckClasspath') {
        doLast { println rootProject.sourceSets.main.runtimeClasspath.asPath }
    }
}
GRADLE
CP=$(./gradlew --offline --quiet -I "$OUT/classpath.gradle" printConfigCheckClasspath | tail -1)
javac -cp "$CP" -d "$OUT" checks/config/ConfigRegistrationCheck.java
java -cp "$OUT:$CP" com.simibubi.create.infrastructure.config.ConfigRegistrationCheck
