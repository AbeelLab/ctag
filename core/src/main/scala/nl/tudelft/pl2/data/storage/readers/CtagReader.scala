package nl.tudelft.pl2.data.storage.readers

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path

import nl.tudelft.pl2.data.builders.{BuilderBubble, BuilderChain, BuilderIndel, BuilderNode}
import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}
import nl.tudelft.pl2.representation.exceptions.CTagException
import nl.tudelft.pl2.representation.external.Node
import org.apache.logging.log4j.{Logger, LogManager}

import scala.collection.mutable

case class InvalidPrefixException(msg: String, loc: String) extends CTagException(msg, loc)

/**
  * Reads data chunks from disks and parses them to Nodes or BuilderNodes.
  *
  * @author Maaike Visser
  */
class CtagReader(filePath: Path) extends AutoCloseable {

  val LOGGER: Logger = LogManager.getLogger("CtagReader")

  val readFile: RandomAccessFile = new RandomAccessFile(filePath.toString, "r")
  val readChannel: AsynchronousFileChannel = AsynchronousFileChannel.open(filePath)

  /**
    * Reads a chunk of data from disk into memory to pass to the cache.
    *
    * @param offset The offset of this chunk in the file.
    * @param length The length of this chunk in the file.
    * @return A mutable.Buffer with the [[Node]]s from
    *         this chunk.
    */
  def readDataChunk(offset: Long, length: Int): mutable.Buffer[Node] =
    readDataChunkToBuilderNodes(offset, length).map(bn => bn.nodify())

  def readDataChunkToBuilderNodes(offset: Long, length: Int): mutable.Buffer[BuilderNode] = {
    LOGGER.debug("Loading chunk from {} at offset {} with length {}.",
      filePath, offset, length)

    val builderNodeBuf = mutable.Buffer[BuilderNode]()
    val buf = ByteBuffer.allocateDirect(length)

    //TODO: convert to observable pattern
    readChannel.read(buf, offset).get()

    val arr = Array.ofDim[Byte](length)

    buf.flip()
    buf.get(arr)

    var i = 0

    //scalastyle:off while
    while (i < length) {
      val prefix = arr(i)
      assert(prefix == RWC.NOD_PREFIX || prefix == RWC.BUB_PREFIX || prefix == RWC.IND_PREFIX ||
        prefix == RWC.CHA_PREFIX)

      var nodeLen = -1

      if (prefix == RWC.NOD_PREFIX) {
        nodeLen = RWM.readIntFromByteArray(arr.slice(i + 1, i + 1 + RWC.INT_BYTES))
        builderNodeBuf += buildBuilderNode(arr.slice(i + 1, i + 1 + nodeLen))
      } else if (prefix == RWC.BUB_PREFIX) {
        nodeLen = RWM.readIntFromByteArray(arr.slice(i + 1, i + 1 + RWC.INT_BYTES))
        builderNodeBuf += buildBuilderBubble(arr.slice(i + 1, i + 1 + nodeLen))
      } else if (prefix == RWC.IND_PREFIX) {
        nodeLen = RWM.readIntFromByteArray(arr.slice(i + 1, i + 1 + RWC.INT_BYTES))
        builderNodeBuf += buildBuilderIndel(arr.slice(i + 1, i + 1 + nodeLen))
      } else if (prefix == RWC.CHA_PREFIX) {
        nodeLen = RWM.readShortFromBytes(arr(i + 1), arr(i + 1 + 1))
        builderNodeBuf += buildBuilderChain(arr.slice(i + 1, i + 1 + nodeLen))
      } else {
        throw InvalidPrefixException(s"$prefix is not a valid component prefix at this zoom level.",
          "While loading the TwoZoomCache.")
      }
      i += nodeLen + 1
    }
    //scalastyle:on while
    LOGGER.debug("Successfully read chunk.")
    builderNodeBuf
  }

