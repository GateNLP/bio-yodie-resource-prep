import org.rogach.scallop._;

object TsvGrep {
  
  def main(args: Array[String]): Unit = {
    
    val opts = Scallop(args)
      .opt[Boolean]("invert",'v', descr = "invert matching: pick rows that do not match",required=false)
      .trailArg[Int]("col", required=false)
      .trailArg[String]("pattern",required=true)
          
    val opt_v = opts.get[Boolean]("invert")
    val opt_col = opts.get[Int]("col")
    val opt_pat = opts.get[String]("pattern")
    
    if(opt_pat.isEmpty) {
      System.err.println("Need at least a pattern!")
      System.err.println(opts.help)
      System.exit(1)
    }

    val col = opt_col.getOrElse(-1)
    val pattern = opt_pat.getOrElse("")
    val invert: Boolean = opt_v.getOrElse(false)
    
    val matcher = new scala.util.matching.Regex(pattern)
    
    var nlines = 0
    var nwritten = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      try {
        val toMatch = if(col >= 0) line.split("""\t""",-1)(col) else line        
        if(!matcher.findFirstIn(toMatch).isEmpty == !invert) { 
          System.out.println(line)
          nwritten += 1
        }
      } catch {
        case e: Exception => {
          System.err.println("Exception processing line: "+line);
          System.err.println(e);
          e.printStackTrace(System.err);
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("TsvGrep - processed: "+nlines)
      }      
    }
    System.err.println("TsvGrep DONE for '"+pattern+"' - total: "+nlines+" written: "+nwritten)
  }

}
