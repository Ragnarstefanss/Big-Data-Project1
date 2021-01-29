package dht

import java.io._
import java.util.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.annotation.meta.param
import java.util.ArrayList

class Stats(fileName: String) {


  init()
  val fileWrite = new FileWriter(fileName, true)
  private def init() = {
    val file = new File(fileName)    
    if (file.exists) {
       file.delete()
    }
    file.createNewFile();
  }

  def destroy() = {
    fileWrite.close()
  }

  def writeLine(line: String) = {
    fileWrite.write(s"$line\n")
  }

  private def writeArgs(s: Int, params: HashMap[String, Int]) = {
    val e = params.get("E")
    val n = params.get("N")
    val w = params.get("W")
    val i = params.get("I")
    val m = params.get("M")
    val b = params.get("B")
    writeLine("S E N W I M B")
    writeLine(s"$s $e $n $w $i $m $b")
  }

  private def writeNodeDistributions(dht: DHT) = {    
    writeLine("Node IDs (sorted)")
    writeLine(dht.sortedNodeIds.toArray().mkString(" "))
    writeLine("Extent distribution (over nodes)")
    writeLine(dht.sortedNodeIds.toArray().map((id) => dht.nodes.get(id).extents.size()).mkString(" "))
    writeLine("Write distribution (over nodes)")
    writeLine(dht.sortedNodeIds.toArray().map((id) => dht.nodes.get(id).writes).mkString(" "))
  }

  private def writeAllExtents(dht: DHT) = {
    val extentKeys = new ArrayList[BigInt]()
    dht.nodes.values().forEach((n) => n.extents.keySet().forEach((id) => {
      extentKeys.add(id)
    }))
    extentKeys.sort((id1,id2) => id1.compareTo(id2))
    writeLine("Extent keys (sorted)")
    writeLine(extentKeys.toArray().mkString(" "))
  }

  private def writeExtentResponsibility(dht: DHT) = {
    writeLine("Extents by nodes")
    dht.sortedNodeIds.forEach((nId) => {
      val extents = new ArrayList[BigInt]()
      dht.nodes.get(nId).extents.keySet().forEach((eId) => {
        extents.add(eId)
      })
      extents.sort((id1,id2) => id1.compareTo(id2))
      val sortedExtentString = extents.toArray().mkString(" ")
      writeLine(s"$nId: $sortedExtentString")
    })
  }

  private def writeJumps(dht: DHT) = {
    writeLine("Jumps")
    writeLine(dht.jumps.toArray().mkString(" "))
  }

  private def writeFingerTable(dht: DHT) = {
    writeLine("Fingertable")
    dht.sortedNodeIds.forEach((id) => {
      val fingers = dht.nodes.get(id).fingerTable.mkString(" ")
      writeLine(s"$id: $fingers")
    })
  }
  
  def analyze(params: HashMap[String, Int], dht: DHT, iteration: Int) = {
    writeLine("########################")
    writeLine(s"Iteration $iteration")    
    writeArgs(dht.nodeCount, params)
    writeNodeDistributions(dht)
    writeAllExtents(dht)
    writeExtentResponsibility(dht)
    writeJumps(dht)
    writeFingerTable(dht)
  }
}
