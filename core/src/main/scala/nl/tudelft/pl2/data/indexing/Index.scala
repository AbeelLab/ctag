package nl.tudelft.pl2.data.indexing

import java.util
import java.nio.file.Path

import nl.tudelft.pl2.representation.external.Node

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * An entry in the [[Index]]. Each [[IndexChunk]] stores a number
  * of values that are needed for fast retrieval of information from
  * a parsed graph file, as well as a number of values that are needed
  * for efficient indexing.
  *
  * The offset and the length are used to find the position of a chunk
  * of data in the parsed file. By using the offset + length, we don't
  * have to traverse and parse the entire file to find the information
  * we want.
  *
  * The index, layers, and nodes are used to create different binary
  * search trees that allow for fast retrieval of an [[IndexChunk]]
  * based on the provided information.
  *
  * @param index      The index of this chunk in the [[Index]].
  * @param offset     Offset of this chunk within the parsed graph file.
  * @param length     Length of this chunk within the parsed graph file.
  * @param layerRange Set of layers that have nodes in this chunk. A set of layers
  *                   is used instead of a a range, since some layers may be skipped.
  * @param nodeRange  Lowest and highest [[Node]] id in this [[IndexChunk]].
  */
case class IndexChunk(index: Int,
                      length: Int,
                      offset: Long,
                      layerRange: (Int, Int),
                      nodeRange: (Int, Int))

object IndexChunk {
  val BYTES_PER_CHUNK: Int = 6 * 4 + 8
}

/**
  * An index for a parsed graph file.
  *
  * @param indexPath The path at which the index is stored.
  * @author Maaike Visser
  */
class Index(val indexPath: Path) {
  /**
    * Tree ordered by minimum layer ID.
    */
  val minLayerTreeMap: util.TreeMap[Int, ListBuffer[IndexChunk]] =
    new util.TreeMap[Int, ListBuffer[IndexChunk]]()
  /**
    * Index tree ordered by minimum [[Node]] ID.
    */
  val minNodeTreeMap: util.TreeMap[Int, IndexChunk] = new util.TreeMap()
  /**
    * Index tree ordered by Chunk index.
    */
  val indexTreeMap: util.TreeMap[Int, IndexChunk] = new util.TreeMap()

  /**
    * Retrieves an [[IndexChunk]] by [[Node]] id.
    *
    * @param id The index the chunk should contain.
    * @return The matching [[IndexChunk]].
    */
  def getIndexedChunksByNodeID(id: Int): mutable.Buffer[IndexChunk] =
    minNodeTreeMap.headMap(id, true).asScala.filter(entry => entry._2.nodeRange
    ._2 >= id).values.toBuffer


  /**
    * Retrieve a list of IndexedChunks by layer.
    *
    * @param layer The layer the chunks should contain.
    * @return A list of matching IndexedChunks.
    */
  def getIndexedChunksByLayer(layer: Int): mutable.Buffer[IndexChunk] =
    minLayerTreeMap.headMap(layer, true)
      .asScala.flatMap(entry => entry._2).filter(ic => ic.layerRange._2 >= layer).toBuffer

  /**
    * Retrieve an IndexedChunk based on id.
    *
    * @param id The id of the Chunk.
    * @return The IndexedChunk.
    */
  def getIndexedChunkByIndex(id: Int): IndexChunk = indexTreeMap.get(id)

  /**
    * Gets the size of the trees in the index.
    *
    * @return The size of the trees in the index.
    */
  def size: Int = indexTreeMap.size()

  /**
    * Inserts an [[IndexChunk]] into the index.
    *
    * @param chunk the chunk to enter into the tree
    */
  def insertChunk(chunk: IndexChunk): Unit = {
    if (minLayerTreeMap.containsKey(chunk.layerRange._1)) {
      minLayerTreeMap.get(chunk.layerRange._1) += chunk
    } else {
      minLayerTreeMap.put(chunk.layerRange._1, ListBuffer(chunk))
    }
    minNodeTreeMap.put(chunk.nodeRange._1, chunk)
    indexTreeMap.put(chunk.index, chunk)
  }
}
