package nl.tudelft.pl2.data.storage.writers

import java.io.File
import java.nio.file.Paths

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class HeaderWriterTest extends FunSuite with BeforeAndAfter {

  private val TEMP: File = File.createTempFile("temp", ".hdr")

  private val PATH = Paths.get(TEMP.getAbsolutePath)
  private val SINGLE_OPTION = Map("A" -> ('B', "C"))
  private val DOUBLE_OPTION = Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2"))
  private val SINGLE_LEN = 10
  private val DOUBLE_LEN = 21

  private var writer: HeaderWriter = _

  before {
    writer = new HeaderWriter(PATH)
  }


  test("Store header with one option") {
    writer.storeHeader(SINGLE_OPTION) should be {
      SINGLE_LEN
    }
  }

  test("Store header with two options") {
    writer.storeHeader(DOUBLE_OPTION) should be {
      DOUBLE_LEN
    }
  }

  after {
    writer.clearFile()
    writer.close()
  }

}
