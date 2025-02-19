#!/usr/bin/env bash

set -e

TAG_NAME="$1"

# Ensure tag name is not empty.
if [ -z "$TAG_NAME" ]; then
	echo "Error: tag name cannot be empty."
	exit 1
fi

# Ensure xmlstarlet is installed.
if ! command -v xmlstarlet &> /dev/null; then
	echo "Error: xmlstarlet is required but not installed. Please install it to proceed."
	exit 1
fi

# Update the version in pom.xml to match the tag.
xmlstarlet ed -S -N maven="http://maven.apache.org/POM/4.0.0" -u "/maven:project/maven:version" -v "$TAG_NAME" pom.xml > pom.xml.tmp
mv pom.xml.tmp pom.xml

# Commit the updated pom.xml.
git commit -m "pom.xml: incrementa versão para ‘$TAG_NAME’" pom.xml
git tag "$TAG_NAME"

echo "Updated pom.xml version to $TAG_NAME for tag push"
