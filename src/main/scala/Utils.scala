package dht

import scala.collection.mutable.Queue
import scala.io.Source
import java.util.Random
import java.util.HashMap
import java.util.ArrayList

final case class CollisionException() extends Exception("Collision!", None.orNull) 


class KeyMaker() {

    val ips = new Queue[String]()
    val files = new Queue[String]()

    for (line <- Source.fromFile("ips.txt").getLines()) {
        ips.enqueue(line.trim())
    }

    for (line <- Source.fromFile("movies.txt").getLines()) {
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