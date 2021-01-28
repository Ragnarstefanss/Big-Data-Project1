package dht

import scala.math.pow
import scala.math.BigInt
import java.util.HashMap
import java.util.Random
import java.util.HashSet
import java.util.Dictionary
import java.util.ArrayList
import scala.collection.mutable.Queue
import scala.io.Source
import java.awt.RenderingHints.Key

final case class CollisionException() extends Exception("Collision!", None.orNull) 


class KeyMaker() {

    val ips = new Queue[String]()
    val files = new Queue[String]()

    for (line <- Source.fromFile("ips.txt").getLines) {
        ips.enqueue(line.trim())
    }

    for (line <- Source.fromFile("movies.txt").getLines) {
        files.enqueue(line.trim() + ".avi")
    }
}

class RandomHelper() {
    private val nodeAccessRNG = new Random()
    private val extentAccessRNG = new Random()

    def randomExtent(extents: Array[String]): String = {
        return extents(extentAccessRNG.nextInt(extents.length))
    }

    def randomNode(nodes: HashMap[BigInt, Node], ids: ArrayList[BigInt]): Node = {
        return nodes.get(ids.get(nodeAccessRNG.nextInt(nodes.size())))
    }
}


class Sha1(keyBits: Int) {
    private val mod = BigInt(2).pow(keyBits)
    private val md = java.security.MessageDigest.getInstance("SHA-1")
    
    def hash(key: String): BigInt = {
        return BigInt(this.md.digest(key.getBytes("UTF-8")).map("%02x".format(_)).mkString, 16).mod(this.mod)
    }
}

class Extent() {
    var writes: Int = 0
    def incrementWrites() = {
        writes += 1
    }
}

class Node(keyBits: Int, val id: BigInt) {
    var prev: Node = null
    var fingerTable: Array[Node] = new Array[Node](keyBits)
    var extents = new HashMap[BigInt, Extent]()
    var extentCopies = new HashMap[BigInt, Extent]
    var writes: Int = 0

    def addExtent(id: BigInt) {
        if (extents.containsKey(id)) throw new CollisionException()
        extents.put(id, new Extent())
    }

    def addExtentCopy(id: BigInt) {
        if (extents.containsKey(id)) throw new CollisionException()
        extentCopies.put(id, new Extent())
    }

    def incrementWrites(id: BigInt) = {
        this.writes += 1
        this.extents.get(id).incrementWrites()
    }

    override def toString() : String = {
        return id.toString()
    }
}

class DHT (var nodeCount: Int, val extents: Int, val copies: Int) {
    val keyBits = 40
    val mod = BigInt(2).pow(keyBits)
    val rand = new RandomHelper()
    val nodeHasher = new Sha1(keyBits)
    val extentHasher = new Sha1(keyBits)
    val keyMaker = new KeyMaker()

    var nodes = new HashMap[BigInt, Node]()
    val extentKeys = new Array[String](extents)
    var sortedNodeIds = new ArrayList[BigInt]()
    
    createExtentKeys()
    generateInitialNodes()
    generateInitialExtents()

    private def createExtentKeys() = {
        var i = 0
        while (i < extents) {
            extentKeys(i) = keyMaker.files.dequeue()
            i+= 1
        }
    }

