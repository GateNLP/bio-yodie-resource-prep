#!/bin/bash
#
# loadUmls.sh
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

#echo "Using $H2_JAR_LOC."

if [ -f "$DB.h2.db" ]
 then
  echo "Using extant DB at $DB.h2.db"
 else
  echo "Creating new DB at $DB.h2.db"
fi

echo "Loading UMLS into database."

umlsloc=$SRC/umls/

echo "Updating the UMLS location in h2_tables script to $umlsloc."

cp $ROOTDIR/sql/h2_tables_master.sql $ROOTDIR/sql/h2_tables_autogen.sql
sed -i "s|###UMLSLOC|$umlsloc|g" $ROOTDIR/sql/h2_tables_autogen.sql

echo "Updating the UMLS version in h2_tables script to 2015AB."

sed -i 's/###UMLSVERSION/2015AB/g' $ROOTDIR/sql/h2_tables_autogen.sql

# If $LANGS is set, use it to change a parameter in the SQL script sql/h2_tables_autogen.sql
# that will be used as a filter, to keep only the lines in the MRCONSO table with strings in
# the languages specified

LANGLIST=`echo ${LANGS:-"en"} | sed "s/^/(\'/g" | sed "s/ /\',\'/g" | sed "s/$/\')/g"`

echo -n "Updating the language filter in h2_tables script to "
echo -n ${LANGLIST}
echo "."

sed -i 's/###LANGLIST/'$LANGLIST'/g' $ROOTDIR/sql/h2_tables_autogen.sql

echo "Create and load tables ... `/bin/date`"
#java -cp $H2_JAR_LOC org.h2.tools.RunScript -url jdbc:h2:$DB;MV_STORE=FALSE -script $ROOTDIR/sql/h2_tables_autogen.sql -continueOnError -user sa
$ROOTDIR/bin/runScala.sh ExecuteSql $DB $ROOTDIR/sql/h2_tables_autogen.sql

echo "Finished loading tables ... `/bin/date`"

echo "Create indexes ... `/bin/date`"
#java -cp $H2_JAR_LOC org.h2.tools.RunScript -url jdbc:h2:$DB;MV_STORE=FALSE -script $ROOTDIR/sql/h2_indexes.sql -continueOnError -user sa
$ROOTDIR/bin/runScala.sh ExecuteSql $DB $ROOTDIR/sql/h2_indexes.sql

echo "Finished indexes ... `/bin/date`"

echo -n "Done loading UMLS for "
echo ${LANGLIST}


