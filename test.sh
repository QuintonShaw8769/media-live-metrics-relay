#!/bin/sh
set -eu

classes="${TMPDIR:-/tmp}/media-live-dashboard-test-classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java src/test/java -name '*.java')
java -cp "$classes" dev.infrai.media.MediaMetricsRelayTest
