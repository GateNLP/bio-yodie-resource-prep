import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

// For each line consisting of key and value of the input TSV file,
// set the target value field in the target table to the value for rows
// with that key.
// We assume that both key and value are strings
// If a key is not in the target db, nothing is done, but the event is logged to stdout
//
// Parameters:
// Dbname,tblname,keyFieldName,valueFieldName of table to be updated
// Standard input should be a TSV file with exactly two string columns, containing key and value

object AddValueForKey {
  def main(args: Array[String]) = {
    
    if(args.size != 4) {
      System.err.println("AddValueForKey parms: inputDB inputTable keyField valueField < 2col_key_val.tsv")
      System.exit(1)
    }
    
    val dbname = args(0)
    val tablename = args(1)
    val keyField = args(2)
    val valueField = args(3)
    
    Class.forName("org.hsqldb.jdbcDriver")
    
    val conn = DriverManager.getConnection("jdbc:hsqldb:file:"+dbname,"","")
    
    val st = conn.createStatement()
    st.execute("SET FILES LOG FALSE")
    st.execute("SET FILES NIO SIZE 4096")  // depending on memory, try other values or remove
    st.execute("CHECKPOINT")
    st.execute("SET AUTOCOMMIT FALSE")
    
    val updateSt = conn.prepareStatement(
      "UPDATE "+tablename+" SET "+valueField+" = ? WHERE "+keyField+" = ? ;")
        
    // read TSV file with 2 columns from stdin, expecting key, value
    var nlines = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(cols.size != 2) {
        throw new RuntimeException("AddValueForKey: not 2 columns in tsv line "+nlines+": "+line)
      }
      updateSt.setString(1,cols(1))
      updateSt.setString(2,cols(0))
      try {
        updateSt.executeUpdate()
      } catch {
        case e: Exception => {
          System.err.println("AddValueForKey - Error when updating");
          System.err.println("Line: "+line);
          System.err.println("key: "+cols(0))
          System.err.println("value: "+cols(1))
          e.printStackTrace(System.err)
          System.err.println("IGNORED .... ")
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("AddValueForKey - processed: "+nlines)
        st.execute("COMMIT")
      }
    }
    st.execute("COMMIT")
    conn.createStatement().execute("SHUTDOWN")
    conn.close()
    System.err.println("AddValueForKey - Completed, total lines: "+nlines)    
  }
}
