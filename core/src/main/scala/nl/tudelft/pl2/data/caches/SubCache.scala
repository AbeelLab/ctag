package nl.tudelft.pl2.data.caches

import nl.tudelft.pl2.data.indexing.{Index, IndexChunk}
import nl.tudelft.pl2.data.storage.readers.CtagReader
import nl.tudelft.pl2.representation.exceptions.NodeNotFoundException
import nl.tudelft.pl2.representation.external.{Edge, Node}
import nl.tudelft.pl2.representation.external.chunking.Chunk
import org.apache.logging.log4j.LogManager

import scala.collection.mutable
import scala.collection.JavaConverters.asScalaSetConverter

/**
  * A a sub-cache.
  *
  * @param index  The [[Index]] of this cache.
  * @param reader The reader for the file containing the zero zoom level information.
  */
class SubCache(index: Index, reader: CtagReader) extends Cache {

  /**
    * The [[CacheChunk]]s that currently reside in main memory.
    */
  val cachedChunks: mutable.TreeSet[CacheChunk] = mutable.TreeSet()

  /**
    * The maximum number of [[CacheChunk]]s in this [[Cache]].
    */
  private val CACHE_SIZE = 11

  /**
    * Logger for this class.
    */
  private val LOGGER = LogManager.getLogger(this.getClass)

  /**
    * The highest encountered layer.
    */
  private var _maxLayer: Int = index.getIndexedChunkByIndex(index.size - 1).layerRange._2

  /**
    * The highest encountered [[Node]] ID.
    */
  private var maxNodeID: Int = index.getIndexedChunkByIndex(index.size - 1).nodeRange._2

  def maxLayer: Int = _maxLayer

  override def retrieveNodeID(nodeName: String): Int = ???

  override def retrieveNodeByID(id: Int): Node = {
    try {
      val nodes = treeFilter(cachedChunks)(cc => cc.nodeIndices.contains(id))
        .getOrElse(index
          .getIndexedChunksByNodeID(id)
          .filterNot(ic => cachedChunks.exists(cc => ic.index == cc.index))
          .map(ic => addChunkToCache(buildCacheChunk(ic))))
        .flatMap(a => a.nodes)
      if (nodes.nonEmpty) {
        nodes.minBy(v => math.abs(v.id - id))
      } else {
        throw new NodeNotFoundException("Node " + id + " could not be found.",
          "In " + this.getClass)
      }
    } catch {
      case e: Exception =>
        LOGGER.error("Something something: ", e)
        throw new NodeNotFoundException("Node " + id + " could not be found.",
          "In " + this.getClass)
    }
  }

  /**
    * Takes an IndexedChunk, retrieves the data it points to
    * from disk, and creates a CachedChunk.
    *
    * @param indexChunk The IndexedChunk based on which information
    *                   is retrieved.
    * @return A Cachechunk with the retrieved information.
    */
  def buildCacheChunk(indexChunk: IndexChunk): CacheChunk = {
    val nodes = reader.readDataChunk(indexChunk.offset, indexChunk.length)
    var maxLayer = Integer.MIN_VALUE
    var minLayer = Integer.MAX_VALUE
    val layers = nodes.map(s => {
      val layer = s.layer
      maxLayer = Math.max(maxLayer, layer)
      minLayer = Math.min(minLayer, layer)
      layer
    }).toList
    val segmentIds = nodes.map(s => s.id).toList
    CacheChunk(
      indexChunk.index,
      layers,
      minLayer, maxLayer,
      segmentIds,
      nodes,
      System.currentTimeMillis / 1000
    )
  }

  override def retrieveMaxNodeID: Int = maxNodeID

  override def updateMaxNodeID(id: Int): Unit =
    maxNodeID = Math.max(id, maxNodeID)

  override def createEdgeList: List[Edge] =
    createNodeList.flatMap(node => {
      node.outgoing
    })

  /**
    * Adds a CachedChunk to the cache.
    *
    * @param cacheChunk The chunk to add to the cache.
    * @return The chunk that was added to the cache.
    */
  def addChunkToCache(cacheChunk: CacheChunk): CacheChunk = {
    if (cachedChunks.size >= CACHE_SIZE) {
      cachedChunks.remove(cachedChunks.min)
    }
    _maxLayer = Math.max(cacheChunk.layers.max, _maxLayer)
    cachedChunks.add(cacheChunk)

    LOGGER.debug(s"Chunk ${cacheChunk.index} was added to the cache.")
    cacheChunk
  }

  override def retrieveChunksByLayer(layer: Int): List[Chunk] =
    treeFilter(cachedChunks)(cc => {
      cc.layers.contains(layer)
    }).getOrElse({
      index.getIndexedChunksByLayer(layer)
        .filterNot(ic => cachedChunks.exists(cc => ic.index == cc.index))
        .map({
          ic => {
            LOGGER.debug(s"Retrieving chunk: ${ic.index} with layers ${ic.layerRange}")
            addChunkToCache(buildCacheChunk(ic))
          }
        })
    }).map(cc => {
      val ec = new Chunk(cc.index, cc.layers, Nil, Nil, Map())
      ec.addCacheChunk(cc)
      ec
    }).toList

  /**
    * Filters a TreeSet by a certain predicate and checks whether or not
    * it is empty. Converts the result to a mutable.Buffer
    *
    * @param tree      The tree to filter.
    * @param predicate The predicate to filter on.
    * @tparam A The type of the TreeSet
    * @return A filtered buffer of type A.
    */
  def treeFilter[A](tree: mutable.TreeSet[A])
                   (predicate: A => Boolean): Option[mutable.Buffer[A]] =
    Some(tree.filter(predicate).toBuffer).filter(_.nonEmpty)

  override def createNodeList: List[Node] =
    index.indexTreeMap.keySet().asScala.toList.sorted.flatMap(id => {
      buildCacheChunk(index.getIndexedChunkByIndex(id)).nodes
    })

  override def close(): Unit = {
    reader.close()
  }

  def clear(): Unit =
    cachedChunks.clear()
}
