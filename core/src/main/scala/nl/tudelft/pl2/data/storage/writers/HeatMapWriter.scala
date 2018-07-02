package nl.tudelft.pl2.data.storage.writers

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Path, StandardOpenOption}

import nl.tudelft.pl2.data.storage.HeatMapInfo
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.mutable

/**
  * Writer for writing heat-map files after first
  * registering the data to write.
  *
  * Registering data in this HeatMapWriter is done with
  * the [[incrementLayerAt()]] method. Writing the file
  * after registering data is done is done all at once
  * in the [[flush()]] method.
  *
  * Currently, this requires an in-mem representation of
  * the nodes-per-layer mapping.
  *
  * @param file The path to the file to write.
  */
class HeatMapWriter(file: Path) extends AutoCloseable {

  /**
    * Log4J [[org.apache.logging.log4j.Logger]] used to log
    * debug information and other significant events.
    */
  private val LOGGER: Logger = LogManager.getLogger("HeatMapReader")

  /**
    * The default buffer size used to cap the number
    * of bytes written to disk at once.
    */
  private val BUFF_SIZE: Int = 131072

  /**
    * File channel to the file to write.
    * By default, a new file is created upon opening.
    */
  private var fileChannel: FileChannel = _

  /**
    * The mapping of layer IDs to the number of nodes
    * in that layer. Used to store the mapping before
    * writing to disk.
    */
  private val nodesPerLayer: mutable.Map[Int, HeatMapInfo] = mutable.HashMap()

  /**
    * Flushes the current buffer to disk in a blocking
    * manner. If the buffer is of zero-length, writing
    * is ignored. After writing, the buffer is cleared
    * and ready for re-use.
    *
    * @param buffer The buffer to write to disk.
    */
  private def flushBuffer(buffer: ByteBuffer): Unit =
    if (buffer.position() > 0) {
      buffer.flip()
      fileChannel.write(buffer)
      buffer.clear()
    }

  /**
    * Flushes the current nodes-per-layer mapping as
    * built in the [[incrementLayerAt()]] function to
    * disk in the [[file]] given as parameter to this
    * class.
    */
  def flush(): Unit = {
    fileChannel = FileChannel.open(file,
      StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    val buffer = ByteBuffer.allocateDirect(BUFF_SIZE)

    val maximumLayer = nodesPerLayer.maxBy(_._2.layers)
    val maximumSeq = nodesPerLayer.maxBy(_._2.sequences)
    buffer.putInt(maximumLayer._1)
    buffer.putInt(maximumLayer._2.layers)
    buffer.putInt(maximumSeq._2.sequences)

    nodesPerLayer.foreach { case (layer, nodes) =>
      if (buffer.position() + 12 > BUFF_SIZE) {
        flushBuffer(buffer)
      }

      buffer.putInt(layer)
      buffer.putInt(nodes.layers)
      buffer.putInt(nodes.sequences)
      LOGGER.debug("Written layer: {} with nodes: {}", layer, nodes )
    }
    flushBuffer(buffer)
    LOGGER.debug("Finished writing heatmap")
  }

  /**
    * Increments the number of nodes in the given layer
    * by one or sets the number of nodes in the given
    * layer to 1 if the layer has not yet been encountered.
    *
    * @param layer The layer at which to increment.
    */
  def incrementLayerAt(layer: Int): Unit =
    if (nodesPerLayer.contains(layer)) {
      nodesPerLayer(layer).layers += 1
    } else {
      nodesPerLayer(layer) = HeatMapInfo(1, 0)
    }

  /**
    * This method increments the number of sequences going
    * through a layer.
    *
    * @param layer The layer through which the sequence is going.
    * @param count The number of sequences going through it.
    */
  def incrementSequenceAt(layer: Int, count: Int): Unit = {
    if (nodesPerLayer.contains(layer)) {
      nodesPerLayer(layer).sequences += count
    } else {
      nodesPerLayer(layer) = HeatMapInfo(0, 1)
    }
  }

  override def close(): Unit =
    fileChannel.close()
}
