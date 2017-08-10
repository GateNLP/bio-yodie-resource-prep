import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

// Read tsv file, lookup column number args(1) in the disambiguations table of database
// args(0) and for each row found, replace with the disambiguated URI and write row, also add a new column
// that contains either "disamb" or "nondisamb"

object ReplaceUrisByDisambiguations {
  def main(args: Array[String]) = {
    
    if(args.size != 2) {
      System.err.println("parms: databaseFilePathPrefux, uri-column-number (0-based)")
      System.exit(1)
    }
    
    val dbname = args(0)
    val uricolnr = args(1).toInt
    
    val conn = utils.JdbcConnection.getConnection(dbname,true)

    val selectSt = conn.prepareStatement("SELECT toUri FROM disambiguations WHERE fromUri = ?;")
        
    // read TSV file with 2 columns from stdin, expecting URI, URI where redirected to
    var nlines = 0
    var ndisamb = 0
    var nnondisamb = 0    
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(cols.size <= uricolnr) {
        throw new RuntimeException("not at least this many columns in tsv line "+(uricolnr+1)+": "+line)
      }
      selectSt.setString(1,cols(uricolnr))
      try {
        val rs = selectSt.executeQuery()
        var isDisamb = false
        while(rs.next()) {
          isDisamb = true
          var disambUri = rs.getString("toUri")
          cols(uricolnr) = disambUri
          System.out.println(cols.mkString("\t")+"\tdisamb")
          ndisamb += 1
        } 
        if(!isDisamb) {
          System.out.println(line+"\tnodisamb")
          nnondisamb += 1
        }
      } catch {
        case e: Exception => {
          throw e;
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("ReplaceUrisByDisambiguations - processed: "+nlines)
      }
    }
    utils.JdbcConnection.shutdown()    
    System.err.println("ReplaceUrisByDisambiguations - Completed, total lines: "+nlines+" of which are disambs: "+ndisamb)    
  }
}
