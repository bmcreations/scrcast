#!/bin/bash

# Deploy updated docs
sh deploy-docs.sh

# Upload release to maven
./gradlew uploadArchives --no-daemon --no-parallel
