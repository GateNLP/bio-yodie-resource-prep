import org.rogach.scallop._
import org.rogach.scallop.exceptions.Help
import java.io._
import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.model.WikiModel;
import org.apache.commons.io.FileUtils
import info.bliki.wiki.filter.ParsedPageName;
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.nio.file.Paths
import gate.Gate
import gate.Factory
import gate.Document
import gate.DocumentExporter
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.lang.StringEscapeUtils

// read from a wikipedia pages dump file and create GATE format documents
// in fast infoset format and store them in a directory or within 
// subdirectories within a target directory (to keep directory sizes small)

// The document will be stored with a file name identical to the mediawiki
// page id and if subdirectories are used we use four digits 4 digits to the left 
// (by default), padded to the left with zeroes if necessary 
// to create the subdirectory name. In other words each directory will 
// contain a maximum of 10,000 documents and there will be a maximum of 
// 10,000 directories, starting with directory 0000. Thus the maximum number
// of docouments to store is 100 million.

// This will write progress information and errors to stderr and a tsv
// file with information about each parsed/written document to stdout


object Wiki2Gate {
  
  val progName = "Wiki2Gate"
  var finf_exporter: DocumentExporter = null
  var verbose = false
  var progress = 10000
  var digits = 4
  var debug = false


  
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
    
    class Conf(arguments: Seq[String], onError: (Throwable,Scallop) => Nothing) extends ScallopConf(args) {
      version("runScala.sh "+progName)
      banner("""Usage: runScala.sh Wiki2Gate [OPTIONS] outdir < inWikiDump """)
      val help = opt[Boolean]("help",'h',descr="Show usage information",required=false,default=Some(false))      
      val debug = opt[Boolean]("debug",'D', descr = "Debugging mode: save raw wiki format files too",required=false,default=Some(false))
      val verbose = opt[Boolean]("verbose",'v', descr = "Provide lots of information about what is happening, default is not verbose",required=false,default=Some(false))
      val progress = opt[Int]("progress",'p', descr = "Number of articles after which to show a progress report message, default=10000",required = false,default=Some(10000))
      val digits = opt[Int]("digits",'d',descr = "Number of digits to use for the subdirectory names, 0=do not creat subdirs, default is 4",required=false,default=Some(4))
      val max = opt[Int]("max",'m',descr = "Maximum number of GATE documents to create, default: no maximum",required=false,default=Some(Int.MaxValue))
      val lang = opt[String]("lang",'l',descr = "Language code to use",required=false,default=Some("en"))
      val outdir = trailArg[String]("outdir", descr = "Output directory which is expected to exist", required=true)
      override protected def onError(e: Throwable) = onError(e, builder)
    }

    
    
    val opts = new Conf(args,onError)      
    if(opts.help()) {
      opts.printHelp()
      System.exit(0)
    }

    
    
    debug = opts.debug()
    verbose = opts.verbose()
    progress = opts.progress()
    digits = opts.digits()
    if(digits < 0 ||digits > 6) {
      exitWithError("digits < 0 or > 6 do not really make sense")
    }
    //val infilename = opt_infile.getOrElse("")
    //val infile = new File(infilename)
    val outdir = new File(opts.outdir())
    val maxdocs = opts.max()
    val languageCode = opts.lang()

    System.err.println(progName+" Verbose: "+verbose)
    System.err.println(progName+" Progress: "+progress)
    System.err.println(progName+" Language: "+languageCode)
    System.err.println(progName+" Digits: "+digits)
    //System.err.println("Infile: "+infile)
    System.err.println(progName+" Outdir: "+outdir)
    
    
    //if(!infile.exists()) {
    //  exitWithError(opts,"Input file does not exist: "+infile)
    //}
    
    
    // Initialize GATE and load the required plugins
    val gateHome =  System.getenv().get("GATE_HOME")
    if(gateHome == null) {
      exitWithError("Environment variable GATE_HOME not set!")
    }
    
    Gate.setGateHome(new File(gateHome))
    Gate.runInSandbox(true)
    Gate.init()
    
