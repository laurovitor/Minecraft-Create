#!/bin/sh
# Compiles and runs EnergyTransferCheck and PortableEnergyCheck against the built classes.
# Run from the repository root after ./gradlew build --offline (classpath taken from Gradle, offline).
set -eu
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
cat > "$OUT/classpath.gradle" <<'GRADLE'
gradle.projectsEvaluated {
    rootProject.tasks.register('printEnergyCheckClasspath') {
        doLast { println rootProject.sourceSets.main.runtimeClasspath.asPath }
    }
}
GRADLE
CP=$(./gradlew --offline --quiet -I "$OUT/classpath.gradle" printEnergyCheckClasspath | tail -1)
javac -d "$OUT" -cp "$CP" checks/cca-native/EnergyTransferCheck.java checks/cca-native/PortableEnergyCheck.java
java -ea -cp "$OUT:$CP" com.simibubi.create.content.electricity.energy.EnergyTransferCheck
java -ea -cp "$OUT:$CP" com.simibubi.create.content.electricity.portable.PortableEnergyCheck
