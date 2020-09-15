#!/bin/bash
#
# addYODIETables.sh
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

H2_JAR_LOC="$ROOTDIR/scala/lib/h2-*.jar"

STYCLASSESLIST=`echo ${STYCLASSES:-"KCO"} | sed "s/^/(\'/g" | sed "s/ /\',\'/g" | sed "s/$/\')/g"`

if [ -f $ROOTDIR/tmpdata/styclasses.csv ]
 then
  rm $ROOTDIR/tmpdata/styclasses.csv
fi

# Putting the class information for the semantic types in CSV form
cat $ROOTDIR/config/styclasses.lst | awk -F\: '/^[A-Z][A-Z][A-Z]:$/ {class=$1} /^[\047][^\047]+[\047]$/ {print "\047"class"\047,"$1}' > $ROOTDIR/tmpdata/styclasses.csv

#if [ 1 -eq 0 ]; then

if [ $NLP = "true" ]
 then
  echo "Subsetting UMLS for NLP .."
  $ROOTDIR/bin/runScala.sh ExecuteSql $DB $ROOTDIR/sql/nlp-subset.sql
fi

if [ -f $ROOTDIR/sql/addYODIETables_autogen.sql ]
 then
  rm $ROOTDIR/sql/addYODIETables_autogen.sql
fi

cp $ROOTDIR/sql/addYODIETables_master.sql $ROOTDIR/sql/addYODIETables_autogen.sql

#Putting the correct file path for that table in sql/addYODIETables_autogen.sql (line #19)
sed -i 's:###ROOTDIR:'$ROOTDIR':g' $ROOTDIR/sql/addYODIETables_autogen.sql

#Putting the desired STYCLASSES in sql/addYODIETables_autogen.sql (line #32)
sed -i 's/###SELECTEDSTYCLASSES/'${STYCLASSESLIST}'/g' $ROOTDIR/sql/addYODIETables_autogen.sql

#Putting in the right location for SRC
sed -i 's|###SRCS|'${SRC}'|g' $ROOTDIR/sql/addYODIETables_autogen.sql

#Putting in the right location for the TMPDATA
sed -i 's|###TMPDATA|'${TMP}'|g' $ROOTDIR/sql/addYODIETables_autogen.sql

echo "Adding table with MeSH frequencies, and a restricted set of semantic types (STY)."

#java -cp $H2_JAR_LOC org.h2.tools.RunScript -url jdbc:h2:$DB;MV_STORE=FALSE -script $ROOTDIR/sql/add-mesh-freq.sql -user sa
$ROOTDIR/bin/runScala.sh ExecuteSql $DB $ROOTDIR/sql/addYODIETables_autogen.sql

rm $ROOTDIR/tmpdata/styclasses.csv

echo "Done."

#fi

