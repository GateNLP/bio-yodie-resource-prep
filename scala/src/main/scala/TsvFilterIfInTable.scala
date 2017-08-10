import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

// Given a 0-based column, check if the value in that column is (not) in 
// a databse/table/field. If the test succeeds, filter and do not output the row.

object TsvFilterIfInTable {
  def main(args: Array[String]) = {
    
    if(args.size != 5) {
      System.err.println("TsvFilterIfInTable parms: column-0-based databaseFilePathPrefix tablename keyField filterIfMissing < tsvfile.tsv")
      System.exit(1)
    }
    
    val colnum = args(0).toInt
    val dbname = args(1)
    val tablename = args(2)
    val keyField = args(3)
    
    val filterIfMissing = args(4).toBoolean
    
    val conn = utils.JdbcConnection.getConnection(dbname,readOnly = true)

    val selectSt = conn.prepareStatement("SELECT 1 FROM "+tablename+" WHERE "+keyField+" = ?;")
    
    var nlines = 0
    var nwritten = 0
    var nfiltered = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      
      selectSt.setString(1,cols(colnum))
      try {
        val rs = selectSt.executeQuery()
        var haveOne = rs.next()
        if(!haveOne && filterIfMissing) {
          nfiltered += 1
        } else if(haveOne && !filterIfMissing) {
          nfiltered += 1
        } else {
          nwritten += 1
          System.out.println(cols.mkString("\t"))
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
    System.err.println("TsvReplaceColumnFromDB - Completed, total lines: "+nlines)    
    System.err.println("TsvReplaceColumnFromDB - Filtered lines: "+nfiltered)    
    System.err.println("TsvReplaceColumnFromDB - Written lines: "+nwritten)    
  }
}
