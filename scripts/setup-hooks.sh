#!/bin/bash
# Installerer git-hooks for prosjektet.
# Kjør én gang etter kloning: ./scripts/setup-hooks.sh

set -e

HOOKS_DIR="$(git rev-parse --git-dir)/hooks"

ln -sf "$(pwd)/scripts/pre-commit" "$HOOKS_DIR/pre-commit"
chmod +x scripts/pre-commit

echo "✅ pre-commit hook installert."
