package nl.tudelft.pl2.data.caches

import nl.tudelft.pl2.representation.external.Node

import scala.collection.mutable

/**
  * Represents a chunk of data in the cache.
  *
  * @param index       The index of this chunk.
  * @param layers      The layers of the segments in this chunk.
  * @param minLayer    The lowest layer in the chunk
  * @param maxLayer    The highest layer in the chunk
  * @param nodeIndices The indexes of the segments in this chunk.
  * @param nodes       The segments in this chunk.
  * @param timestamp   The last time this chunk was accessed.
  */
case class CacheChunk(index: Int,
                      layers: List[Int],
                      minLayer: Int,
                      maxLayer: Int,
                      nodeIndices: List[Int],
                      nodes: mutable.Buffer[Node],
                      var timestamp: Long)

/**
  * The companion object for CacheChunk.
  */
object CacheChunk {

  /**
    * Ordering by timestamp and index for CacheChunks.
    *
    * @tparam A The type of this ordering.
    * @return An ordering of CacheChunks.
    */
  implicit def orderingByTimestampAndIndex[A <: CacheChunk]: Ordering[A] =
    Ordering.by(cc => (cc.timestamp, cc.index))
}
