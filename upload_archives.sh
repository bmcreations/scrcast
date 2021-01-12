#!/bin/bash

# Deploy updated docs
sh deploy-docs.sh

# Upload release to maven
./gradlew uploadArchives -PSONATYPE_NEXUS_USERNAME=$SONATYPE_NEXUS_USERNAME -PSONATYPE_NEXUS_PASSWORD=$SONATYPE_NEXUS_PASSWORD --no-daemon --no-parallel
