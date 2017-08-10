import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

// Lookup the value of column N in the table given, in the field "key" given and
// retrieve the value of field "value". This can result in zero, one or more rows.
// If there are zero rows, do what the "missing" options requires: leave row 
// unchanged or filter out.
// If there is one or more rows, output the original with the column replaced
// with each value of the result rows, so 1 to N output lines are created.

object TsvReplaceColumnFromDB {
  def main(args: Array[String]) = {
    
    if(args.size != 6) {
      System.err.println("TsvReplaceColumnFromDB parms: column-0-based databaseFilePathPrefix tablename keyField valueField writeUnchanged < tsvfile.tsv")
      System.exit(1)
    }
    
    val colnum = args(0).toInt
    val dbname = args(1)
    val tablename = args(2)
    val keyField = args(3)
    val valueField = args(4)
    
    val writeUnchanged = args(5).toBoolean
    
    val conn = utils.JdbcConnection.getConnection(dbname,readOnly = true)

    val selectSt = conn.prepareStatement("SELECT "+valueField+" FROM "+tablename+" WHERE "+keyField+" = ?;")
    
    var nlines = 0
    var nwritten = 0
    var nfound = 0
    var nreplaced = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      
      selectSt.setString(1,cols(colnum))
      try {
        val rs = selectSt.executeQuery()
        var haveOne = false
        var found = false
        while(rs.next()) {
          found = true
          haveOne = true
          var value = rs.getString(valueField)
          cols(colnum) = value
          System.out.println(cols.mkString("\t"))
          nwritten += 1
          nreplaced += 1
        } 
        if(found) { nfound += 1 }
        if(!haveOne && writeUnchanged) {
          System.out.println(cols.mkString("\t"))
          nwritten += 1
        }
      } catch {
        case e: Exception => {
          throw e;
        }
      }
      
      
      
      if((nlines % 100000) == 0) {
        System.err.println("TsvReplaceColumnFromDB - processed: "+nlines)
      }
    }
    utils.JdbcConnection.shutdown()
    System.err.println("TsvReplaceColumnFromDB - Completed, total read lines: "+nlines)    
    System.err.println("TsvReplaceColumnFromDB - Written lines: "+nwritten)    
    System.err.println("TsvReplaceColumnFromDB - Rows with found key: "+nfound)    
    System.err.println("TsvReplaceColumnFromDB - Written rows with key replaced: "+nreplaced)    
  }
}
