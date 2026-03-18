#!/bin/bash

JAVA_HOME="/home/helly/lang/jdk17"
MVN="mvn"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME=$JAVA_HOME

echo "========================================"
echo "Running benchmarks with JDK 17"
echo "========================================"

echo ""
echo "[1/2] Building all modules..."
$MVN clean package -Pjdk17 -pl jdbcmon-core,jdbcmon-test -am -DskipTests -q 2>/dev/null

if [ $? -ne 0 ]; then
    echo "ERROR: Build failed"
    exit 1
fi

cd jdbcmon-test
$MVN dependency:copy-dependencies -DoutputDirectory=target/dependency -q 2>/dev/null

echo ""
echo "[2/2] Running benchmarks..."
echo ""

CP="target/test-classes:../jdbcmon-core/target/jdbcmon-core-1.0.0-SNAPSHOT-jdk17.jar:target/dependency/*"
$JAVA_HOME/bin/java -cp "$CP" cn.itcraft.jdbcmon.benchmark.BenchmarkRunner 2>&1 | tail -n 40