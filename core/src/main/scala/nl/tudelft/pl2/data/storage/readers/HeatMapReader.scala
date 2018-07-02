package nl.tudelft.pl2.data.storage.readers

import java.nio.{ByteBuffer, IntBuffer}
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{Path, StandardOpenOption}
import java.util.concurrent.{ConcurrentHashMap, CountDownLatch}

import nl.tudelft.pl2.data.Scheduler
import nl.tudelft.pl2.data.storage.{HeatMap, HeatMapInfo}
import org.apache.logging.log4j.{Logger, LogManager}

/**
  * Reader for reading heat-maps from file. Reading heat-maps
  * creates a mapping from layers to the number of nodes in
  * each of these layers.
  *
  * @param file The file to read the heat-map from.
  */
class HeatMapReader(file: Path) extends AutoCloseable {

  /**
    * Log4J [[org.apache.logging.log4j.Logger]] used to log
    * debug information and other significant events.
    */
  private val LOGGER: Logger = LogManager.getLogger("HeatMapReader")
  LOGGER.info("Created heatmap reader")

  private val HEADER_SIZE: Int = 12

  /**
    * The size of the buffer to use.
    */
  private val BUFF_SIZE: Int = 196608

  /**
    * The size of each entry in the stored file in bytes.
    */
  private val ENTRY_SIZE: Int = 12

  /**
    * The heat map, keeping track of how many nodes
    * there are in each layer.
    */
  private val nodesPerLayer = new ConcurrentHashMap[Integer, HeatMapInfo]()

  /**
    * The maximum in the nodes-per-layer mapping as
    * read from the stored heat map. A pair of the
    * layer at which most nodes are stacked and the
    * number of nodes in that layer.
    */
  private var maximum: (Integer, HeatMapInfo) = (-1, HeatMapInfo(0, 0))

  /**
    * The lock that is used to keep track of how many
    * threads are still reading. When this latch hits
    * 0, reading is done.
    */
  private var completionLock: CountDownLatch = _

  /**
    * The [[AsynchronousFileChannel]] used to read from
    * the .hmp file asynchronously.
    */
  private var fileChannel: AsynchronousFileChannel = _

  /**
    * Handler called upon completion or failure of reading
    * a part of the file asynchronously. This handler also
    * decrements the completionLock upon completing its task.
    */
  private class LayerReadHandler extends CompletionHandler[Integer, ByteBuffer] {
    override def completed(result: Integer, buffer: ByteBuffer): Unit = {
      assert(buffer.position() % 2 == 0)

      Scheduler.schedule(() => {
        readBuffer(buffer.flip().asInstanceOf[ByteBuffer].asIntBuffer())

        completionLock.countDown()
      })
    }

    override def failed(exc: Throwable, attachment: ByteBuffer): Unit = {
      LOGGER.error(s"Reading heat-map failed for buffer $attachment...", exc)
      completionLock.countDown()
    }
  }

  /**
    * Reads the provided [[file]] as a list of integer-pairs
    * that each represent a layer ID and the number of nodes
    * in that layer. Returns a mapping of these layer IDs to
    * the number of nodes once finished reading.
    *
    * @return The [[HeatMap]] object representing the
    *         nodes-per-layer map and the maximum layer found.
    */
  def read(): HeatMap = {
    fileChannel = AsynchronousFileChannel.open(file, StandardOpenOption.READ)

    val length = fileChannel.size()
    assert((length - HEADER_SIZE) % ENTRY_SIZE == 0)

    completionLock = new CountDownLatch((length / ENTRY_SIZE).toInt)
    var threadCount = 1
    for (i <- 0L to (length - HEADER_SIZE) / BUFF_SIZE) {
      val buffer = ByteBuffer.allocateDirect(BUFF_SIZE)

      if (i * BUFF_SIZE + HEADER_SIZE < length) {
        fileChannel.read(buffer, i * BUFF_SIZE + HEADER_SIZE,
          buffer, new LayerReadHandler())
        threadCount += 1
      }
    }
    val buffer = ByteBuffer.allocateDirect(HEADER_SIZE)
    fileChannel.read(buffer, 0, buffer, new MaximumReadHandler())

    for (_ <- 1 to (length / ENTRY_SIZE).toInt - threadCount)
      completionLock.countDown()
    completionLock.await()

    new HeatMap(maximum, nodesPerLayer)
  }

  /**
    * Reads layers and number of nodes in that layer from
    * the given buffer and stores them in the shared [[nodesPerLayer]]
    * heat-map.
    *
    * @param buffer The buffer to read layer-nodes pairs from.
    */
  private def readBuffer(buffer: IntBuffer): Unit = {
    val length = buffer.capacity()
    for (i <- 0 until length / 3) {
      val layer = buffer.get(3 * i)
      val nodes = buffer.get(3 * i + 1)
      val sequences = buffer.get(3 * i + 2)

      assert(!nodesPerLayer.containsKey(layer))
      nodesPerLayer.put(layer, HeatMapInfo(nodes, sequences))
    }
  }

  /**
    * Handler called upon completion or failure of reading
    * the maximum part of the file asynchronously.
    */
  private class MaximumReadHandler extends CompletionHandler[Integer, ByteBuffer] {
    override def completed(result: Integer, buffer: ByteBuffer): Unit = {
      assert(buffer.position() == HEADER_SIZE)

      val intBuffer = buffer.flip().asInstanceOf[ByteBuffer].asIntBuffer()

      maximum = (intBuffer.get(0), HeatMapInfo(intBuffer.get(1), intBuffer
        .get(2)))
      LOGGER.debug(s"Found maximum nodes to be ${maximum._1} and maximum " +
        s"sequences to be" + s" ${maximum._2}.")

      completionLock.countDown()
    }

    override def failed(exc: Throwable, attachment: ByteBuffer): Unit = {
      LOGGER.error(s"Reading maximum failed for buffer $attachment...", exc)
      completionLock.countDown()
    }
  }

  /**
    * Closes the internally used channels.
    */
  override def close(): Unit =
    fileChannel.close()

}

/**
  * Companion object for [[HeatMapReader]] to define statically
  * usable functions that allow for easy reading.
  */
object HeatMapReader {

  /**
    * Reads the given file as a list of integer-pairs
    * that each represent a layer ID and the number of nodes
    * in that layer. Returns a mapping of these layer IDs to
    * the number of nodes once finished reading.
    *
    * @param file The file to read the mapping from.
    * @return The mapping as read from the given file.
    */
  def read(file: Path): HeatMap = {
    val reader = new HeatMapReader(file)
    try {
      reader.read()
    } finally {
      reader.close()
    }
  }

}
