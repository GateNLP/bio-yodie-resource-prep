import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

/**
 ** Execute SQL statements on a HSQL database.
 ** This uses a simple approach where an empty line is considered to separate
 ** statements.
 ** Lines starting with // or -- are considered to be comments
 **
 ** NOTE: this programs does a SHUTDOWN and tries a SHUTDOWN when an 
 ** exception occurs. By default, AUTOCOMMIT will be true, but the 
 ** SQL file can change this and do commits separately. 
 */ 



object ExecuteSql {
  def main(args: Array[String]) = {
    
    if(args.size != 2) {
      System.err.println("parms: databaseFilePathPrefix statementfilename|-")
      System.exit(1)
    }
    
    val dbname = args(0)
    val statementfile = args(1)
    
    val source = 
      if(statementfile == "-") { 
        scala.io.Source.fromInputStream(System.in) 
      } else {
        scala.io.Source.fromFile(statementfile)        
      }
    

    val conn = utils.JdbcConnection.getConnection(dbname,false)
    val st = conn.createStatement()
    
    var statement = ""
    var statementToShow = ""
    var ignore = false
    source.getLines.foreach { line =>
      //System.err.println("ExecuteSql - got line: "+line)
      if(ignore) {
        System.err.println("ExecuteSql - After error, ignored line:\n"+line)
      } else {
        if(!line.matches("""\s*(//|--).*""")) {  // not a comment
          if(!line.matches("""\s*""")) {
            statement += " "+line
            statementToShow += "\n"+line
          } else {
            if(!statement.matches("""\s*""")) {
            try {
              System.err.println("ExecuteSql - Executing: "+statementToShow)
              val ret = st.execute(statement)
              System.err.println("ExecuteSql - OK, returned: "+ret)
            } catch {
              case e: Exception => {
                System.err.println("ExecuteSql - Exception when executing: "+statementToShow)
                System.err.println("ExecuteSql - Ignoring rest of file ...")
                e.printStackTrace(System.err)
                ignore = true
              }
            }
            statement = ""
            statementToShow = ""
            }
          }
        } else {
          System.err.println("ExecuteSqlDdl comment: "+line)
        }
      }
    }
    if(!statement.matches("""\s*""")) {
      try {
              System.err.println("ExecuteSql - Executing: "+statementToShow)
              val ret = st.execute(statement)
              System.err.println("ExecuteSql - OK, returned: "+ret)
      } catch {
              case e: Exception => {
                System.err.println("ExecuteSql - Exception when executing: "+statementToShow)
                System.err.println("ExecuteSql - Ignoring rest of file ...")
                e.printStackTrace(System.err)
                ignore = true
              }
      }    
    }
    st.execute("COMMIT");
    utils.JdbcConnection.shutdown()
    System.err.println("ExecuteSql completed")    
  }
}
