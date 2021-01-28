package dht

import scala.collection.mutable.Queue
import scala.io.Source
import java.util.Random
import java.util.HashMap
import java.util.ArrayList

// A collision exception. Consider increasing key bits if this occurs.
final case class CollisionException()
    extends Exception("Collision!", None.orNull)

class KeyMaker() {
  /**
    * Reads files with available keys and stores in queues.
    */

  val ips = new Queue[String]()
  val files = new Queue[String]()
  for (line <- Source.fromFile("ips.txt").getLines()) 
    ips.enqueue(line.trim())
  for (line <- Source.fromFile("movies.txt")("UTF-8").getLines())
    files.enqueue(line.trim() + ".avi")
}

class RandomHelper() {
  private val nodeAccessRNG = new Random()
  private val extentAccessRNG = new Random()

  def randomExtent(extents: Array[String]): String = {
    /**
      * Pick a random extent for experiment purposes.
      */
    return extents(extentAccessRNG.nextInt(extents.length))
  }

  def randomNode(nodes: HashMap[BigInt, Node], ids: ArrayList[BigInt]): Node = {
    /**
      * Pick a random node for experiment purposes.
      */
    return nodes.get(ids.get(nodeAccessRNG.nextInt(nodes.size())))
  }
}

class Sha1(keyBits: Int) {
  private val mod = BigInt(2).pow(keyBits)
  private val md = java.security.MessageDigest.getInstance("SHA-1")

  def hash(key: String): BigInt = {
    /**
      * Hash a string with SHA1, and mod it to keyspace.
      */
    val hex =
      this.md.digest(key.getBytes("UTF-8")).map("%02x".format(_)).mkString
    return BigInt(hex, 16).mod(this.mod)
  }
}
