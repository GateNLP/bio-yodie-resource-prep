import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException


/**
 * This program tries to create a table that associates each dbpedia instance
 * with its most specific DBPedia classes and with it's "interesting DBPedia classes".
 * The input for this is the processed TSV file which was created from either
 * the mapping-based types file or the heuristic mapping based types file.
 * This TSV file should contain two columns: the dbpedia instance URI and the dbpedia
 * class URI, both already in canonical, shortened format.
 * In the input file, each instance URI is associated with ALL class URIs that
 * apply (or at least many) so for each instance URI there can be many rows. 
 * This table will contain just one row per instance
 * which should contain the most specific DBPedia classes for the instance as
 * a |-separated list of shortened URIs and the most specific interesting classes,
 * also as a |-separated list of shortened URIs. The list of interesting classes
 * can also be completely empty (we do NOT use owl:Thing as an interesting class
 * in the output of this program, although the interesting class in the mappings
 * file can be owl:Thing)
 * 
 * Here is the algorithm in more detail:
 * For each input row
 * - look up the entry in the table for the instance URI
 * - if now row was found in the table add one 
 *   where the specific class is the canonical class we just read and where 
 *   the interesting class is empty or the interesting class of the class we just read
 * - if a row was found,
 *   find the canonical form of the class specified in the input row
 *   find the interesting class of that canonical form
 *   if the canonical form is a subclass of any of our current most specific
 *   classes, replace the existing one with it
 *   Add the interesting class if it is not already in the list 
 *
 * The program needs the location of a tsv file that contains, for each 
 * known class URI, the canonical URI, the interesting class URI and all
 * other canonical URIs which are a superclass.
 * The file is a tsv file with 3 columns:
 * yodie:hasCanonicalUri\torigUri\tcanonicalUri
 * yodie:hasInterestingClass\torigUri\tinterestingClass
 * yodie:hasSuperClass\torigUri\tsuperClass 
**/

