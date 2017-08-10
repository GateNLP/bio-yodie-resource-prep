import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException

/**
 ** Create/use a HSQL database and (re)create tables "labeluri", "label", "uri"
 ** to store the frequencies of label/uri, label, and uris respectively.
 ** Then create the freqs and calculate relative frequencies and ranks too.
 **
 ** Data is read from stdin and expected to have two fields: URI, label
 */ 



object CreateLabelUriStatsDB {
  def main(args: Array[String]) = {

    if(args.size != 1) {
      System.err.println("parms: dbname")
      System.exit(1)
    }
    
    val dbname = args(0)
    
    Class.forName("org.hsqldb.jdbcDriver")
    
    val conn = DriverManager.getConnection("jdbc:hsqldb:file:"+dbname,"","")
    
    val st = conn.createStatement()
    
    st.execute("DROP TABLE labeluriall IF EXISTS;")
    st.execute(
      """CREATE CACHED TABLE labeluriall ( 
           uri VARCHAR(1000) NOT NULL, 
           label VARCHAR(1000) NOT NULL,
           freq INTEGER DEFAULT 1 NOT NULL
           )""")
    st.execute("DROP INDEX labeluriallprimary IF EXISTS;")     
    st.execute("CREATE INDEX labeluriallprimary on labeluriall ( label, uri );")
    st.execute("DROP INDEX labelurialllabel IF EXISTS;")     
    st.execute("CREATE INDEX labelurialllabel on labeluriall ( label );")
    st.execute("DROP INDEX labelurialluri IF EXISTS;")     
    st.execute("CREATE INDEX labelurialluri on labeluriall ( uri );")

           
    st.execute("DROP TABLE labeluris IF EXISTS;")
    st.execute(
      """CREATE CACHED TABLE labeluris ( 
           uri VARCHAR(1000) NOT NULL, 
           label VARCHAR(1000) NOT NULL,
           freq INTEGER DEFAULT 0 NOT NULL,
           relLabelFreqByUri FLOAT DEFAULT 0.0 NOT NULL,
           relUriFreqByLabel FLOAT DEFAULT 0.0 NOT NULL
           )""")           
    st.execute("DROP INDEX labelurisprimary IF EXISTS;")     
    st.execute("CREATE UNIQUE INDEX labelurisprimary on labeluris ( label, uri );")
    st.execute("DROP INDEX labelurislabel IF EXISTS;")     
    st.execute("CREATE INDEX labelurislabel on labeluris ( label, freq );")
    st.execute("DROP INDEX labelurisuri IF EXISTS;")     
    st.execute("CREATE INDEX labelurisuri on labeluris ( uri, freq );")
           
    st.execute("DROP TABLE labeluristatslabel IF EXISTS;")
    st.execute(
      """CREATE CACHED TABLE labeluristatslabel ( 
           label VARCHAR(1000) NOT NULL,
           uri VARCHAR(1000) NOT NULL,
           prop FLOAT DEFAULT 0.0 NOT NULL,
           )""")
    st.execute("DROP INDEX labeluristatslabellabel IF EXISTS;")     
    st.execute("CREATE INDEX labeluristatslabellabel on labeluristatslabel ( label );")
    st.execute("DROP INDEX labeluristatslabeluri IF EXISTS;")     
    st.execute("CREATE INDEX labeluristatslabeluri on labeluristatslabel ( uri );")
    
    st.execute("DROP TABLE labeluristatsuri IF EXISTS;")
    st.execute(
      """CREATE CACHED TABLE labeluristatsuri ( 
           label VARCHAR(1000) NOT NULL,
           uri VARCHAR(1000) NOT NULL,
           prop FLOAT DEFAULT 0.0 NOT NULL,
           )""")
    st.execute("DROP INDEX labeluristatsurilabel IF EXISTS;")     
    st.execute("CREATE INDEX labeluristatsurilabel on labeluristatsuri ( label );")
    st.execute("DROP INDEX labeluristatsuriuri IF EXISTS;")     
    st.execute("CREATE INDEX labeluristatsuriuri on labeluristatsuri ( uri );")

    st.execute("DROP TABLE labels IF EXISTS;")
    st.execute("CREATE CACHED TABLE labels ( label VARCHAR(1000) NOT NULL, freq INTEGER DEFAULT 0 NOT NULL)")
    st.execute("DROP INDEX labelsprimary IF EXISTS;")     
    st.execute("CREATE INDEX labelsprimary on labels ( label );")

    st.execute("DROP TABLE uris IF EXISTS;")
    st.execute("CREATE CACHED TABLE uris ( uri VARCHAR(1000) NOT NULL, freq INTEGER DEFAULT 0 NOT NULL)")
    st.execute("DROP INDEX urisprimary IF EXISTS;")     
    st.execute("CREATE INDEX urisprimary on uris ( uri );")

    
    st.execute("SET FILES LOG FALSE")
    st.execute("SET AUTOCOMMIT FALSE")
    st.execute("SET FILES NIO SIZE 4096")  // depending on memory, try other values or remove
    st.execute("CHECKPOINT")
    
    val insertAllPairSt = conn.prepareStatement("INSERT INTO labeluriall (label,uri) VALUES(?,?);")
        
    // read TSV file with 2 columns from stdin, expecting URI, label
    var nlines = 0
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(cols.size != 2) {
        throw new RuntimeException("not 2 columns in tsv line "+nlines+": "+line)
      }
      var label = cols(1)
      var uri = cols(0)
      if(label.size > 1000) {
        System.err.println("CreateLabelUriStatsDB: label longer than 1000 chars: "+label)
        label = label.substring(0,1000)
      }
      if(uri.size > 1000) {
        System.err.println("CreateLabelUriStatsDB: uri longer than 1000 chars: "+uri)
        uri = uri.substring(0,1000)
      }
      insertAllPairSt.setString(1,label)
      insertAllPairSt.setString(2,uri)
      try {
        insertAllPairSt.execute()
      } catch {
        case e: Exception => {
          throw e;
        }
      }
      if((nlines % 100000) == 0) {
        System.err.println("CreateLabelUriStatsDB - phase1: lines read: "+nlines)
        st.execute("COMMIT")
      }
    }
    System.err.println("CreateLabelUriStatsDB - phase1: completed, lines read: "+nlines)
    st.execute("COMMIT")


