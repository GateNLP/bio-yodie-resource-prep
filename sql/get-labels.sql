
--get-labels.sql

--Copyright (c) 1995-2016, The University of Sheffield. See the file
--COPYRIGHT.txt in the software or at
--http://gate.ac.uk/gate/COPYRIGHT.txt

--This file is part of Bio-YODIE (see 
--https://gate.ac.uk/applications/bio-yodie.html), and is free 
--software, licenced under the GNU Affero General Public License
--Version 3, 19 November 2007

--A copy of this licence is included in the distribution in the file
--LICENSE-AGPL3.html, and is also available at
--http://www.gnu.org/licenses/agpl-3.0-standalone.html

--G. Gorrell, 26 September 2016


SELECT DISTINCT(MRCONSO.STR) AS STR
FROM MRCONSO
INNER JOIN MRSTYSELECTED
ON MRCONSO.CUI=MRSTYSELECTED.CUI
INNER JOIN STYCLASS
ON MRSTYSELECTED.STY=STYCLASS.STY
WHERE STYCLASS.STYCLASS IN ###SELECTEDSTYCLASSES
AND MRCONSO.LAT IN (SELECT umlslabel
 FROM umls_labels_iso639_codes
 WHERE iso6391 IN ###LANGLIST)
ORDER BY MRCONSO.STR;


