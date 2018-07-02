package nl.tudelft.pl2.data.storage.writers

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path

import nl.tudelft.pl2.data.Graph
import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}
import nl.tudelft.pl2.representation.external.{Bubble, Chain, Indel, Node}
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.mutable

/**
  * Writes different kinds of Nodes to disk.
  *
  * @author Maaike Visser
  */
class CtagWriter(filePath: Path) extends AutoCloseable {

  val LOGGER: Logger = LogManager.getLogger("CtagWriter")

  val writeFile: RandomAccessFile = new RandomAccessFile(filePath.toString, "rw")
  val writeChannel: FileChannel = writeFile.getChannel

  val BUFF_SIZE = 131072

  var buf: ByteBuffer = ByteBuffer.allocateDirect(BUFF_SIZE)

  /**
    * Stores a [[Node]] to disk in the following format:
    *
    * [S][LEN][N_LEN][C_LEN][O_LEN][IL_NUM][OL_NUM][id][layer][name ][content][options][links     ]
    * [1][4  ][2    ][4    ][2    ][2     ][2     ][4 ][4    ][N_LEN][C_LEN  ][O_NUM  ][4*LINK_NUM]
    *
    * @param id      The index of this [[Node]].
    * @param name    The name of this [[Node]].
    * @param layer   The layer this [[Node]] belongs to.
    * @param content The content of this [[Node]].
    * @param options The options that apply to this [[Node]].
    * @return The length of the [[Node]] on disk.
    */
  def storeNode(id: Int,
                name: String,
                layer: Int,
                content: String,
                incoming: mutable.Buffer[Int],
                outgoing: mutable.Buffer[Int],
                options: Options,
                genomeCoordinates: Graph.Coordinates): Int = {

    val optLen = RWM.optionLength(options)
    val len = (RWC.CHAR_BYTES + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + name.length
        + content.length
        + optLen
        + RWC.INT_BYTES * incoming.length
        + RWC.INT_BYTES * outgoing.length
        + (RWC.INT_BYTES + RWC.LONG_BYTES) * genomeCoordinates.size)

    expandBuffer(len)

    buf.put(RWC.NOD_PREFIX)
    buf.putInt(len - RWC.CHAR_BYTES)
    buf.putShort(name.length.toShort)
    buf.putInt(content.length)
    buf.putShort(optLen.toShort)
    buf.putShort(incoming.length.toShort)
    buf.putShort(outgoing.length.toShort)
    buf.putShort(genomeCoordinates.size.toShort)

    buf.putInt(id)
    buf.putInt(layer)
    buf.put(name.getBytes("US-ASCII"))
    buf.put(content.getBytes("US-ASCII"))
    buf.put(RWM.optionsToByteArray(options))
    incoming.foreach(i => buf.putInt(i))
    outgoing.foreach(o => buf.putInt(o))
    genomeCoordinates.foreach(gen => {
      buf.putInt(gen._1)
      buf.putLong(gen._2)
    })

    len
  }

  /**
    * Stores a [[Bubble]] to disk in the following format:
    *
    * [B][LEN][N_LEN][C_LEN][O_LEN][IL_NUM][id][layer][end][cHi][cLo][name ][content][options]
    * [inlinks]
    * [1][4  ][2    ][4    ][2    ][2     ][4 ][4    ][4  ][1  ][1  ][N_LEN][C_LEN  ][O_LEN  ]
    * [IL_NUM ]
    *
    * @param id         The index of this [[Bubble]].
    * @param layer      The layer this [[Bubble]] belongs to.
    * @param name       The name of this [[Bubble]].
    * @param content    The content of this [[Bubble]].
    * @param midContent The content of the mid [[Bubble]]s.
    * @param options    The options that apply to this [[Bubble]].
    * @param end        The ID of the end [[Bubble]].
    * @return The length of the [[Bubble]] on disk.
    */
  def storeBubble(id: Int,
                  layer: Int,
                  name: String,
                  content: String,
                  midContent: (Char, Char),
                  options: Options,
                  incoming: mutable.Buffer[Int],
                  end: Int): Int = {
    val optLen = RWM.optionLength(options)
    val len = (RWC.CHAR_BYTES
        + RWC.B_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.B_INT_FIELDS * RWC.INT_BYTES
        + RWC.B_BYTE_FIELDS * RWC.CHAR_BYTES
        + name.length
        + content.length
        + optLen
        + RWC.INT_BYTES * incoming.length)

    expandBuffer(len)

    buf.put(RWC.BUB_PREFIX)
    buf.putInt(len - RWC.CHAR_BYTES)
    buf.putShort(name.length.toShort)
    buf.putInt(content.length)
    buf.putShort(optLen.toShort)
    buf.putShort(incoming.length.toShort)
    buf.putInt(id)
    buf.putInt(layer)
    buf.putInt(end)
    buf.put(midContent._1.toByte)
    buf.put(midContent._2.toByte)
    buf.put(name.getBytes("US-ASCII"))
    buf.put(content.getBytes("US-ASCII"))
    buf.put(RWM.optionsToByteArray(options))
    incoming.foreach(i => buf.putInt(i))

    len
  }

