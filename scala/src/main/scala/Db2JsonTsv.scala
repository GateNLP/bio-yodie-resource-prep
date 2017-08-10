import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException
//import net.liftweb.json._
//import net.liftweb.json.JsonDSL._
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.JavaConversions
import java.util.HashMap
import java.util.ArrayList


// Execute a query on a database and write the result as a TSV file that contains
// two fields: the key and a JSON object.
// At the moment the JSON object will always be an array which contains 
// for each row that was returned for the key a map with the key/value pairs.
// If the key is a unique key, there will be one element in the array always,
// otherwise there will be 1 or more elements.

object Db2JsonTsv {
  def main(args: Array[String]) = {
    
    if(!(args.size == 2)) {
      System.err.println("parms: queryfilename|- 1-based-keyfieldindex")
      System.exit(1)
    }
    val queryfile = args(0)
    val keyfieldindex = args(1).toInt // 1-based!!
    
    val query = 
      if(queryfile == "-") { 
        scala.io.Source.fromInputStream(System.in).mkString 
      } else {
        scala.io.Source.fromFile(queryfile).mkString        
      }
    val conn = utils.JdbcConnection.getConnection(readOnly=true)
    
    val selectSt = conn.prepareStatement(query)

    System.err.println("Db2JsonTsv: Executing query\n"+query)
    val rs = selectSt.executeQuery()
    var nlines = 0
    var njson = 0
    var lastKey = ""
    var firstTime = true
    var curList: ArrayList[HashMap[String,Object]] = null
    val mapper = new ObjectMapper()
    while(rs.next()) {
      nlines += 1
      val meta = rs.getMetaData()
      val nrCols = meta.getColumnCount()

      val key = rs.getObject(keyfieldindex).toString()    
      val map = new HashMap[String,Object]()
      for ( i <- 1 to nrCols ) {
        if(meta.getColumnTypeName(i) == "DOUBLE") {
          map.put(meta.getColumnName(i),rs.getDouble(i): java.lang.Double)
        } else if(meta.getColumnTypeName(i) == "INTEGER") {
          map.put(meta.getColumnName(i),rs.getInt(i): java.lang.Integer)
        } else {
          map.put(meta.getColumnName(i),rs.getObject(i).toString())
        }
      }
      
      //System.err.println("key="+key+", value="+mapper.writeValueAsString(map))
      
      if(firstTime || key != lastKey) {
        if(!firstTime) {
          System.out.println(lastKey+"\t"+mapper.writeValueAsString(curList))
          njson += 1
        } else {
          firstTime = false
        }
        curList = new ArrayList[HashMap[String,Object]]()
        lastKey=key
      }
      curList.add(map)
      
      if((nlines % 100000) == 0) {
        System.err.println("Db2JsonTsv - retrieved: "+nlines+" written: "+njson)
      }
    }
    System.out.println(lastKey+"\t"+mapper.writeValueAsString(curList))
    njson += 1

    utils.JdbcConnection.shutdown()
    System.err.println("Db2JsonTsv - Total rows retrieved: "+nlines+" lines written: "+njson)
  }
}
