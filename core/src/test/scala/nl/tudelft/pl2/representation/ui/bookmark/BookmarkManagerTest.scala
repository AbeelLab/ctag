package nl.tudelft.pl2.representation.ui.bookmark

import java.util

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * This class tests the bookmarkbuilder class.
  */
@RunWith(classOf[JUnitRunner])
class BookmarkManagerTest extends FunSuite {

  test("test basic bookmark creation") {
    val treest: util.TreeSet[Int] = new util.TreeSet[Int]()
    val bookmark: Bookmark = BookmarkManager.buildBookmark(treest,
      1, 2, 1, "Test", "desc", false)

    val bookmarkNew = new Bookmark(treest, 1, 2, 1, "Test", "desc")

    assertResult(bookmark.toString) {
      bookmarkNew.toString
    }
  }

  test("test basic bookmark creation from string") {
    val input: String = "BK:Test;Z:1;T:2;R:1;N:[1];DESC:desc;"

    val treeSet: util.TreeSet[Int] = new util.TreeSet[Int]()
    treeSet.add(1)

    val bookmark: Bookmark = BookmarkManager.buildBookmark(input, false)

    val bookmarkNew = new Bookmark(treeSet, 1, 2, 1,
      "Test", "desc")

    assertResult(bookmark.toString) {
      bookmarkNew.toString
    }
  }

  test("Test more advanced bookmark creation from string") {
    val input: String = "BK:name;Z:2;T:1;R:1;N:[1, 2];DESC:desc;"

    val treeSet: util.TreeSet[Int] = new util.TreeSet[Int]()
    treeSet.add(1)
    treeSet.add(2)

    val bookmark: Bookmark = BookmarkManager.buildBookmark(input, false)
    val bookmarkNew = new Bookmark(treeSet, 2, 1, 1,
      "name", "desc")

    assertResult(bookmark.toString) {
      bookmarkNew.toString
    }
  }

  test("test exceptions when input string is too long") {
    val input: String = "BK:Test;L:[1];N:[1];DESC:desc;Something:test"

    assertThrows[InvalidBookmarkException] {
      BookmarkManager.buildBookmark(input, false)
    }
  }

  test("test exceptions when input string is incorrect") {
    val input: String = "BK:Test;L:[a];N:[a];DESC:desc;"

    assertThrows[InvalidBookmarkException] {
      BookmarkManager.buildBookmark(input, false)
    }
  }

}
