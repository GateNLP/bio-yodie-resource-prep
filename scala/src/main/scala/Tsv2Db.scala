import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException
import org.rogach.scallop._

// it works like this: the SQL to insert into the table needs to be
// stored somewhere. then each line is inserted separately using that
// prepared statement. 
// The TSV file is read from stdin

object Tsv2Db {
  
  val progDesc = "Import a TSV into a table"
  val progName = this.getClass()
  
   
  def main(args: Array[String]) = {
    
    class Opts(arguments: Seq[String]) extends ScallopConf(arguments) {
      banner("""Usage: Tsv2Db dbPathPrefix sqlfile|-|sqlstatement tsvfile|- [0-based_commaseparated_fieldnumbers]
                 |Imports the rows from the given tsv file according to the sql statement given.
                 |The second parameter is interpreted if it contains at at least two spaces and 
                 |starts with "insert" and also contains at least one opening parenthesis.""".stripMargin)
      var exitOnError = opt[Boolean]("exitOnError",'x',descr="If there is an exception abort, if not given try to continue",default=Some(false),required=false);
      var silentDupes = opt[Boolean]("silentDupes",'q',descr="If there is an exception about a duplicate row, do not abort and do not show an error message, just count the error",default=Some(false),required=false);
      var dbname = trailArg[String]("databaseFilePathPrefix",descr = "File path prefix of the database file",required = true)
      var sql = trailArg[String]("sqlfileOrSqlOrHyphen",descr="Either the path to a SQL file, or a hyphen to indicate standard input, or the quoted insert statement",required=true)
      var tsv = trailArg[String]("tsvFileOrHyphen",descr="Either the path to the TSV file, or a hyphen to indicate standard input.",required=true)
      var fields = trailArg[String]("fields0based",descr="A comma separated list of 0-based field index numbers",required=false,default=Some(""))
    }
    val opts = new Opts(args)
    
    val dbname = opts.dbname()
    val sqlfile = opts.sql()
    val tsvfile = opts.tsv()
    val fieldnumberlist = opts.fields()
    val haveIndices = !fieldnumberlist.isEmpty
    val exitOnError = opts.exitOnError()
    val silentDupes = opts.silentDupes()
        
    // if we got field numbers, convert to array of indices
    val indices = if(haveIndices) fieldnumberlist.split(",") map { _.toInt } else Array[Int]()
    
    val sql = 
      if(sqlfile == "-") { 
        scala.io.Source.fromInputStream(System.in).mkString 
      } else {
        if(sqlfile.matches("""(?i)\s*(?:insert|merge) .+\(.+""")) {
          sqlfile
        } else {
          scala.io.Source.fromFile(sqlfile).mkString
        }
      }
    val tsvSource = 
      if(tsvfile == "-") {
        if(sqlfile == "-") {
          System.err.println(progName+": sqlfile and tsvfile cannot be both '-' indicating stdin")
          System.exit(1)
        }
        scala.io.Source.fromInputStream(System.in) 
      } else {
        scala.io.Source.fromFile(tsvfile)        
      }
    
    
    val conn = utils.JdbcConnection.getConnection(dbname,false)
    
    val insertSt = conn.prepareStatement(sql)
    val commitSt = conn.prepareStatement("COMMIT")

       
    
    var nlines = 0
    var nerrors = 0
    var nduplicates = 0
    var nwritten = 0
    tsvSource.getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(haveIndices) {
        0 until indices.size map { i =>
          insertSt.setObject(i+1,cols(indices(i)))
        }
      } else {
        0 until cols.size map { i =>
          insertSt.setObject(i+1,cols(i))
        }
      }
      try {
        insertSt.execute()
        nwritten += 1
      } catch {
        case e: Exception => {
          var haveDupe = false
          if(e.getMessage().startsWith("Unique index or primary key violation")) {
            nduplicates += 1
            haveDupe = true
          }
          if(!haveDupe || !silentDupes) {
            System.err.println(progName+" - Error when inserting line number "+nlines+": "+line);
            e.printStackTrace(System.err)
          }
          if(exitOnError && !(haveDupe && silentDupes)) {
            System.err.println(progName+" - ABORTING")
            utils.JdbcConnection.shutdownError()
            System.exit(1)
          } else {
            if(!haveDupe || !silentDupes) {
              System.err.println(progName+" - Error ignored, continuing")
            }
            nerrors += 1
          }
        }
      }

      if((nlines % 100000) == 0) {
        System.err.println(progName+" - read: "+nlines)
        //commitSt.execute()
      }      
    }
    commitSt.execute()
    utils.JdbcConnection.shutdown()
    System.err.println(progName+" - Total rows read: "+nlines)
    System.err.println(progName+" - Total rows written: "+nwritten)
    if(nerrors > 0) {
      System.err.println(progName+": TOTAL ERRORS: "+nerrors)
      System.err.println(progName+": OF WHICH DUPLICATES: "+nduplicates)
    }
  }
  
  
  
  
}
