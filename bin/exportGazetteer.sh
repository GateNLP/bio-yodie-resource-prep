#!/bin/bash
#
# exportGazetteer.sh
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

STYCLASSESLIST=`echo ${STYCLASSES:-"BMH"} | sed "s/^/(\'/g" | sed "s/ /\',\'/g" | sed "s/$/\')/g"`

for LANG in $LANGS ; do

  echo Exporting the gazetteer for "'"${LANG}"'" "("${LANGNAMES[${LANG}]}")".

 if [ ! -d ${OUT}/${LANG} ]
  then
   mkdir ${OUT}/${LANG}
 fi

 if [ ! -d ${OUT}/${LANG}/gazetteer-${LANG}-bio ]
  then
   mkdir ${OUT}/${LANG}/gazetteer-${LANG}-bio
 fi

 ## extract the list of unique labels for $LANG and place it into the output directory

 echo "Extracting the "${LANGNAMES[${LANG}]}" label list from the database .."

 if [ -f $ROOTDIR/sql/get-labels-${LANG}_autogen.sql ]
  then
   rm $ROOTDIR/sql/get-labels-${LANG}_autogen.sql
 fi

 cp $ROOTDIR/sql/get-labels.sql $ROOTDIR/sql/get-labels-${LANG}_autogen.sql

 # Filter the MRCONSO table to keep only semantic types belonging to the chosen subset(s):
 # put the desired STYCLASSES in sql/get-labels-${LANG}_autogen.sql (line #7)
 sed -i 's/###SELECTEDSTYCLASSES/'${STYCLASSESLIST}'/g' $ROOTDIR/sql/get-labels-${LANG}_autogen.sql

 # Filter the MRCONSO table to keep only strings in ${LANG}:
 # put the correct language filter in sql/get-labels-${LANG}_autogen.sql (line #8)
 LANGLIST=`echo ${LANG:-"en"} | sed "s/^/(\'/g" | sed "s/$/\')/g"`

 echo -n "Updating the language filter in the get-labels SQL script to "
 echo -n ${LANGLIST}
 echo "."

 sed -i 's/###LANGLIST/'$LANGLIST'/g' $ROOTDIR/sql/get-labels-${LANG}_autogen.sql

 echo "Extracting the "${LANGNAMES[${LANG}]}" label list from the database ..."

 #java -cp $H2_JAR_LOC org.h2.tools.RunScript -url jdbc:h2:$DB;MV_STORE=FALSE -script $ROOTDIR/sql/get-labels.sql -user sa -showResults > ${OUT}/gazetteer-en-bio/labels.lst
 $ROOTDIR/bin/runScala.sh Db2Tsv ${DB} $ROOTDIR/sql/get-labels-${LANG}_autogen.sql > ${OUT}/${LANG}/gazetteer-${LANG}-bio/labels.lst

 sed '/\s/d' ${OUT}/${LANG}/gazetteer-${LANG}-bio/labels.lst | grep '[a-zA-Z][A-Z]' >${OUT}/${LANG}/gazetteer-${LANG}-bio/cased-labels.lst

 grep ' ' ${OUT}/${LANG}/gazetteer-${LANG}-bio/labels.lst >${OUT}/${LANG}/gazetteer-${LANG}-bio/uncased-labels.lst
 sed '/[a-zA-Z][A-Z]/d' ${OUT}/${LANG}/gazetteer-${LANG}-bio/labels.lst | sed '/\s/d' >>${OUT}/${LANG}/gazetteer-${LANG}-bio/uncased-labels.lst

 echo "Done!"

 cp $ROOTDIR/templates/cased-labels.def ${OUT}/${LANG}/gazetteer-${LANG}-bio
 cp $ROOTDIR/templates/uncased-labels.def ${OUT}/${LANG}/gazetteer-${LANG}-bio

 #echo "Pre-generating the cache file .."
 # Not pregenerating the cache because prep is simpler to move around without the dependency on the plugin.
 # Not doing this means there will be a significant delay the first time Bio-YODIE is run, but not
 # subsequent times.

 #$ROOTDIR/bin/createCache.sh ${OUT}/${LANG}/gazetteer-${LANG}-bio/cased-labels.def false en-US
 #$ROOTDIR/bin/createCache.sh ${OUT}/${LANG}/gazetteer-${LANG}-bio/uncased-labels.def false en-US

 # Rudolf Cardinal, 15 Sep 2020:
 # "set -e" is helpful above, but with the comments above, the "rm" statements
 # are trying to remove non-existent files. So we could either "set +e" again,
 # or use "rm -f" to ignore nonexistent files. The latter is more consistent
 # with error-checking.

 rm -f ${OUT}/${LANG}/gazetteer-${LANG}-bio/cased-labels.gazbin
 rm -f ${OUT}/${LANG}/gazetteer-${LANG}-bio/uncased-labels.gazbin

 echo "Finished preparing the "${LANGNAMES[${LANG}]}" gazetteer!"

done