object CreateUriClassDB {
  val progName = "CreateUriClassDB"
  def main(args: Array[String]) = {

    if(args.size != 3) {
      System.err.println(progName+" parms: databaseFilePathPrefix tablename subClassMapFile.tsv  < input.tsv > output.tsv")
      System.exit(1)
    }
    
    
    
    val dbname = args(0)
    val tablename = args(1)
    val mapFileName = args(2) 
    
    // This set contains subclassUri\tsuperclassUri strings where both
    // URIs are already canonical URIs
    val subClassMappings = scala.collection.mutable.Set[String]()
    
    // This map maps all known class URIs to their canonical URIs, if the 
    // the URI is already canonical, it is mapped to itself
    val uri2canonical = scala.collection.mutable.Map[String,String]()
    
    // This map maps all known canonical(!) URIs to their interesting class or owl:Thing
    val uri2interesting = scala.collection.mutable.Map[String,String]()
    
    scala.io.Source.fromFile(new java.io.File(mapFileName)).getLines.foreach { line =>
      val cols = line.split("""\t""",-1)
      if(cols.size != 3) {
        throw new RuntimeException(progName+" - not 3 columns (property, origclass, mappedclass) in subclass mappings file: "+line)
      }
      var propertyUri = cols(0)
      val origClassUri = cols(1)
      var mappedClassUri = cols(2)
      if(propertyUri.equals("yodie:hasSuperClass")) {
        subClassMappings += origClassUri+"\t"+mappedClassUri
      } else if(propertyUri.equals("yodie:hasInterestingClass")) {
        uri2interesting(origClassUri) = mappedClassUri
      } else if(propertyUri.equals("yodie:hasCanonicalUri")) {
        uri2canonical(origClassUri) = mappedClassUri
      } else {
        throw new RuntimeException(progName+": unknown property in subclass mappings file: "+propertyUri)
      }
    }
    uri2interesting("owl:Thing") = "owl:Thing" 


    val conn = utils.JdbcConnection.getConnection(dbname,false)
    
    val st = conn.createStatement()
    st.execute("SET AUTOCOMMIT FALSE")

    st.execute("DROP TABLE "+tablename+" IF EXISTS;")
    st.execute("CREATE TABLE "+tablename+
      " ( uri VARCHAR(100) NOT NULL PRIMARY KEY, specificClass VARCHAR(1000) NOT NULL, interestingClass VARCHAR(1000) NOT NULL)")
    
    val insertSt = conn.prepareStatement(
      "INSERT INTO "+tablename+" (uri,specificClass,interestingClass) VALUES(?,?,?);")
    val updateSt = conn.prepareStatement(
      "UPDATE "+tablename+" SET specificClass = ?, interestingClass = ? WHERE uri = ?;")
    val findSt = conn.prepareStatement(
      "SELECT specificClass, interestingClass FROM "+tablename+" WHERE uri = ?;");
      
    // read TSV file with 2 columns from stdin
    var nlines = 0
    var nadded = 0
    var nignored = 0
    var nerrors = 0
    val unknownClasses = scala.collection.mutable.Set[String]()
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      if(cols.size != 2) {
        throw new RuntimeException(progName+" - not 2 columns in stdin tsv line "+nlines+": "+line)
      }
      val uri = cols(0)
      val theClass = cols(1)
      // System.out.println("Processing uri "+uri+" and class "+theClass)
      
      
      // try to find a canonical URI for theClass: if we do not find anything
      // at all, the class is not in the dbpedia ontology, so for now we remember 
      // the class and count the row.
      val canonicalClass = uri2canonical.get(theClass).orNull
      if(canonicalClass == null) {
        nignored += 1
        unknownClasses.add(theClass)
      } else {
      
        var interestingClass = uri2interesting.get(canonicalClass).orNull
        // we should have a known interesting class for all known canonical
        // class URIs so if we get a null here, something is seriously wrong
        if(interestingClass == null) {
          throw new RuntimeException(progName+" - could not find interesting class for class "+canonicalClass+" input line "+nlines)
        }
        
        // we do not use owl:Thing for the interesting class, so we use an
        // empty string instead of owl:Thing
        if(interestingClass.equals("owl:Thing")) {
          interestingClass = ""
        }
        
        findSt.setString(1,uri)
        val rs = findSt.executeQuery()
        
        // check if we found something
        if(rs.next()) {
          // found something: check if we need to replace something or add to the lists
          val db_svalue = rs.getString(1)   // specific classes
          val db_ivalue = rs.getString(2)   // interesting class
          // System.out.println("have specific: "+db_svalue)
          // System.out.println("have interest: "+db_ivalue)
          var db_spec_classes = db_svalue.split("\\|") // array of all the most specific classes
          var db_int_classes = db_ivalue.split("\\|") // array of all the interesting classes
        
          // check if the new class is more specific, i.e. a subclass of any of the ones already stored
          // if yes, replace that one. Otherwise check if it is more generic or equal to one
          // of the ones already stored, if yes, just finish and do nothing.
          // If we go through and neither was the case, add the new class to the list
          // of specific classes
          var can_end = false     // we are finished with the loop
          var need_update = false
          for( i <- 0 until db_spec_classes.size; if !can_end) {
            var db_spec_class = db_spec_classes(i) 
            if(db_spec_class.equals(canonicalClass)) {
              can_end = true
            } else {
              // check if the the class we have is a superclass
              var check = db_spec_class+"\t"+canonicalClass
              if(subClassMappings.contains(check)) {
                can_end = true
              } else {
                // not equal and not a superclass, check if it is a subclass
                check = canonicalClass+"\t"+db_spec_class
                if(subClassMappings.contains(check)) {
                  // replace
                  db_spec_classes(i) = canonicalClass
                  can_end = true
                  need_update = true
                } // if mappings contains check (subclass)
              } // else of if mappings contains check (superclass)
            } // else of if classes equals
          } // for
        
          // if can_end is true, we have either replaced the subclass or found
          // an identical or superclasse. But if can_end is false, none of
          // the classes in the list was any of that, so we need to add the
          // class
          if(!can_end) {
            db_spec_classes = db_spec_classes :+ canonicalClass
            need_update = true
          }
        
          // if we need to update, first also re-calculate all the interesting
          // classes, then update the row.
          if(need_update) {
            // store the updated list of specific classes 
            //System.out.println("New specific: "+db_spec_classes.mkString("|"))
            updateSt.setString(1,db_spec_classes.mkString("|"))
            // now check if the interesting class is already in our list of interesting classes
            // if not: we need to add it
            // But we do never actually add Thing to the list!!
            if(!interestingClass.equals("") && !db_int_classes.contains(interestingClass)) {
              if(db_ivalue.equals("")) {
                updateSt.setString(2,interestingClass)
              } else {
                // at the moment we add ALL interesting classes, even if one
                // is a sub/superclass of the other (e.g. sports team and organization)
                db_int_classes = db_int_classes :+ interestingClass
                updateSt.setString(2,db_int_classes.mkString("|"))
              }
            } else {
              updateSt.setString(2,db_ivalue)
            }
            //System.out.println("New specific: "+db_int_classes.mkString("|"))
            updateSt.setString(3,uri)
            try {
              updateSt.executeUpdate()
            } catch {
              case e: Exception => {
                System.err.println(progName+" - Error when updating, IGNORED");
                System.err.println(progName+" - Line: "+line);
                System.err.println(progName+" - uri: "+uri)
                e.printStackTrace(System.err)
                nerrors += 1
              }
            }
          } // if need update
        } else {  // we do not have a result
        //System.out.println("No row in DB yet")
        // nothing found, create
        insertSt.setString(1,uri)
        insertSt.setString(2,canonicalClass)
        //System.out.println("Specific class: "+theClass)
        //System.out.println("interesting cl: "+interestingClass)
        insertSt.setString(3,interestingClass)
        try {
          insertSt.execute()
          nadded += 1
        } catch {
          case e: Exception => {
            System.err.println(progName+" - Error when inserting, IGNORED:");
            System.err.println(progName+" - Line: "+line);
            System.err.println(progName+" - uri: "+uri)
            e.printStackTrace(System.err)
            nerrors += 1
          }
        }
      } // end else we have a result
    } // end we found a canonical URI
    if((nlines % 100000) == 0) {
      System.err.println(progName+" - processed: "+nlines)
    }
    } // end foreach
    st.execute("COMMIT")
    utils.JdbcConnection.shutdown()
    System.err.println(progName+" - completed, total lines: "+nlines+" added: "+nadded+" ignored: "+nignored+" errors: "+nerrors)    
    unknownClasses.foreach { c =>
      System.err.println(progName+" - unknown class URI: "+c)
    }
  } // end main 
} // end class 
