#!/usr/bin/env bash

set -e

# Check if any staged .java files exist
if ! git diff --cached --name-only | grep -qE '\.java$'; then
    echo "No Java files staged for commit. Skipping version bump."
    exit 0
fi

# Get the current version and revision number.
VERSION="$(git describe --tags | sed 's/^v\([0-9]*\.[0-9]*\.[0-9]*\)-\([0-9]*\)-g.*/v\1-r\2/')"

# Ensure tag name is not empty.
if [ -z "$VERSION" ]; then
    echo "Error: version cannot be empty."
    exit 1
fi

# Extract the base version (e.g., v0.0.1) and the revision number (e.g., 4)
BASE_VERSION="${VERSION%-r*}"  # Remove everything after -r
REVISION="${VERSION##*-r}"     # Extract the revision number after -r

# Increment the revision number by 1
NEW_REVISION=$((REVISION + 1))

# Construct the new version with the incremented revision
NEW_VERSION="${BASE_VERSION}.r${NEW_REVISION}"

# Ensure xmlstarlet is installed.
if ! command -v xmlstarlet &> /dev/null; then
    echo "Error: xmlstarlet is required but not installed. Please install it to proceed."
    exit 1
fi

# Update the version in pom.xml to match the incremented version.
xmlstarlet ed -S -N maven="http://maven.apache.org/POM/4.0.0" -u "/maven:project/maven:version" -v "$NEW_VERSION" pom.xml > pom.xml.tmp
mv pom.xml.tmp pom.xml

# Commit the updated pom.xml.
git add pom.xml

echo "Updated pom.xml version to $NEW_VERSION for commit"