    def BinarySearchGreaterOrEqual(arr: ArrayList[BigInt], lookingFor: BigInt): Int = {
        var lo = 0
        var hi = arr.size()
        val cap = hi
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (arr.get(mid) < lookingFor) {
                lo = mid + 1
            } else {
                hi = mid
            }
        }
        if (hi == cap) return 0
        return hi
    }

    private def generateInitialNodes() = {
        while (this.nodes.size() < nodeCount) {
            val newKey = keyMaker.ips.dequeue()
            val newId = nodeHasher.hash(newKey)
            if (this.nodes.containsKey(newId)) {
                throw new CollisionException()
            }
            this.nodes.put(newId, new Node(keyBits, newId))
        }
        this.nodes.keySet().stream().sorted().forEach((id) => this.sortedNodeIds.add(id))
        
        val largestId = this.sortedNodeIds.get(nodeCount - 1)
        var n = 0
        for(n <- 1 to nodeCount) {          
            val currNode = this.nodes.get(this.sortedNodeIds.get((n - 1) % nodeCount))
            currNode.prev = this.nodes.get(this.sortedNodeIds.get((nodeCount + n - 2) % nodeCount))

            var i = 0
            var offset = 1
            for (i <- 1 to keyBits) {
                val index = BinarySearchGreaterOrEqual(sortedNodeIds, (currNode.id + offset) % mod)
                currNode.fingerTable(i - 1) = this.nodes.get(this.sortedNodeIds.get(index))
                offset *= 2
            }
        }
    }

    private def generateInitialExtents() = {
        extentKeys.foreach((key) => {
            val id = extentHasher.hash(key)
            val index = BinarySearchGreaterOrEqual(sortedNodeIds, id)
            val node = this.nodes.get(this.sortedNodeIds.get(index))
            node.addExtent(id)            
            var copyNode = node.fingerTable(0)
            for(n <- 1 to copies - 1) {
                copyNode.addExtentCopy(id)
                copyNode = copyNode.fingerTable(0)
            }
        })
    }

    def addNodes(nodes: Int) = {
        var n = 0;
        for(n <- 1 to nodes) {            
            // Generate random key
            val key = "placeholder"
            this.addNode(key)
        }
    }

    def addNode(key: String) {
        // TODO: Add node logic here
        this.nodeCount += 1
    }

    def randomWrites(writes: Int) = {
        val startingNode = rand.randomNode(nodes, sortedNodeIds)
        var w = 0;
        for(w <- 1 to writes) {
            val key = rand.randomExtent(extentKeys)
            write(key, startingNode)
        }
    }

    def write(extentKey: String, startingNode: Node) = {
        val id = this.extentHasher.hash(extentKey)
        println(startingNode)
        nodes.values().forEach((n) => {
            if (n.extents.containsKey(id)) {
                println("in " + n.id.toString())
            }
        })
        System.exit(0)
        val node = findNodeResponsibleForId(startingNode, id)
        node.incrementWrites(id)
    }

    def distance(id1: BigInt, id2: BigInt): BigInt = {
        if (id1 <= id2) return id2 - id1
        return this.mod + id2 - id1
    }

    def findNodeResponsibleForId(currentNode: Node, id: BigInt): Node = {
        if (currentNode.extents.containsKey(id)) return currentNode        
        var i = 0
        for (i <- keyBits - 1 to 0 by -1) {
            val neighborId = currentNode.fingerTable(i).id
            // If this doesnt work, try 
            if (distance(neighborId, id) <= distance(currentNode.id, id)) {
            //if (currentNode.id < neighborId && neighborId <= id) {
                return findNodeResponsibleForId(currentNode.fingerTable(i), id)
            }
        }
        throw new Exception("WAT!")
    }
}

object Main extends App {
  def analyze(params: HashMap[String, Int], dht: DHT) {
    println("Do statistical analysis here")
    // TODO: stats
  }

  def defaultArgs(): HashMap[String, Int] = {        
    var defaultArgs = new HashMap[String, Int]()
    defaultArgs.put("S", 10)
    defaultArgs.put("E", 10000)
    defaultArgs.put("N", 3)
    defaultArgs.put("W", 1000000)
    defaultArgs.put("I", 5)
    defaultArgs.put("M", 30)
    return defaultArgs        
  }

  def argparse(args: Array[String]): HashMap[String, Int] = {
    var argMap = defaultArgs()   
    def next(i: Int) {
      if (i < args.length - 1) {
        var value = 0
        try {
          value = args(i+1).toInt
        } catch {
          case e: NumberFormatException => {
            next(i+1)
            return
          }
        }
        var jump = 2
        args(i) match {
          case "-S" => argMap.put("S", value)
          case "-E" => argMap.put("E", value)
          case "-N" => argMap.put("N", value)
          case "-W" => argMap.put("W", value)
          case "-I" => argMap.put("I", value)
          case "-M" => argMap.put("M", value)
          case _ => jump = 1
        }
        next(i + jump)
      }
    }
    next(0)
    return argMap
  }


  val params = argparse(args)
  val dht = new DHT(params.get("S"), params.get("E"), params.get("N"))
  val maxNodes = params.get("M")
  val writes = params.get("W")
  val increment = params.get("I")
  while (dht.nodeCount <= maxNodes) {
    dht.randomWrites(writes)
    System.exit(0)
    analyze(params, dht)
    dht.addNodes(increment)
  }
}