package nl.tudelft.pl2.data.storage.readers

import java.io.File
import java.nio.file.{Path, Paths}

import nl.tudelft.pl2.data.storage.writers.CtagWriter
import nl.tudelft.pl2.representation.external.{Chain, Edge, Node}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC}

@RunWith(classOf[JUnitRunner])
class TwoZoomReaderTest extends FunSuite with BeforeAndAfter {

  val TEMP: File = File.createTempFile("temp", ".ctg2")
  val TEMP_PATH: Path = Paths.get(TEMP.getAbsolutePath)

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

  test("Store one chain with no options and read") {
    val chainLen = writer.storeChain(1,
      1,
      Map(),
      ListBuffer(),
      0)
    writer.close()
    val chunk = reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.C_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.C_INT_FIELDS * RWC.INT_BYTES)
    chunk should be {
      mutable.Buffer(Chain(1, 1,
        ListBuffer(),
        ListBuffer(Edge(1, 0)),
        Map()))
    }
  }

  after {
    reader.close()
  }

}
