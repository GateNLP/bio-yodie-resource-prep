
--nlp-subset.sql

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

DROP TABLE IF EXISTS MRCONSOSUBSETTED;

CREATE TABLE MRCONSOSUBSETTED (
    CUI char(8) NOT NULL,
    LAT char(3) NOT NULL,
    TS  char(1) NOT NULL,
    LUI varchar(10) NOT NULL,
    STT varchar(3) NOT NULL,
    SUI varchar(10) NOT NULL,
    ISPREF      char(1) NOT NULL,
    AUI varchar(9) NOT NULL,
    SAUI        varchar(50),
    SCUI        varchar(100),
    SDUI        varchar(100),
    SAB varchar(40) NOT NULL,
    TTY varchar(40) NOT NULL,
    CODE        varchar(100) NOT NULL,
    STR varchar(3000) NOT NULL,
    SRL int unsigned NOT NULL,
    SUPPRESS    char(1) NOT NULL,
    CVF int unsigned
) AS SELECT
    *
FROM
    MRCONSO
WHERE
    BITAND(CVF, 256) <> 0;

DROP TABLE IF EXISTS MRCONSO;

ALTER TABLE MRCONSOSUBSETTED RENAME TO MRCONSO;
