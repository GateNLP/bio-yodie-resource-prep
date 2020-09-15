#!/bin/bash
#
# all.sh
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

. $ROOTDIR/config/getvars.sh

DATE=$(date +%Y%m%d-%H%M%S)
LNGS=$(echo ${LANGS} | sed s/\ /-/)
FILENAME=benchmarking-${LNGS}-${DATE}.txt
echo "Saving benchmarking times to $FILENAME"
echo "Beginning to export resources for $LANGS." > $FILENAME
echo "Loading UMLS." >> $FILENAME
date >> $FILENAME

#Load your UMLS subset into an H2 database. This may require adaptation
#if your UMLS subset differs substantially from the 2015AB English NLP
#subset on which the script was designed.

$SCRIPTDIR/loadUmls.sh

#echo "Loaded UMLS. Adding MRCOC." >> $FILENAME
echo "Loaded UMLS. Creating link probabilities from training corpus." >> $FILENAME
#date >> $FILENAME


#The MRCOC table is a very large table that ships separately to UMLS,
#and therefore is loaded separately for flexibility. It is important for
#concept co-occurrences, which are used for scoring, so it's not language
#specific.

#$SCRIPTDIR/addMRCOC.sh

#echo "Added MRCOC. Creating PageRank graph." >> $FILENAME
#date >> $FILENAME


#This is a good scoring resource based on the co-occurrences table, 
#which is a rich resource. It is likely that the English
#one will work for other languages. We run it now so we can include the
#result in the cui info, as it isn't context sensitive (it's a static
#pagerank).

#$SCRIPTDIR/createUKBgraph.sh

#echo "Created PageRank graph. Creating link probabilities from training corpus." >> $FILENAME
date >> $FILENAME


#Use training data to extract probabilities for CUIs. We do this now
#because the information will be included in the CUI info.

$SCRIPTDIR/createLinkProbabilities.sh

echo "Created link probabilities. Adding YODIE-required tables to the UMLS database." >> $FILENAME
date >> $FILENAME


#This creates all the tables that will subsequently be used to make the
#Bio-YODIE resources. It includes the relations table which is later used
#to make the fastgraph resource for relation-based scoring. This can be
#considered optional.

$SCRIPTDIR/addYODIETables.sh

echo "Added YODIE tables. Exporting gazetteers for use in the final application." >> $FILENAME
date >> $FILENAME


#This is the step that exports the gazetteer. Once you have this, you will
#have what you need to find mentions based on all the names of entities in
#your subset.

$SCRIPTDIR/exportGazetteer.sh

echo "Exported the gazetteers. Exporting minimal database connecting labels to candidates." >> $FILENAME
date >> $FILENAME


#This next creates the minimal database required to look up every entity
#matching a mention that the gazetteer has found, and find the several
#things that it could be. It is on the critical path.

$SCRIPTDIR/createLabelInfoDB.sh

echo "Done!!" >> $FILENAME
date >> $FILENAME

echo "Preparation script completed. Note that the first time you use"
echo "Bio-YODIE there will be a significant delay while the gazetteer"
echo "cache is created. This is one time only."

##BELOW HERE EXCLUDED FOR NOW!!!

#This gets back a piece of text describing an entity, so we can use that
#information to choose the best option. It's language specific, so we can't
#just use the English resource, but it is relevant only to the scoring
#stage so in that sense is less critical than the gazetteer and label info DB
#above. There are a few different ways of scoring and this is just one of them.

#$SCRIPTDIR/createAbstractsDB.sh


#This makes a resource from the relations table that enables joint disambiguation
# (scoring). It is not critical. The English one can potentially be used for 
#other languages.

#$SCRIPTDIR/createFastGraph.sh

