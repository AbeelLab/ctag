package nl.tudelft.pl2.data.storage.readers

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.util.logging.Logger

import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}
import org.apache.logging.log4j.LogManager

import scala.collection.mutable

/**
  * Class that reads data from a parsed graph .hdr file.
  *
  * @author Maaike Visser
  */
object HeaderReader {

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("HeaderReader")

  /**
    * Reads in headers until a different kind of component
    * is encountered. Headers are separated by global delimiters.
    *
    * @return A [[mutable.Buffer]] with the headers from this file.
    */
  def readHeaders(headerPath: Path): mutable.Buffer[Options] = {
    LOGGER.debug("Reading headers from " + headerPath.getFileName)

    val channel: AsynchronousFileChannel = AsynchronousFileChannel.open(headerPath)
    try {
      assert(channel.size() < Int.MaxValue)

      val length = channel.size().toInt
      val buf = ByteBuffer.allocateDirect(length)

      //TODO: convert to observable pattern
      channel.read(buf, 0).get()
      channel.close()

      val arr = Array.ofDim[Byte](length)

      buf.flip()
      buf.get(arr)

      val headerBuf = mutable.Buffer[Options]()

      var i = 0

      //scalastyle:off while
      while (i < length) {
        val prefix = arr(i)
        val headLen = RWM.readShortFromBytes(arr(i + 1), arr(i + 2))
        assert(prefix == RWC.HDR_PREFIX)
        headerBuf += RWM.buildOptions(arr.slice(i + 3, i + 1 + headLen))
        i += headLen + 1
      }
      //scalastyle:on while

      LOGGER.debug("Done reading headers from " + headerPath.getFileName)
      headerBuf
    } finally {
      channel.close()
    }
  }
}
