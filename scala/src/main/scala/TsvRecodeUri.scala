import org.rogach.scallop._
import gate.trendminer.lodie.utils.LodieUtils

// encode the uri in a tsv file column so it matches a dbpedia encoding
// as used by dbpedia version 3.8 or later:
// See http://wiki.dbpedia.org/URIencoding?show_comments=1

// This essentially conforms to the way how Java encodes URIs so we use
// the native Java methods for this.

// IMPORTANT: this not only *encodes* the URIs but also tries to first 
// *decode* any other encoding that may have been used. This includes
// pre-version 3.8 percent encodings (e.g. of parentheses which should not
// be percent encoded now) or using Java unicode escapes.

// TODO: test if this does the right thing for percent-encoded accented characters
// or non-latin characters and find out how to deal with different ways how
// the same character can be represented in unicode (e.g. accented characters).

object TsvRecodeUri {
  
  def main(args: Array[String]): Unit = {
    
    if(args.size != 1) {
      System.err.println("Need one argument: zero-based index of column to recode")
      System.exit(1)
    }
    val col = args(0).toInt

/*    
    val gatehome= System.getenv().get("GATE_HOME")
    if(gatehome == null) {
      System.err.println("Environment variable GATE_HOME not set!")
      System.exit(1)
    }

    Gate.setGateHome(new File(gatehome))

    Gate.runInSandbox(true)
    Gate.init()
*/    
    
    var nlines = 0
    var nchanged = 0
    var nerror = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      val uri_orig = cols(col)
      try {
        var uri_new = LodieUtils.recodeUri(uri_orig)
        if(uri_new.contains("\\u")) {
          throw new RuntimeException("URI contains java escaped unicode character: "+uri_orig+" in line "+nlines)
        }
      
        if(uri_orig != uri_new) {
          nchanged += 1
          cols(col) = uri_new
        }
        System.out.println(cols.mkString("\t"))
      } catch {
        case e: Exception => {
          System.err.println("ERROR when trying to recode "+uri_orig)
          e.printStackTrace(System.err)
          nerror += 1
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("TsvRecodeUri - processed: "+nlines)
      }      
    }
    System.err.println("TsvRecodeUri DONE - total: "+nlines+" changed: "+nchanged+" errors: "+nerror)
  }

}
