// Import data from a tsv into a neovj database, using the embedded API for speed

import collection.JavaConversions._
import scala.language.implicitConversions
import com.jpetrak.miscfastcompact.graph.GraphStore
import com.jpetrak.miscfastcompact.graph.NodeNameStore
import com.jpetrak.miscfastcompact.graph.Edge
import java.util.zip.GZIPInputStream;
import java.io.File;
import java.util.ArrayList
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.ObjectOutputStream
import java.util.zip.GZIPOutputStream
import org.rogach.scallop._;

object CreateFastGraph {

  val runtime = Runtime.getRuntime()
  val MB = 1024*1024
  
  def main(args: Array[String]) = {
    if(args.size != 3) {
      System.err.println("Need 3 args: edgesByFrom.tsv.gz edgesByTo.tsv.gz graphFileToCreate.gz")
      System.exit(1)
    }
    
    var nrlines = 0
    var uri1 = ""
    var uri2 = ""
    
    val gstore = new GraphStore
    val nodeNames = new NodeNameStore
    System.out.println("Initial used memory: "+usedMem)
    
    val fromEdgesUrl = new File(args(0)).toURI().toURL()
    scala.io.Source.fromInputStream(new GZIPInputStream(fromEdgesUrl.openStream())).getLines.foreach { line =>
      nrlines += 1
      val cols = line.split("""\t""",-1)
      uri1 = cols(0)
      uri2 = cols(1)
      gstore.addNode(uri1);
      //nodeNames.addNode(uri1);
      gstore.addNode(uri2);
      //nodeNames.addNode(uri2);
      if((nrlines % 100000) == 0) { report(nrlines) } 
    }
    report(nrlines)
    System.out.println("Finished loading the URIs, loding the outgoing edges")
    
    nrlines = 0
    var nodeId = 0
    var oldUri = ""
    var edges: ArrayList[Edge] = null
    scala.io.Source.fromInputStream(new GZIPInputStream(fromEdgesUrl.openStream())).getLines.foreach { line =>
      nrlines += 1
      // split and get the two Uris and the count
      val cols = line.split("""\t""",-1)
      uri1 = cols(0)
      uri2 = cols(1)
      val count = cols(2).toInt
      // whenever we get a new from uri (uri1) we do the following:
      // = if we already have edge data, add it to the graph
      // = re-initialize the edge data lists
      if(uri1 != oldUri) {
        if(edges != null) {
          nodeId = gstore.getNodeId(oldUri)
          if(nodeId == -1) {
            System.err.println("Node not found: "+oldUri)
            System.exit(1)
          }
          gstore.addOutEdges(nodeId,edges)
        }
        edges = new ArrayList[Edge]()
        oldUri = uri1
      }
      val otherId = gstore.getNodeId(uri2)
      if(otherId == -1) {
        System.err.println("Other Node not found: "+oldUri)
        System.exit(1)
      }
      edges.add(new Edge(count,otherId))
      if((nrlines % 100000) == 0) { report(nrlines) }
    }
    gstore.addOutEdges(gstore.getNodeId(oldUri),edges)
    report(nrlines)
    System.out.println("Finished loading the outgoing edges, loding the ingoing edges")
    
    nrlines = 0
    oldUri = ""
    val toEdgesUrl = new File(args(1)).toURI().toURL()
    edges = null
    scala.io.Source.fromInputStream(new GZIPInputStream(toEdgesUrl.openStream())).getLines.foreach { line =>
      nrlines += 1
      // split and get the two Uris and the count
      val cols = line.split("""\t""",-1)
      uri1 = cols(0)
      uri2 = cols(1)
      val count = cols(2).toInt
      // whenever we get a new to uri (uri2) we do the following:
      // = if we already have edge data, add it to the graph
      // = re-initialize the edge data lists
      if(uri2 != oldUri) {
        if(edges != null) {
          gstore.addInEdges(gstore.getNodeId(oldUri),edges)
        }
        edges = new ArrayList[Edge]()
        oldUri = uri2
      }
      val otherId = gstore.getNodeId(uri1)
      edges.add(new Edge(count, otherId))
      if((nrlines % 100000) == 0) { report(nrlines) }
    }
    gstore.addInEdges(gstore.getNodeId(oldUri),edges)
    report(nrlines)
    
    // Now save the whole Graph into a file
    var outFile = new File(args(2))
    System.out.println("Saving graph to output file "+outFile)
    var output: OutputStream = new FileOutputStream(outFile);
    output = new GZIPOutputStream(output)
    val outobject = new ObjectOutputStream(output)
    outobject.writeObject(gstore)
    //outobject.writeObject(nodeNames)
    outobject.flush()
    outobject.close()
    System.out.println("Graph saved")
  } // main  
    
    
    
  def report(nrlines: Int): Unit = {
    System.out.println("Processed "+nrlines+" mem="+usedMem)
  }
  def usedMem: Long = {
    System.gc()
    (runtime.totalMemory-runtime.freeMemory())/MB
  }

} // object FastGraph
