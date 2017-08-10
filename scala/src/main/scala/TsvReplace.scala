import org.rogach.scallop._;

// if a value matches the regexp, replace with the replacement string
// the replacement string can contain the usual references to capture groups
// as used in java.

object TsvReplace {
  
  def main(args: Array[String]): Unit = {
    
    val opts = Scallop(args)
      .trailArg[Int]("col", required=false)
      .trailArg[String]("pattern",required=true)
      .trailArg[String]("replacement",required=true)
          
    val opt_col = opts.get[Int]("col")
    val opt_pat = opts.get[String]("pattern")
    val opt_rep = opts.get[String]("replacement")
    
    if(opt_pat.isEmpty) {
      System.err.println("Need a pattern!")
      System.err.println(opts.help)
      System.exit(1)
    }
    if(opt_rep.isEmpty) {
      System.err.println("Need a replacement!")
      System.err.println(opts.help)
      System.exit(1)
    }

    val col = opt_col.getOrElse(-1)
    val pattern = opt_pat.getOrElse("")  // at this point we are sure it is not None
    val replacement = opt_rep.getOrElse("")  // sure it is not None
    
    val matcher = new scala.util.matching.Regex(pattern)
    
    var nlines = 0
    var nchanged = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      try {
        if(col >= 0) {
          val cols = line.split("""\t""",-1)
          val toUse = cols(col) 
          val toUseChanged = matcher.replaceAllIn(toUse,replacement)
          if(toUse != toUseChanged) { nchanged += 1 }
          cols(col) = toUseChanged
          System.out.println(cols.mkString("\t"))
        } else {
          val newLine = matcher.replaceAllIn(line,replacement)
          if(newLine != line) { nchanged += 1 }
          System.out.println(newLine)
        }
      } catch {
        case e: Exception => {
          System.err.println("Exception processing line: "+line);
          System.err.println(e);
          e.printStackTrace(System.err);
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("TsvReplace - processed: "+nlines)
      }      
    }
    System.err.println("TsvReplace DONE - total: "+nlines+" changed: "+nchanged)
  }

}
