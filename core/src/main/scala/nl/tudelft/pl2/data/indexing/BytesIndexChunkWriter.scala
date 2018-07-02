package nl.tudelft.pl2.data.indexing

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger

import org.apache.logging.log4j.LogManager

/**
  * Writes an [[Index]] as a string of bytes.
  *
  * @param indexPath The path to the index file.
  */
class BytesIndexChunkWriter(indexPath: Path) extends IndexChunkWriter {
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("BytesIndexChunkWriter")

  /**
    * [[RandomAccessFile]] of the [[Index]] file.
    */
  val writeFile: RandomAccessFile = {
    LOGGER.debug("BytesIndexChunkWriter with {} opened.", indexPath.getFileName)
    new RandomAccessFile(indexPath.toString, "rw")
  }

  /**
    * Channel to the [[Index]] file.
    */
  val writeChannel: FileChannel = writeFile.getChannel


  /**
    * Writes an [[IndexChunk]] to file as a string of bytes.
    *
    * @param indexChunk The [[IndexChunk]] to write.
    */
  override def writeIndexChunk(indexChunk: IndexChunk): Unit = {
    val len = IndexChunk.BYTES_PER_CHUNK
    val buf = ByteBuffer.allocateDirect(len)
    buf.putInt(indexChunk.index)
    buf.putInt(indexChunk.length)
    buf.putLong(indexChunk.offset)
    buf.putInt(indexChunk.layerRange._1)
    buf.putInt(indexChunk.layerRange._2)
    buf.putInt(indexChunk.nodeRange._1)
    buf.putInt(indexChunk.nodeRange._2)
    buf.flip()
    writeChannel.write(buf)
  }

  /**
    * Closes the file accesses.
    */
  def close(): Unit = {
    writeChannel.close()
    writeFile.close()
    LOGGER.debug("BytesIndexChunkWriter with {} closed.", indexPath.getFileName)
  }
}