    // phase 2: create the initial labeluris DB 
    System.err.println("CreateLabelUriStatsDB: Filling the labeluris table ....")
    st.execute("INSERT INTO LABELURIS (label,uri,freq) SELECT label, uri, SUM(freq) FROM labeluriall GROUP BY label,uri")
    st.execute("COMMIT")
    System.err.println("CreateLabelUriStatsDB - phase2a, create labeluris table completed")
    
    System.err.println("CreateLabelUriStatsDB: Dropping the labeluriall table ....")
    st.execute("DROP TABLE labeluriall")
    st.execute("COMMIT")
    System.err.println("CreateLabelUriStatsDB - phase2b, dropping labeluris table completed")
    
    // phase 3,4: create the label and uri tables
    System.err.println("CreateLabelUriStatsDB: Filling the labels table ....")
    st.execute("INSERT INTO labels (label, freq) SELECT label, sum(freq) FROM labeluris GROUP BY label")
    System.err.println("CreateLabelUriStatsDB - phase3, create labels table completed")
    System.err.println("CreateLabelUriStatsDB: Filling the uris table ....")
    st.execute("INSERT INTO uris (uri, freq) SELECT uri, sum(freq) FROM labeluris GROUP BY uri")
    System.err.println("CreateLabelUriStatsDB - phase4, create uris table completed")
    
    // phase 5: calculate the relative frequencies
    // sql SELECT p.label, p.uri,((p.freq + 0.0) / l.freq) as per FROM labeluris as p JOIN labels AS l ON p.label = l.label order by p.label
    
