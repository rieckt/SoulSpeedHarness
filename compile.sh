#!/bin/bash
# Simple compilation script for SoulSpeedHarness plugin
# Requires: Java 21, Paper API JAR

echo "Kompiliere SoulSpeedHarness Plugin..."

# Find Paper API JAR and all dependencies
PAPER_API=$(find /home/rieckt/Documents/paper-dev-server -name "paper-api*.jar" 2>/dev/null | head -1)
ALL_LIBS=$(find /home/rieckt/Documents/paper-dev-server/libraries -name "*.jar" 2>/dev/null | tr '\n' ':')

if [ -z "$PAPER_API" ]; then
    echo "FEHLER: Paper API JAR nicht gefunden!"
    echo "Bitte installiere Gradle oder lade die Paper API JAR manuell herunter."
    echo "Oder verwende: ./gradlew build (nach Installation von Gradle)"
    exit 1
fi

# Build classpath with all libraries
CLASSPATH="$PAPER_API:$ALL_LIBS"

echo "Verwende Paper API: $PAPER_API"

# Create build directories
mkdir -p build/classes/java/main
mkdir -p build/libs

# Compile Java files
javac -cp "$CLASSPATH" -d build/classes/java/main \
    src/main/java/com/soulspeedharness/*.java

if [ $? -ne 0 ]; then
    echo "Kompilierung fehlgeschlagen!"
    exit 1
fi

# Copy resources
cp -r src/main/resources/* build/classes/java/main/

# Create JAR with manifest
cd build/classes/java/main
jar cfm ../../../libs/SoulSpeedHarness-1.0.0.jar ../../../../META-INF/MANIFEST.MF * 2>/dev/null || jar cf ../../../libs/SoulSpeedHarness-1.0.0.jar *
cd ../../../../

echo "Plugin erfolgreich kompiliert: build/libs/SoulSpeedHarness-1.0.0.jar"

