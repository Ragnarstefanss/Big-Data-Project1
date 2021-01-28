package dht

import java.util.HashMap
import java.util.ArrayList

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

  createExtentKeys()
  generateInitialNodes()
  generateInitialExtents()

  // Interface

  def addNodes(nodes: Int) = {

    /** Add <nodes> new Nodes to the system.
      */
    var n = 0;
    for (n <- 1 to nodes) {
      // Generate random key
      val key = "placeholder"
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
      this.nodes.put(newId, new Node(keyBits, newId))
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
      for (n <- 1 to copies - 1) {
        copyNode.addExtentCopy(id)
        copyNode = copyNode.fingerTable(0)
      }
    })
  }

  private def addNode(key: String) = {

    /** Add a single node given its key. Its id is the hash of the key.
      */
    // TODO: Add node logic here
    this.nodeCount += 1
  }

  private def write(extentKey: String, startingNode: Node) = {

    /** Write to a given extent by key, starting from some node.
      */
    val id = hasher.hash(extentKey)
    /*
        // For-Debugging
        println(startingNode)
        nodes.values().forEach((n) => {
            if (n.extents.containsKey(id)) {
                println("in " + n.id.toString())
            }
        })
        System.exit(0)
     */
    val node = findNodeResponsibleForId(startingNode, id)
    node.incrementWrites(id)
  }

  private def distance(id1: BigInt, id2: BigInt): BigInt = {
    // Might not use this...
    if (id1 <= id2) return id2 - id1
    return this.mod + id2 - id1
  }

  private def findNodeResponsibleForId(currentNode: Node, id: BigInt): Node = {

    /** Find the node responsible for a given id. We assume this id exists in dht.
      * It uses fingerbale to find the node closest to the id.
      */

    // TODO: Not working!
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
