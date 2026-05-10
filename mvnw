#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_VERSION=3.9.9
INSTALL_DIR="$BASE_DIR/.mvn/apache-maven"
ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/${ARCHIVE}"

if [ ! -x "$INSTALL_DIR/bin/mvn" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  TMP_DIR=$(mktemp -d)
  trap 'rm -rf "$TMP_DIR"' EXIT
  curl -fsSL "$DOWNLOAD_URL" -o "$TMP_DIR/$ARCHIVE"
  tar -xzf "$TMP_DIR/$ARCHIVE" -C "$BASE_DIR/.mvn"
  rm -rf "$INSTALL_DIR"
  mv "$BASE_DIR/.mvn/apache-maven-${MAVEN_VERSION}" "$INSTALL_DIR"
fi

exec "$INSTALL_DIR/bin/mvn" "$@"
