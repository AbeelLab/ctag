package nl.tudelft.pl2.data

import nl.tudelft.pl2.representation.external.IntegerInterval
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}

//scalastyle:off magic.number
@RunWith(classOf[JUnitRunner])
//scalastyle:off magic.number
class IntervalMapTest extends FunSuite with BeforeAndAfter {

  private var underTest: IntervalTreeMap[java.lang.Long, Integer] = _

  before {
    underTest = new IntervalTreeMap[java.lang.Long, Integer]()
  }

  test("Interval map with 3 disjoint unit-length ranges") {
    underTest.put(IntegerInterval(1L, 1L), 1)
    underTest.put(IntegerInterval(3L, 3L), 3)
    underTest.put(IntegerInterval(5L, 5L), 5)

    underTest.get(IntegerInterval(1, 1)).toList.flatten should be(List(1))
    underTest.get(IntegerInterval(3, 3)).toList.flatten should be(List(3))
    underTest.get(IntegerInterval(5, 5)).toList.flatten should be(List(5))
  }

  test("Interval map with 3 closely disjoint unit-length ranges") {
    underTest.put(IntegerInterval(1, 1), 1)
    underTest.put(IntegerInterval(2, 2), 2)
    underTest.put(IntegerInterval(3, 3), 3)

    underTest.get(IntegerInterval(1, 1)).toList.flatten should be(List(1))
    underTest.get(IntegerInterval(2, 2)).toList.flatten should be(List(2))
    underTest.get(IntegerInterval(3, 3)).toList.flatten should be(List(3))
  }

  test("Interval map with overlapping ranges 1") {
    underTest.put(IntegerInterval(1, 1), 1)
    underTest.put(IntegerInterval(1, 1), 2)
    underTest.put(IntegerInterval(1, 1), 3)

    underTest.get(IntegerInterval(1, 1)).toList.flatten should be(List(1, 2, 3))
  }

  test("Interval map with overlapping ranges 2") {
    underTest.put(IntegerInterval(1, 1), 1)
    underTest.put(IntegerInterval(1, 2), 2)
    underTest.put(IntegerInterval(1, 3), 3)

    underTest.valuesIntersecting(IntegerInterval(1, 1)) should be(Set(1, 2, 3))
    underTest.valuesIntersecting(IntegerInterval(2, 2)) should be(Set(2, 3))
    underTest.valuesIntersecting(IntegerInterval(3, 3)) should be(Set(3))
  }

  test("Interval map with overlapping ranges 3") {
    underTest.put(IntegerInterval(1, 1), 1)
    underTest.put(IntegerInterval(2, 2), 2)
    underTest.put(IntegerInterval(3, 3), 3)
    underTest.put(IntegerInterval(1, 3), 4)

    underTest.valuesIntersecting(IntegerInterval(1, 1)) should be(Set(1, 4))
    underTest.valuesIntersecting(IntegerInterval(2, 2)) should be(Set(2, 4))
    underTest.valuesIntersecting(IntegerInterval(3, 3)) should be(Set(3, 4))
  }

  test("Interval map adding a surrounded interval") {
    underTest.put(IntegerInterval(1, 5), 1)
    underTest.put(IntegerInterval(2, 2), 2)

    underTest.valuesIntersecting(IntegerInterval(1, 1)) should be(Set(1))
    underTest.valuesIntersecting(IntegerInterval(2, 2)) should be(Set(1, 2))
    underTest.valuesIntersecting(IntegerInterval(3, 5)) should be(Set(1))
  }

  test("Interval map adding a right intersecting interval") {
    underTest.put(IntegerInterval(3, 5), 3)
    underTest.put(IntegerInterval(2, 4), 2)
    underTest.put(IntegerInterval(1, 3), 1)

    underTest.valuesIntersecting(IntegerInterval(1, 1)) should be(Set(1))
    underTest.valuesIntersecting(IntegerInterval(2, 2)) should be(Set(2, 1))
    underTest.valuesIntersecting(IntegerInterval(3, 3)) should be(Set(3, 2, 1))
    underTest.valuesIntersecting(IntegerInterval(4, 4)) should be(Set(3, 2))
    underTest.valuesIntersecting(IntegerInterval(5, 5)) should be(Set(3))
  }

  test("Interval map adding a overlapping, left and right intersecting interval") {
    underTest.put(IntegerInterval(1, 5), 1)
    underTest.put(IntegerInterval(0, 2), 2)
    underTest.put(IntegerInterval(0, 6), 3)
    underTest.put(IntegerInterval(4, 6), 4)

    underTest.valuesIntersecting(IntegerInterval(0, 0)) should be(Set(2, 3))
    underTest.valuesIntersecting(IntegerInterval(1, 2)) should be(Set(1, 2, 3))
    underTest.valuesIntersecting(IntegerInterval(3, 3)) should be(Set(1, 3))
    underTest.valuesIntersecting(IntegerInterval(4, 5)) should be(Set(1, 3, 4))
    underTest.valuesIntersecting(IntegerInterval(6, 6)) should be(Set(3, 4))
  }

  test("Interval map getting should work") {
    underTest.put(IntegerInterval(1, 5), 1)
    underTest.put(IntegerInterval(0, 2), 2)
    underTest.put(IntegerInterval(0, 6), 3)
    underTest.put(IntegerInterval(4, 6), 4)

    underTest.valuesIntersecting(IntegerInterval(2L, 3L)) should be(Set(1, 2, 3))
    underTest.valuesIntersecting(IntegerInterval(0L, 6L)) should be(Set(1, 2, 3, 4))
    underTest.valuesIntersecting(IntegerInterval(2L, 4L)) should be(Set(1, 2, 3, 4))
    underTest.valuesIntersecting(IntegerInterval(3L, 4L)) should be(Set(1, 3, 4))
  }

}

//scalastyle:on magic.number
