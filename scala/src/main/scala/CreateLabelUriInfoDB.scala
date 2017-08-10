import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

/**
 ** Load the LabelUriInfo DB/table from a TSV file.
 ** This expects the database and table already to exist!
 **
 ** This expects an input TSV file of exactly 7 columns
 */ 

object CreateLabelUriInfoDB {
  def main(args: Array[String]) = {

    if(args.size != 1) {
      System.err.println("parms: dbname")
      System.exit(1)
    }
    
    val dbname = args(0)
    val conn = utils.JdbcConnection.getConnection(dbname,false)
    
    
    val st = conn.createStatement()
        
    val insertSt = conn.prepareStatement(
      "INSERT INTO LabelUriInfo (label,uri,sources,origLabels,parenthesesInfo,redirInfo,disambInfo) VALUES(?,?,?,?,?,?,?);")
    val updateSt = conn.prepareStatement(
      "UPDATE LabelUriInfo SET sources = ?, origLabels =?, parenthesesInfo = ?, redirInfo = ?, disambInfo = ? WHERE label = ? AND uri = ?;")
    val findSt = conn.prepareStatement(
      "SELECT * FROM LabelUriInfo WHERE label = ? AND uri = ?;");
      
    // read TSV file with 6 columns from stdin
    var nlines = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(cols.size != 7) {
        throw new RuntimeException("not 7 columns in tsv line "+nlines+": "+line)
      }
      var label = cols(0)
      val uri = cols(1)
      val source = cols(2)
      val origLabel = cols(3)
      val parenthesesInfo = cols(4)
      val redirInfo = cols(6)
      val disambInfo = cols(5)
      
      // try to find a row for label/uri
      findSt.setString(1,label)
      findSt.setString(2,uri)
      val rs = findSt.executeQuery() 
      // check if we found something
      if(rs.next()) {
        // found something: update
        // first get the existing values for the info fields
        val db_sources = rs.getString(3)
        val db_origLabels = rs.getString(4)
        val db_parenthesesInfo = rs.getString(5)
        val db_redirInfo = rs.getString(6)
        val db_disambInfo = rs.getString(7)
        // make sure we only found one row: this must be unique
        if(rs.next()) {
          System.err.println("Found more than one row for "+label+"/"+uri)
          System.exit(1)
        }
        // check if the new values are already there, if not, add
        // TODO: this should be done more properly, e.g. order alphabetically, .. ?
        if(!db_sources.contains(source)) {
          updateSt.setString(1,db_sources+"|"+source)
        } else {
          updateSt.setString(1,db_sources)
        }
        if(!db_origLabels.contains(origLabel)) {
          updateSt.setString(2,db_origLabels+"|"+origLabel)
        } else {
          updateSt.setString(2,db_origLabels)
        }
        if(!db_parenthesesInfo.contains(parenthesesInfo)) {
          updateSt.setString(3,db_parenthesesInfo+"|"+parenthesesInfo)
        } else {
          updateSt.setString(3,db_parenthesesInfo)
        }
        if(!db_redirInfo.contains(redirInfo)) {
          updateSt.setString(4,db_redirInfo+"|"+redirInfo)
        } else {
          updateSt.setString(4,db_redirInfo)
        }
        if(!db_disambInfo.contains(disambInfo)) {
          updateSt.setString(5,db_disambInfo+"|"+disambInfo)
        } else {
          updateSt.setString(5,db_disambInfo)
        }
        updateSt.setString(6,label)
        updateSt.setString(7,uri)
        try {
          updateSt.executeUpdate()
        } catch {
          case e: Exception => {
            System.err.println("Error when updating");
            System.err.println("Line: "+line);
            System.err.println("current sources: "+db_sources)
            System.err.println("current labels: "+db_origLabels)
            System.err.println("current redir: "+db_redirInfo)
            System.err.println("current parenthesesInfo: "+db_parenthesesInfo)
            System.err.println("current disamb: "+db_disambInfo)
            e.printStackTrace(System.err)
            System.err.println("IGNORED .... ")
          }
        }
      } else { 
        // nothing found, create
        insertSt.setString(1,label)
        insertSt.setString(2,uri)
        insertSt.setString(3,source)
        insertSt.setString(4,origLabel)
        insertSt.setString(5,parenthesesInfo)
        insertSt.setString(6,redirInfo)
        insertSt.setString(7,disambInfo)
        try {
          insertSt.execute()
        } catch {
          case e: Exception => {
            System.err.println("Error when updating");
            System.err.println("Line: "+line);
            System.err.println("current sources: "+source)
            System.err.println("current labels: "+origLabel)
            System.err.println("current parenthesesInfo: "+parenthesesInfo)
            System.err.println("current redir: "+redirInfo)
            System.err.println("current disamb: "+disambInfo)
            e.printStackTrace(System.err)
            System.err.println("IGNORED ... ")
          }
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("CreateLabelUriInfoDB - processed: "+nlines)
        st.execute("COMMIT")
      }
    }
    
    conn.close()
    System.err.println("CreateLabelUriInfoDB - completed, total lines: "+nlines)    
  }
}
