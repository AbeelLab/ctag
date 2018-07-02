package nl.tudelft.pl2.representation.external

import nl.tudelft.pl2.data.Graph.Options

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * The external representation of a link.
  * This representation is seen and processed
  * by the view, containing requested objects
  * and properties in 'info' as a mapping from
  * strings to strings.
  *
  * @param from         The segment from which this
  *                     link originates.
  * @param to           The segment to which this
  *                     link points.
  */
case class Edge(from: Int,
                to: Int) {
  /**
    * Checks if the link is a dummy link.
    *
    * @return Whether the link is a dummy link
    */
  def isDummy: Boolean = false

  override def hashCode(): Int = {
    val value = (from + ":" + to).toCharArray
    var h = 0
    if (h == 0 && value.nonEmpty) {
      val temp = value

      for (i <- 0 until value.length) {
        h = 31 * h + temp(i)
      }
    }
    h
  }

  override def toString: String = "Edge:(" + from + "->" + to + ")"
}
