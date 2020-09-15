#!/bin/bash
#
# createAbstractsDB.sh
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

LANGLABELS=`echo ${LANGS:-"en"} | sed "s/ /+/g"`
ABSTRACTSDBNAME="abstracts"${LANGS:+"-"${LANGLABELS}}

if ! [ -d ${TMP}/${LANGLABELS} ]
then
 mkdir ${TMP}/${LANGLABELS}
fi

if [ -f ${OUT}/databases/${ABSTRACTSDBNAME}.h2.db ]
then
 if ! [ -d ${OUT}/databases/old ]
  then
   mkdir ${OUT}/databases/old
 fi
 mv ${OUT}/databases/${ABSTRACTSDBNAME}.h2.db ${OUT}/databases/old/
fi

$ROOTDIR/bin/runScala.sh Db2Tsv ${DB} $ROOTDIR/sql/ExportAbstracts.sql > ${TMP}/${LANGLABELS}/tmp.tsv

$ROOTDIR/bin/runScala.sh ExecuteSql ${OUT}/databases/${ABSTRACTSDBNAME} $ROOTDIR/sql/createAbstractsTable.sql

$ROOTDIR/bin/runScala.sh Tsv2Db ${OUT}/databases/${ABSTRACTSDBNAME} sql/ImportAbstracts.sql ${TMP}/${LANGLABELS}/tmp.tsv

for LANG in $LANGS ; do

  if ! [ -d ${OUT}/${LANG} ]
  then
   mkdir ${OUT}/${LANG}
  fi

  if ! [ -d ${OUT}/${LANG}/databases ]
  then
   mkdir ${OUT}/${LANG}/databases
  fi

  if [ -f ${OUT}/${LANG}/databases/abstracts.h2.db ]
  then
   rm -rf ${OUT}/${LANG}/databases/abstracts.h2.db
  fi

  if [ -f ${OUT}/${LANG}/databases/abstracts.trace.db ]
  then
   rm -rf ${OUT}/${LANG}/databases/abstracts.trace.db
  fi

  cp ${OUT}/databases/${ABSTRACTSDBNAME}.h2.db ${OUT}/${LANG}/databases/abstracts.h2.db

  cp ${OUT}/databases/${ABSTRACTSDBNAME}.trace.db ${OUT}/${LANG}/databases/abstracts.trace.db

done

rm ${TMP}/${LANGLABELS}/tmp.tsv

rm ${OUT}/databases/${ABSTRACTSDBNAME}.h2.db

rm ${OUT}/databases/${ABSTRACTSDBNAME}.trace.db

