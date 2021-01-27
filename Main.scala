
import scala.math.pow
import java.util.HashMap

class Node() {
    // TODO: implement
}

class DHT (keyBits: Int, var nodes: Int, val blocks: Int, val copies: Int) {
    var mod: Int = pow(2, keyBits).intValue()

    // TODO: Initialize the system


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
        this.nodes += 1
    }

    def randomWrites(writes: Int) = {
        // TODO: Find out if all writes should start from the same node
        var w = 0;
        for(w <- 1 to writes) {
            // TODO: Pick random extent and "write to it"
        }
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
        val keyBits = 16 // TODO: Change as needed, maybe make dht take care of this
        val dht = new DHT(keyBits, params.get("N"), params.get("E"), params.get("N"))
        val maxNodes = params.get("M")
        val writes = params.get("W")
        val increment = params.get("I")
        while (dht.nodes <= maxNodes) {
            dht.randomWrites(writes)
            analyze(params, dht)
            dht.addNodes(increment)
        }
    }
}
