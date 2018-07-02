package nl.tudelft.pl2.data.storage.readers

import java.io.File
import java.nio.file.{Path, Paths}

import nl.tudelft.pl2.data.storage.writers.CtagWriter
import nl.tudelft.pl2.representation.external.{Edge, Node}
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}

@RunWith(classOf[JUnitRunner])
class ZeroZoomReaderTest extends FunSuite with BeforeAndAfter {

  val TEMP: File = File.createTempFile("temp", ".ctg.0")
  val TEMP_PATH: Path = Paths.get(TEMP.getAbsolutePath)
  var writer: CtagWriter = _
  var reader: CtagReader = _

  before {
    writer = new CtagWriter(TEMP_PATH)
    reader = new CtagReader(TEMP_PATH)
  }

  test("Store one node with no options or links and read") {
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map(),
      Map())
    writer.close()
    reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length) should be {
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
  test("Store one node with options and no links and read") {
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map())
    writer.close()

    val chunk = reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2"))))

    chunk.head.id should be {
      1
    }
    chunk.head.name should be {
      "name"
    }
    chunk.head.content should be {
      "content"
    }
    chunk.head.layer should be {
      1
    }
    chunk.head.outgoing should be {
      mutable.ListBuffer()
    }
    chunk.head.incoming should be {
      mutable.ListBuffer()
    }
    chunk.head.options should be {
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2"))
    }
    chunk.head should be {
      new Node(1,
        "name",
        1,
        "content",
        mutable.ListBuffer(),
        mutable.ListBuffer(),
        Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
        Map())
    }
  }

  test("Store one node with no options but with links and read") {
    writer.storeNode(
      1,
      "name",
      1,
      "content",
      mutable.Buffer(0),
      mutable.Buffer(2, 3),
      Map(),
      Map())
    writer.close()

    reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + 3 * RWC.INT_BYTES) should be {
      mutable.Buffer(
        new Node(1, "name", 1, "content",
          mutable.ListBuffer(
            Edge(0, 1)
          ),
          mutable.ListBuffer(
            Edge(1, 2),
            Edge(1, 3)
          ), Map(), Map())
      )
    }
  }

  test("Store one node with options and links and read") {
    writer.storeNode(
      1,
      "name",
      1,
      "content",
      mutable.Buffer(0),
      mutable.Buffer(2, 3),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map())
    writer.close()

    reader.readDataChunk(0,
      RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
        + 3 * RWC.INT_BYTES) should be {
      mutable.Buffer(
        new Node(1, "name", 1, "content",
          mutable.ListBuffer(
            Edge(0, 1)
          ),
          mutable.ListBuffer(
            Edge(1, 2),
            Edge(1, 3)
          ), Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
          Map()
        )
      )
    }
  }
  test("Store two nodes with options and links and read") {
    val firstToLink = 3
    val secondToLink = 4
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(0),
      mutable.Buffer(2, 3),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map())
    writer.storeNode(2,
      "name",
      2,
      "content",
      mutable.Buffer(1),
      mutable.Buffer(3, secondToLink),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map())
    writer.close()

    reader.readDataChunk(0,
      2 * (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
        + 3 * RWC.INT_BYTES)) should be {
      mutable.Buffer(
        new Node(1, "name", 1, "content",
          mutable.ListBuffer(Edge(0, 1)),
          mutable.ListBuffer(
            Edge(1, 2),
            Edge(1, firstToLink)
          ), Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")), Map()),
        new Node(2, "name", 2, "content",
          mutable.ListBuffer(Edge(1, 2)),
          mutable.ListBuffer(
            Edge(2, firstToLink),
            Edge(2, secondToLink)
          ), Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")), Map())
      )
    }

    reader.readDataChunk(
      RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
        + 3 * RWC.INT_BYTES,
      RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
        + 3 * RWC.INT_BYTES) should be {

      mutable.Buffer(
        new Node(2, "name", 2, "content", mutable.ListBuffer(
          Edge(2, firstToLink),
          Edge(2, secondToLink)
        ), mutable.ListBuffer(), Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
          Map())
      )
    }
  }

  after {
    reader.close()
  }

}
