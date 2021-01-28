package dht

import java.util.HashMap

object Main extends App {

  def defaultArgs(): HashMap[String, Int] = {

    /** Default values for args.
      */
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

    /** Parse arguments.
      *  - S 7
      *     Node count at start is 7
      *  - E 1600
      *     Extent count is 1600
      *  - N 2
      *     Each extent is stored in 2 copies
      *  - W 150
      *     Write operations performed each iteration is 150
      *  - I 2
      *     We add 2 more nodes after each iteration
      *  - M 11
      *     We stop when node have reached a total of 11
      */
    var argMap = defaultArgs()
    def next(i: Int): Unit = {
      if (i < args.length - 1) {
        var value = 0
        try {
          value = args(i + 1).toInt
        } catch {
          case e: NumberFormatException => {
            next(i + 1)
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
          case _    => jump = 1
        }
        next(i + jump)
      }
    }
    next(0)
    return argMap
  }

  val stats = new Stats()
  val params = argparse(args)
  val dht = new DHT(params.get("S"), params.get("E"), params.get("N"))
  val maxNodes = params.get("M")
  val writes = params.get("W")
  val increment = params.get("I")

  while (dht.nodeCount <= maxNodes) {
    dht.randomWrites(writes)
    stats.analyze(params, dht)
    //dht.addNodes(increment)
    System.exit(0)
  }
}
 