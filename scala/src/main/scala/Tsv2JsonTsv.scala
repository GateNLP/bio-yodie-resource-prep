//import net.liftweb.json._
//import net.liftweb.json.JsonDSL._
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.JavaConversions
import java.util.HashMap
import java.util.ArrayList


// Read in a TSV file which is assumed to be ordered by key then whatever 
// fields we need to be used for sorting of array elements for the same key.
// At the moment the JSON object will always be an array which contains 
// for each row that was returned for the key a map with the key/value pairs.
// If the key is a unique key, there will be one element in the array always,
// otherwise there will be 1 or more elements.
// The first parameter is the 1 based index of the column that contains the
// key, the additional parameters are the names of the columns in the TSV
// file and are used as the map keys in the JSON.
// Note that for each row, the number of columns and the number of field names
// specified mus match exactly!
// 
// This reads from stdin and writes to stdout, log and error to stderr

object Tsv2JsonTsv {
    
  val MAX_SIZE = 100000  
  val mapper = new ObjectMapper()  
    
  def main(args: Array[String]) = {
    
    if(!(args.size > 2)) {
      System.err.println("parms: 1-based-keyfieldindex fieldname1 fieldname2 ...")
      System.exit(1)
    }
    val keyfieldindex = args(0).toInt - 1 // 1-based!!
    val fieldNames = args.slice(1, args.size)
    val nrFields = fieldNames.size
    
    var nlines = 0
    var njson = 0
    var lastKey = ""
    var firstTime = true
    var curList: ArrayList[HashMap[String,Object]] = null
    
    scala.io.Source.fromInputStream(System.in).getLines.foreach { line =>
      nlines += 1
      val cols = line.split("""\t""",-1)
      val nrCols = cols.size
      if(nrCols != nrFields) {
        System.err.println("Tsv2JsonTsv ERROR: number of columns:"+
                           nrCols+" and number of fieldnames:"+
                           nrFields+" does not match for line nr:"+nlines+": "+line);
        System.exit(1);
      }
      val key = cols(keyfieldindex)
      val map = new HashMap[String,Object]()
      for ( i <- 0 until nrCols ) {
        val v = cols(i)
        map.put(fieldNames(i),toObject(v))
      }
      
      //System.err.println("key="+key+", value="+mapper.writeValueAsString(map))
      
      if(firstTime || key != lastKey) {
        if(!firstTime) {
          writeRow(lastKey,curList)
          njson += 1
        } else {
          firstTime = false
        }
        curList = new ArrayList[HashMap[String,Object]]()
        lastKey=key
      }
      curList.add(map)
      
      if((nlines % 100000) == 0) {
        System.err.println("Tsv2JsonTsv - read: "+nlines+" written: "+njson)
      }
    }
    writeRow(lastKey,curList)
    njson += 1

    System.err.println("Tsv2JsonTsv - Total rows retrieved: "+nlines+" lines written: "+njson)
  }
  
  // Write one row of label->Json mapping.
  // Currently we write whatever we get, no matter how big it is, BUT:
  // This could check if the String representation
  // is longer than our maximum. If yes (which should happen rarely), we could remove
  // entries from the array until the String representation is short enough.
  // The strategy of removing elements could be based on the following heuristic:
  // - scFreqLabelInstByLabel (commonness) must be low
  // - absolute frequency of the uri should be low
  // - difference between the candidate label and this label (lastKey) must be
  //   large
  def writeRow(lastKey: String, curList: ArrayList[HashMap[String,Object]]): Unit = {
    var asString = mapper.writeValueAsString(curList)
    if(asString.size > MAX_SIZE) {
      // could reduce size here!
    }
    // avoid temporary concatenation here and print parts separately
    System.out.print(lastKey)
    System.out.print("\t")
    System.out.println(asString)
  }
  
  
  
  def getStringType(s: String): String = {
    var t = "s"
    try {
      val tmp = s.toInt
      t = "i"
    } catch {
      case _: Throwable => {
        try {
          val tmp = s.toDouble
          t = "d"
        } catch {
          case _: Throwable => {
            //
          }
        }
      }
    }
    t
  }
  def toObject(s: String): Object = {
    var ret: Object = s
    try {
      val tmp = s.toInt
      ret = tmp.asInstanceOf[java.lang.Integer]
    } catch {
      case _: Throwable => {
        try {
          val tmp = s.toDouble
          ret = tmp.asInstanceOf[java.lang.Double]
        } catch {
          case _: Throwable => { }
        }
      }
    }
    ret
  }
                
                
  
}
