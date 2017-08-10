import org.rogach.scallop._;

// This program reads a tsv file from standard input and creates another 
// tsv file on standard output. Errors, progress and summary information is
// logged to standard error.
// The expected input format is a row with one or two fields:
// = the first field contains the mention
// = the second field contains some string if the mention is also an anchor,
//   otherwise the field is empty or missing
// 
// This program will count the total number of times a mention occurs, the
// number of times it occurs as an anchor and will output these two and
// the anchor/total fraction. 
// 
// If the option -i is given, these counts are calculated ignoring the 
// case of the mention (by converting them to lowercase).

import scala.language.reflectiveCalls

object CountMentions  {
    
  def main(args: Array[String]): Unit = {
    
    val opts = new ScallopConf(args) {
      val ignoreCase = opt[Boolean]("ignoreCase",'i', descr = "Case-insensitive counting",required=false)
      val help = opt[Boolean]("help",'h', descr = "Show usage information",required=false)
    }
          
    val ignoreCase = opts.ignoreCase()

    var countsAll = Map[String,Integer]()
    var countsLink = Map[String,Integer]()
    
    var nlines = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val fields = line.split("""\t""",-1)
      val mention = if(ignoreCase) fields(0).toLowerCase else fields(0)
      val link = if(fields.size > 1) fields(1) else ""
      if(countsAll.contains(mention)) {
        countsAll = countsAll + (mention -> (countsAll(mention)+1))
      } else {
        countsAll = countsAll + (mention -> 1)
      }
      if(!link.isEmpty) {
        if(countsLink.contains(mention)) {
          countsLink = countsLink + (mention -> (countsLink(mention)+1))
        } else {
          countsLink = countsLink + (mention -> 1)
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("CountMentions - processed: "+nlines)
      }      
    }
    countsAll.keys.foreach { key =>
      print(key)
      print("\t")
      print(countsAll(key))
      print("\t")
      val links = if(countsLink.contains(key)) countsLink(key) else 0:Integer
      print(links)
      print("\t")
      print((links*1.0)/countsAll(key))
      println
    }
    System.err.println("CountMentions DONE total: "+nlines)
  }

}
