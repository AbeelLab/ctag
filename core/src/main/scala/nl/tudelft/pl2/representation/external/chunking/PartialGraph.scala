package nl.tudelft.pl2.representation.external.chunking

import nl.tudelft.pl2.representation.external.{Edge, Node}

import scala.collection.JavaConverters.seqAsJavaListConverter

/**
  * Class that represents a part of the graph in memory.
  *
  * @param segments The segments in the part
  * @param links    The links in the part
  */
case class PartialGraph(segments: List[Node],
                        links: List[Edge]) {

  /**
    * Combines two partial graphs into a single
    * partial graph by merging the vertex and
    * edge lists of the two.
    *
    * @param other          The PartialGraph to merge
    *                       this graph with.
    * @param connectorLinks Links connecting this
    *                       PartialGraph with the
    *                       other PartialGraph.
    * @return PartialGraph representing the union
    *         of the two graphs.
    */
  def union(other: PartialGraph, connectorLinks: List[Edge] = Nil): PartialGraph =
    PartialGraph(segments ++ other.segments, links ++ other.links ++ connectorLinks)

  /**
    * Method for retrieving the nodes of the partial graph
    * in a Java List.
    *
    * @return List containing the nodes in the graph
    */
  def getJavaChunkNodes: java.util.List[Node] =
    segments.asJava
}
