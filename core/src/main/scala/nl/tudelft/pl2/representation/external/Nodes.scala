package nl.tudelft.pl2.representation.external

import nl.tudelft.pl2.data.Graph.{Coordinates, Options}
import nl.tudelft.pl2.representation.ui.UIHelper

import scala.collection.mutable
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * The external representation of a segment.
  * This representation is seen and processed
  * by the view, containing requested objects
  * and properties in 'info' as a mapping from
  * strings to strings. This Segment is not necessarily
  * an actually sequenced Segment, it might as well be
  * a chunk of the graph represented as a single Segment.
  *
  * @param id       The ID of the Segment.
  * @param name     The name of this Segment.
  * @param layer    The 'layer' of the graph this
  *                 Segment should be in.
  * @param content  The content represented by this
  *                 Segment, might be a nucleotide string
  *                 when representing an actual segment
  *                 in the graph.
  * @param outgoing The links going out of this
  *                 Segment.
  * @param incoming The links coming into this
  *                 Segment.
  * @param options  The additional information on
  *                 this link as requested during
  *                 the API call creating this object.
  */
//scalastyle:off covariant.equals
class Node(val id: Int,
           val name: String,
           val layer: Int,
           val content: String,
           val incoming: mutable.Buffer[Edge],
           val outgoing: mutable.Buffer[Edge],
           val options: Options,
           val genomeCoordinates: Coordinates) {

  /**
    * Creates a java Mapping of option names to option values
    * and returns it. This format can more easily be used than
    * the [[Options]] format as it is more portable.
    *
    * @return Mapping from option names to option values of
    *         all options for this [[Node]].
    */
  def getOptions: java.util.Map[String, String] = {
    if (options == null) {
      val scalaMap: Map[String, String] = Map()
      scalaMap.asJava
    } else {
      val scalaMap = options.map { case (s1: String, (_: Char, s2: String)) =>
        (s1, s2)
      }
      scalaMap.asJava
    }
  }

  /**
    * Returns if the node is a dummy segment.
    *
    * @return Whether the node is a dummy
    */
  def isDummy: Boolean = false

  override def hashCode: Int = {
    val value =
      (id + ":" + layer + ":" + name + ":" + content).toCharArray
    var h = 0
    if (h == 0 && value.nonEmpty) {
      val temp = value

      for (i <- 0 until value.length) {
        h = 31 * h + temp(i)
      }
    }
    h
  }

  /**
    * Reconstructs the GFA line string representing this node.
    *
    * @return A reconstructed GFA line representing this node
    *         as a Segment.
    */
  def gfaString: String = s"S\t$name\t$content\t*\t${
    options.map(kv => s"${kv._1}:${kv._2._1}:${kv._2._2}").mkString(" ")
  }"

  /**
    * Constructs a line-separated string representing this node.
    *
    * @return The String representing this node.
    */
  def lineSeparatedString: String =
    s"Segment '$name',\n" +
      s"in layer: $layer,\n" +
      s"with id: $id\n\n" +
      s"Content length: ${content.length}\n" +
      s"Content: $content\n\n" +
      s"Provided options:\n${
        options.map(o =>
          s"\t${o._1} of type ${o._2._1} = ${o._2._2}").mkString("\n")
      }\n" +
      s"Coordinates for genomes:\n${
        genomeCoordinates.map(gc =>
          s"\t${UIHelper.getGraph.getGenomes()(gc._1)}\t=> (" +
            s"${gc._2}, ${gc._2 + content.length})").mkString("\n")
      }"

  override def equals(obj: scala.Any): Boolean

  = {
    (obj.isInstanceOf[Node]
      && this.id == obj.asInstanceOf[Node].id
      )
  }

  override def toString: String

  = "Node:" + name
}


case class Bubble(override val id: Int,
                  override val name: String,
                  override val layer: Int,
                  override val content: String,
                  cHi: Char, cLo: Char,
                  override val incoming: mutable.Buffer[Edge],
                  override val options: Options,
                  end: Int) extends Node(id,
  name,
  layer,
  content,
  incoming,
  mutable.Buffer(Edge(id, end)),
  options,
  Map()) {

  override def equals(obj: Any): Boolean =
    obj.isInstanceOf[Bubble] && super.equals(obj)

}

case class Indel(override val id: Int,
                 override val name: String,
                 override val layer: Int,
                 override val content: String,
                 midContent: String,
                 override val incoming: mutable.Buffer[Edge],
                 override val options: Options,
                 end: Int) extends Node(id,
  name,
  layer,
  content,
  incoming,
  mutable.Buffer(Edge(id, end)),
  options,
  Map()) {
  override def equals(obj: Any): Boolean =
    obj.isInstanceOf[Indel] && super.equals(obj)
}

case class Chain(override val id: Int,
                 override val layer: Int,
                 override val incoming: mutable.Buffer[Edge],
                 override val outgoing: mutable.Buffer[Edge],
                 override val options: Options) extends Node(id,
  "",
  layer,
  "",
  incoming,
  outgoing,
  options,
  Map()) {
  override def equals(obj: Any): Boolean =
    obj.isInstanceOf[Chain] && super.equals(obj)
}
//scalastyle:on covariant.equals