    var pluginHome = new File(new File(gateHome,"plugins"), "Format_FastInfoset")
    Gate.getCreoleRegister().registerDirectories(pluginHome.toURI().toURL())
    pluginHome = new File(new File(gateHome,"plugins"), "Format_MediaWiki")
    Gate.getCreoleRegister().registerDirectories(pluginHome.toURI().toURL())    
    finf_exporter = 
      Gate.getCreoleRegister().
      getAllInstances("gate.corpora.FastInfosetExporter").
      iterator().next().asInstanceOf[DocumentExporter]
    
    val handler = new MyArticleFilter(outdir,maxdocs,languageCode)
    //System.err.println("parsing intput file: "+infilename)
    
    //val instream = new BZip2CompressorInputStream( 
    //  new BufferedInputStream(new FileInputStream( infilename )), 
    //  true )
    
    val instream = new BufferedInputStream(System.in)
    
    //val wxp = new WikiXMLParser(new File(infilename), handler)
    val wxp = new WikiXMLParser(instream, handler)
    
    // write the TSV header
    // id, title, revision, namespace, base, language, file name
    System.out.println("id\ttitle\trevision\tnamespace\tsitename\tlanguage\tfilename\textLength\nrOrigMarkups")
    
    wxp.parse()
    if(instream != null) {
      //instream.close()
    }
    System.err.println("Finished")
          System.err.println("Wiki2Gate: total articles: "+handler.n_total_articles)
          System.err.println("Wiki2Gate: main articles:  "+handler.n_main_articles)
          System.err.println("Wiki2Gate: ignored empty: "+handler.n_empty)
          System.err.println("Wiki2Gate: ignored redirects: "+handler.n_redirects)
          System.err.println("Wiki2Gate: errors:         "+handler.n_errors)
          System.err.println("Wiki2Gate: GATE documents: "+handler.n_documents)
          System.err.println("Wiki2Gate: Finished")
  } // main
  
  def exitWithError(msg: String): Unit = {
    System.err.println(progName+": convert a mediawiki dump file to a corpus of GATE documents in fast infoset format")
    System.err.println(progName+" ERROR: "+msg)
    System.err.println(progName+" To find the id of a wikipedia page:")
    System.err.println(progName+"   http://en.wikipedia.org/w/api.php?action=query&prop=info&titles=Sheffield")
    System.err.println(progName+" To find the page for an id:")
    System.err.println(progName+"   http://en.wikipedia.org/?curid=1092923")
    System.exit(1)
  }
  
  class MyArticleFilter(dir: File, maxnr: Int, lang: String) extends IArticleFilter {
    val max_documents = maxnr
    val languageCode = lang
    val outDir = dir
    
    var n_total_articles = 0
    var n_main_articles = 0
    var n_documents = 0
    var n_errors = 0
    var n_redirects = 0
    var n_empty = 0
    
    override def process(article: WikiArticle, site: Siteinfo): Unit = {
      n_total_articles += 1
      if(article.isMain()) {
        n_main_articles += 1
        val features = Factory.newFeatureMap()
        val id = article.getId()
        var text = article.getText()
        
        if(text == null || id == null) {
          System.err.println("Got a null id and/or text")
          n_errors += 1
          return;
        }
        text = text.trim()
        val title = article.getTitle()
        if(text.isEmpty()) {
          n_empty += 1
          System.err.println("Empty text ignored for: "+id+", title="+title)
          return
        }
        if(text.toUpperCase().startsWith("#REDIRECT")) {
          n_redirects += 1
          //System.err.println("Redirect page ignored: "+id+", title="+title)
          return
        }
        features.put("mediawiki.title", title);
        features.put("mediawiki.timestamp", article.getTimeStamp());
        features.put("mediawiki.id", id);
        val revision = article.getRevisionId()
        features.put("mediawiki.revision", revision);
        val namespace = article.getNamespace()
        features.put("mediawiki.namespace", namespace);
        val sitename = site.getSitename()
        features.put("mediawiki.sitename", sitename);
        val base = site.getBase()
        features.put("mediawiki.base", base);
        features.put("language",languageCode)
        val textLength = text.size
        features.put("mediawiki.textLength",textLength: java.lang.Integer)
        //features.put("mediawiki.rawText",text)
        val params = Factory.newFeatureMap()
        //if (debug) System.out.println("TEXT: "+text)
        
        // NOTE: we cannot use the format_mediawiki plugin to convert the 
        // page using that plugin because the bliki version the plugin uses
        // is different from the version we have to use to parse the mediawiki
        // dump file. 
        // We therefore first render the page ourself and then parse the 
        // HTML rendered version as such

        val model = new MyWikiModel("${image}","${title}")
        model.setUp()
        // this is copied from the Format_MediaWiki plugin and probably needed
        val unescaped = StringEscapeUtils.unescapeHtml(text);
        val html = model.render(unescaped).trim()
        model.tearDown()

        
        params.put(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME,html)
        params.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME,"text/html")
        params.put(Document.DOCUMENT_ENCODING_PARAMETER_NAME, "UTF-8")
        var doc: Document = null
        try {
          doc = Factory.createResource(
            "gate.corpora.DocumentImpl",
            params, features, article.getTitle()).asInstanceOf[Document]
        } catch {
          case ex: Exception => {
            System.err.println("Error creating the GATE document for id: "+id);
            ex.printStackTrace(System.err)
            n_errors += 1
            return
          }
        }
        val nrOrigMarkups = doc.getAnnotations("Original markups").size
        // write the document
        var fn = "" 
        val outFile = 
          if(digits == 0) {
            fn = id+".finf" 
            new File(outDir,fn)
          } else {
            val fullid = ("000000000000"+id.toString).takeRight(digits * 2)
            val subDirName = fullid.substring(0,digits)
            val filename = fullid.takeRight(digits) + ".finf"
            val subDir = new File(outDir,subDirName)
            if(!subDir.exists()) { subDir.mkdir() }
            val ret = new File(subDir,filename)
            fn = new File(new File(subDirName),filename).toString()
            ret
          }
        if(verbose) {
          System.err.println("Wiki2Gate: Writing document "+outFile)
        }
        // create the tsv row with data bout the written article/page. We 
        // write the following columns:
        // id, title, revision, namespace, base, language, file name
        System.out.print(id)
        System.out.print("\t")
        System.out.print(title)
        System.out.print("\t")
        System.out.print(revision)
        System.out.print("\t")
        System.out.print(namespace)
        System.out.print("\t")
        System.out.print(base)
        System.out.print("\t")
        System.out.print(languageCode)
        System.out.print("\t")
        System.out.print(fn)
        System.out.print("\t")
        System.out.print(textLength)
        System.out.print("\t")
        System.out.print(nrOrigMarkups)
        System.out.println()
        
        if(debug) {
          // write the raw wiki content to a file with additional extension .wiki
          new PrintWriter(new File(outFile.getPath()+".wiki")) { write(text); close }
          new PrintWriter(new File(outFile.getPath()+".html")) { write(html); close }
        }
        finf_exporter.export(doc,outFile,Factory.newFeatureMap())
        
        Factory.deleteResource(doc)
        n_documents += 1
        // are we done yet?
        if(n_documents >= max_documents) {
          System.err.println("Wiki2Gate: total articles: "+n_total_articles)
          System.err.println("Wiki2Gate: main articles:  "+n_main_articles)
          System.err.println("Wiki2Gate: ignored empty: "+n_empty)
          System.err.println("Wiki2Gate: ignored redirects: "+n_redirects)
          System.err.println("Wiki2Gate: errors:         "+n_errors)
          System.err.println("Wiki2Gate: GATE documents: "+n_documents)
          System.err.println("Wiki2Gate: Finished")
          System.exit(0)
        }
        if((n_documents % progress) == 0) {
          System.err.println("Wiki2Gate: written: "+n_documents)
        }
        
      }
    }
  }
  
  class MyWikiModel(img:String,title:String) extends WikiModel(img, title) {
    override def getRawWikiContent(
          name: ParsedPageName, 
          templateParameters: java.util.Map[String,String]): String = {
      val rawContent = super.getRawWikiContent(name,templateParameters);
      //System.err.println("In getRawWikiContent")
      if(rawContent == null) { "" } else { rawContent }
    }
  }
  
  
}
