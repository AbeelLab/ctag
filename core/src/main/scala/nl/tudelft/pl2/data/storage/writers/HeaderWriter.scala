package nl.tudelft.pl2.data.storage.writers

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger

import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}
import org.apache.logging.log4j.LogManager


/**
  * Writes parsed graph data to a .hdr file on disk.
  *
  * @param headerPath The path to the .hdr file.
  * @author Maaike Visser
  */
class HeaderWriter(headerPath: Path) {

  /**
    * Channel to the parsed graph file.
    */
  val writeFile: RandomAccessFile = new RandomAccessFile(headerPath.toString, "rw")

  /**
    * The channel to the parsed graph file.
    */
  val writeChannel: FileChannel = writeFile.getChannel

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("HeaderWriter")

  /**
    * Stores a header to disk.
    * Headers are formatted as following:
    *
    * [H][LEN][OPTIONS]
    * [1][2  ][var    ]
    *
    * @param options The options that apply to this header.
    * @return The length of the header on disk.
    */
  def storeHeader(options: Options): Int = {
    val optLen = RWM.optionLength(options)
    val len = (RWC.CHAR_BYTES + RWC.SHORT_BYTES + optLen).toShort
    assert(len < Short.MaxValue)
    val buf = ByteBuffer.allocateDirect(len)
    buf.put(RWC.HDR_PREFIX)
    buf.putShort((len - RWC.CHAR_BYTES).toShort)
    buf.put(RWM.optionsToByteArray(options))
    buf.flip()
    writeChannel.write(buf)
  }

  /**
    * Clears the file indicated by ctagPath.
    */
  def clearFile(): Unit = writeFile.setLength(0)

  /**
    * Closes the writers channel to the compressed file,
    * as well as the random access channel.
    */
  def close(): Unit = {
    writeChannel.close()
    writeFile.close()
    LOGGER.debug("HeaderWriter with {} closed.", headerPath.getFileName)
  }
}
