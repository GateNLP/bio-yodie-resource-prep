import org.rogach.scallop._
import org.rogach.scallop.exceptions.Help
import gate.miscutils.UriCanonicalization
import com.jpetrak.gate.jdbclookup._
import gate._
import java.io._

// encode the uri in a tsv file column so it matches a dbpedia encoding
// as used by dbpedia version 3.8 or later:
// See http://wiki.dbpedia.org/URIencoding?show_comments=1

// This essentially conforms to the way how Java encodes URIs so we use
// the native Java methods for this.

// IMPORTANT: this not only *encodes* the URIs but also tries to first 
// *decode* any other encoding that may have been used. This includes
// pre-version 3.8 percent encodings (e.g. of parentheses which should not
// be percent encoded now) or using Java unicode escapes.


// Depending on the parameters given, this will also replace by transitive
// redirects, map interlanguage sameAs or filter URIs which are disambiguation
// URIs. 

object TsvCanonicalizeUri {

  val progName = this.getClass()
  
  def main(args: Array[String]): Unit = {    
    
    def onError(e: Throwable, scallop: Scallop) = e match {
      case Help(_) => 
        scallop.printHelp
        sys.exit(0)
      case _ =>
        System.err.println(progName+" Error: "+e.getMessage)
        scallop.printHelp
        sys.exit(1)
    }        
    
    class Conf(arguments: Seq[String], onError: (Throwable,Scallop) => Nothing) extends ScallopConf(arguments) {
      version("runScala.sh "+progName)
      banner("""Usage: runScala.sh [OPTIONS] colNumber0based < inTsv > outTsv """)
      val help = opt[Boolean]("help",'h',descr="Show usage information",required=false,default=Some(false))      
      val interlangDb = opt[String]("interlangDb",'l',descr="file name, table name, key, value columns for interlanguage table, if wanted. only file name is required, other fields can be changed by adding them, separated by commas",required=false,default=Some(""))
      val redirectsDb = opt[String]("redirectsDb",'r',descr="file name, table name, key, value columns for transitive redirects table, if wanted. only file name is required, other fields can be changed by adding them, separated by commas",required=false,default=Some(""))
      val irisameasuriDb = opt[String]("irisameasuriDb",'i',descr="file name, table name, key, value columns for irisameasuri table, if wanted. only file name is required, other fields can be changed by adding them, separated by commas",required=false,default=Some(""))
      val disamburisDb = opt[String]("disamburisDb",'d',descr="file name, table name, key, value columns for disambiguations uris table, if wanted. only file name is required, other fields can be changed by adding them, separated by commas",required=false,default=Some(""))
      val pluginsDir = opt[String]("pluginsDir",'p',descr="Plugin directory path for where to find the JdbcLookup plugin, if not given, the property 'pluginsdir' must be set",required=false,default=Some(""))
      val filterDisambRows = opt[Boolean]("filterDisambRows",'f',descr="If specified, filter rows where the IRIs refers to a disambiguation page, default: do not filter",required=false,default=Some(false))
      val keepEmptyRows = opt[Boolean]("keepEmptyRows",'k',descr="Keep rows where the URI is completely empty and leave it empty (e.g. to represent absence of an URI), otherwise filter", required=false,default=Some(false)) 
      val colIndex = trailArg[Int](required=true,descr="column number, 0-based")
      override protected def onError(e: Throwable) = onError(e, builder)
    }
    val conf = new Conf(args,onError)
    if(conf.help()) {
      conf.printHelp()
      System.exit(0)
    }
    
    var pluginsDir = conf.pluginsDir()
    if(pluginsDir.isEmpty) {
      pluginsDir = System.getProperty("pluginsdir")
      if(pluginsDir == null || pluginsDir.isEmpty) {
        System.err.println(progName+": No plugin directory specified, cannot load JdbcLookup plugin")
        conf.printHelp()
        System.exit(1)
      }
    }        
    
    val col = conf.colIndex()
    val keepEmptyRows = conf.keepEmptyRows()
    
    val gatehome= System.getenv().get("GATE_HOME")
    if(gatehome == null) {
      System.err.println(progName+": Environment variable GATE_HOME not set!")
      System.exit(1)
    }

    Gate.setGateHome(new File(gatehome))

    Gate.runInSandbox(true)
    Gate.init()

    // Load the JdbcLookup and YodiePlugin plugins
    gate.Utils.loadPlugin(new File(new File(pluginsDir),"JdbcLookup"))
    gate.Utils.loadPlugin(new File(new File(pluginsDir),"YodiePlugin"))
    
    def getLR(dbName: String, tableName: String, keyName: String, valueName: String): JdbcString2StringLR = {
      if(!dbName.isEmpty()) {
        var db = dbName;
        var table = tableName;
        var key = keyName;
        var value = valueName;
        if(dbName.contains(",")) {
          val parts = dbName.split(",")
          // we have to have either just the dbName or all of the fields
          if(parts.size != 1 && parts.size != 4) {
            System.err.println("Database parameter must either be just the db name or db,table,key,value: "+dbName)
            System.exit(1)
          }
          if(parts.size == 4) {
            db = parts(0)
            table = parts(1)
            key = parts(2)
            value = parts(3)
          }
        }
        val fm = Factory.newFeatureMap()
        fm.put("dbDirectoryUrl",".")
        fm.put("keyColumnName",key)
        fm.put("readOnly",true: java.lang.Boolean)
        fm.put("tableName",table)
        fm.put("valueColumnName",value)
        fm.put("jdbcUrl","jdbc:h2:"+db+";MV_STORE=FALSE;ACCESS_MODE_DATA=r;FILE_LOCK=NO;IFEXISTS=TRUE")
        Factory.createResource("com.jpetrak.gate.jdbclookup.JdbcString2StringLR",fm).asInstanceOf[JdbcString2StringLR]
      } else {
        null;
      }
    }
    
    
    var interlangDb: JdbcString2StringLR = getLR(conf.interlangDb(),"interlanguage","fromUri","toUri")
    var redirectsDb: JdbcString2StringLR = getLR(conf.redirectsDb(),"redirects","fromUri","toUri")
    var irisameasuriDb: JdbcString2StringLR = getLR(conf.irisameasuriDb(),"irisameasuri","uri","iri")
    var disamburisDb: JdbcString2StringLR = getLR(conf.disamburisDb(),"disambiguationuris","uri","uri")
    

    
    val canonicalizer = new UriCanonicalization(interlangDb,redirectsDb,irisameasuriDb,disamburisDb)
    
    
    var nlines = 0
    var nchanged = 0
    var nfiltered = 0
    var nerror = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(col >= cols.size) {
        throw new RuntimeException(progName+": not enough fields in line "+nlines+" for column "+col+"  ==>"+line+"<==")
      }
      val uri_orig = cols(col).trim()
      if(uri_orig.isEmpty()) {
        if(keepEmptyRows) {
          System.out.println(line)
        }
      } else {
          try {
            var uri_new = canonicalizer.canonicalize(uri_orig)
            if(uri_new == null) { uri_new = "" }
            if(conf.filterDisambRows() && uri_new.isEmpty()) {
              nfiltered += 1
            } else {
            
              if(uri_new.contains("\\u")) {
                throw new RuntimeException(progName+": URI contains java escaped unicode character: "+uri_orig+" in line "+nlines)
              }
          
              if(uri_orig != uri_new) {
                nchanged += 1
                cols(col) = uri_new
              }
              // instead of constructing a string, output in parts
              cols.foreach { col =>
                System.out.print(col)
                System.out.print("\t")
              }
              System.out.println();
            }
          } catch {
            case e: Exception => {
              System.err.println("ERROR when trying to recode "+uri_orig)
              e.printStackTrace(System.err)
              nerror += 1
            }
          }
      } // uri_orig was not empty
      if((nlines % 100000) == 0) {
        System.err.println(progName+" - processed: "+nlines)
      }      
    }
    System.err.println(progName+" DONE - total: "+nlines+" changed: "+nchanged+" filtered: "+nfiltered+" errors: "+nerror)
  }

}
