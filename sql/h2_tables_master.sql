
--h2_tables_master.sql

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


DROP TABLE IF EXISTS umls_labels_iso639_codes;

CREATE TABLE IF NOT EXISTS umls_labels_iso639_codes (
  iso6391 CHAR(2),
  iso6393 CHAR(3),
  umlslabel CHAR(3));

INSERT INTO umls_labels_iso639_codes VALUES
('bg','bul','BUL'),
('cs','ces','CZE'), ('da','dan','DAN'), ('de','deu','GER'),
('el','ell','GRE'), ('en','eng','ENG'), ('es','spa','SPA'),
('et','est','EST'), ('eu','eus','BAQ'), ('fi','fin','FIN'),
('fr','fra','FRE'), ('it','ita','ITA'), ('ja','jpn','JPN'),
('he','heb','HEB'), ('hr','hrv','SCR'), ('hu','hun','HUN'),
('ko','kor','KOR'), ('lv','lav','LAV'), ('nl','nld','DUT'),
('no','nor','NOR'), ('pl','pol','POL'), ('pt','por','POR'),
('ru','rus','RUS'), ('sh','hbs','SCR'), ('sr','srp','SCR'),
('sv','swe','SWE'), ('tr','tur','TUR'), ('zh','zho','CHI');


DROP TABLE IF EXISTS MRCOLS;
CREATE TABLE MRCOLS (
    COL	varchar(40),
    DES	varchar(200),
    REF	varchar(40),
    MIN	int unsigned,
    AV	numeric(5,2),
    MAX	int unsigned,
    FIL	varchar(50),
    DTY	varchar(40)
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRCOLS.RRF',
    null,
    'fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRCONSO_ALLLANGS;
CREATE TABLE MRCONSO_ALLLANGS (
    CUI	char(8) NOT NULL,
    LAT	char(3) NOT NULL,
    TS	char(1) NOT NULL,
    LUI	varchar(10) NOT NULL,
    STT	varchar(3) NOT NULL,
    SUI	varchar(10) NOT NULL,
    ISPREF	char(1) NOT NULL,
    AUI	varchar(9) NOT NULL,
    SAUI	varchar(50),
    SCUI	varchar(100),
    SDUI	varchar(100),
    SAB	varchar(40) NOT NULL,
    TTY	varchar(40) NOT NULL,
    CODE	varchar(100) NOT NULL,
    STR	varchar(3000) NOT NULL,
    SRL	int unsigned NOT NULL,
    SUPPRESS	char(1) NOT NULL,
    CVF	int unsigned
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRCONSO.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);

DROP TABLE IF EXISTS MRCONSO;

CREATE TABLE MRCONSO (
    CUI	char(8) NOT NULL,
    LAT	char(3) NOT NULL,
    TS	char(1) NOT NULL,
    LUI	varchar(10) NOT NULL,
    STT	varchar(3) NOT NULL,
    SUI	varchar(10) NOT NULL,
    ISPREF	char(1) NOT NULL,
    AUI	varchar(9) NOT NULL,
    SAUI	varchar(50),
    SCUI	varchar(100),
    SDUI	varchar(100),
    SAB	varchar(40) NOT NULL,
    TTY	varchar(40) NOT NULL,
    CODE	varchar(100) NOT NULL,
    STR	varchar(3000) NOT NULL,
    SRL	int unsigned NOT NULL,
    SUPPRESS	char(1) NOT NULL,
    CVF	int unsigned
) AS SELECT
    MRCONSO_ALLLANGS.*
FROM
    umls_labels_iso639_codes
JOIN
    MRCONSO_ALLLANGS
    ON (umls_labels_iso639_codes.umlslabel=MRCONSO_ALLLANGS.LAT)
WHERE
    umls_labels_iso639_codes.iso6391 IN ###LANGLIST;

DROP TABLE IF EXISTS MRCONSO_ALLLANGS;


DROP TABLE IF EXISTS MRCUI;
CREATE TABLE MRCUI (
    CUI1	char(8) NOT NULL,
    VER	varchar(10) NOT NULL,
    REL	varchar(4) NOT NULL,
    RELA	varchar(100),
    MAPREASON	text,
    CUI2	char(8),
    MAPIN	char(1)
) AS SELECT SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRCUI.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRDEF;
CREATE TABLE MRDEF (
    CUI	char(8) NOT NULL,
    AUI	varchar(9) NOT NULL,
    ATUI	varchar(11) NOT NULL,
    SATUI	varchar(50),
    SAB	varchar(40) NOT NULL,
    DEF	text NOT NULL,
    SUPPRESS	char(1) NOT NULL,
    CVF	int unsigned
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRDEF.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRDOC;
CREATE TABLE MRDOC (
    DOCKEY	varchar(50) NOT NULL,
    VALUE	varchar(200),
    TYPE	varchar(50) NOT NULL,
    EXPL	text
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRDOC.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRFILES;
CREATE TABLE MRFILES (
    FIL	varchar(50),
    DES	varchar(200),
    FMT	text,
    CLS	int unsigned,
    RWS	int unsigned,
    BTS	bigint
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRFILES.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRHIER;
CREATE TABLE MRHIER (
    CUI	char(8) NOT NULL,
    AUI	varchar(9) NOT NULL,
    CXN	int unsigned NOT NULL,
    PAUI	varchar(10),
    SAB	varchar(40) NOT NULL,
    RELA	varchar(100),
    PTR	text,
    HCD	varchar(100),
    CVF	int unsigned
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRHIER.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRRANK;
CREATE TABLE MRRANK (
    RANK	int unsigned NOT NULL,
    SAB	varchar(40) NOT NULL,
    TTY	varchar(40) NOT NULL,
    SUPPRESS	char(1) NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRRANK.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRREL;
CREATE TABLE MRREL (
    CUI1	char(8) NOT NULL,
    AUI1	varchar(9),
    STYPE1	varchar(50) NOT NULL,
    REL	varchar(4) NOT NULL,
    CUI2	char(8) NOT NULL,
    AUI2	varchar(9),
    STYPE2	varchar(50) NOT NULL,
    RELA	varchar(100),
    RUI	varchar(10) NOT NULL,
    SRUI	varchar(50),
    SAB	varchar(40) NOT NULL,
    SL	varchar(40) NOT NULL,
    RG	varchar(10),
    DIR	varchar(1),
    SUPPRESS	char(1) NOT NULL,
    CVF	int unsigned
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRREL.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRSAB;
CREATE TABLE MRSAB (
    VCUI	char(8),
    RCUI	char(8),
    VSAB	varchar(40) NOT NULL,
    RSAB	varchar(40) NOT NULL,
    SON	text NOT NULL,
    SF	varchar(40) NOT NULL,
    SVER	varchar(40),
    VSTART	char(8),
    VEND	char(8),
    IMETA	varchar(10) NOT NULL,
    RMETA	varchar(10),
    SLC	text,
    SCC	text,
    SRL	int unsigned NOT NULL,
    TFR	int unsigned,
    CFR	int unsigned,
    CXTY	varchar(50),
    TTYL	varchar(400),
    ATNL	text,
    LAT	char(3),
    CENC	varchar(40) NOT NULL,
    CURVER	char(1) NOT NULL,
    SABIN	char(1) NOT NULL,
    SSN	text NOT NULL,
    SCIT	text NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRSAB.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRSAT;
CREATE TABLE MRSAT (
    CUI	char(8) NOT NULL,
    LUI	varchar(10),
    SUI	varchar(10),
    METAUI	varchar(100),
    STYPE	varchar(50) NOT NULL,
    CODE	varchar(100),
    ATUI	varchar(11) NOT NULL,
    SATUI	varchar(50),
    ATN	varchar(100) NOT NULL,
    SAB	varchar(40) NOT NULL,
    ATV	text,
    SUPPRESS	char(1) NOT NULL,
    CVF	int unsigned
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRSAT.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRSTY;
CREATE TABLE MRSTY (
    CUI	char(8) NOT NULL,
    TUI	char(4) NOT NULL,
    STN	varchar(100) NOT NULL,
    STY	varchar(50) NOT NULL,
    ATUI	varchar(11) NOT NULL,
    CVF	int unsigned
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRSTY.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRXNS_ENG;
CREATE TABLE MRXNS_ENG (
    LAT	char(3) NOT NULL,
    NSTR	text NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRXNS_ENG.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRXNW_ENG;
CREATE TABLE MRXNW_ENG (
    LAT	char(3) NOT NULL,
    NWD	varchar(100) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRXNW_ENG.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRAUI;
CREATE TABLE MRAUI (
    AUI1	varchar(9) NOT NULL,
    CUI1	char(8) NOT NULL,
    VER	varchar(10) NOT NULL,
    REL	varchar(4),
    RELA	varchar(100),
    MAPREASON	text NOT NULL,
    AUI2	varchar(9) NOT NULL,
    CUI2	char(8) NOT NULL,
    MAPIN	char(1) NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRAUI.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS MRXW_ENG;
CREATE TABLE MRXW_ENG (
    LAT	char(3) NOT NULL,
    WD	varchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/MRXW_ENG.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS AMBIGSUI;
CREATE TABLE AMBIGSUI (
    SUI	varchar(10) NOT NULL,
    CUI	char(8) NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/AMBIGSUI.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


DROP TABLE IF EXISTS AMBIGLUI;
CREATE TABLE AMBIGLUI (
    LUI	varchar(10) NOT NULL,
    CUI	char(8) NOT NULL
) AS SELECT * FROM CSVREAD(
    '###UMLSLOC###UMLSVERSION/META/AMBIGLUI.RRF',
    null,
    'charset=UTF-8 fieldSeparator=| escape= fieldDelimiter= '
);


