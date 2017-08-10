import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException
import org.rogach.scallop._

// Execute a query on a database and write the result as TSV
// Program takes two arguments: path prefix of the database, filename of the
// select statement or "-"  to read the query(s) from standard input

object Db2Tsv {
  def main(args: Array[String]) = {
    
    class Opts(arguments: Seq[String]) extends ScallopConf(arguments) {
      banner("""Usage: Db2Tsv dbPathPrefix sqlfile|-|sqlstatement 
                 |Exports the rows resulting from the sqlfile/statement into the tsvfile or to standard output
                 |The second parameter is interpreted if it contains at at least two spaces and 
                 |starts with "select" and also contains ' from ' somewhere.""".stripMargin)
      var dbname = trailArg[String]("databaseFilePathPrefix",descr = "File path prefix of the database file",required = true)
      var sql = trailArg[String]("sqlfileOrSqlOrHyphen",descr="Either the path to a SQL file, or a hyphen to indicate standard input, or the quoted insert statement",required=true)
    }
    val opts = new Opts(args)
    
    
    val dbname = opts.dbname()
    val queryfile = opts.sql()
    
    val query = 
      if(queryfile == "-") { 
        scala.io.Source.fromInputStream(System.in).mkString 
      } else {
        if(queryfile.matches("""(?i)\s*select .+ from .+""")) {
          queryfile
        } else {
          scala.io.Source.fromFile(queryfile).mkString
        }
      }

    val conn = utils.JdbcConnection.getConnection(dbname,true)
    
    val selectSt = conn.prepareStatement(query)

    System.err.println("Executing query: "+query)
    val rs = selectSt.executeQuery()
    var nlines = 0
    System.err.println("Got a result set")
    while(rs.next()) {
      nlines += 1
      val nrCols = rs.getMetaData().getColumnCount()
      for (i <- 1 to nrCols) {
        print(rs.getString(i))
        if(i < nrCols) {
          print("\t")
        } else {
          print("\n")
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("Db2Tsv - retrieved: "+nlines)
      }
      
    }
    utils.JdbcConnection.shutdown();
    System.err.println("Db2Tsv - Total rows retrieved: "+nlines)
  }
}
