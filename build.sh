#!/bin/bash

MVND="/home/helly/app/maven-mvnd/bin/mvnd"
if [ ! -f "$MVND" ]; then
    MVND="mvn"
fi

JAVA8="/home/helly/lang/jdk8"
JAVA17="/home/helly/lang/jdk17"
JAVA23="/home/helly/lang/jdk23"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo "Building jdbcmon for multiple JDK versions"
echo "========================================"

echo ""
echo "[1/3] Building JDK 8 version..."
export JAVA_HOME=$JAVA8
$MVND clean install -Pjdk8 -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: JDK 8 build failed"
    exit 1
fi

echo ""
echo "[2/3] Building JDK 17 version..."
export JAVA_HOME=$JAVA17
$MVND clean install -Pjdk17 -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: JDK 17 build failed"
    exit 1
fi

echo ""
echo "[3/3] Building JDK 23 version..."
export JAVA_HOME=$JAVA23
$MVND clean install -Pjdk23 -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: JDK 23 build failed"
    exit 1
fi

echo ""
echo "========================================"
echo "All builds completed successfully!"
echo "========================================"
echo ""
echo "Artifacts:"
ls -la jdbcmon-core/target/*.jar 2>/dev/null || echo "No jars found"