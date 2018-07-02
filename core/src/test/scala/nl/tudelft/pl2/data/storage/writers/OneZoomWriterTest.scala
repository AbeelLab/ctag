package nl.tudelft.pl2.data.storage.writers

import java.io.File
import java.nio.file.{Path, Paths}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class OneZoomWriterTest extends FunSuite with BeforeAndAfter {

  val TEMP: File = File.createTempFile("temp", ".ctg1")
  val TEMP_PATH: Path = Paths.get(TEMP.getAbsolutePath)
  val END = 5
  var writer: CtagWriter = _

  before {
    writer = new CtagWriter(TEMP_PATH)
  }

  test("Store one node with no options or links") {
    writer.storeNode(1, "name",
      1, "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map(),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length
        + "content".length)
    }
  }
  test("Store bubble with no options") {
    writer.storeBubble(1, 1,
      "name", "content",
      ('1', '2'),
      Map(),
      ListBuffer(),
      END) should be {
      (RWC.CHAR_BYTES
        + RWC.B_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.B_INT_FIELDS * RWC.INT_BYTES
        + RWC.B_BYTE_FIELDS * RWC.CHAR_BYTES
        + "name".length
        + "content".length)
    }
  }

  test("Store indel with no options") {
    writer.storeIndel(1,
      1,
      "name",
      "content", "content",
      Map(),
      ListBuffer(),
      END) should be {
      (RWC.CHAR_BYTES
        + RWC.I_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.I_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + "content".length)
    }
  }

  test("Store seg, bub, indel with options") {
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map("A" -> ('B', "C")),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length
        + "content".length
        + RWM.optionLength(Map("A" -> ('B', "C"))))
    }
    writer.storeBubble(1,
      1,
      "name",
      "content",
      ('1', '2'),
      Map("A" -> ('B', "C")),
      ListBuffer(),
      END + 1) should be {
      (RWC.CHAR_BYTES
        + RWC.B_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.B_INT_FIELDS * RWC.INT_BYTES
        + RWC.B_BYTE_FIELDS * RWC.CHAR_BYTES
        + "name".length
        + "content".length
        + RWM.optionLength(Map("A" -> ('B', "C"))))
    }
    writer.storeIndel(1,
      1,
      "name",
      "content", "content",
      Map("A" -> ('B', "C")),
      ListBuffer(),
      END) should be {
      (RWC.CHAR_BYTES
        + RWC.I_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.I_INT_FIELDS * RWC.INT_BYTES
        + "name".length + "content".length
        + "content".length
        + RWM.optionLength(Map("A" -> ('B', "C"))))
    }
  }

  after {
    writer.clearFile()
    writer.close()
  }
}

