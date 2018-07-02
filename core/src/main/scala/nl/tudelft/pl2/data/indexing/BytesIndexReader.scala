package nl.tudelft.pl2.data.indexing

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.util.logging.Logger

import nl.tudelft.pl2.representation.exceptions.CTagException
import org.apache.logging.log4j.LogManager

case class EmptyIndexException(reason: String, loc: String)
  extends CTagException(reason, loc)

/**
  * Creates and [[Index]] from a file.
  */
object BytesIndexReader extends IndexReader {

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("BytesIndexReader")

  override def loadIndex(indexPath: Path): Index = {
    LOGGER.debug("Reading index from " + indexPath.getFileName)
    val index = new Index(indexPath)
    val channel: AsynchronousFileChannel = AsynchronousFileChannel.open(indexPath)


    assert(channel.size() < Int.MaxValue)

    if (channel.size() == 0) {
      channel.close()
    } else {

      val buf = ByteBuffer.allocateDirect(channel.size().toInt)

      //TODO: convert to observable pattern
      channel.read(buf, 0).get()

      val numChunks = channel.size().toInt / IndexChunk.BYTES_PER_CHUNK
      channel.close()

      buf.flip()
      for (i <- 0 until numChunks) {
        val layer = buf.getInt
        val length = buf.getInt
        val offset = buf.getLong
        val loLayer = buf.getInt
        val hiLayer = buf.getInt
        val loNode = buf.getInt
        val hiNode = buf.getInt
        index.insertChunk(
          IndexChunk(layer, length, offset, (loLayer, hiLayer), (loNode, hiNode))
        )
      }
    }
    LOGGER.debug("Done reading index.")
    LOGGER.debug("Index has {} chunks.", index.size)
    index
  }
}
