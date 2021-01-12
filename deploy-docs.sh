#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean :library:dokka :lifecycle:dokka

# Copy outside files into the docs folder.
sed -e '/full configuration details and documentation here/ { N; d; }' < README.md > docs/index.md
cp readme-header.png docs/
cp lifecycle/README.md docs/lifecycle.md

# Deploy to Github pages.
python3 -m mkdocs gh-deploy --force --verbose

# Clean up.
rm -rf docs/index.md docs/lifecycle.md docs/readme-header.png
