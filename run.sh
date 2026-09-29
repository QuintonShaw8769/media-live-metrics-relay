#!/bin/sh
set -eu

classes="${TMPDIR:-/tmp}/media-live-dashboard-classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java -name '*.java')
java -cp "$classes" dev.infrai.media.MediaDashboardCommand "$@"
