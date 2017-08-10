#!/bin/bash
#
# createLabelInfoDB.sh
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

## createLabelUriInfoDB.sh

## extract the fully joined data as tsv, then create the 
## json info file and import into the output database.
## Also extract those label/uri combinations from the wiki link stats
## table which are not matched in the labelUriInfo table.

. config/getvars.sh

LANGLABELS=`echo ${LANGS:-"en"} | sed "s/ /+/g"`
LABELDBNAME="labelinfo"${LANGS:+"-"${LANGLABELS}}

set -x
set -v

if ! [ -d ${TMP}/${LANGLABELS} ]
then
 mkdir ${TMP}/${LANGLABELS}
fi

if [ -f ${TMP}/${LANGLABELS}/labelInfoFull.tsv.gz ]
then
 rm ${TMP}/${LANGLABELS}/labelInfoFull.tsv.gz
fi

## export the data using a left outer join, replacing the missing values with zeros 
$ROOTDIR/bin/runScala.sh Db2Tsv ${DB} $ROOTDIR/sql/ExportJoinedLabelInfoFull.sql | \
gzip > ${TMP}/${LANGLABELS}/labelInfoFull.tsv.gz

#$ROOTDIR/bin/runScala.sh ExecuteSql ${DB} $ROOTDIR/sql/CreateLabelInfoFull.sql

#./bin/runScala.sh ExecuteSql ${DB} sql/InsertLabelInfoFull.sql

## 
zcat ${TMP}/${LANGLABELS}/labelInfoFull.tsv.gz | \
$ROOTDIR/bin/runScala.sh Tsv2JsonTsv 1 label inst STY TUI PREF LABELVOCABS CUIVOCABS scCRISLabelCUIFreq scCRISLabelCUINorm scCRISCUIFreq scCRISCUINorm scPageRank| \
gzip > ${TMP}/${LANGLABELS}/labelInfoJson.tsv.gz

$ROOTDIR/bin/runScala.sh ExecuteSql ${OUT}/databases/${LABELDBNAME} sql/CreateLabelInfo.sql

## only import the JSON infos that are less than 65536 characters long for now
zcat ${TMP}/${LANGLABELS}/labelInfoJson.tsv.gz | \
$ROOTDIR/bin/runScala.sh TsvGrep -v 1 '.{65536,}' | \
$ROOTDIR/bin/runScala.sh Tsv2Db ${OUT}/databases/${LABELDBNAME} sql/ImportLabelInfo.sql -

for LANG in $LANGS ; do

  if ! [ -d ${OUT}/${LANG} ]
  then
   mkdir ${OUT}/${LANG}
  fi

  if ! [ -d ${OUT}/${LANG}/databases ]
  then
   mkdir ${OUT}/${LANG}/databases
  fi

  if [ -f ${OUT}/${LANG}/databases/labelinfo.h2.db ]
  then
   rm -rf ${OUT}/${LANG}/databases/labelinfo.h2.db
  fi

  if [ -f ${OUT}/${LANG}/databases/labelinfo.trace.db ]
  then
   rm -rf ${OUT}/${LANG}/databases/labelinfo.trace.db
  fi

  cp ${OUT}/databases/${LABELDBNAME}.h2.db ${OUT}/${LANG}/databases/labelinfo.h2.db

  cp ${OUT}/databases/${LABELDBNAME}.trace.db ${OUT}/${LANG}/databases/labelinfo.trace.db

done

rm ${OUT}/databases/${LABELDBNAME}.h2.db

rm ${OUT}/databases/${LABELDBNAME}.trace.db

