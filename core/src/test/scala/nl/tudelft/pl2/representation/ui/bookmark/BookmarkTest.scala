package nl.tudelft.pl2.representation.ui.bookmark

import java.util

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * This class tests the bookmark class.
  */
@RunWith(classOf[JUnitRunner])
class BookmarkTest extends FunSuite {

  /**
    * Test the toString method of the bookmark object
    */
  test("testToString") {
    val treeset1: util.TreeSet[Int] = new util.TreeSet[Int]()
    treeset1.add(1)
    treeset1.add(2)

    val bookmark: Bookmark = new Bookmark(treeset1, 1, 2, 1,
      "name", "desc")
    assertResult(bookmark.toString) {
      "BK:name;Z:1.0;T:2.0;R:1.0;N:[1, 2];DESC:desc;"
    }

  }

}
