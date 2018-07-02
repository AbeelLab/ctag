package nl.tudelft.pl2.representation.external.chunking

import nl.tudelft.pl2.data.caches.CacheChunk
import nl.tudelft.pl2.representation.external.Node

/**
  * Class that represents a chunk of [[Node]]s in a Graph.
  *
  * @param chunkId       The ID of the [[Chunk]]
  * @param layer         The layer the [[Chunk]] is in
  * @param incomingLinks The [[ChunkLink]]s that go to this [[Chunk]]
  * @param outgoingLinks The [[ChunkLink]]s that come from this [[Chunk]]
  * @param info          Map containing METADATA about this [[Chunk]]
  */
class Chunk(chunkId: Int,
            layer: List[Int],
            incomingLinks: List[ChunkLink],
            outgoingLinks: List[ChunkLink],
            info: Map[String, String]) {

  /**
    * The cached chunk containing the data for this chunk.
    */
  var _cacheChunk: CacheChunk = _

  /**
    * Add a cached chunk to this chunk.
    *
    * @param cacheChunk The cached chunk to add
    */
  def addCacheChunk(cacheChunk: CacheChunk): Unit =
    _cacheChunk = cacheChunk

  /**
    * Get the layers in the chunk as a list of java integers.
    *
    * @return The list of layers
    */
  def layers(): List[Integer] =
    cacheChunk.layers.map(Integer.valueOf)

  /**
    * Getter for the cached chunk.
    *
    * @return The cached chunk
    */
  def cacheChunk: CacheChunk = _cacheChunk

  /**
    * Assure this chunk is in-memory and, after
    * doing so, return a PartialGraph object
    * representing this chunk.
    *
    * @return PartialGraph representing this chunk.
    */
  def graph(): PartialGraph =
    PartialGraph(
      cacheChunk.nodes.toList,
      cacheChunk.nodes.flatMap(_.outgoing).toList
    )

  /**
    * Assures this chunk's presence in
    * memory. If this chunk was not yet
    * represented fully in-memory, it will
    * ask to be cached.
    *
    * @return `true` when this chunk is present
    *         in memory, `false` otherwise.
    */
  def assurePresence(): Boolean =
    cacheChunk != null

  /**
    * Get a set of layers in the chunk.
    *
    * @return The set of layers in the chunk
    */
  def getLayerSet: java.util.Set[Integer] = {
    val set: java.util.TreeSet[Integer] =
      new java.util.TreeSet[Integer]()
    layers().foreach(set.add)
    set
  }
}
