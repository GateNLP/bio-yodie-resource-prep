import gate.*

// 0) Check the parameters: 
// = directory containing the documents
if(args.size() != 2) {
  System.err.println("Usage: cui-counts <input-directory> <cuis/labels>")
  System.exit(1)
}
File docDir = new File(args[0]);
String cuisorlabels = args[1];

// 1) set up GATE
String gatehome= System.getenv()['GATE_HOME']
if(gatehome == null) {
  System.err.println("Environment variable GATE_HOME not set!")
  System.exit(1);
}
Gate.setGateHome(new File(gatehome))
Gate.runInSandbox(true)
Gate.init()

gate.Utils.loadPlugin("Format_FastInfoset")

Map<String, Integer> cuicounts = new HashMap<String, Integer>();
Map<String, String> cuilabels = new HashMap<String, String>();
Map<String, Map<String, Integer>> labelcuicounts = new HashMap<String, Map<String, Integer>>();
Map<String, Integer> labelcounts = new HashMap<String, Integer>();

int totalanns = 0;
int totaldocs = 0;

// 5) iterate over the documents in the directory
// for now, we only read files with an .xml extension
def extFilter = ~/.+\.(?:xml|finf)/
docDir.traverse(type: groovy.io.FileType.FILES, nameFilter: extFilter) { file ->
  // create a document from that file
  String fileName = file.getName()
  FeatureMap parms = Factory.newFeatureMap();
  parms.put(Document.DOCUMENT_ENCODING_PARAMETER_NAME, "UTF-8")
  parms.put("sourceUrl", file.toURI().toURL())
  Document doc = (Document) gate.Factory.createResource("gate.corpora.DocumentImpl", parms)
  doc.setName(fileName)

  totaldocs++;

  AnnotationSet lls = doc.getAnnotations("Key").get("Mention")
  for(Annotation ll : lls){
    totalanns++;    
    String cleanstring = gate.Utils.cleanStringFor(doc, ll).toLowerCase();
    String cui = ll.getFeatures().get("CUI").toString();
    if(cui.equals("null")) cui=ll.getFeatures().get("readable").toString();

    HashMap cuicount = labelcuicounts.get(cleanstring);
    if(cuicount==null){
      cuicount = new HashMap<String, Integer>();
      cuicount.put(cui, 1);
      labelcuicounts.put(cleanstring, cuicount)
    } else {
      Integer count = cuicount.get(cui);
      if(count==null){
        cuicount.put(cui, 1);
      } else {
        cuicount.put(cui, count+=1);
      }
    }

    Integer labelcount = labelcounts.get(cleanstring);
    if(labelcount==null){
      labelcounts.put(cleanstring, 1);
    } else {
      labelcounts.put(cleanstring, labelcount+=1);
    }

    Integer count = cuicounts.get(cui);
    if(count==null){
      cuicounts.put(cui, 1);
    } else {
      cuicounts.put(cui, count+=1);
    }
    cuilabels.put(cui, ll.getFeatures().get("readable"));
  }
}

if(cuisorlabels.equals("cuis")){
 Iterator keyIt = cuicounts.keySet().iterator()
 while(keyIt.hasNext()){
   key = keyIt.next()
   Integer count = cuicounts.get(key);
   String readable = cuilabels.get(key);
   println(key + "\t" + count + "\t" + count/totalanns);
 }
}

if(cuisorlabels.equals("labels")){
 Iterator labelIt = labelcuicounts.keySet().iterator();
 while(labelIt.hasNext()){
   String label = labelIt.next();
   HashMap cuicountsforlabel = labelcuicounts.get(label);
   int totalforlabel = 0;
   Iterator cuiIt = cuicountsforlabel.keySet().iterator();
   while(cuiIt.hasNext()){
   String cui = cuiIt.next();
    if(!cui.equals("<SPURIOUS>") && !cui.equals("<NONE OF ABOVE>")){
     Integer count = cuicountsforlabel.get(cui);
     println(label.replace("\"","'") + "\t" + cui + "\t" + count + "\t" + count/labelcounts.get(label));
    }
   }
 }
}

//println(totalanns + " total annotations in " + totaldocs + " documents (" + totalanns/totaldocs + " average).")