    // this is commenness: the relative frequencies of each URI for some label, 
    // as a proprotion of the frequency of all occurrences of the label:
    // label uri1 n1  p1
    // label uri2 n2  p2
    // label uri3 n3  p3
    // relUriFreqByLabel(uri1) = p1 = n1 / (n1+n2+n3)
    System.err.println("CreateLabelUriStatsDB: Filling the LabelUriStatsUri table ...")
    st.execute(
      """INSERT INTO labeluristatsuri 
           (SELECT p.label, p.uri,((p.freq + 0.0) / l.freq) as prop 
            FROM labeluris as p 
                 JOIN labels AS l 
                 ON p.label = l.label)""")
    st.execute("COMMIT")                 
    System.err.println("CreateLabelUriStatsDB: labeluristatsuri table filled ...")
    
    // TODO: we should do this directly in the main labelUriInfo database
    // and directly update the labeluriinfo table in two steps: step one
    // where we insert into the labelUriInfo new rows with the label, uri
    // and source = "wp-anchor" if label/uri does not yet exist:
    //   INSERT INTO labeluriinfo (label,uri,source) (SELECT prop FROM labeluristatsuri where labeluriinfo.label = labeluristatsuri.label 
    //     AND labeluriinfo.uri = labeluristatsuri.uri) WHERE NOT EXISTS (SELECT * FROM labeluriinfo WHERE labeluriinfo.label = labeluristatsuri.label ..... 
    // Then do an update using UPDATE .... SET ... but can we do this with multiple fields?
    // Does UPDATE table1 t1 SET t1.f1 = t2.f1, t1.f2=t2.f2 FROM (SELECT ....) t2 WHERE ... work?
    
    System.err.println("CreateLabelUriStatsDB: updating labeluris with the labeluristatsuri table info ...")                 
    st.execute(
      """UPDATE labeluris 
         SET relUriFreqByLabel = 
           (SELECT prop 
            FROM labeluristatsuri 
            WHERE labeluris.label = labeluristatsuri.label 
                  AND labeluris.uri = labeluristatsuri.uri)""")                 
    st.execute("COMMIT")                 
    System.err.println("CreateLabelUriStatsDB: updating completed ...")
    System.err.println("Dropping the labeluristatsuri table ... ")
    //st.execute("DROP TABLE labeluristatsuri")
    st.execute("COMMIT")                 

    System.err.println("CreateLabelUriStatsDB: Filling the labeluristatslabel table ...")
    st.execute(
      """INSERT INTO labeluristatslabel 
           (SELECT p.label, p.uri,((p.freq + 0.0) / u.freq) as prop 
            FROM labeluris as p 
                 JOIN uris AS u 
                 ON p.uri = u.uri)""")
    st.execute("COMMIT")                 
    System.err.println("CreateLabelUriStatsDB: labeluristatslabel table filled ...")
    System.err.println("CreateLabelUriStatsDB: updating labeluris with the labeluristatslabel table info ...")                 
    st.execute(
      """UPDATE labeluris 
         SET rellabelfreqbyuri = 
           (SELECT prop 
            FROM labeluristatslabel 
            WHERE labeluris.label = labeluristatslabel.label 
                  AND labeluris.uri = labeluristatslabel.uri)""")                 
    st.execute("COMMIT")                 
    System.err.println("CreateLabelUriStatsDB: updating completed ...")
    System.err.println("Dropping the labeluristatslabel table ... ")
    //st.execute("DROP TABLE labeluristatsuri")
    st.execute("COMMIT")                 

    
    st.execute("SET FILES LOG TRUE")
    st.execute("CHECKPOINT DEFRAG")
    st.execute("SHUTDOWN")
    conn.close()
    System.err.println("CreateLabelUriStatsDB - Completed, total lines: "+nlines)    
  }
}
