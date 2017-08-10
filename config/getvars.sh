#!/bin/bash
#
# getvars.sh
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

## CAUTION! 
## This assumes that the invoking script is running from the root
## of the yodie-preparation directory or that the invoking script
## has exported the variable YODIE_PREPARATION to point to the root of the 
## yodie-preparation directory

if [ x"${YODIE_PREPARATION}" == x ] 
then
  export ROOT=`pwd`
else 
  export ROOT="${YODIE_PREPARATION}"
fi

export JAVA_OPTS=-Xmx30G

error=0

export LANGS='en'
export LANGSALL='en bg de es zh ru fr it nl ja'
export NLP="false"

# Language ISO-639-1 codes and corresponding English names (to write more informative messages)
# (Only the languages present in UMLS meta-thesaurus are listed here)

declare -A LANGNAMES
LANGNAMES['cs']='Czech'
LANGNAMES['da']='Danish'
LANGNAMES['de']='German'
LANGNAMES['el']='Greek'
LANGNAMES['en']='English'
LANGNAMES['es']='Spanish'
LANGNAMES['et']='Estonian'
LANGNAMES['eu']='Basque'
LANGNAMES['fi']='Finnish'
LANGNAMES['fr']='French'
LANGNAMES['it']='Italian'
LANGNAMES['ja']='Japanese'
LANGNAMES['he']='Hebrew'
LANGNAMES['hr']='Croatian'
LANGNAMES['hu']='Hungarian'
LANGNAMES['ko']='Korean'
LANGNAMES['lv']='Latvian'
LANGNAMES['nl']='Dutch'
LANGNAMES['no']='Norwegian'
LANGNAMES['pl']='Polish'
LANGNAMES['pt']='Portuguese'
LANGNAMES['ru']='Russian'
LANGNAMES['sh']='Serbo-Croatian'
LANGNAMES['sr']='Serbian'
LANGNAMES['sv']='Swedish'
LANGNAMES['tr']='Turkish'
LANGNAMES['zh']='Chinese'
export LANGNAMES

# The variable STYCLASSES restricts the set of STY (Semantic TYpes) used for specific application domains.
# Possible choices are listed (and may be edited) in config/styclasses.lst

# (if more than one, just separated by spaces)
# HNU : Health and Nutrition

STYCLASSES='KCX'


#srcs should be where you have your UMLS, MRCOC and Mesh frequency table

if [ -d "${ROOT}"/srcs ]  
then 
  srcexp=$(readlink -f "${ROOT}"/srcs)
  export SRC=$srcexp
else
  echo no directory or link $ROOT/srcs found
  error=1
fi

#tmpdata should point somewhere you have free space that can be used for intermediate files

if [ -d "${ROOT}"/tmpdata ]  
then
  tmpexp=$(readlink -f "${ROOT}"/tmpdata)
  export TMP=$tmpexp
else
  echo no directory or link $ROOT/tmpdata found
  error=1
fi

#output is the directory of runtime resources you are going to create

if [ -d "${ROOT}"/output ]  
then 
  tmpout=$(readlink -f "${ROOT}"/output)
  export OUT=$tmpout
else
  echo no directory or link $ROOT/output found
  error=1
fi

#databases should contain your original database that you are going to compile
#the runtime resources from, e.g. the full UMLS.

if [ -d "${ROOT}"/databases ]  
then 
  tmproot=$(readlink -f "${ROOT}"/databases)
  export WORK=$tmproot
  export DB=$tmproot/umls
else
  echo no directory or link $ROOT/databases found
  error=1
fi

#if [ -d "${ROOT}"/processed ]  
#then 
#  export PRC="${ROOT}"/processed
#else
#  echo no directory or link $ROOT/processed found
#  error=1
#fi


echo SRC=$SRC
echo TMP=$TMP
echo WORK=$WORK
echo DB=$DB
echo OUT=$OUT
#echo PRC=$PRC

## silently make sure we have all the directories in $OUT we need, but only if OUT is defined
#if [ "$OUT" != "" ] 
#then
#  mkdir $OUT/databases &> /dev/null
#  mkdir $OUT/fastgraph &> /dev/null
#  mkdir $OUT/gazetteer-en &> /dev/null
#  mkdir $OUT/semspace &> /dev/null
#fi

runScript () {
  scriptname="$1"
  if [ "$scriptname" == "" ]
  then
    echo use of runScript without an argument
    ## only exit if called from a bash script (not if from the terminal,
    ## this would close the terminal!)
    if [ "`ps --no-heading -o %c -p $PPID`" == "bash" ] ; then exit 1; fi  
  else 
    starttime=$(date +%s)
    echo ==== START $scriptname $starttime
    /usr/bin/time -f"elapsed for $scriptname is %e" $ROOT/bin/"$scriptname".sh |& tee "$scriptname".log
    ret=$?
    stoptime=$(date +%s)
    echo ==== STOP $scriptname $stoptime 
    if [ "$ret" != "0" ]
    then
      echo ERROR script exited with exit code $ret, aborting
      ## only exit if called from a bash script (not if from the terminal,
      ## this would close the terminal!)
      if [ "`ps --no-heading -o %c -p $PPID`" == "bash" ] ; then exit 1; fi
    fi
  fi 
}


if [ "$error" == "1" ]
then 
  echo ENCOUNTERED ERRORS, SEE ABOVE
fi