  /**
    * Builds a BuilderNode from a byte array.
    * The array is formatted as follows:
    *
    * [N][LEN][N_LEN][C_LEN][O_LEN][IL_NUM][OL_NUM][id][layer][name ][content][options][links     ]
    * [1][4  ][2    ][4    ][2    ][2     ][2     ][4 ][4    ][N_LEN][C_LEN  ][O_NUM  ][4*LINK_NUM]
    *
    * @param arr The byte array with the Node information.
    * @return A Node.
    */
  def buildBuilderNode(arr: Array[Byte]):
  BuilderNode = {
    val len =
      RWM.readIntFromByteArray(arr.slice(RWC.LEN_POS, RWC.LEN_POS + RWC.INT_BYTES))
    val nameLen =
      RWM.readShortFromBytes(arr(RWC.NAMELEN_POS), arr(RWC.NAMELEN_POS + 1))
    val contentLen =
      RWM.readIntFromByteArray(arr.slice(RWC.CONTLEN_POS, RWC.CONTLEN_POS + RWC.INT_BYTES))

    val optLen = RWM.readShortFromBytes(arr(RWC.N_OPTLEN_POS), arr(RWC.N_OPTLEN_POS + 1))
    val inLinkNum = RWM.readShortFromBytes(arr(RWC.N_ILNUM_POS), arr(RWC.N_ILNUM_POS + 1))
    val outLinkNum = RWM.readShortFromBytes(arr(RWC.N_OLNUM_POS), arr(RWC.N_OLNUM_POS + 1))
    val numGenomes = RWM.readShortFromBytes(arr(RWC.N_GC_NUM_POS), arr(RWC.N_GC_NUM_POS + 1))

    val id = ByteBuffer.wrap(arr.slice(RWC.N_ID_POS, RWC.N_ID_POS + RWC.INT_BYTES)).getInt
    val layer = ByteBuffer.wrap(
      arr.slice(RWC.N_LAYER_POS, RWC.N_LAYER_POS + RWC.INT_BYTES)).getInt

    val name = new String(arr.slice(RWC.N_NAME_POS, RWC.N_NAME_POS + nameLen))
    val content = new String(
      arr.slice(RWC.N_NAME_POS + nameLen, RWC.N_NAME_POS + nameLen + contentLen))

    val options = RWM.buildOptions(arr.slice(RWC.N_NAME_POS + nameLen + contentLen,
      RWC.N_NAME_POS + nameLen + contentLen + optLen))

    val incomingIDs = RWM.buildEdgeIDs(arr.slice(RWC.N_NAME_POS + nameLen + contentLen + optLen,
      RWC.N_NAME_POS + nameLen + contentLen + optLen + RWC.INT_BYTES * inLinkNum))

    val outgoingIDs = RWM.buildEdgeIDs(
      arr.slice(RWC.N_NAME_POS + nameLen + contentLen + optLen + RWC.INT_BYTES * inLinkNum,
        RWC.N_NAME_POS + nameLen + contentLen + optLen
          + RWC.INT_BYTES * inLinkNum
          + RWC.INT_BYTES * outLinkNum))

    val genomeCoordinates = arr.slice(RWC.N_NAME_POS + nameLen + contentLen + optLen
      + RWC.INT_BYTES * inLinkNum + RWC.INT_BYTES * outLinkNum, RWC.N_NAME_POS
      + nameLen + contentLen + optLen + RWC.INT_BYTES * inLinkNum + RWC.INT_BYTES
      * outLinkNum + (RWC.INT_BYTES + RWC.LONG_BYTES) * numGenomes)
      .grouped(RWC.INT_BYTES + RWC.LONG_BYTES).map(genomeArr =>
      (new Integer(ByteBuffer.wrap(genomeArr.slice(0, RWC.INT_BYTES)).getInt),
        ByteBuffer.wrap(genomeArr.slice(RWC.INT_BYTES, genomeArr.length)).getLong)).toMap

    new BuilderNode(id, name, layer, content, incomingIDs, outgoingIDs, options, genomeCoordinates)
  }

