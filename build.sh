#!/bin/sh
set -eu
if ! command -v java >/dev/null 2>&1 || ! command -v gradle >/dev/null 2>&1; then
  echo 'Java 17 and Gradle 8.x are required. Install both before running.' >&2
  exit 1
fi
gradle build
printf 'Built files are in: build/libs/\n'