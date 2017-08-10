#!/bin/bash
#
# createUKBgraph.sh
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

## createFastGraph.sh

if [ ! -f config/getvars.sh ]
then
  echo 'config/getvars.sh not found!! -- are you running from the top yodie-preparation directory?'
  exit 1
fi
. config/getvars.sh

# You'll need UKB on your classpath for this to work.

set -x
set -v

./bin/runScala.sh ExecuteSql ${WORK}/umls sql/computeUKBWeights.sql

./bin/runScala.sh Db2Tsv ${WORK}/umls sql/exportUKBWeights.sql > ${TMP}/umls_ukb.tsv

perl bin/reformatGraph.pl ${TMP}/umls_ukb.tsv > ${TMP}/umls_ukbgraph.txt

compile_kb --nopos -o ${TMP}/umls_ukbgraph.bin ${TMP}/umls_ukbgraph.txt

ukb_ppv -K ${TMP}/umls_ukbgraph.bin -S >${SRC}/umls_ukbstatic.txt

