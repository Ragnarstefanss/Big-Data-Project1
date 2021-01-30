package dht

import java.util.HashMap
import java.util.ArrayList
import javax.swing.text.StyledEditorKit.BoldAction

class DHT(
    var nodeCount: Int,
    val extents: Int,
    val copies: Int,
    val keyBits: Int = 40
) {
  val mod = BigInt(2).pow(keyBits)
  val rand = new RandomHelper()
  val hasher = new Sha1(keyBits)
  val keyMaker = new KeyMaker()

  var nodes = new HashMap[BigInt, Node]()
  val extentKeys = new Array[String](extents)
  var sortedNodeIds = new ArrayList[BigInt]()

  var jumps = new ArrayList[Int]()
  var currJump = -1

  createExtentKeys()
  generateInitialNodes()
  generateInitialExtents()

  // Interface

  def resetJumps() = {

    /** Reset jump tracking between experiments.
      */
    jumps.clear()
    currJump = -1
  }

  def addNodes(numberOfNodes: Int) = {

    /** Add <numberOfNodes> new Nodes to the system.
      */
    var n = 0;
    for (n <- 1 to numberOfNodes) {
      val key = hasher.hash(keyMaker.ips.dequeue())
      if (this.nodes.containsKey(key)) throw new CollisionException()
      println(s"Adding new node with id $key")
      this.addNode(key)
    }
  }

  def randomWrites(writes: Int) = {

    /** Write to <writes> random extents starting from a random node.
      */
    val startingNode = rand.randomNode(nodes, sortedNodeIds)
    var w = 0;
    for (w <- 1 to writes) {
      val key = rand.randomExtent(extentKeys)
      write(key, startingNode)
    }
  }

  // Private helpers

  private def createExtentKeys() = {

    /** Create the key values we use for our extent. This
      * values are hashed into ids.
      */
    var i = 0
    while (i < extents) {
      extentKeys(i) = keyMaker.files.dequeue()
      i += 1
    }
  }

  private def binarySearchGreaterOrEqual(
      arr: ArrayList[BigInt],
      lookingFor: BigInt
  ): Int = {

    /** Given a sorted list of node ids, we find the index of the one greater to or equal
      * to the provided id. If its greater than all, we return the first element (wrap in ring).
      */
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

    /** Initialize the number of nodes the system should have at start.
      * The keys (which are ips) are hashed into ids, they form a circle
      * and for each node we create a prev-link and a fingertable.
      */

    // Extract keys from ip file
    while (this.nodes.size() < nodeCount) {
      val newKey = keyMaker.ips.dequeue()
      val newId = hasher.hash(newKey)

      if (this.nodes.containsKey(newId)) {
        throw new CollisionException()
      }
      this.nodes.put(newId, new Node(keyBits, newId, copies))
    }
    this.nodes
      .keySet()
      .stream()
      .sorted()
      .forEach((id) => this.sortedNodeIds.add(id))

    // Create prev and finger table links
    val largestId = this.sortedNodeIds.get(nodeCount - 1)
    var n = 0
    for (n <- 1 to nodeCount) {
      val currNode = this.nodes.get(this.sortedNodeIds.get((n - 1) % nodeCount))
      currNode.prev =
        this.nodes.get(this.sortedNodeIds.get((nodeCount + n - 2) % nodeCount))

      var i = 0
      var offset = BigInt(1)
      for (i <- 1 to keyBits) {
        val index = binarySearchGreaterOrEqual(
          sortedNodeIds,
          (currNode.id + offset).mod(mod)
        )
        currNode.fingerTable(i - 1) =
          this.nodes.get(this.sortedNodeIds.get(index))
        offset *= 2
      }
    }
  }
  
  private def generateInitialExtents() = {

    /** Add extents to the system, assigning each to the node after its id
      * in the ring and the copies are placed on the succeding neighbors.
      */
    extentKeys.foreach((key) => {
      val id = hasher.hash(key)
      val index = binarySearchGreaterOrEqual(sortedNodeIds, id)
      val node = this.nodes.get(this.sortedNodeIds.get(index))
      node.addExtent(id)
      var copyNode = node.fingerTable(0)
      for (n <- 1 to copies) {
        copyNode.addExtentCopy(id, node.id)
        copyNode = copyNode.fingerTable(0)
      }
    })
  }

  private def addNode(id: BigInt) = {

    /** Add a single node given its key. Its id is the hash of the key.
      */
    if (nodes.containsKey(id)) throw new CollisionException()
    val newNode = new Node(keyBits, id, copies)

    // Find successor
    val successor =
      findNodeResponsibleForId(rand.randomNode(nodes, sortedNodeIds), id, false)
    nodes.put(id, newNode)

    // Update links    
    var offset = BigInt(2)
    for (i <- 1 to keyBits - 1) {
      newNode.fingerTable(i) = findNodeResponsibleForId(successor, (id + offset).mod(mod), false)
      offset *= 2
    }
    newNode.prev = successor.prev
    newNode.prev.fingerTable(0) = newNode
    successor.prev = newNode
    newNode.fingerTable(0) = successor

    // Move extents
    val wrap = id < newNode.prev.id
    val extentsToMove = new ArrayList[BigInt]()
    successor.extents.keySet.forEach((k) => {
      if (wrap) {
        if (k <= id || k > newNode.prev.id) extentsToMove.add(k)
      } else {
        if (k <= id) extentsToMove.add(k)
      }
    })
    extentsToMove.forEach((k) => {
        newNode.extents.put(k, successor.extents.get(k))
        successor.extents.remove(k)
      })

    // Done for experimental purposes, not used for adding or finding nodes
    sortedNodeIds.add(id)
    sortedNodeIds.sort((id1, id2) => id1.compareTo(id2))

    // Update fingertables
    nodes.values().forEach((n) => {
      if (n.id != newNode.id) {
        updateFingerTable(n, newNode)
      }
    })
    
    // Move copies
    var lastCopyOfKeys = successor
    var preCopies = newNode
    for (i <- 1 to copies) lastCopyOfKeys = lastCopyOfKeys.fingerTable(0)
    successor.extentCopies.put(newNode.id, new HashMap[BigInt, Extent]())
    extentsToMove.forEach((eId) => {
      successor.extentCopies.get(newNode.id).put(eId, lastCopyOfKeys.extentCopies.get(successor.id).get(eId))
      lastCopyOfKeys.extentCopies.get(successor.id).remove(eId)
    })
    for (i <- 1 to copies) {
      lastCopyOfKeys = lastCopyOfKeys.prev
      preCopies = preCopies.prev
      newNode.extentCopies.put(preCopies.id, lastCopyOfKeys.extentCopies.get(preCopies.id))
      lastCopyOfKeys.extentCopies.remove(preCopies.id)
      if (lastCopyOfKeys.id != successor.id) {
        lastCopyOfKeys.extentCopies.put(newNode.id, new HashMap[BigInt, Extent]())
        extentsToMove.forEach((eId) => {
          lastCopyOfKeys.extentCopies.get(newNode.id).put(eId, lastCopyOfKeys.extentCopies.get(successor.id).get(eId))
          lastCopyOfKeys.extentCopies.get(successor.id).remove(eId)
        })
      }
    }

    // Counter
    this.nodeCount += 1
  }

  private def updateFingerTable(node: Node, from: Node) = {
    var offset = BigInt(2)
    for (i <- 1 to keyBits - 1) {
      node.fingerTable(i) = findNodeResponsibleForId(from, (node.id + offset).mod(mod), false)
      offset *= 2
    }
  }

  private def write(extentKey: String, startingNode: Node) = {

    /** Write to a given extent by key, starting from some node.
      */
    jumps.add(0)
    currJump += 1
    val id = hasher.hash(extentKey)
    var node = startingNode
    if (!node.extents.containsKey(id)) {
      node = findNodeResponsibleForId(startingNode, id)
    }
    node.incrementWrites(id)

    var copy = node.fingerTable(0)
    for (i <- 1 to copies) {
      copy.incrementWritesCopy(id, node.id)
      copy = copy.fingerTable(0)      
    }
  }

  private def findNodeResponsibleForId(currentNode: Node, id: BigInt, trackJumps: Boolean = true): Node = {

    /** Find the node responsible for a given id. We assume this id exists in dht.
      * It uses fingerbale to find the node closest to the id.
      */
    var successor = currentNode.fingerTable(0)
    val found =
      (currentNode.id < successor.id && currentNode.id < id && id <= successor.id) ||
        (currentNode.id > successor.id && (id > currentNode.id || id < successor.id))
    if (!found) {
      if (trackJumps) jumps.set(currJump, jumps.get(currJump) + 1)
      var i = 0
      for (i <- 1 to keyBits - 1) {
        val neighbor = currentNode.fingerTable(keyBits - i)
        if (neighbor.id > currentNode.id) {
          if (id < currentNode.id || neighbor.id <= id)
            return findNodeResponsibleForId(neighbor, id, trackJumps)
        } else {
          if (id >= neighbor.id && id < currentNode.id)
            return findNodeResponsibleForId(neighbor, id, trackJumps)
        }
      }
      return findNodeResponsibleForId(successor, id, trackJumps)
    }
    return successor
  }
}
