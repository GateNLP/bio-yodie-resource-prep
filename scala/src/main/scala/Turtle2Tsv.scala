// Read Turtle format (or NT format) from standard input and 
// create a TSV file at standard output with the following 5 columns
// - full subject URI  (baseURI/NS resolved)
// - full predicate URI (baseURI/NS resolved)
// - either full object URI or label string
// - label language code, if it exists
// - full label datatype URI, if it exists
//
// This uses the Sesame RIO parser and should therefore handle things like 
// escaped characters, namespaces, comments etc.


import org.openrdf.rio.turtle._
import org.openrdf.rio.helpers._
import org.openrdf.model._
import java.io.StringReader
import org.rogach.scallop._;

object Turtle2Tsv {

  class StatementProcessor extends RDFHandlerBase {
    var keepOriginal = false;
    var ntriples = 0;
    override def handleStatement(statement: Statement): Unit = {
      val subj = statement.getSubject().stringValue()
      val pred = statement.getPredicate().toString()
      val obj = statement.getObject()
      var objString = statement.getObject().stringValue()
      var objLang = ""
      var objDT = ""
      if(obj.isInstanceOf[Literal]) {
        val lit = obj.asInstanceOf[Literal]
        val tmpLang = lit.getLanguage()
        if(tmpLang != null) {
          objLang = tmpLang.toString()
        }
        val tmpDT = lit.getDatatype()
        if(tmpDT != null) {
          objDT = tmpDT.toString()
        }
        if(!keepOriginal) {
          objString = objString.replaceAll("[\t\n\r\f]"," ")
        }
      }
      println(subj+"\t"+pred+"\t"+objString+"\t"+objLang+"\t"+objDT)
      ntriples += 1;
      if((ntriples % 100000) == 0) {
        System.err.println("Turtle2Tsv - processed: "+ntriples)
      }      
    }
  }
  
  
  def main(args: Array[String]): Unit = {
    
    val opts = Scallop(args)
      .opt[Boolean]("singletriples",'s', descr = "Parse individual single triples",required=false)
      .opt[Boolean]("keeporiginal",'k',descr = "Do not convert tabs, newlines, CRs, etc. to spaces")
          
    val opt_s = opts.get[Boolean]("singletriples")

    val singletriples = opt_s.getOrElse(false)
    val keepOriginal = opts.get[Boolean]("keeporiginal").getOrElse(false)
    
    val rdfParser = new TurtleParser()
    val processor = new StatementProcessor()
    processor.keepOriginal = keepOriginal;
    rdfParser.setRDFHandler(processor);
    var nerrors = 0
    var nline = 0
    if(singletriples) {
      scala.io.Source.fromInputStream(System.in).getLines.foreach { line => 
        nline += 1
        val reader = new StringReader(line)
        try {
          rdfParser.parse(reader,"")
        } catch {
          case ex: Exception => {
            System.err.println("Error parsing line "+nline+": "+line)
            ex.printStackTrace(System.err)
            nerrors += 1
          }
        }
      }
    } else {
      rdfParser.parse(System.in,"")
    }
    System.err.println("Turtle2Tsv - total processed: "+processor.ntriples+" errors: "+nerrors)
  }
  

}
