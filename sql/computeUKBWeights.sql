
--computeUKBWeights.sql

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


DROP TABLE IF EXISTS MRCOCSUBSET;

CREATE TABLE IF NOT EXISTS MRCOCSUBSET (
       CUI1 CHAR(8) NOT NULL,
       CUI2 CHAR(8) NOT NULL,
       FREQ INTEGER UNSIGNED NOT NULL
);

CREATE INDEX IF NOT EXISTS X_MRCOCSUBSET_CUI1 ON MRCOCSUBSET(CUI1);

DROP TABLE IF EXISTS MRCOCCUI1;

CREATE TABLE IF NOT EXISTS MRCOCCUI1 (CUI1 CHAR(8) NOT NULL);

INSERT INTO MRCOCCUI1 SELECT DISTINCT CUI1 FROM MRCOC2000SELECTED;

DROP TABLE IF EXISTS MRCOCCUI1SUBSET;

CREATE TABLE IF NOT EXISTS MRCOCCUI1SUBSET (CUI1 CHAR(8) NOT NULL);

CREATE INDEX IF NOT EXISTS X_MRCOCCUI1_CUI1 ON MRCOCCUI1(CUI1);

DROP TABLE IF EXISTS CUI2CUIWEIGHT;

CREATE TABLE IF NOT EXISTS CUI2CUIWEIGHT (
          CUI1 CHAR(8) NOT NULL,
          CUI2 CHAR(8) NOT NULL,
          WEIGHT INTEGER(10) NOT NULL
          );

CREATE INDEX IF NOT EXISTS X_CUI2CUIWEIGHT_CUI1 ON CUI2CUIWEIGHT(CUI1);

DROP ALIAS IF EXISTS COMPUTE_CUI2CUI_WEIGHTS;

CREATE ALIAS COMPUTE_CUI2CUI_WEIGHTS AS $$
int computeWeights(Connection cnx) throws SQLException {
  int chunksize=8192;
  int nbOfCUI1 = 0;
  int totalNbOfCUI1 = 0;
  int nbAffLines = 0;
  ResultSet rs=cnx.createStatement().executeQuery("SELECT COUNT(*) AS NB_OF_CUI1 FROM MRCOCCUI1;");
  if (rs.absolute(1)) { nbOfCUI1 = rs.getInt("NB_OF_CUI1"); };
  totalNbOfCUI1 = nbOfCUI1;
  do {
    nbAffLines=cnx.createStatement().executeUpdate("INSERT INTO MRCOCCUI1SUBSET SELECT CUI1 FROM MRCOCCUI1 LIMIT "+chunksize+";");
    nbAffLines=cnx.createStatement().executeUpdate("INSERT INTO MRCOCSUBSET SELECT CUI1, CUI2, FREQ FROM MRCOC2000SELECTED WHERE CUI1 IN (SELECT CUI1 FROM MRCOCCUI1SUBSET);");
    nbAffLines=cnx.createStatement().executeUpdate("INSERT INTO CUI2CUIWEIGHT SELECT CUI1, CUI2, SUM(FREQ) AS WEIGHT FROM MRCOCSUBSET GROUP BY CUI1, CUI2 HAVING WEIGHT>1;");
    nbAffLines=cnx.createStatement().executeUpdate("DELETE FROM MRCOCSUBSET;");
    nbAffLines=cnx.createStatement().executeUpdate("DELETE FROM MRCOCCUI1 WHERE CUI1 IN (SELECT CUI1 FROM MRCOCCUI1SUBSET);");
    nbAffLines=cnx.createStatement().executeUpdate("DELETE FROM MRCOCCUI1SUBSET;");
    rs=cnx.createStatement().executeQuery("SELECT COUNT(*) AS NB_OF_CUI1 FROM MRCOCCUI1;");
    if (rs.absolute(1)) { nbOfCUI1 = rs.getInt("NB_OF_CUI1"); };
  } while (nbOfCUI1>0);
  return totalNbOfCUI1;
}
$$;

CALL COMPUTE_CUI2CUI_WEIGHTS();

DROP TABLE IF EXISTS MRCOCCUI1SUBSET;

DROP TABLE IF EXISTS MRCOCCUI1;

DROP TABLE IF EXISTS MRCOCSUBSET;

