#!/usr/bin/env bash
set -e

# Change directory to the script's location (project root)
cd "$(dirname "$0")"

# Ensure bin directory exists
mkdir -p bin

echo "Compiling Digital Resource Manager from Main.java using -sourcepath..."
javac -cp "lib/*" -sourcepath src/main/java -d bin src/main/java/Main.java

echo "Compilation successful. Launching application..."
java -cp "bin:lib/*" Main "$@"
