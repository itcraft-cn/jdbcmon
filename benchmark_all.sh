#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo "Running all benchmarks (JDK 8, 17, 23)"
echo "========================================"

echo ""
echo "========== JDK 8 =========="
./benchmark8.sh

echo ""
echo "========== JDK 17 =========="
./benchmark17.sh

echo ""
echo "========== JDK 23 =========="
./benchmark23.sh

echo ""
echo "========================================"
echo "All benchmarks completed"
echo "========================================"