  /**
    * Stores an [[Indel]] to disk in the following format:
    *
    * [I][LEN][N_LEN][C_LEN][CM_LEN][O_LEN][IL_NUM][id][layer][end][name ][content][midContent]
    * [options][incoming]
    * [1][4  ][2    ][4    ][4     ][2    ][2     ][4 ][4    ][4  ][N_LEN][C_LEN  ][CM_LEN    ]
    * [O_LEN  ][4*IL_NUM]
    *
    * @param id         The index of this [[Indel]].
    * @param layer      The layer this [[Indel]] belongs to.
    * @param name       The name of this [[Indel]].
    * @param content    The content of this [[Indel]].
    * @param midContent The content of the middle [[Indel]].
    * @param options    The options that apply to this [[Indel]].
    * @param end        The id of the end [[Indel]].
    * @return The length of the [[Indel]] on disk.
    */
  def storeIndel(id: Int,
                 layer: Int,
                 name: String,
                 content: String,
                 midContent: String,
                 options: Options,
                 incoming: mutable.Buffer[Int],
                 end: Int): Int = {
    val optLen = RWM.optionLength(options)
    val len = (RWC.CHAR_BYTES
        + RWC.I_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.I_INT_FIELDS * RWC.INT_BYTES
        + name.length
        + content.length
        + midContent.length
        + optLen
        + RWC.INT_BYTES * incoming.length)

    expandBuffer(len)

    buf.put(RWC.IND_PREFIX)
    buf.putInt(len - RWC.CHAR_BYTES)
    buf.putShort(name.length.toShort)
    buf.putInt(content.length)
    buf.putInt(midContent.length)
    buf.putShort(optLen.toShort)
    buf.putShort(incoming.length.toShort)
    buf.putInt(id)
    buf.putInt(layer)
    buf.putInt(end)
    buf.put(name.getBytes("US-ASCII"))
    buf.put(content.getBytes("US-ASCII"))
    buf.put(midContent.getBytes("US-ASCII"))
    buf.put(RWM.optionsToByteArray(options))
    incoming.foreach(i => buf.putInt(i))

    len
  }

  /**
    * Stores a [[Chain]] to disk in the following format:
    *
    * [B][LEN][O_LEN][IL_NUM][id][layer][end][options][inlinks]
    * [1][2  ][2    ][2     ][4 ][4    ][4  ][O_LEN  ][IL_NUM ]
    *
    */
  def storeChain(id: Int,
                 layer: Int,
                 options: Options,
                 incoming: mutable.Buffer[Int],
                 end: Int): Int = {
    val optLen = RWM.optionLength(options)
    val len = (RWC.CHAR_BYTES
        + RWC.C_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.C_INT_FIELDS * RWC.INT_BYTES
        + optLen
        + RWC.INT_BYTES * incoming.length)

    expandBuffer(len)

    buf.put(RWC.CHA_PREFIX)
    buf.putShort((len - RWC.CHAR_BYTES).toShort)
    buf.putShort(optLen.toShort)
    buf.putShort(incoming.length.toShort)
    buf.putInt(id)
    buf.putInt(layer)
    buf.putInt(end)
    buf.put(RWM.optionsToByteArray(options))
    incoming.foreach(i => buf.putInt(i))

    len
  }

  /**
    * Flushes the current buffer if there were bytes written
    * to it and clears the byte buffer afterwards.
    */
  def flushBuffer(): Unit =
    if (buf.position > 0) {
      buf.flip()
      writeChannel.write(buf)

      buf.clear()
    }


  /**
    * Gets the length of the compressed gfa file.
    *
    * @return The length of the compressed file in bytes.
    */
  def getFileLength: Long = writeFile.length() + buf.position

  /**
    * Clears the parsed graph file.
    */
  def clearFile(): Unit = {
    writeFile.setLength(0)
    buf.clear()
  }

  override def close(): Unit = {
    flushBuffer()
    writeChannel.close()
    writeFile.close()
    LOGGER.debug(this.getClass + " with {} closed.", filePath.getFileName)
  }

  /**
    * Flushes the buffer and expands it if necessary.
    *
    * @param len The potential new length of the buffer.
    */
  private def expandBuffer(len: Int): Unit = {
    if (buf.position + len > BUFF_SIZE) {
      flushBuffer()
      if (len > BUFF_SIZE) {
        buf = ByteBuffer.allocateDirect(len)
      }
    }
  }

}
