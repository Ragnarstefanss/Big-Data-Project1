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
    /** Add an extent with <id> to node.
      */

    if (extents.containsKey(id)) throw new CollisionException()
    extents.put(id, new Extent())
  }

  def addExtentCopy(id: BigInt, original: BigInt) = {
    /** Add an extent copy with <id> to node.
      */
    
    if (!extentCopies.containsKey(original))
      extentCopies.put(original, new HashMap[BigInt, Extent]())
    if (extentCopies.get(original).containsKey(id))
      throw new CollisionException()
    extentCopies.get(original).put(id, new Extent())
  }

  def incrementWrites(id: BigInt) = {
    /** Increment write counter for node for a specific extent.
      */

    this.writes += 1
    this.extents.get(id).incrementWrites()
  }

  def incrementWritesCopy(id: BigInt, original: BigInt) = {
    /** Update write counter for extent copy with given id.
      */

    this.extentCopies.get(original).get(id).incrementWrites()
  }

  override def toString(): String = {
    return id.toString()
  }
}
