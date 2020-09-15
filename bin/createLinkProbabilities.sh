#!/bin/bash

set -e

PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SCRIPTDIR=`dirname "$PRG"`
SCRIPTDIR=`cd "$SCRIPTDIR"; pwd -P`
ROOTDIR=`cd "$SCRIPTDIR"/..; pwd -P`

. config/getvars.sh

set -x
set -v

groovy -cp ${GATE_HOME}/bin/gate.jar:${GATE_HOME}/lib/'*' $ROOTDIR/groovy/cui-counts.groovy $SRC/training-corpus cuis > $TMP/cuifreq.tsv
groovy -cp ${GATE_HOME}/bin/gate.jar:${GATE_HOME}/lib/'*' $ROOTDIR/groovy/cui-counts.groovy $SRC/training-corpus labels > $TMP/labelfreq.tsv

