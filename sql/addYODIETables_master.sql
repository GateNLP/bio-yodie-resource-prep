
--addYODIETables_master.sql

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


-- Rudolf Cardinal, 15 Sep 2020:
-- For speed (http://h2database.com/html/performance.html#fast_import):
SET LOG 0;  -- disablethe transaction log
SET CACHE_SIZE 65536;  -- a large cache is faster; units are KB
SET LOCK_MODE 0;  -- disable locking
SET UNDO_LOG 0;  -- disable the session undo log
-- ... and, as below, use CREATE TABLE (...) AS SELECT ..., rather than
--     CREATE TABLE (...); INSERT INTO ... SELECT ...


--DROP TABLE IF EXISTS MESHFREQ;
--CREATE TABLE MESHFREQ (
--    FREQ int unsigned,
--    CUI	varchar(40)
--);
--INSERT INTO MESHFREQ SELECT * FROM CSVREAD(
--'###SRCS/Mesh-freq/freq-cui.csv');

--CREATE INDEX X_MESHFREQ_CUI ON MESHFREQ(CUI);


DROP TABLE IF EXISTS PAGERANK;
CREATE TABLE PAGERANK (
    CUI varchar(40),
    PROB float
) AS SELECT * FROM CSVREAD(
    '###SRCS/umls_ukbstatic.txt',
    null,
    'fieldSeparator=' || CHAR(9)
);

CREATE INDEX X_PAGERANK_CUI ON PAGERANK(CUI);


DROP TABLE IF EXISTS CUIFREQ;
CREATE TABLE CUIFREQ (
    CUI char(8) NOT NULL,
    COUNT int unsigned,
    NORM float
) AS SELECT * FROM CSVREAD(
    '###TMPDATA/cuifreq.tsv',
    null,
    'fieldSeparator=' || CHAR(9)
);

CREATE INDEX X_CUIFREQ_CUI ON CUIFREQ(CUI);

DROP TABLE IF EXISTS LABELFREQ;
CREATE TABLE LABELFREQ (
    LABEL char(100) NOT NULL,
    CUI char(8) NOT NULL,
    COUNT int unsigned,
    NORM float
) AS SELECT * FROM CSVREAD(
    '###TMPDATA/labelfreq.tsv',
    null,
    'fieldSeparator=' || CHAR(9)
);

CREATE INDEX X_LABELFREQ_CUI ON LABELFREQ(CUI);


DROP TABLE IF EXISTS STYCLASS;
CREATE TABLE IF NOT EXISTS STYCLASS (
    STYCLASS CHAR(3) NOT NULL,
    STY VARCHAR(50) NOT NULL,
) AS SELECT * FROM CSVREAD(
    '###TMPDATA/styclasses.csv',
    'STYCLASS,STY',
    'fieldDelimiter='''
);

CREATE INDEX X_STYCLASS_STY ON STYCLASS(STY);

DROP TABLE IF EXISTS MRSTYSELECTED;
CREATE TABLE MRSTYSELECTED (
    CUI char(8) NOT NULL,
    TUI char(4) NOT NULL,
    STN varchar(100) NOT NULL,
    STY varchar(50) NOT NULL,
    ATUI        varchar(11) NOT NULL,
    CVF int unsigned
) AS SELECT
    CUI, TUI, STN, STY, ATUI, CVF
FROM MRSTY
WHERE STY IN (
    SELECT DISTINCT STY
    FROM STYCLASS WHERE
    STYCLASS IN ###SELECTEDSTYCLASSES
);

CREATE INDEX X_MRSTYSELECTED_STY ON MRSTYSELECTED(STY);
CREATE INDEX X_MRSTYSELECTED_CUI ON MRSTYSELECTED(CUI);






DROP TABLE IF EXISTS PREFERREDLABELS;
CREATE TABLE PREFERREDLABELS (
 cui char(8) NOT NULL,
 preflabel char(100),
 score integer(10),
 ispref char(1),
 maxscore integer(10),
 minlen integer(10),
 VOCABCOUNT int,
 VOCABS varchar(2500)
) AS SELECT
    mrconso.cui,
    substring(mrconso.str,1,100),
    '10',
    mrconso.ispref,
    '0',
    '0',
    COUNT(MRCONSO.SAB) AS VOCABCOUNT,
    GROUP_CONCAT(MRCONSO.SAB) AS VOCABS
FROM
    mrconso
GROUP BY
     MRCONSO.CUI, mrconso.ispref, mrconso.str;

update PREFERREDLABELS set score = score - 3*((length(preflabel) - length(
replace(
 (replace(
  (replace(
   (replace(
    (replace(
     (replace(
      (replace(
       (replace(
        (replace(
         (replace(
          (replace(preflabel, '-', '')),
         '0', '')),
        '1', '')),
       '2', '')),
      '3', '')),
     '4', '')),
    '5', '')),
   '6', '')),
  '7', '')),
 '8', '')),
'9', ''))
));

update PREFERREDLABELS set score = score +20 where ispref = 'Y';

update PREFERREDLABELS set score = score - ((8-length(preflabel))/3) where length(preflabel) < 8;
update PREFERREDLABELS set score = score - (length(preflabel)/13) where length(preflabel) > 12;

update PREFERREDLABELS set score = score + 1 
 where ucase(substring(preflabel,1,1)) = substring(preflabel,1,1)
 and not ucase(substring(preflabel,2,2)) = substring(preflabel,2,2);

drop table if exists maxscores;

create table maxscores(
    cui char(8),
    maxscore integer(10)
) AS select
    cui, max(score)
from
    PREFERREDLABELS
group by
     cui;

drop index if exists maxscore_cui;
drop index if exists preflabel_cui;

create index maxscore_cui on maxscores (cui);
create index PREFERREDLABELS_cui on PREFERREDLABELS (cui);

update PREFERREDLABELS set maxscore = (select maxscore from maxscores where maxscores.cui = PREFERREDLABELS.cui);

delete from PREFERREDLABELS where score < maxscore;

drop table if exists minlens;

create table minlens(
    cui char(8),
    minlen integer(10)
) AS select
    cui, min(length(preflabel))
from
    PREFERREDLABELS
group by
    cui;

drop index if exists minlens_cui;

create index minlens_cui on minlens (cui);

update PREFERREDLABELS set minlen = (select minlen from minlens where minlens.cui = PREFERREDLABELS.cui);

delete from PREFERREDLABELS where length(preflabel) > minlen;

DROP TABLE IF EXISTS PREF;
CREATE TABLE PREF (
    CUI char(8) NOT NULL,
    PREF char(500) NOT NULL
) AS select
    cui, min(preflabel)
from
    preferredlabels
group by
    cui, lcase(preflabel);

create index pref_cui on pref (cui);

DROP TABLE IF EXISTS PREFFINAL;
CREATE TABLE PREFFINAL (
    CUI char(8) NOT NULL,
    PREF char(500) NOT NULL,
--    VOCABCOUNT int,
--    VOCABS varchar(2500)
) AS select
    cui,
    min(pref)
--    0,
--    ''
FROM
    pref
GROUP BY cui;

create index preffinal_cui on preffinal (cui);

--update PREFFINAL set VOCABCOUNT = (select min(VOCABCOUNT) from PREFERREDLABELS where PREFFINAL.cui = PREFERREDLABELS.cui);
--update PREFFINAL set VOCABS = (select min(VOCABS) from PREFERREDLABELS where PREFFINAL.cui = PREFERREDLABELS.cui);



DROP TABLE IF EXISTS CUIINFO;
CREATE TABLE CUIINFO (
    CUI char(8) NOT NULL,
    TUIS char(500) NOT NULL,
    STYS varchar(5000) NOT NULL,
    PREF varchar(100),
    VOCABS varchar(2500),
    VOCABCOUNT int,
    CRISFREQ int,
    CRISNORM float,
    PAGERANK float
) AS SELECT DISTINCT
    MRSTYSELECTED.CUI,
    GROUP_CONCAT(DISTINCT(MRSTYSELECTED.TUI)),
    GROUP_CONCAT(DISTINCT(MRSTYSELECTED.STY)),
    MIN(PREFFINAL.PREF),
    GROUP_CONCAT(DISTINCT(MRCONSO.SAB)) AS VOCABS,
    COUNT(MRCONSO.SAB) AS VOCABCOUNT,
    --  PREFFINAL.VOCABS,
    --  PREFFINAL.VOCABCOUNT,
    CUIFREQ.COUNT,
    CUIFREQ.NORM,
    PAGERANK.PROB
FROM
   MRSTYSELECTED
LEFT OUTER JOIN PREFFINAL ON PREFFINAL.CUI = MRSTYSELECTED.CUI
LEFT OUTER JOIN CUIFREQ ON CUIFREQ.CUI = MRSTYSELECTED.CUI
LEFT OUTER JOIN PAGERANK ON PAGERANK.CUI = MRSTYSELECTED.CUI
LEFT OUTER JOIN MRCONSO ON MRCONSO.CUI = MRSTYSELECTED.CUI
--GROUP BY MRSTYSELECTED.CUI, PREFFINAL.VOCABS;
GROUP BY MRSTYSELECTED.CUI;

CREATE INDEX X_CUIINFO_CUI ON CUIINFO(CUI);

DROP TABLE IF EXISTS LABELINFO;
CREATE TABLE LABELINFO (
    LABEL varchar(100),
    CUI char(8),
    STYS varchar(5000),
    TUIS char(500),
    PREF varchar(100),
    LABELVOCABS varchar(5000),
    CUIVOCABS varchar(5000),
    CRISLABELCUIFREQ int,
    CRISLABELCUINORM float,
    CRISCUIFREQ int,
    CRISCUINORM float,
    PAGERANK float
) AS SELECT DISTINCT
    MRCONSO.STR AS LABEL,
    MRCONSO.CUI,
    CUIINFO.STYS,
    CUIINFO.TUIS,
    MIN(CUIINFO.PREF),
    GROUP_CONCAT(DISTINCT(MRCONSO.SAB)),
    CUIINFO.VOCABS,
    LABELFREQ.COUNT,
    LABELFREQ.NORM,
    CUIINFO.CRISFREQ,
    CUIINFO.CRISNORM,
    CUIINFO.PAGERANK
FROM
    MRCONSO
INNER JOIN CUIINFO ON MRCONSO.CUI = CUIINFO.CUI
LEFT OUTER JOIN LABELFREQ ON MRCONSO.CUI = LABELFREQ.CUI AND MRCONSO.STR = LABELFREQ.LABEL 
WHERE LENGTH(MRCONSO.STR)<100
AND MRCONSO.STR regexp '[a-zA-Z][A-Z]' and MRCONSO.STR not like '% %'
GROUP BY MRCONSO.CUI, LABEL, CUIINFO.STYS, LABELFREQ.COUNT, LABELFREQ.NORM
ORDER BY LABEL;

INSERT INTO LABELINFO SELECT DISTINCT
    LCASE(MRCONSO.STR) AS LABEL,
    MRCONSO.CUI,
    CUIINFO.STYS,
    CUIINFO.TUIS,
    MIN(CUIINFO.PREF),
    GROUP_CONCAT(DISTINCT(MRCONSO.SAB)),
    CUIINFO.VOCABS,
    LABELFREQ.COUNT,
    LABELFREQ.NORM,
    CUIINFO.CRISFREQ,
    CUIINFO.CRISNORM,
    CUIINFO.PAGERANK
FROM
    MRCONSO
INNER JOIN CUIINFO ON MRCONSO.CUI = CUIINFO.CUI
LEFT OUTER JOIN LABELFREQ ON MRCONSO.CUI = LABELFREQ.CUI AND MRCONSO.STR = LABELFREQ.LABEL 
WHERE LENGTH(MRCONSO.STR)<100
AND (MRCONSO.STR not regexp '[a-zA-Z][A-Z]' OR MRCONSO.STR like '% %')
GROUP BY MRCONSO.CUI, LABEL, CUIINFO.STYS, LABELFREQ.COUNT, LABELFREQ.NORM
ORDER BY LABEL;


CREATE INDEX X_LABELINFO_CUI ON LABELINFO(CUI);
CREATE INDEX X_LABELINFO_LABEL ON LABELINFO(LABEL);

DROP TABLE IF EXISTS PREFERREDLABELS;
DROP TABLE IF EXISTS PREF;

--DROP TABLE IF EXISTS MRRELRESTRICTED;

--CREATE TABLE IF NOT EXISTS MRRELRESTRICTED (
--          CUI1 VARCHAR(100) NOT NULL,
--          CUI2 VARCHAR(100) NOT NULL
--          );

--INSERT INTO MRRELRESTRICTED SELECT CUI1, CUI2 FROM MRREL WHERE CUI1<>CUI2;

--CREATE INDEX IF NOT EXISTS X_MRRELRESTRICTED_CUI1 ON MRRELRESTRICTED(CUI1);

--DROP TABLE IF EXISTS MRRELRESTRICTEDSUBSET;

--CREATE TABLE IF NOT EXISTS MRRELRESTRICTEDSUBSET (
--          CUI1 VARCHAR(100) NOT NULL,
--          CUI2 VARCHAR(100) NOT NULL
--          );

--CREATE INDEX IF NOT EXISTS X_MRRELRESTRICTEDSUBSET_CUI1 ON MRRELRESTRICTEDSUBSET(CUI1);

--DROP TABLE IF EXISTS MRRELCUI1;

--CREATE TABLE IF NOT EXISTS MRRELCUI1 (CUI1 VARCHAR(100) NOT NULL);

--INSERT INTO MRRELCUI1 SELECT DISTINCT CUI1 FROM MRRELRESTRICTED;

--CREATE INDEX IF NOT EXISTS X_MRRELCUI1_CUI1 ON MRRELCUI1(CUI1);

--DROP TABLE IF EXISTS MRRELCUI1SUBSET;

--CREATE TABLE IF NOT EXISTS MRRELCUI1SUBSET (CUI1 VARCHAR(100) NOT NULL);

--CREATE INDEX IF NOT EXISTS X_MRRELCUI1SUBSET_CUI1 ON MRRELCUI1SUBSET(CUI1);


--DROP TABLE IF EXISTS RELATIONCOUNTS;

--CREATE TABLE IF NOT EXISTS RELATIONCOUNTS (
--          CUI1 VARCHAR(100) NOT NULL,
--          CUI2 VARCHAR(100) NOT NULL,
--          COUNT INTEGER(10) NOT NULL
--          );

--DROP ALIAS IF EXISTS COUNTS_RELATIONS;

--CREATE ALIAS COUNTS_RELATIONS AS $$
--int countsRelations(Connection cnx) throws SQLException {
--  int chunksize=262144;
--  int nbOfCUI1 = 0;
--  int totalNbOfCUI1 = 0;
--  int nbAffLines = 0;
--  ResultSet rs=cnx.createStatement().executeQuery("SELECT COUNT(*) AS NB_OF_CUI1 FROM MRRELCUI1;");
--  if (rs.absolute(1)) { nbOfCUI1 = rs.getInt("NB_OF_CUI1"); };
--  totalNbOfCUI1 = nbOfCUI1;
--  do {
--    nbAffLines=cnx.createStatement().executeUpdate("INSERT INTO MRRELCUI1SUBSET SELECT CUI1 FROM MRRELCUI1 LIMIT "+chunksize+";");
--    nbAffLines=cnx.createStatement().executeUpdate("INSERT INTO MRRELRESTRICTEDSUBSET SELECT MRRELRESTRICTED.CUI1, MRRELRESTRICTED.CUI2 FROM MRRELRESTRICTED WHERE MRRELRESTRICTED.CUI1 IN (SELECT CUI1 FROM MRRELCUI1SUBSET);");
--    nbAffLines=cnx.createStatement().executeUpdate("INSERT INTO RELATIONCOUNTS SELECT CUI1, CUI2, COUNT(*) FROM MRRELRESTRICTEDSUBSET GROUP BY CUI1, CUI2 HAVING COUNT(*)>1;");
--    nbAffLines=cnx.createStatement().executeUpdate("DELETE FROM MRRELRESTRICTEDSUBSET;");
--    nbAffLines=cnx.createStatement().executeUpdate("DELETE FROM MRRELCUI1 WHERE CUI1 IN (SELECT CUI1 FROM MRRELCUI1SUBSET);");
--    nbAffLines=cnx.createStatement().executeUpdate("DELETE FROM MRRELRESTRICTED WHERE CUI1 IN (SELECT CUI1 FROM MRRELCUI1SUBSET);");
--    nbAffLines=cnx.createStatement().executeUpdate("DELETE FROM MRRELCUI1SUBSET;");
--    rs=cnx.createStatement().executeQuery("SELECT COUNT(*) AS NB_OF_CUI1 FROM MRRELCUI1;");
--    if (rs.absolute(1)) { nbOfCUI1 = rs.getInt("NB_OF_CUI1"); };
--  } while (nbOfCUI1>0);
--  return totalNbOfCUI1;
--}
--$$;

--CALL COUNTS_RELATIONS();

--DROP INDEX IF EXISTS X_MRRELCUI1SUBSET_CUI1;

--DROP TABLE IF EXISTS MRRELCUI1SUBSET;

--DROP INDEX IF EXISTS X_MRRELCUI1_CUI1;

--DROP TABLE IF EXISTS MRRELCUI1;

--DROP INDEX IF EXISTS X_MRRELRESTRICTEDSUBSET_CUI1;

--DROP TABLE IF EXISTS MRRELRESTRICTEDSUBSET;

--DROP INDEX IF EXISTS X_MRRELRESTRICTED_CUI1;

--DROP TABLE IF EXISTS MRRELRESTRICTED;

--DROP ALIAS IF EXISTS COUNTS_RELATIONS;


--CREATE INDEX IF NOT EXISTS X_RELATIONCOUNTS_CUI1 ON RELATIONCOUNTS(CUI1);
--CREATE INDEX IF NOT EXISTS X_RELATIONCOUNTS_CUI2 ON RELATIONCOUNTS(CUI2);

