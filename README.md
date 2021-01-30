# Simple DHT with Chord

This is a very simple DHT using Chord overlay. It only support writes and adding new nodes.

## Resources
There are 2 resource files called `ips.txt` and `movies.txt`. They serve as keys for the nodes and extents respectively. The IPs are randomly generated non-private IPs and the movies are from [alphabetizer](https://alphabetizer.flap.tv/lists/list-of-every-movie-ever-made.php). This way our distribution of nodes and extents is deterministic.

## Extent
An extent is a file stored on some node. The Extent object is just a placeholder with nothing but a counter to track writes.

## Node
Nodes have an internal dictionary to store their extent and their copies of other node's extents. The also have a prev link and a fingertable and additionally track all writes that occurs in any of their extents.

## Utils
Contains various helper functions for hashing, randomly selecting for experiments and loading keys from files.

## DHT
The DHT class contains our network of nodes. Its interface is simple and only supports adding random nodes and doing random writes (and reseting the jump array which is only for statistical purposes). 

When initialized we grab keys for nodes and extents from a queue and hash them with SHA-1 into a `BigInt` modulo `pow(2,m)`. We then sort the nodes (by there id) and form a Chord ring, initialize fingertable and add the extents to their appropriate nodes. The latter two are done with altered binary search since we have the sorted nodes ids (greater or equal binary search that wraps). The sorted ids are maintained thereafter only for statistical and testing purposes. Each extent has their copies in the nodes succeding theirs. 

Random writes will pick a random starting node and simulate all the writes from it. The correct node is found using the fingertable. We track the jumps for every write as well as the number of writes each node and extent gets. We also update the writes in the extent copies (but not the Nodes containing them).

Adding a new node will grab a key from the queue and hash it and use the fingertable (starting from a random node) to find its successor. We update the links between the new node and the two nearest neighbors as well as the entire fingertable for the new node. We grab the extents that should belong to the new node from its successor and move them to the new node and finally we update all fingertables (since we do not have a background task for stabilizing). Copies are moved to their appropriate place as well.

In any collision cases we throw an exception in which case one should increase the keyspace.

## Stats
Is a class that writes the result of an experiment to file. It divides it into iterations when we
are dynamically adding new nodes.

## Python script
The python script serves two purposes. First, it has asserts to validate fingertable and extent ownerships. Second, it processes the stats file produces in Scala and creates plots from it.

## How to run
We used [Metals](https://scalameta.org/metals/) to build, run and debug the project. The most simple way to run this is using
```sh
sbt run
```
There are optional arguments as well and any subset of them can be set. They are 
* `-S` The number of initial nodes. Defaults to 10.
* `-E` The number of extents. Defaults to 10000.
* `-N` The number of copies of each extents. Defaults to 3.
* `-W` The number of random writes in each iteration. Defaults to 1000000.
* `-I` The number of servers to add in each iteration. Defaults to 5.
* `-M` The maximum number of servers. Defaults to 30.
* `-B` Makes the keyspace `[0, pow(2, B))`. Defaults to 40.
```sh
sbt "run -S 30 -W 5000"
```