#!/bin/bash

MVND="/home/helly/app/maven-mvnd/bin/mvnd"
if [ ! -f "$MVND" ]; then
    MVND="mvn"
fi

JAVA8="/home/helly/lang/jdk8"
JAVA17="/home/helly/lang/jdk17"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

rm -rf jdbcmon-core/target/*.jar 2>/dev/null

echo "========================================"
echo "Building jdbcmon for multiple JDK versions"
echo "========================================"

echo ""
echo "[1/2] Building JDK 8 version (core only)..."
export JAVA_HOME=$JAVA8
$MVND clean package -Pjdk8 -pl jdbcmon-core -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: JDK 8 build failed"
    exit 1
fi

echo ""
echo "[2/2] Building JDK 17 version..."
export JAVA_HOME=$JAVA17
$MVND package -Pjdk17 -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: JDK 17 build failed"
    exit 1
fi

echo ""
echo "========================================"
echo "All builds completed successfully!"
echo "========================================"
echo ""
echo "Artifacts:"
ls -la jdbcmon-core/target/*.jar 2>/dev/null || echo "No jars found"