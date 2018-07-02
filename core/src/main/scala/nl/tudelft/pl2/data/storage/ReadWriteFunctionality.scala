package nl.tudelft.pl2.data.storage

import java.nio.ByteBuffer

import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC}
import nl.tudelft.pl2.representation.external.Edge

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Constants for reading and writing compressed files.
  */
object ReadWriteConstants {
  /**
    * Number of bytes for registering the length of a component
    * or sub-component
    */
  val LONG_BYTES = 8
  val INT_BYTES = 4
  val SHORT_BYTES = 2
  val CHAR_BYTES = 1

  /**
    * Prefixes for different kinds of components.
    */
  val HDR_PREFIX: Byte = 'H'.toByte
  val NOD_PREFIX: Byte = 'N'.toByte
  val BUB_PREFIX: Byte = 'B'.toByte
  val IND_PREFIX: Byte = 'I'.toByte
  val CHA_PREFIX: Byte = 'C'.toByte

  /**
    * Offsets of [[nl.tudelft.pl2.representation.external.Node]] components on disk.
    */
  val LEN_POS = 0
  val NAMELEN_POS: Int = LEN_POS + RWC.INT_BYTES
  val CONTLEN_POS: Int = NAMELEN_POS + RWC.SHORT_BYTES

  /**
    * Offsets of [[nl.tudelft.pl2.representation.external.Node]] components on disk.
    */
  val N_OPTLEN_POS: Int = CONTLEN_POS + RWC.INT_BYTES
  val N_ILNUM_POS: Int = N_OPTLEN_POS + RWC.SHORT_BYTES
  val N_OLNUM_POS: Int = N_ILNUM_POS + RWC.SHORT_BYTES
  val N_GC_NUM_POS: Int = N_OLNUM_POS + RWC.SHORT_BYTES
  val N_ID_POS: Int = N_GC_NUM_POS + RWC.SHORT_BYTES
  val N_LAYER_POS: Int = N_ID_POS + RWC.INT_BYTES
  val N_NAME_POS: Int = N_LAYER_POS + RWC.INT_BYTES

  /**
    * Offsets of [[nl.tudelft.pl2.representation.external.Indel]] components on disk.
    */
  val I_MIDLEN_POS: Int = CONTLEN_POS + RWC.INT_BYTES
  val I_OPTLEN_POS: Int = I_MIDLEN_POS + RWC.INT_BYTES
  val I_ILNUM_POS: Int = I_OPTLEN_POS + RWC.SHORT_BYTES
  val I_ID_OFFSET: Int = I_ILNUM_POS + RWC.SHORT_BYTES
  val I_LAYER_POS: Int = I_ID_OFFSET + RWC.INT_BYTES
  val I_END_POS: Int = I_LAYER_POS + RWC.INT_BYTES
  val I_NAME_POS: Int = I_END_POS + RWC.INT_BYTES

  /**
    * Offsets of [[nl.tudelft.pl2.representation.external.Bubble]] components on disk.
    */
  val B_OPTLEN_POS: Int = CONTLEN_POS + RWC.INT_BYTES
  val B_ILNUM_POS: Int = B_OPTLEN_POS + RWC.SHORT_BYTES
  val B_ID_POS: Int = B_ILNUM_POS + RWC.SHORT_BYTES
  val B_LAYER_POS: Int = B_ID_POS + RWC.INT_BYTES
  val B_END_POS: Int = B_LAYER_POS + RWC.INT_BYTES
  val B_CHI_POS: Int = B_END_POS + RWC.INT_BYTES
  val B_CLO_POS: Int = B_CHI_POS + RWC.CHAR_BYTES
  val B_NAME_POS: Int = B_CLO_POS + RWC.CHAR_BYTES

  /**
    * Offsets of [[nl.tudelft.pl2.representation.external.Chain]] components on disk.
    */
  val C_OPTLEN_POS: Int = LEN_POS + RWC.SHORT_BYTES
  val C_ILNUM_POS: Int = C_OPTLEN_POS + RWC.SHORT_BYTES
  val C_ID_POS: Int = C_ILNUM_POS + RWC.SHORT_BYTES
  val C_LAYER_POS: Int = C_ID_POS + RWC.INT_BYTES
  val C_END_POS: Int = C_LAYER_POS + RWC.INT_BYTES
  val C_OPT_POS: Int = C_LAYER_POS + RWC.INT_BYTES


  /**
    * Number of fields in a Node that take either a char, short, or int in bytes on disk.
    */
  val N_SHORT_FIELDS = 5
  val N_INT_FIELDS = 4

  /**
    * Number of fields in a bubble that take either a char, short, or int in bytes on disk.
    */
  val B_BYTE_FIELDS = 2
  val B_SHORT_FIELDS = 3
  val B_INT_FIELDS = 5

