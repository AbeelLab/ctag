package nl.tudelft.pl2.data.storage.writers

import java.io.File
import java.nio.file.{Path, Paths}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class ZeroZoomWriterTest extends FunSuite with BeforeAndAfter {

  val TEMP: File = File.createTempFile("temp", ".ctg.0")
  val TEMP_PATH: Path = Paths.get(TEMP.getAbsolutePath)
  var writer: CtagWriter = _

  before {
    writer = new CtagWriter(TEMP_PATH)
  }

  test("Store one node with no options or links") {
    writer.storeNode(1, "name", 1, "content",
      mutable.Buffer(), mutable.Buffer(), Map(), Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length
        + "content".length)
    }
  }
  test("Store one node with options and no links") {
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2"))))
    }
  }
  test("Store one node with no options but with links") {
    val NUM_LINKS = 3
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(0),
      mutable.Buffer(2, 3),
      Map(),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + NUM_LINKS * RWC.INT_BYTES)
    }
  }
  test("Store one node with options and links") {
    val NUM_LINKS = 3
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(0),
      mutable.Buffer(2, 3),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
        + NUM_LINKS * RWC.INT_BYTES)
    }
  }
  test("Store two nodes with options and links") {
    val NUM_LINKS = 3
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(0),
      mutable.Buffer(2, 3),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length
        + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
        + NUM_LINKS * RWC.INT_BYTES)
    }
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(0),
      mutable.Buffer(2, 3),
      Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
        + NUM_LINKS * RWC.INT_BYTES)
    }
    writer.getFileLength should be {
      2 *
        (RWC.CHAR_BYTES
          + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
          + RWC.N_INT_FIELDS * RWC.INT_BYTES
          + "name".length + "content".length
          + RWM.optionLength(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
          + NUM_LINKS * RWC.INT_BYTES)
    }
  }


  after {
    writer.clearFile()
    writer.close()
  }
}

