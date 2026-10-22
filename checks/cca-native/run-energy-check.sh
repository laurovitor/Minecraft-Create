#!/bin/sh
# Compiles and runs EnergyTransferCheck against the built classes. Run from the repository root after
# ./gradlew build --offline. Libraries come from the local Gradle cache (offline).
set -e
lib() { find ~/.gradle/caches/modules-2 -name "$1" ! -name '*sources*' | sort | tail -1; }
CP="build/classes/java/main:build/moddev/artifacts/minecraft-patched-26.3.0.89.jar"
CP="$CP:$(lib 'neoforge-26.3.0.89-universal.jar'):$(lib 'guava-*.jar'):$(lib 'datafixerupper-*.jar'):$(lib 'logging-1*.jar'):$(lib 'slf4j-api-*.jar'):$(lib 'fastutil-*.jar')"
OUT=$(mktemp -d)
javac -d "$OUT" -cp "$CP" checks/cca-native/EnergyTransferCheck.java
java -ea -cp "$OUT:$CP" com.simibubi.create.content.electricity.energy.EnergyTransferCheck
rm -rf "$OUT"