  /**
    * Number of fields in a bubble that take either a char, short, or int in bytes on disk.
    */
  val I_SHORT_FIELDS = 3
  val I_INT_FIELDS = 6

  /**
    * Number of fields in a Node that take either a char, short, or int in bytes on disk.
    */
  val C_SHORT_FIELDS = 3
  val C_INT_FIELDS = 3
}

/**
  * Methods for reading and writing compressed files.
  */
object ReadWriteMethods {

  /**
    * Builds options from an array of strings.
    *
    * [LEN][POS][Type][Tag][Content]
    * [2  ][2  ][1   ][var][var    ]
    * [0-1][2-3][4   ][5-?][?      ]
    *
    * @param arr Array of bytes with the option information.
    * @return An [[Options]] map.
    */
  def buildOptions(arr: Array[Byte]): Options = {
    if (arr.length > 0) {
      val optMap = mutable.Map[String, (Char, String)]()
      //scalastyle:off while
      var i = 0
      while (i < arr.length) {
        val optLen = readShortFromBytes(arr(i), arr(i + 1))
        val tagLen = readShortFromBytes(arr(i + 2), arr(i + 3))
        val oType = arr(i + 4).toChar
        val tag = new String(arr.slice(i + 5, i + 5 + tagLen))
        val content = new String(arr.slice(i + 5 + tagLen, i + optLen))
        optMap.put(tag, (oType, content))
        i += optLen
      }
      //scalastyle:on while
      optMap.toMap
    } else {
      Map[String, (Char, String)]()
    }
  }

  /**
    * Builds a Buffer of IDs from a Byte array.
    * @param arr The array.
    * @return A buffer of IDs.
    */
  def buildEdgeIDs(arr: Array[Byte]): mutable.Buffer[Int] = {
    ListBuffer(
      arr.grouped(RWC.INT_BYTES).map(
        linkArr =>
          ByteBuffer.wrap(linkArr).getInt).toList: _*)
  }

  /**
    * Converts two bytes to a short.
    *
    * @param firstByte  The most significant byte.
    * @param secondByte The least significant byte.
    * @return A short.
    */
  def readShortFromBytes(firstByte: Byte, secondByte: Byte): Short =
    ((firstByte << 8) | (secondByte & 0xff)).toShort

  /**
    * Converts four bytes to an int.
    *
    * @param bytes An array of four bytes.
    * @return An int.
    */
  def readIntFromByteArray(bytes: Array[Byte]): Int = {
    ByteBuffer.wrap(bytes).getInt
  }

  /**
    * Gets the length of the [[Options]] that
    * belong to this element. Necessary to
    * allocate enough buffer space.
    *
    * The length of an options consists of the length of
    * its type + the tag length (always 1) + the length of
    * the content, plus two bytes for storing the length of the
    * options and two bytes for storing the length of the type,
    * which is needed to find the location of the tag within the
    * option.
    *
    * @param options The options to write to disk.
    * @return The length of the options in bytes.
    */
  def optionLength(options: Options): Int = {
    options.map(
      o =>
        o._1.length
          + o._2._2.length
          + RWC.CHAR_BYTES
          + RWC.SHORT_BYTES
          + RWC.SHORT_BYTES).sum
  }

  /**
    * Converts [[Options]] to a Byte array so that
    * it can be written to disk.
    *
    * Options are formatted as follows:
    *
    *
    * [LEN][POS][Type][Tag][Content]
    * [2  ][2  ][1   ][var][var    ]
    *
    * @param options The [[Options]] that are written to disk.
    */
  def optionsToByteArray(options: Options): Array[Byte] = {
    var optArray = Array[Byte]()

    options.foreach(o => {
      optArray = optArray ++ shortToByteArray((o._1.length + 1 + o._2._2.length + 4).toShort)
      optArray = optArray ++ shortToByteArray(o._1.length.toShort)
      optArray = optArray.:+(o._2._1.toByte)
      optArray = optArray ++ o._1.getBytes()
      optArray = optArray ++ o._2._2.getBytes()
    })
    optArray
  }

  /**
    * Converts a short to two bytes.
    *
    * @param s The short to convert.
    * @return A two-byte array.
    */
  def shortToByteArray(s: Short): Array[Byte] =
    Array[Byte](((s & 0xFF00) >> 8).toByte, (s & 0x00FF).toByte)

  /**
    * Converts an int to a four byte array.
    *
    * @param i The int to convert.
    * @return A four-byte array.
    */
  def intToByteArray(i: Int): Array[Byte] = ByteBuffer.allocate(RWC.INT_BYTES).putInt(i).array()
}
