#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean :library:dokka

# TODO
# Deploy to Github pages or vercel
mkdocs gh-deploy

# Clean up.
rm -rf docs/index.md docs/contributing.md docs/changelog.md docs/logo.svg docs/gifs.md docs/svgs.md docs/videos.md
