#!/bin/bash

if [[ "$BUILD_TYPE" == "" || "$BUILD_TYPE" == none ]]; then
  BUILD_TYPE=nightly
fi

if [[ "$BUILD_KEY" == "" ]]; then
  if [[ "$BUILD_TYPE" == milestone ]]; then
    TYPE="S"
  elif [[ "$BUILD_TYPE" == nightly ]]; then
    TYPE="N"
  fi

  if [[ "$TYPE" != "" ]]; then
    BUILD_KEY=$TYPE`echo $BUILD_ID | sed 's/\([0-9]*\)-\([0-9]*\)-\([0-9]*\)_\([0-9]*\)-\([0-9]*\)-\([0-9]*\)/\1\2\3-\4\5\6/g'`
  fi
fi

if [[ "$BUILD_LABEL" == "" ]]; then
  BUILD_LABEL=""
fi

FOLDER=$BUILD_KEY
if [[ "$BUILD_LABEL" != "" ]]; then
  FOLDER=$FOLDER-$BUILD_LABEL
fi

if [[ "$GIT" == "" ]]; then
  GIT=$WORKSPACE/git
fi

if [[ "$SCRIPTS" == "" ]]; then
  SCRIPTS=$GIT/releng/org.eclipse.userstorage.releng/hudson
fi

if [[ "$DOWNLOADS" == "" ]]; then
  DOWNLOADS=/home/data/httpd/download.eclipse.org/oomph/uss
fi

set -o nounset
set -o errexit

##################################################################################################
#
# At this point $WORKSPACE points to the following build folder structure:
#
#   $WORKSPACE/
#   $WORKSPACE/updates/
#   $WORKSPACE/updates/features/
#   $WORKSPACE/updates/plugins/
#   $WORKSPACE/updates/artifacts.jar
#   $WORKSPACE/updates/content.jar
#   $WORKSPACE/updates/org.eclipse.userstorage.site.zip
#
##################################################################################################

echo ""

PROPERTIES=$WORKSPACE/updates/repository.properties
echo "branch = $GIT_BRANCH" >> $PROPERTIES
echo "commit = $GIT_COMMIT" >> $PROPERTIES
echo "number = $BUILD_NUMBER" >> $PROPERTIES
echo "key = $BUILD_KEY" >> $PROPERTIES
echo "label = $BUILD_LABEL" >> $PROPERTIES

BUILDS=$DOWNLOADS/builds
UPDATES=$DOWNLOADS/updates
DROPS=$DOWNLOADS/drops
DROP_TYPE=$DROPS/$BUILD_TYPE
DROP=$DROP_TYPE/$FOLDER

mkdir -p $DOWNLOADS
mkdir -p $BUILDS
mkdir -p $UPDATES
mkdir -p $DROPS
mkdir -p $DROP_TYPE

cp -a $PROPERTIES $BUILDS/$BUILD_NUMBER.properties

###################
# DOWNLOADS/DROPS #
###################

echo "Promoting $WORKSPACE/updates"
rm -rf $DROP
mkdir $DROP
cp -a $WORKSPACE/updates/* $DROP
$BASH $SCRIPTS/adjustArtifactRepository.sh \
  $DROP \
  $DROP \
  "User Storage Updates $FOLDER" \
  $BUILD_TYPE

#####################
# DOWNLOADS/UPDATES #
#####################

cd $WORKSPACE
rm -rf $UPDATES.tmp
cp -a $UPDATES $UPDATES.tmp

$BASH $SCRIPTS/composeRepositories.sh \
  "$DOWNLOADS" \
  "$BUILD_TYPE" \
  "$BUILD_KEY" \
  "$BUILD_LABEL"

mkdir -p $UPDATES.tmp/$BUILD_TYPE/latest
cp -a $DROP/org.eclipse.userstorage.site.zip $UPDATES.tmp/$BUILD_TYPE/latest

mkdir -p $UPDATES.tmp/latest
cp -a $DROP/org.eclipse.userstorage.site.zip $UPDATES.tmp/latest

mv $UPDATES $UPDATES.bak; mv $UPDATES.tmp $UPDATES

cd $WORKSPACE
rm -rf $UPDATES.bak

cd $WORKSPACE
for t in nightly milestone; do
  for f in $DROPS/$t/*; do
    if [[ -f $f/REMOVE ]]; then
      echo "Deleting $f"
      rm -rf $f
    fi
  done
done

echo ""
