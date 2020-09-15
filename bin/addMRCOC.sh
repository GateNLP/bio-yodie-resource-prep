#!/bin/bash
#
# addMRCOC.sh
#
# Copyright (c) 1995-2016, The University of Sheffield. See the file
# COPYRIGHT.txt in the software or at
# http://gate.ac.uk/gate/COPYRIGHT.txt
#
# This file is part of Bio-YODIE (see 
# https://gate.ac.uk/applications/bio-yodie.html), and is free 
# software, licenced under the GNU Affero General Public License
# Version 3, 19 November 2007
#
# A copy of this licence is included in the distribution in the file
# LICENSE-AGPL3.html, and is also available at
# http://www.gnu.org/licenses/agpl-3.0-standalone.html
#
# G. Gorrell, 26 September 2016

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

H2_JAR_LOC="$ROOTDIR/../yodie-preparation/lib/h2-*.jar"

# The contents is based on co-occurrence frequencies of terms computed on MEDLINE abstracts
# and is available from https://mbr.nlm.nih.gov/MRCOC.shtml

if [ -f $ROOTDIR/sql/addMRCOC_autogen.sql ]
 then
  rm $ROOTDIR/sql/addMRCOC_autogen.sql
fi

cp $ROOTDIR/sql/addMRCOC_master.sql $ROOTDIR/sql/addMRCOC_autogen.sql

#Putting the desired STYCLASSES in sql/addYODIETables_autogen.sql (line #32)
sed -i 's:###SRCS:'$SRC':g' $ROOTDIR/sql/addMRCOC_autogen.sql

echo "Adding table with MeSH terms co-occurrence frequencies in MEDLINE abstracts."

#java -cp $H2_JAR_LOC org.h2.tools.RunScript -url jdbc:h2:$DB;MV_STORE=FALSE -script $ROOTDIR/sql/add-mesh-freq.sql -user sa

JAVA_OPTS="-Xmx10G"

$ROOTDIR/bin/runScala.sh ExecuteSql $DB $ROOTDIR/sql/addMRCOC_autogen.sql

echo "Done."