  /**
    * Builds a BuilderBubble from a byte array.
    * The array is formatted as follows:
    *
    * [B][LEN][N_LEN][C_LEN][O_LEN][IL_NUM][id][layer][end][cHi][cLo][name ][content][options]
    * [inlinks]
    * [1][4  ][2    ][4    ][2    ][2     ][4 ][4    ][4  ][1  ][1  ][N_LEN][C_LEN  ][O_LEN  ]
    * [IL_NUM ]
    *
    * @param arr The byte array with the Node information.
    * @return A Node.
    */
  def buildBuilderBubble(arr: Array[Byte]): BuilderBubble = {
    val len = RWM.readIntFromByteArray(arr.slice(RWC.LEN_POS, RWC.LEN_POS + RWC.INT_BYTES))
    val nameLen = RWM.readShortFromBytes(arr(RWC.NAMELEN_POS), arr(RWC.NAMELEN_POS + 1))
    val contentLen = RWM.readIntFromByteArray(arr.slice(RWC.CONTLEN_POS,
      RWC.CONTLEN_POS + RWC.INT_BYTES))
    val optLen = RWM.readShortFromBytes(arr(RWC.B_OPTLEN_POS), arr(RWC.B_OPTLEN_POS + 1))
    val inLinkNum = RWM.readShortFromBytes(arr(RWC.B_ILNUM_POS), arr(RWC.B_ILNUM_POS + 1))
    val id = ByteBuffer.wrap(arr.slice(RWC.B_ID_POS, RWC.B_ID_POS + RWC.INT_BYTES)).getInt
    val layer = ByteBuffer.wrap(arr.slice(RWC.B_LAYER_POS, RWC.B_LAYER_POS + RWC.INT_BYTES)).getInt
    val end = ByteBuffer.wrap(arr.slice(RWC.B_END_POS, RWC.B_END_POS + RWC.INT_BYTES)).getInt
    val cHi = arr(RWC.B_CHI_POS).toChar
    val cLo = arr(RWC.B_CLO_POS).toChar
    val name = new String(arr.slice(RWC.B_NAME_POS, RWC.B_NAME_POS + nameLen))
    val content = new String(arr.slice(RWC.B_NAME_POS + nameLen,
      RWC.B_NAME_POS + nameLen + contentLen))
    val options = RWM.buildOptions(arr.slice(RWC.B_NAME_POS + nameLen + contentLen,
      RWC.B_NAME_POS + nameLen + contentLen + optLen))
    val inEdges = RWM.buildEdgeIDs(arr.slice(RWC.B_NAME_POS + nameLen +
      contentLen + optLen,
      RWC.B_NAME_POS + nameLen + contentLen + optLen + RWC.INT_BYTES * inLinkNum))

    BuilderBubble(id, name, layer, content, cHi, cLo, options, inEdges, end)
  }

