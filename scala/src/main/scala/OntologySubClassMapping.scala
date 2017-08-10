import gate.Gate
import gate.Factory
import gate.creole.ontology._
import java.io.File
import scala.collection.JavaConversions._
import scala.collection.mutable.Set

// create a datastructure for very fast lookup of subclass relationships
// and store it as a file.
// The final datastructure is a set that will contain all strings of 
// form "URIA\tURIB\tURIC" for which URIA is a (proper) subclass of URIB.
// (so "URIA\tURIA" is not part of the set)
// The set will be written as a file that contains all pairs as
// separate lines.
// URIC is the "interesting class" associated with URIA. 

// TODO: at the moment the list of interesting classes is so short we
// directly hard-code it in here. Could also be read from a file!!

object OntologySubClassMapping {
  def main(args: Array[String]) = {

    if(args.size != 1) {
      System.err.println("parms: inputOWLFile.owl (output to standard output)")
      System.exit(1)
    }
   
    // TODO: maybe read from an external file eventually!
    val interestingClassUris = Array(
      "http://dbpedia.org/ontology/Person",
      "http://dbpedia.org/ontology/PopulatedPlace",
      "http://dbpedia.org/ontology/SportsTeam",
      "http://dbpedia.org/ontology/Organisation"
      )
    
    
    val gateHome = System.getenv().get("GATE_HOME")
    if(gateHome == null) {
      System.err.println("Environment variable GATE_HOME not set!")
      System.exit(1)
    }
    System.err.println("OntologySubClassMapping - Initializeing GATE")
    Gate.setGateHome(new File(gateHome))
    Gate.runInSandbox(true)
    Gate.init()
    // register the Ontology plugin
    System.err.println("OntologySubClassMapping: Registering Ontology plugin")
    val pluginHome = new File(new File(gateHome,"plugins"), "Ontology")
    Gate.getCreoleRegister().registerDirectories(pluginHome.toURI().toURL())
    
    // create an ontology object from the OWL file given
    val parms = Factory.newFeatureMap()
    parms.put("rdfXmlURL",new File(args(0)).toURI().toURL())
    parms.put("persistent",new java.lang.Boolean(false))
    parms.put("loadImports",new java.lang.Boolean(true))
    System.err.println("OntologySubClassMappign - Creating and loading ontology")
    val onto = Factory.createResource("gate.creole.ontology.impl.sesame.OWLIMOntology",parms).asInstanceOf[Ontology]
    //println("Ontology created")
    // get all the class uris
    val allClasses1 = onto.getOClasses(false)
    //println("OntologySubClassMapping - Got classes: "+allClasses.size)
    // now convert the interesting class URIs into interesting class objects
    val interestingClasses = interestingClassUris.map { uri =>       
      onto.getOClass(onto.createOURI(uri)) 
    }
    val defaultInterestingClass = onto.getOClass(onto.createOURI("http://www.w3.org/2002/07/owl#Thing"))

    // the dbpedia ontology includes several different URIs for the same entity, for example
    // Settlement is the same as PopulatedPlace. In order to make it easier to always refer
    // to a class with the same URI, we use the following strategy to find a canoncial URI
    // for all classes:
    // = if a class is equivalent to an interesting class URI, replace its URI with the interesting class URI
    // = otherwise if a class is equivalent to one or more other classes, use the URI which 
    //   is a dbpedia.org URI and sorts lowest lexically. If the original class is not a dbpedia.org
    //   URI and there is no equivalent dbpedia.org URI, remove the class 
    
    // for each class which either is already has its canonical URI or 
    // for which a canonical URI exists, we output a line 
    // yodie:hasCanonicalUri  originalUri  canonicalUri
    // where both URIs are already shortened.
    var nequiv = 0
    val allClasses = scala.collection.mutable.Set[OClass]()
    allClasses1.foreach { aClass =>
      val uriString = aClass.getONodeID().toString()
      val origUriString = uriString
      // check if this class has an equivalent class which is an interesting class
      // in that case, use that interesting class instead
      var intClass = interestingClasses.find { c => c.isEquivalentClassAs(aClass) }.orNull
      if(intClass != null) {
        //System.err.println("Adding class 1: "+intClass.getONodeID().toString());
        allClasses.add(intClass)
        System.out.println("yodie:hasCanonicalUri\t"+origUriString+"\t"+intClass.getONodeID().toString());
        nequiv += 1
      } else {
        // we did not find an equivalent interesting class, so lets check all the equivalent
        // classes to see if there is one that should get preferred
        
        // first get all equivalent classes which are in the dbpedia.org namespace, or null
        val equivClasses = aClass.getEquivalentClasses().find { c => c.getONodeID().toString().startsWith("http://dbpedia.org/ontology/") }.orNull
        // if we did not find any alternative, then add the current class, but only if it is from the dbpedia namespace
        if(equivClasses == null) {
          if(uriString.startsWith("http://dbpedia.org/ontology/")) {
            //System.err.println("Adding class 2: "+aClass.getONodeID().toString());
            allClasses.add(aClass)
            System.out.println("yodie:hasCanonicalUri\t"+origUriString+"\t"+aClass.getONodeID().toString());
            nequiv += 1
          } else {
            //System.err.println("NOT adding class 2: "+aClass.getONodeID().toString())
          }
        } else {
          //val ecs = scala.collection.mutable.Set[OClass]()
          //ecs.add(equivClasses)
          //System.err.println("Equivalent classes: "+ ecs.map { _.getONodeID().toString() })
          // we got equivalent classes: find the class with the lexically smallest
          // URI among the equvalent classes and this class (if it is from the dbpedia namespace)
          val alternatives = scala.collection.mutable.Set[OClass]()
          alternatives.add(equivClasses)
          if(uriString.startsWith("http://dbpedia.org/ontology/")) {
            alternatives.add(aClass)
          }
          //System.err.println("Alternatives: "+ alternatives.map { _.getONodeID().toString() })
          val lowest = alternatives.toSeq.sortWith(_.getONodeID().toString() < _.getONodeID().toString()).head
          //System.err.println("Adding class 3: "+lowest.getONodeID().toString());
          allClasses.add(lowest)
          System.out.println("yodie:hasCanonicalUri\t"+origUriString+"\t"+lowest.getONodeID().toString());
          nequiv += 1
        }
      }
    }
    
    var nsubs = 0
    var nints = 0
    
    //println("interesting classes: "+interestingClasses.mkString(","))
    allClasses.foreach { aClass => 
      val uriString = aClass.getONodeID().toString()
      //if(uriString.matches("""http://dbpedia.org/ontology/.*""")) {
        // get all the superclasses of this class
        val superClasses = aClass.getSuperClasses(OConstants.Closure.TRANSITIVE_CLOSURE)
        //println("Got DBPedia class "+uriString+" with superclasses: "+superClasses.size)
        // find the first interesting class that is a superclass of the class
        val intClass = interestingClasses.find{ c => 
          c.equals(aClass) || c.isSuperClassOf(aClass,OConstants.Closure.TRANSITIVE_CLOSURE) 
        }.getOrElse(defaultInterestingClass)
        System.out.println("yodie:hasInterestingClass\t"+uriString+"\t"+intClass.getONodeID().toString());
        nints += 1          
        // create the set of all classes which are superclasses, are identical
        // to one of the classes which have a canonical URI, and are not 
        // identical to the original class itself.
        val canonicalSuperClasses = scala.collection.mutable.Set[OClass]()
        superClasses.foreach { superClass =>
          if(!superClass.equals(aClass)) {
            if(allClasses.contains(superClass)) {
              canonicalSuperClasses += superClass
            }
          }
        }
        canonicalSuperClasses.foreach { superClass =>
          System.out.println("yodie:hasSuperClass\t"+uriString+"\t"+superClass.getONodeID().toString());
          nsubs += 1          
        }
        // The ontology plugin does not return Thing as superclass, so we have to add it here
        System.out.println("yodie:hasSuperClass\t"+uriString+"\t"+"http://www.w3.org/2002/07/owl#Thing");
        nsubs += 1
      //}
    }
    System.err.println("OntologySubClassMapping: DONE, subclass relations: "+nsubs+", canoncal mappings: "+nequiv+", interesting class mappings: "+nints)
    onto.cleanup()
    
  }
}
    
