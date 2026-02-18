#!/bin/bash
# Installs an idverse-api JAR into local-repo/.
# Usage: ./update-lib.sh [path/to/idverse-api-1.0-SNAPSHOT.jar]
# Defaults to ../idverse-api/target/idverse-api-1.0-SNAPSHOT.jar

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="${1:-$SCRIPT_DIR/../idverse-api/target/idverse-api-1.0-SNAPSHOT.jar}"

if [ ! -f "$JAR" ]; then
    echo "ERROR: JAR not found at $JAR"
    echo "Usage: ./update-lib.sh [path/to/idverse-api-1.0-SNAPSHOT.jar]"
    exit 1
fi

echo "==> Installing $JAR into local-repo/..."
mvn install:install-file \
    -Dfile="$JAR" \
    -DgroupId=org.itnaf \
    -DartifactId=idverse-api \
    -Dversion=1.0-SNAPSHOT \
    -Dpackaging=jar \
    -DlocalRepositoryPath="$SCRIPT_DIR/local-repo"

echo ""
echo "Done. To commit the updated library:"
echo "  git add local-repo/"
echo "  git commit -m 'Update idverse-api library JAR'"
