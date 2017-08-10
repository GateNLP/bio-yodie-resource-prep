
// Expand URIs
// This is LODIE-specific in that the base URI is always http://dbpedia.org/resource/
// and the namespaces are also LODIE specific.
//
// Parameters: one or more of the form N:X where 
// N: a 0-based index of the column
// X: either "b" to remove base URI or "n" to substitute namespace name
// This uses the method expandUri from the Yodie plugin.

import gate.Utils
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import gate.trendminer.lodie.utils.LodieUtils;

object TsvExpandUri {
  
  val progress = 10000
  
  val baseUri = "http://dbpedia.org/resource/"
  
  val progDesc = "Expand base URI or expand namespace prefix for selected tsv columns"
  val progName = this.getClass()
  
  def main(args: Array[String]) = {

    if(args.size == 0) {
      System.err.println(progName+": Need at least one parameter of the form N:X (e.g. 0:b 1:n 3:n)")
      System.exit(1)
    }
    var colnrs = Array[Int]()
    var usenss = Array[Boolean]() // if true replace NS prefix, otherwise just expand base uri
    
    args.foreach { arg =>
      var subparms = arg.split(":")
      if(subparms.size != 2 || !subparms(0).matches("[0-9]+") || !subparms(1).matches("n|b") ) {
        System.err.println(progName+": Parameter not of the form N:X where N is a 0-based column nr and X is 'b' or 'n'")
        System.exit(1)
      }
      val colnr = subparms(0).toInt
      val usens = subparms(1).equals("n")
      colnrs = colnrs :+ colnr
      usenss = usenss :+ usens
    }
    
    var nlines = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      for(i<-0 until colnrs.size) {
        val col = colnrs(i)
        val usens = usenss(i)
        var value = cols(col)
        if(value == null || value.isEmpty()) {
          // we do nothing if there is nothing!!
        } else {
        var uris = value.split("\\|")
        for(u <- 0 until uris.size) {
          var uri = uris(u)
          var error = false
          var expanded = 
          if(usens) {
            try {
              LodieUtils.expandUri(uri)
            } catch {
              case _: Throwable => { error = true; "" }              
            }
          } else {
            try {
              baseUri + uri
            } catch {
              case _: Throwable => { error = true; "" }
            }
          }
          if(error || expanded.equals(uri)) {
            System.err.println(progName+": URI could not be expanded: "+uri)
            System.err.println(progName+": column: "+value)
            System.err.println(progName+": line: "+line)
            System.err.println(progName+": In line "+nlines)
            System.err.println(progName+": In column "+col)
            System.exit(1)
          }
          uris(u) = expanded
        }
        cols(col) = uris.mkString("|")
        } // if there is something to shorten at all
      }
      System.out.println(cols.mkString("\t"))
      
      if((nlines % progress) == 0) {
        System.err.println(progName+" - processed: "+nlines)
      }
    }
    System.err.println(progName+" - Completed, total lines: "+nlines)    
  }


  
}
