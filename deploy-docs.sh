#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean :library:dokka

# Copy outside files into the docs folder.
sed -e '/full configuration details and documentation here/ { N; d; }' < README.md > docs/index.md
cp readme-header.png docs/

# Deploy to Github pages.
mkdocs gh-deploy

# Clean up.
rm -rf docs/index.md docs/readme-header.png
