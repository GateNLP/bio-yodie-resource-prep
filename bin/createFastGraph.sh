#!/bin/bash
#
# createFastGraph.sh
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

if [ ! -f config/getvars.sh ]
then
  echo 'config/getvars.sh not found!! -- are you running from the top yodie-preparation directory?'
  exit 1
fi
. config/getvars.sh

if ! [ -d ${OUT}/fastgraph ]
 then
  mkdir ${OUT}/fastgraph
fi


./bin/runScala.sh Db2Tsv ${WORK}/umls 'SELECT CUI1, CUI2, COUNT FROM RELATIONCOUNTS ORDER BY CUI1' | \
gzip > ${TMP}/relationcounts_byFrom.tsv.gz

./bin/runScala.sh Db2Tsv ${WORK}/umls 'SELECT CUI1, CUI2, COUNT FROM RELATIONCOUNTS ORDER BY CUI2' | \
gzip > ${TMP}/relationcounts_byTo.tsv.gz

./bin/runScala.sh CreateFastGraph ${TMP}/relationcounts_byFrom.tsv.gz ${TMP}/relationcounts_byTo.tsv.gz ${OUT}/fastgraph/graph.gz

