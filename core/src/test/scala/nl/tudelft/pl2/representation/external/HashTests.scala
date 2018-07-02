package nl.tudelft.pl2.representation.external

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable.ListBuffer

/**
  * Class for testing hash code methods of the Segment and Link classes.
  */
@RunWith(classOf[JUnitRunner])
class HashTests extends FunSuite {

  /**
    * Verify that the toString methods of the
    * link and segment don't give exceptions.
    */
  test("Verify toString methods") {
    val segment = new Node(0, "1", 0, "A",
      ListBuffer(), ListBuffer(), Map(), Map())
    val link = Edge(segment.id, segment.id)
    segment.outgoing += link
    segment.incoming += link

    assertResult("Node:1") {
      segment.toString()
    }

    assertResult("Edge:(0->0)") {
      link.toString()
    }
  }

  /**
    * Verify the hash method of a Segment.
    */
  test("Verify that the hash method of a segment does not give an exception") {
    val segment1 = new Node(0, "1", 0, "A",
      ListBuffer(), ListBuffer(), Map(), Map())
    assertResult(segment1.hashCode()) {
      val segment2 = new Node(0, "1", 0, "A",
        ListBuffer(), ListBuffer(), Map(), Map())
      segment2.hashCode()
    }
  }

  /**
    * Verify the hash method of a Link.
    */
  test("Verify that the hash method of a link does not give an exception") {
    val segment1 = new Node(0, "1", 0, "A", ListBuffer(),
      ListBuffer(), Map(), Map())
    val segment2 = new Node(0, "1", 0, "A", ListBuffer(),
      ListBuffer(), Map(), Map())

    val link1 = Edge(segment1.id, segment2.id)
    assertResult(link1.hashCode()) {
      val link2 = Edge(segment1.id, segment2.id)
      link2.hashCode()
    }
  }

}
