
// Shorten URIs in one or more columns either by assuming a base-URI or
// by replacing URI prefixes with namespace names.
// This is LODIE-specific in that the base URI is always http://dbpedia.org/resource/
// and the namespaces are also LODIE specific.
//
// Parameters: one or more of the form N:X where 
// N: a 0-based index of the column
// X: either "b" to remove base URI or "n" to substitute namespace name
// This uses the method shortenUri from the Yodie plugin.

import gate.Utils
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

import gate.trendminer.lodie.utils.LodieUtils;

object TsvShortenUri {
  
  val progress = 10000
  
  
  val progDesc = "Remove base URI or replace namespace prefix for selected tsv columns"
  val progName = this.getClass()
  
  def usage() = {
      System.err.println(progName+": usage: [-c] [-q] (<N>:b|<N>:n)+ (e.g. -c 0:b 1:n 3:n)")
      System.err.println(progName+": -c: continue if a URI cannot be shortened")
      System.err.println(progName+": -i: continue and ignore the row if a URI cannot be shortened, still show a message")
      System.err.println(progName+": -q: continue if a URI cannot be shortened and do not show a message")
      System.exit(1)
  }
  
  def main(args: Array[String]) = {

    if(args.size == 0) {      
      usage();
    }
    var colnrs = Array[Int]()
    var usenss = Array[Boolean]() // if true replace NS, otherwise just remove base uri
    
    var exitOnError = true
    var quiet = false
    var ignore = false
    
    args.foreach { arg =>
      //System.err.println("Processing arg "+arg)
      if(arg.equals("-c")) {
        exitOnError = false
      } else if(arg.equals("-q")) {
        quiet = true
        exitOnError = false
      } else if(arg.equals("-i")) {
        quiet = false
        exitOnError = false
        ignore = true
      } else {
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
    }
    if(colnrs.size == 0) {
      usage();
    }
    var nlines = 0
    var nerror = 0
    var nwritten = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      var haveSomeError = false
      val cols = line.split("""\t""",-1)
      for(i<-0 until colnrs.size) {
        val col = colnrs(i)
        val usens = usenss(i)
        var value = cols(col)
        if(value == null || value.isEmpty()) {
          // we do nothing if there is nothing!!
        } else {
        // NOTE: this should not be necessary any more, since we already 
        // shorten the URIs at the very beginning. However we leave this
        // in for now so we can, if necessary, also shorten |-separated lists.
        var uris = value.split("\\|")
        for(u <- 0 until uris.size) {
          var uri = uris(u)
          var error = false
          var exception: Throwable = null
          var shortened = 
          if(usens) {
            try {
              LodieUtils.shortenUri(uri)
            } catch {
              case e: Throwable => { error = true; exception = e; "" }              
            }
          } else {
            try {
              if(uri.startsWith("http://dbpedia.org/resource/")) {
                uri.substring("http://dbpedia.org/resource/".length)
              } else {
                uri
              }
            } catch {
              case e: Throwable => { error = true; exception = e; "" }
            }
          }
          if(error || shortened.equals(uri)) {
            nerror += 1
            if(!quiet) {
              System.err.println(progName+": URI could not be shortened: "+uri+" -> "+shortened)
              if(exception!=null) {
                System.err.println(progName+": exception: "+exception)
                System.err.println(progName+": message: "+exception.getMessage())
              }
              System.err.println(progName+": column: "+value)
              System.err.println(progName+": line: "+line)
              System.err.println(progName+": In line "+nlines)
              System.err.println(progName+": In column "+col)
            }
            if(exitOnError) {
              System.err.println(progName+": ABORTING processing");
              System.exit(1)
            }
            haveSomeError = true            
          }
          uris(u) = shortened
        }
        cols(col) = uris.mkString("|")
        } // if there is something to shorten at all
      }
      if(haveSomeError && ignore) {
        // ignore it
      } else {      
        System.out.println(cols.mkString("\t"))
        nwritten += 1
      }
      
      if((nlines % progress) == 0) {
        System.err.println(progName+" - processed: "+nlines)
      }
    }
    System.err.println(progName+" - Completed, total lines: "+nlines)    
    System.err.println(progName+" - errors: "+nerror)    
  }


  
}
