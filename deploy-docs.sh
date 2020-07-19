#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean :library:dokka

# Copy outside files into the docs folder.
# TODO: Add additional documentation on options and configurations.
#sed -e '/full documentation here/ { N; d; }' < README.md > docs/index.md

# TODO
# Deploy to Github pages or vercel
# mkdocs gh-deploy

# Clean up.
rm -rf docs/index.md docs/contributing.md docs/changelog.md docs/logo.svg docs/gifs.md docs/svgs.md docs/videos.md
