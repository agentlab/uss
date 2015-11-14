#!/bin/bash

if [[ "$GIT" == "" ]]; then
  GIT=$WORKSPACE/git
fi

if [[ "$PACK_AND_SIGN" == "" ]]; then
  PACK_AND_SIGN=false
fi

set -o nounset
set -o errexit

###########################################################################
echo ""

rm -rf $WORKSPACE/updates
mkdir $WORKSPACE/updates
cp -a $GIT/org.eclipse.userstorage.site/target/repository/* $WORKSPACE/updates

cd $WORKSPACE/updates
echo "Zipping update site"
zip -r -9 -qq org.eclipse.userstorage.site.zip * -x plugins/*.pack.gz

echo ""

