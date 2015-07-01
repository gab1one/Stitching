#!/bin/bash

mvn clean install
cp -f target/Stitching_-3.0.3-SNAPSHOT.jar ../knip-stitching/org.knime.knip.stitching/libs/
cp -f target/Stitching_-3.0.3-SNAPSHOT-sources.jar ../knip-stitching/org.knime.knip.stitching/libs/
