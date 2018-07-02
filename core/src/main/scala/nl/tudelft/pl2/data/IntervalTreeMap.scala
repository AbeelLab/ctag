package nl.tudelft.pl2.data

import com.lodborg.intervaltree.{Interval, IntervalTree}

import scala.collection.mutable
import scala.collection.JavaConverters.asScalaSetConverter

class IntervalTreeMap[K <: Comparable[K], V]
  extends mutable.HashMap[Interval[K], mutable.Buffer[V]] {

  /**
    * The [[IntervalTree]] used to keep track of the
    * intervals currently accumulated in this [[IntervalTreeMap]].
    */
  private[this] val intervalTree = new IntervalTree[K]()

  /**
    * Finds all [[Interval]]s containing the given point
    * and returns the values identified by these [[Interval]]s
    * wrapped in a scala set. Executes in {{{O(log(n+m))}}}
    * time as stated in [[IntervalTree]].
    *
    * @param point The point to find containing intervals of.
    * @return The set of intervals containing the point
    *         queried for.
    */
  def valuesIntersecting(point: K): Set[V] =
    query(point).flatMap(this.apply)

  /**
    * Finds all [[Interval]]s containing the given point
    * and returns the resulting set of [[Interval]]s wrapped
    * in a scala set. Executes in {{{O(log(n+m))}}} time as
    * stated in [[IntervalTree]].
    *
    * @param point The point to find containing intervals of.
    * @return The set of intervals containing the point
    *         queried for.
    */
  def query(point: K): Set[Interval[K]] =
    intervalTree.query(point).asScala.toSet

  /**
    * Finds all [[Interval]]s intersecting the given [[Interval]]
    * and returns the values identified by these [[Interval]]s
    * wrapped in a scala set. Completes in {{{O(log(n))}}} time
    * as stated in [[IntervalTree]].
    *
    * @param interval The interval to find intersecting
    *                 intervals for.
    * @return The set of [[Interval]]s intersecting the
    *         [[Interval]] queried for.
    */
  def valuesIntersecting(interval: Interval[K]): Set[V] =
    query(interval).flatMap(this.apply)

  def entriesIntersecting(interval: Interval[K]): Set[(Interval[K], V)] =
    query(interval).flatMap(ii => apply(ii).map(e => (ii, e)))

  /**
    * Finds all [[Interval]]s intersecting the given interval
    * and returns the resulting set of [[Interval]]s wrapped
    * in a scala set. Executes in {{{O(log(n+m))}}} time as
    * stated in [[IntervalTree]].
    *
    * @param interval The interval to find intersecting
    *                 intervals for.
    * @return The set of [[Interval]]s intersecting the
    *         [[Interval]] queried for.
    */
  def query(interval: Interval[K]): Set[Interval[K]] =
    intervalTree.query(interval).asScala.toSet

  /**
    * Puts the given key to map to the given value wrapped in a
    * [[mutable.Buffer]].
    *
    * @param key   The interval to use as key for the pair.
    * @param value The value to put.
    * @return This [[IntervalTreeMap]].
    */
  def put(key: Interval[K],
          value: V): IntervalTreeMap.this.type =
    this += (key -> mutable.Buffer(value))

  override def +=(kv: (Interval[K], mutable.Buffer[V])): IntervalTreeMap.this.type = {
    if (!contains(kv._1)) {
      intervalTree.add(kv._1)
      super.+=(kv)
    } else {
      this(kv._1) ++= kv._2
      this
    }
  }

  /**
    * Removes the given key-value pair from this [[IntervalTreeMap]].
    * This also removes the interval from the underlying
    * [[IntervalTree]] and this map if no entries are left.
    *
    * @param kv The key-value pair to remove from this map.
    * @return This [[IntervalTreeMap]].
    */
  def -=(kv: (Interval[K], V)): IntervalTreeMap[K, V] =
    get(kv._1).map(_ -= kv._2)
      .filter(_.isEmpty)
      .map(_ => {
        intervalTree.remove(kv._1)
        this -= kv._1
      }).getOrElse(this)

  override def -=(key: Interval[K]): IntervalTreeMap.this.type = {
    intervalTree.remove(key)
    super.-=(key)
  }
}