  /**
    * Builds a BuilderIndel from a byte array.
    * The array is formatted as follows:
    *
    * [I][LEN][N_LEN][C_LEN][CM_LEN][O_LEN][IL_NUM][id][layer][end][name ][content][midContent]
    * [options][incoming]
    * [1][4  ][2    ][4    ][4     ][2    ][2     ][4 ][4    ][4  ][N_LEN][C_LEN  ][CM_LEN    ]
    * [O_LEN  ][4*IL_NUM]
    *
    * @param arr The byte array with the Node information.
    * @return A Node.
    */
  def buildBuilderIndel(arr: Array[Byte]): BuilderIndel = {
    val len = RWM.readIntFromByteArray(arr.slice(RWC.LEN_POS, RWC.LEN_POS + RWC.INT_BYTES))
    val nameLen = RWM.readShortFromBytes(arr(RWC.NAMELEN_POS), arr(RWC.NAMELEN_POS + 1))
    val contentLen = RWM.readIntFromByteArray(arr.slice(RWC.CONTLEN_POS,
      RWC.CONTLEN_POS + RWC.INT_BYTES))
    val midContentLen = RWM.readIntFromByteArray(arr.slice(RWC.I_MIDLEN_POS,
      RWC.I_MIDLEN_POS + RWC.INT_BYTES))
    val optLen = RWM.readShortFromBytes(arr(RWC.I_OPTLEN_POS), arr(RWC.I_OPTLEN_POS + 1))
    val inLinkNum = RWM.readShortFromBytes(arr(RWC.I_ILNUM_POS), arr(RWC.I_ILNUM_POS + 1))
    val id = ByteBuffer.wrap(arr.slice(RWC.I_ID_OFFSET, RWC.I_ID_OFFSET + RWC.INT_BYTES)).getInt
    val layer = ByteBuffer.wrap(arr.slice(RWC.I_LAYER_POS, RWC.I_LAYER_POS + RWC.INT_BYTES)).getInt
    val end = ByteBuffer.wrap(arr.slice(RWC.I_END_POS, RWC.I_END_POS + RWC.INT_BYTES)).getInt
    val name = new String(arr.slice(RWC.I_NAME_POS, RWC.I_NAME_POS + nameLen))
    val content = new String(arr.slice(RWC.I_NAME_POS + nameLen,
      RWC.I_NAME_POS + nameLen + contentLen))
    val midContent = new String(arr.slice(RWC.I_NAME_POS + nameLen + contentLen,
      RWC.I_NAME_POS + nameLen + contentLen + midContentLen))
    val options = RWM.buildOptions(arr.slice(RWC.I_NAME_POS + nameLen + contentLen + midContentLen,
      RWC.I_NAME_POS + nameLen + contentLen + midContentLen + optLen))
    val inLinks = RWM.buildEdgeIDs(
      arr.slice(RWC.I_NAME_POS + nameLen + contentLen + midContentLen + optLen,
        RWC.I_NAME_POS + nameLen + contentLen + midContentLen + optLen + RWC.INT_BYTES * inLinkNum))

    BuilderIndel(id, name, layer, content, midContent, options, inLinks, end)
  }

  /**
    * Builds a BuilderChain from a byte array.
    * The array is formatted as follows:
    *
    * [B][LEN][O_LEN][IL_NUM][id][layer][end][options][inlinks]
    * [1][2  ][2    ][2     ][4 ][4    ][4  ][O_LEN  ][IL_NUM ]
    *
    * @param arr The byte array with the Node information.
    * @return A Node.
    */
  def buildBuilderChain(arr: Array[Byte]): BuilderChain = {
    val len = RWM.readShortFromBytes(arr(RWC.LEN_POS), arr(RWC.LEN_POS + 1))
    val optLen = RWM.readShortFromBytes(arr(RWC.C_OPTLEN_POS), arr(RWC.C_OPTLEN_POS + 1))
    val inLinkNum = RWM.readShortFromBytes(arr(RWC.C_ILNUM_POS), arr(RWC.C_ILNUM_POS + 1))
    val id = ByteBuffer.wrap(arr.slice(RWC.C_ID_POS, RWC.C_ID_POS + RWC.INT_BYTES)).getInt
    val layer = ByteBuffer.wrap(arr.slice(RWC.C_LAYER_POS, RWC.C_LAYER_POS + RWC.INT_BYTES)).getInt
    val end = ByteBuffer.wrap(arr.slice(RWC.C_END_POS, RWC.C_END_POS + RWC.INT_BYTES)).getInt
    val options = RWM.buildOptions(arr.slice(RWC.C_OPT_POS, RWC.C_OPT_POS + optLen))
    val inEdges = RWM.buildEdgeIDs(arr.slice(RWC.C_OPT_POS + optLen,
      RWC.C_OPT_POS + optLen + RWC.INT_BYTES * inLinkNum))

    BuilderChain(id, layer, options, inEdges, mutable.Buffer(end))
  }

  def close(): Unit = {
    readChannel.close()
    readFile.close()
    LOGGER.debug(this.getClass + " with file {} closed.", filePath.getFileName)
  }
}
