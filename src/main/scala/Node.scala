package dht

import java.util.HashMap

class Extent() {
  var writes: Int = 0
  def incrementWrites() = {
    writes += 1
  }
}

class Node(keyBits: Int, val id: BigInt, numberOfCopies: Int) {
  var prev: Node = null
  var fingerTable: Array[Node] = new Array[Node](keyBits)
  var extents = new HashMap[BigInt, Extent]()
  var extentCopies = new HashMap[BigInt, HashMap[BigInt, Extent]]()
  var writes: Int = 0

  def addExtent(id: BigInt) = {
    if (extents.containsKey(id)) throw new CollisionException()
    extents.put(id, new Extent())
  }

  def addExtentCopy(id: BigInt, original: BigInt) = {
    if (!extentCopies.containsKey(original)) extentCopies.put(original, new HashMap[BigInt, Extent]())
    if (extentCopies.get(original).containsKey(id)) throw new CollisionException()
    extentCopies.get(original).put(id, new Extent())
  }

  def incrementWrites(id: BigInt) = {
    this.writes += 1
    this.extents.get(id).incrementWrites()
  }

  def incrementWritesCopy(id: BigInt, origina: BigInt) = {
    this.extentCopies.get(origina).get(id).incrementWrites()
  }

  override def toString(): String = {
    return id.toString()
  }
}
