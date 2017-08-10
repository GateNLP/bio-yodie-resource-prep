
// Simple program to add a column with some fixed value (or empty)
// before the given column number. If a column number is specified that
// is larger than any existing column number, the column is appended at 
// the end of the line
// Also allows to add a column name for TSV files which have a header line

import scala.language.postfixOps
import org.rogach.scallop._


object AddTsvColumn {
  
  val progDesc = "add a new column and value to a TSV file"
  val progName = this.getClass()
  
  def main(args: Array[String]) = {

    class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
      val progress = opt[Int]("progress",'p',descr = "Number of rows per progress report message",default=Some(100000),required = false)
      val columnname = opt[String]("columnname",'n',descr = "Name of the column to use in the TSV header, if absent, no header assumed",default=Some(""),required = false)
      val columnnr = trailArg[Int]("columnnr",descr = "Column number (0-based) before which to insert the column",required = true,validate=(0<))
      val columnvalue = trailArg[String]("columnvalue", descr = "Value to insert for the column, default is empty", default=Some(""), required = false)
    }
    val conf = new Conf(args)
        
    val progress = conf.progress()
    val columnname = conf.columnname()
    val columnnr = conf.columnnr()
    val columnvalue = conf.columnvalue()
      
    
    val colnr = columnnr
    val colvalue = columnvalue
    var nlines = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val value = if(columnname != "" && nlines == 1) { columnname } else { colvalue } 
      val cols = line.split("""\t""",-1)
      if(colnr >= cols.size) {
        if(cols.size == 1 && cols(0).isEmpty()) {
          println(value)
        } else {
          println(line+"\t"+value)
        }
      } else {
        if(colnr == 0 || (cols.size == 1 && cols(0).isEmpty())) { // we have to insert at the beginning
          print(value+"\t")
          println(cols.slice(colnr,cols.size).mkString("\t"))
        } else {
          print(cols.slice(0,colnr).mkString("\t"))
          print("\t"+value+"\t")
          println(cols.slice(colnr,cols.size).mkString("\t"))
        }
      }
      
      if((nlines % progress) == 0) {
        System.err.println(progName+" - processed: "+nlines)
      }
    }
    System.err.println(progName+" - Completed, total lines: "+nlines)    
  }

  def exitWithError(opts: Scallop, msg: String): Unit = {
    System.err.println(progName+": "+progDesc)
    if(msg != "") {
      System.err.println("ERROR: "+msg)
    }
    System.err.println(opts.help)
    if(msg != "") {
      System.exit(1)
    } else {
      System.exit(0)
    }
  }

  
}
