
import scala.math.pow
import scala.math.BigInt
import java.util.HashMap
import java.util.Random
import java.util.HashSet

final case class CollisionException() extends Exception("Collision!", None.orNull) 

class RandomHelper() {
    private val nodeKeyRNG = new Random()
    private val extentKeyRNG = new Random()
    private val nodeAccessRNG = new Random()
    private val extentAccessRNG = new Random()
    
    private val printableChars = "!\"#$%&()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"

    private def randChar(rng: Random): Char = {
        return printableChars(rng.nextInt(printableChars.length()))
    }

    private def randStr(rng: Random, n: Int): String = {
        var strB = new StringBuilder()
        var i = 0        
        for(n <- 1 to n) {
            strB.append(randChar(rng))
        }
        return strB.toString()
    }

    def randomNodeKey(n: Int): String = {
        return randStr(this.nodeKeyRNG, n)
    }

    def randomExtentKey(n: Int): String = {
        return randStr(this.extentKeyRNG, n)
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
    val writes: Int = 0
}

class Node(keyBits: Int, val id: BigInt) {
    var prev: Node = null
    var fingerTable: Array[Node] = new Array[Node](keyBits)
    var extents = new HashMap[BigInt, Extent]()
    var extentCopies = new HashMap[BigInt, Extent]
    var writes: Int = 0

    override def toString() : String = {          
        return this.id.toString()
    }
}

class DHT (var nodeCount: Int, val extents: Int, val copies: Int) {
    val keyBits = 10
    val keyLength = 10
    val rand = new RandomHelper()
    val nodeHasher = new Sha1(keyBits)
    val extentHasher = new Sha1(keyBits)

    var nodes = new HashMap[BigInt, Node]()
    val extentKeys = new Array[String](extents)
    generateInitialNodes()

    private def generateInitialNodes() {
        var tmpNodes = new Array[Node](nodeCount)
        var strSet = new HashSet[String]()
        var idSet = new HashSet[BigInt]()
        var i = 0
        while (i < this.nodeCount) {
            val newKey = rand.randomNodeKey(8)
            val newId = nodeHasher.hash(newKey)
            if (strSet.contains(newKey) || idSet.contains(newId)) {
                throw new CollisionException()
            }
            strSet.add(newKey)
            idSet.add(newId)
            tmpNodes(i) = new Node(keyBits, newId)
            i = i + 1
        }
        //this.nodes = tmpNodes.sortWith((n1, n2) => n1.id.compareTo(n2.id) < 0)
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
        // TODO: Find out if all writes should start from the same node
        var w = 0;
        for(w <- 1 to writes) {
            // TODO: Pick random extent and "write to it"
        }
    }

    def write(extentKey: String, startingNode: Node) = {
        val id = this.extentHasher.hash(extentKey)
        val node = findNodeResponsibleForId(startingNode, id)
        // Update stats.... TODO
    }

    def findNodeResponsibleForId(currentNode: Node, id: BigInt): Node {
        // TODO
    }
}

object Main {
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

    def main(args: Array[String]) {
        val params = argparse(args)
        val dht = new DHT(params.get("S"), params.get("E"), params.get("N"))
        dht.nodes.foreach(println)
        return
        val maxNodes = params.get("M")
        val writes = params.get("W")
        val increment = params.get("I")
        while (dht.nodeCount <= maxNodes) {
            dht.randomWrites(writes)
            analyze(params, dht)
            dht.addNodes(increment)
        }
    }
}
