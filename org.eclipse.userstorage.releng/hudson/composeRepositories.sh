#!/bin/bash

if [[ "$GIT" == "" ]]; then
  GIT=$WORKSPACE/git
fi

set -o nounset
set -o errexit

DOWNLOADS_FOLDER=$1
BUILD_TYPE=$2;
BUILD_KEY=$3;
BUILD_LABEL=$4;

java -cp "$GIT/org.eclipse.userstorage.releng/bin/" RepositoryComposer \
  "$DOWNLOADS_FOLDER" \
  "$BUILD_TYPE" \
  "$BUILD_KEY" \
  "$BUILD_LABEL"
