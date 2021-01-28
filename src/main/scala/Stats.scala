package dht

import java.io._
import java.util.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.annotation.meta.param

class Stats() {
  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
  
  def analyze(params: HashMap[String, Int], dht: DHT, experiment: Int) = {
    
    val param_values = (params.get("B") + " " + params.get("S") + " " + params.get("E") + " " + params.get("W") + " " + params.get("I") + " " + params.get("M") + " " + params.get("N"))
    //println(dht.nodes.extent)
    

    /*
    dht.sortedNodeIds.forEach((id) => {
      val x = dht.nodes.get(id).extents
      print(s"$x ")
    })*/
     
    var extents_size: String = ""
    dht.sortedNodeIds.forEach((id) => {
      val x = dht.nodes.get(id).extents.size()
      print(s"$x ")
      extents_size = extents_size.concat(x.toString + " ")
    })
    
    
    dht.randomWrites(params.get("W"))
    var distribution: String = ""
    dht.sortedNodeIds.forEach((id) => {
      val x = dht.nodes.get(id).writes
      distribution = distribution.concat(x.toString + " ")
    })
    //print("sd " + distribution)
    
    val data = Array(
      "Experiment 1",
      "Parameters:",
      "B S E W I M N",  
      param_values, 
      "Extends size over nodes:",
      extents_size,
      "distribution",
      distribution

    )

    var experiment_counter: Int = 1
    var file_name: String = "experiment" + experiment_counter+".txt"
    print("filename: " + file_name)
    printToFile(new File(file_name)) { p => 
      data.foreach(p.println)
    }
    experiment_counter = experiment_counter + 1
    
    
  }
}
