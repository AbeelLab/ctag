package nl.tudelft.pl2.data.indexing

import java.nio.file.Path
import java.util.logging.Logger

import nl.tudelft.pl2.representation.external.Node
import org.apache.logging.log4j.LogManager

/**
  * Indexes [[Node]]s and creates an [[Index]].
  *
  * @param indexPath
  */
class Indexer(indexPath: Path) {

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("Indexer")

  /**
    * The max number of [[Node]]s per [[IndexChunk]].
    */
  final val MAX_NODES = 1000

  /**
    * Stores the [[IndexChunk]]s to disk.
    */
  final val writer = new BytesIndexChunkWriter(indexPath)

  /**
    * The offset of the data represented by the current
    * [[IndexChunk]] in the file.
    */
  private final var chunkOffset = 0L

  /**
    * The index of the current [[IndexChunk]].
    */
  private final var chunkIndex = 0

  /**
    * The length of the current [[IndexChunk]]'s
    * data on file.
    */
  private final var chunkLength = 0

  /**
    * The minimum layer encountered in this chunk.
    */
  private final var chunkMinLayer = Int.MaxValue
  /**
    * The max layer encountered in this chunk.
    */
  private final var chunkMaxLayer = -1
  /**
    * The min segment encountered in this chunk.
    */
  private final var chunkMinNodeID = Int.MaxValue
  /**
    * The max segment encountered in this chunk.
    */
  private final var chunkMaxNodeID = -1
  /**
    * The number of nodes in the current [[IndexChunk]].
    */
  private final var chunkNumOfSegs = 0

  /**
    * Adds the [[Node]] to an [[IndexChunk]].
    *
    * @param id     The ID of the [[Node]].
    * @param length The length of the [[Node]] in bytes.
    * @param layer  The layer of the [[Node]].
    */
  def indexNode(id: Int, length: Int, layer: Int): Unit = {
    chunkNumOfSegs += 1
    chunkLength += length
    if (layer < chunkMinLayer) {
      chunkMinLayer = layer
    }
    if (layer > chunkMaxLayer) {
      chunkMaxLayer = layer
    }
    if (id < chunkMinNodeID) {
      chunkMinNodeID = id
    }
    if (id > chunkMaxNodeID) {
      chunkMaxNodeID = id
    }

    if (chunkNumOfSegs >= MAX_NODES) {
      val indexedChunk =
        IndexChunk(chunkIndex, chunkLength, chunkOffset,
          (chunkMinLayer, chunkMaxLayer),
          (chunkMinNodeID, chunkMaxNodeID))
      writer.writeIndexChunk(indexedChunk)

      chunkIndex += 1
      chunkOffset += chunkLength
      chunkLength = 0
      chunkMinLayer = Int.MaxValue
      chunkMaxLayer = -1
      chunkMinNodeID = Int.MaxValue
      chunkMaxNodeID = -1
      chunkNumOfSegs = 0
    }
  }

  /**
    * Flushes the [[Indexer]] by storing
    * any remaining [[IndexChunk]]s to disk.
    */
  def flush(): Unit = {
    if (chunkNumOfSegs > 0) {
      val indexedChunk =
        IndexChunk(chunkIndex, chunkLength, chunkOffset,
          (chunkMinLayer, chunkMaxLayer),
          (chunkMinNodeID, chunkMaxNodeID))
      writer.writeIndexChunk(indexedChunk)
    }
  }

  /**
    * Closes all channels to any files.
    */
  def close(): Unit = {
    writer.close()
    LOGGER.debug("Indexer with {} closed.", indexPath.getFileName)
  }
}
