package nl.tudelft.pl2.representation.external

import com.lodborg.intervaltree.Interval
import com.lodborg.intervaltree.Interval.Bounded

/**
  * Case class to represent an interval of integers.
  *
  * @param lowerBound The lowest integer
  * @param upperBound The highest integer
  */
case class IntegerInterval(lowerBound: Long,
                           upperBound: Long)
  extends Interval[java.lang.Long](lowerBound, upperBound, Bounded.CLOSED) {

  def asPartOf(lower: Long, upper: Long): (Double, Double) = {
    val range = (upper - lower).toDouble
    ((lowerBound - lower).toDouble / range,
      (upperBound - lower).toDouble / range)
  }

  def asPartOf(other: IntegerInterval): (Double, Double) =
    asPartOf(other.lowerBound, other.upperBound)

  /**
    * Gets the intersection of this interval with the
    * other given interval if one exists, otherwise
    * None is returned.
    *
    * @param lower Lower bound of the other interval
    *              to check the intersection with.
    * @param upper Upper bound of the other interval
    *              to check the intersection with.
    * @return The [[IntegerInterval]] wrapped in an option
    *         that is the intersection, if one exists.
    */
  def intersectionWith(lower: Long, upper: Long): Option[IntegerInterval] =
    Some(IntegerInterval(Math.max(lower, lowerBound), Math.min(upper, upperBound)))
      .filter(ii => ii.lowerBound <= ii.upperBound)

  /**
    * Gets the intersection of this interval with the
    * other given interval if one exists, otherwise
    * None is returned.
    *
    * @param other The other interval to check the
    *              intersection with.
    * @return The [[IntegerInterval]] wrapped in an option
    *         that is the intersection, if one exists.
    */
  def intersectionWith(other: IntegerInterval): Option[IntegerInterval] =
    intersectionWith(other.lowerBound, other.upperBound)

  /**
    * Checks whether the interval intersects with the given other interval.
    *
    * @param other The other interval
    * @return Whether the intervals intersect
    */
  def intersects(other: IntegerInterval): Boolean = {
    val otherLower = intersects(other.lowerBound)
    val otherUpper = intersects(other.upperBound)
    val thisLower = other.intersects(lowerBound)
    val thisUpper = other.intersects(upperBound)
    otherLower || otherUpper || thisLower || thisUpper
  }

  /**
    * Check if the given int lies in the interval.
    *
    * @param int The int to check
    * @return Whether the interval contains the int
    */
  def intersects(int: Long): Boolean =
    int <= upperBound && int >= lowerBound

  override def create(): Interval[java.lang.Long] =
    IntegerInterval(Long.MinValue, Long.MaxValue)

  override def getMidpoint: java.lang.Long =
    (lowerBound + upperBound) / 2L
}
