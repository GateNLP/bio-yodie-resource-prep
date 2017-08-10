# bio-yodie-resource-prep

Scripts to prepare the informational resources required by GATE Bio-YODIE.

## Running the Bio-YODIE preparation

The Bio-YODIE preparation does all the preparation that can be done 
in advance in order to minimize the calculation being done at runtime. 
This includes creating the gazetteer, creating a minimal database 
containing everything that needs to go onto disambiguation candidates, 
and assigning information to these candidates. It also includes 
creating scoring resources that will be used at runtime.

You will need a downloaded UMLS.

You will need the following symbolic links:

databases -> should point to the directory where you will store your 
new UMLS H2 database.

output -> this is the directory that will be populated with the new 
Bio-YODIE resources, that then needs to be copied into yodie-pipeline 
in order for it to work. (Note that yodie-pipeline/bin/download-resources-bio.sh 
is a script that gets it from a hardcoded location on GS8.)

srcs -> should point to where you have your UMLS download. You will 
receive a srcs directory in the prep distribution. You may put your UMLS 
in here, or move the contents to the directory containing UMLS, alongside 
it, and make a symbolic link. It expects a directory called 
"umls" containing "2015AB" (this is hardcoded in bin/loadUMLS.sh).

tmpdata -> this is a temporary space for intermediate files, and just 
needs to be somewhere you have space.

You can then run bin/all.sh.

If you want to run the prep for a language other than English, you can 
set the environment variable LANGS to the appropriate two letter code, 
for example "fr". It will default to English. If you want to do multiple 
languages, separate them with a space. Be sure to put inverted commas 
around the string, for example export LANGS="en fr".

A benchmarking file is automatically produced in the parent directory. To 
stop this, comment out the relevant lines in bin/all.sh

In the "scala" directory, the symbolic link "scala-latest-version" should
point to a scala installation.
