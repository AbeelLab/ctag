package nl.tudelft.pl2.data.storage.readers

import java.io.File
import java.nio.file.{Path, Paths}

import nl.tudelft.pl2.data.storage.writers.CtagWriter
import nl.tudelft.pl2.representation.external.{Bubble, Indel, Node}
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC}

@RunWith(classOf[JUnitRunner])
class OneZoomReaderTest extends FunSuite with BeforeAndAfter {

  val TEMP: File = File.createTempFile("temp", ".ctg1")
  val TEMP_PATH: Path = Paths.get(TEMP.getAbsolutePath)

  val END = 4

  var writer: CtagWriter = _
  var reader: CtagReader = _


  before {
    writer = new CtagWriter(TEMP_PATH)
    reader = new CtagReader(TEMP_PATH)
  }

  test("Store one node with no options and read") {
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map(),
      Map())
    writer.close()
    val chunk = reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length)
    chunk should be {
      mutable.Buffer(
        new Node(1,
          "name",
          1,
          "content",
          mutable.ListBuffer(),
          mutable.ListBuffer(),
          Map(),
          Map()))
    }
  }

  test("Store one bubble with no options and read") {
    writer.storeBubble(1,
      1,
      "name",
      "content",
      ('1', '2'),
      Map(),
      ListBuffer(),
      END)
    writer.close()
    val chunk = reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.B_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.B_INT_FIELDS * RWC.INT_BYTES
        + RWC.B_BYTE_FIELDS * RWC.CHAR_BYTES
        + "name".length + "content".length)
    chunk should be {
      mutable.Buffer(
        Bubble(1,
          "name",
          1,
          "content",
          '1', '2',
          ListBuffer(),
          Map(),
          END))
    }
  }

  test("Store one indel with no options and read") {
    writer.storeIndel(1,
      1,
      "name",
      "content",
      "content",
      Map(),
      ListBuffer(),
      END)
    writer.close()
    val chunk = reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.I_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.I_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + "content".length)
    chunk should be {
      mutable.Buffer(
        Indel(1,
          "name",
          1,
          "content",
          "content",
          ListBuffer(),
          Map(),
          END))
    }
  }

  after {
    reader.close()
  }

}
