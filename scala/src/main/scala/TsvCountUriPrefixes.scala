import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

// Count the number of different URI prefixes that occur in a column
// and for each prefix. also count the number of input rows where such
// a prefix occurs.

object TsvCountUriPrefixes {
  val progName = "TsvCountUriPrefixes"
  
  // we use the following datastructure for counting: 
  // - a hashmap mapping each prefix to a set of all URIs that have that
  //   prefix
  // - a hasmap mapping each prefix to the number of rows that have something
  //   with that prefix in the column
  
  var pref2uris = scala.collection.mutable.HashMap[String, scala.collection.mutable.Set[String]]()
  var pref2rows = scala.collection.mutable.HashMap[String, Int]()
  
  def main(args: Array[String]) = {
    
    if(args.size != 1) {
      System.err.println(progName+": column-0-based < tsvfile.tsv")
      System.exit(1)
    }
    
    val colnum = args(0).toInt
    
    var nlines = 0
    var nempty = 0  // number of rows with no URI
    var nnoprefix = 0 // number of rows with an URI that does not seem to have a prefix (no :)
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      
      val field = cols(colnum)
      if(field.isEmpty()) {
        nempty += 1
      } else {
        val parts = field.split(":",2) 
        if(parts.size == 1) {
          nnoprefix += 1
        } else {
          val prefix = parts(0)
          val uri = parts(1)
          val curcount = pref2rows.get(prefix).getOrElse(0)
          pref2rows.put(prefix,curcount+1)
          val curset = pref2uris.get(prefix).getOrElse(scala.collection.mutable.Set[String]())
          curset.add(uri)
          pref2uris.put(prefix,curset)
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println(progName+" - processed: "+nlines)
      }
    }
    System.err.println(progName+" - Completed, total read lines: "+nlines)    
    System.err.println(progName+" - no URI: "+nempty)    
    System.err.println(progName+" - no prefix: "+nnoprefix)
    System.err.println(progName+" - nr of prefixes: "+pref2rows.size)
    pref2rows.keys.toSeq.sorted.foreach { pref =>
      System.out.println(pref+"\t"+pref2uris.get(pref).orNull.size+"\t"+pref2rows.get(pref).getOrElse(0))
    }
  }
}
