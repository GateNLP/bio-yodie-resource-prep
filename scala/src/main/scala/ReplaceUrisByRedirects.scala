import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException
import org.rogach.scallop._;

// Read tsv file, lookup column number args(1) in the redirs table of database
// args(0) and replace with the redirected URI if found, add a new column
// that contains either "redir" or "nonredir"

// if the optional flag -a is given, add an additional row with the redirected url
// if the optional flag -c N is given the "redir/nonredir" infor will be placed
// into that column (which is added, if needed)

// test on my workstation: 5M rows successfully looked up in 252 seconds,
// thats about 20K lookups/sec.

object ReplaceUrisByRedirects {
  def main(args: Array[String]) = {
    
    class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
      val addRows = opt[Boolean]("addRows",'a',descr="Add rows with the redirected URI instead of replacing rows",required=false)
      val infoCol = opt[Int]("infoCol",'c',descr="column number of the info column that contains redir/nonredir",default=Some(-1),required=false)
      val noInfoCol = opt[Boolean]("noInfoCol",'n',descr="Do not add an info column at all",default=Some(false),required=false)
      val dbPrefix = trailArg[String](required=true)
      val uriCol = trailArg[Int](required=true,descr="0-based column nr for the column that contains the URI to be redirected" )
    }
    val conf = new Conf(args)
    
    val dbname = conf.dbPrefix()
    val uricolnr = conf.uriCol()
    
    var infoCol = conf.infoCol()
    var addRows = conf.addRows()
    var noInfoCol = conf.noInfoCol()
    
    val conn = utils.JdbcConnection.getConnection(dbname,readOnly = true)

    val selectSt = conn.prepareStatement("SELECT toUri FROM redirects WHERE fromUri = ?;")
        
    var nlines = 0
    var nredirected = 0
    var nnotredirected = 0
    var infoColUse = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      var cols = line.split("""\t""",-1)
      if(cols.size <= uricolnr) {
        throw new RuntimeException("not at least this many columns in tsv line "+(uricolnr+1)+": "+line)
      }
      // if we have the column number where to store the info, make sure that
      // column does exist. Otherwise make sure we add one column to the end 
      // in both cases, store the column to use for later in infoColUse
      if(!noInfoCol) {
        if(infoCol >= 0) {
          if(infoCol > (cols.size-1)) {
            cols = cols ++ Array.fill(infoCol-cols.size+1)("")          
          }
          infoColUse = infoCol
        } else {
          // add just exactly one column
          cols = cols :+ ""
          infoColUse = cols.size-1
        }
      }
      selectSt.setString(1,cols(uricolnr))
      try {
        val rs = selectSt.executeQuery()
        // check if there is a match at all
        if(rs.next()) {
          // ok, get the URI and use it!
          // if we want to add rows, first output the existing row
          if(addRows) {
            if(!noInfoCol) {
              cols(infoColUse) = "nonredir"
            }
            System.out.println(cols.mkString("\t"))
            nnotredirected += 1
          }
          val redirUri = rs.getString("toUri")
          cols(uricolnr) = redirUri
          if(!noInfoCol) {
            cols(infoColUse) = "redir"
          }
          System.out.println(cols.mkString("\t"))
          nredirected += 1
        } else {
          // no, output the original line, but set the info column to nonredir
          if(!noInfoCol) {
            cols(infoColUse) = "nonredir"
          }
          System.out.println(cols.mkString("\t"))
          nnotredirected += 1
        }
      } catch {
        case e: Exception => {
          throw e;
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("ReplaceUrisByRedirects - processed: "+nlines)
      }
    }
    utils.JdbcConnection.shutdown()
    System.err.println("ReplaceUrisByRedirects - Completed, total lines: "+nlines+" of which redirected: "+nredirected)    
  }
}
