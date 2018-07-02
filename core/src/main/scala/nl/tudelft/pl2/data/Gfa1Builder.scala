package nl.tudelft.pl2.data

import java.io.FileWriter
import java.nio.file.Path

import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.representation.external.Node
import nl.tudelft.pl2.representation.external.components.DummyLink
import nl.tudelft.pl2.representation.graph.GraphHandle

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable

/**
  * Singleton object for building gfa1 files from node lists.
  */
object Gfa1Builder {
  /**
    * The default delimiter for gfa components.
    */
  final val DEFAULT_DELIMITER = '\t'

  /**
    * The delimiter for option parts.
    */
  final val OPTION_DELIMITER = ':'

  /**
    * Writes a new gfa file to the specified path location
    * using the nodes in the list.
    *
    * @param nodeList    The nodes to write to a gfa file
    * @param graphHandle The handle to get the headers from
    * @param path        The path to the location where the file should be written
    * @param addHeaders  Whether the headers should be included in the file
    */
  def writeGfaFileFromNodes(nodeList: java.util.List[Node],
                            graphHandle: GraphHandle,
                            path: Path,
                            addHeaders: Boolean): Unit = {
    val fileWrite: FileWriter = new FileWriter(path.toFile)

    fileWrite.write(
      if (addHeaders) {
        buildGfaFromNodesWHeaders(
          nodeList, graphHandle.retrieveCache().headers)
      } else {
        buildGfaFromNodes(nodeList)
      })
    fileWrite.flush()
    fileWrite.close()
  }

  /**
    * Build a gfa string from the given nodes with the headers included.
    *
    * @param nodeList The list of nodes to make a gfa string out of
    * @param headers  The headers to add to the file
    * @return The gfa string representing the nodes
    */
  def buildGfaFromNodesWHeaders(nodeList: java.util.List[Node],
                                headers: mutable.Buffer[Options]): String = {
    val stringBuilder: mutable.StringBuilder = new StringBuilder()
    headers.flatMap((headerMap) => {
      headerMap
    }).foreach((header: (String, (Char, String))) => {
      stringBuilder.append('H')
      stringBuilder.append(DEFAULT_DELIMITER)
      stringBuilder.append(header._1)
      stringBuilder.append(OPTION_DELIMITER)
      stringBuilder.append(header._2._1)
      stringBuilder.append(OPTION_DELIMITER)
      stringBuilder.append(header._2._2)
      stringBuilder.append('\n')
    })
    stringBuilder.append(buildGfaFromNodes(nodeList)).toString
  }

  /**
    * Build a gfa string from the given list of nodes.
    *
    * @param nodeList The list of nodes to convert to a gfa string
    * @return The gfa string representing the nodes
    */
  def buildGfaFromNodes(nodeList: java.util.List[Node]): String = {
    val list: mutable.Buffer[Node] = nodeList.asScala
      .sortWith(
        (node1, node2) => try {
          Integer.parseInt(node1.name) < Integer.parseInt(node2.name)
        } catch {
          case _: Throwable => node1.id < node2.id
        })
    val nodeMap: Map[Int, Node] =
      list.map(
        (node) => (node.id, node)
      ).toMap
    val edges: Set[(Int, Int)] =
      list.flatMap((node) => node.outgoing
        .filter((edge) => nodeMap.contains(edge.to))
        .map((edge) => if (!edge.isDummy) {
          (edge.from, edge.to)
        } else {
          val dummy = edge.asInstanceOf[DummyLink]
          (dummy.origin().from, dummy.origin().to)
        })
      ).toSet
    buildGfaString(
      list.filter((node: Node) => !node.name.equals("")).toList,
      nodeMap, edges).toString()
  }

  /**
    * Construct a stringBuilder containing the data needed for the gfa file.
    *
    * @param nodeList The list of nodes to write
    * @param nodeMap  A map that maps node ids to nodes
    * @param edgeSet  The set of edges that should be in the builder
    * @return The string builder containing the nodes data
    */
  private def buildGfaString(nodeList: List[Node],
                             nodeMap: Map[Int, Node],
                             edgeSet: Set[(Int, Int)]): StringBuilder = {
    val stringBuilder: mutable.StringBuilder = new StringBuilder()
    nodeList.foreach((node) => {
      buildNodeString(node, stringBuilder)
      edgeSet.filter(_._1 == node.id).toList.sortWith(
        (edge1, edge2) => edge2._2 <= edge1._2)
        .foreach(buildEdgeString(_, nodeMap, stringBuilder))
    })
    stringBuilder
  }

  /**
    * Build a segment line from a node.
    *
    * @param node          The node to make into a gfa segment
    * @param stringBuilder The builder to add this node to
    */
  private def buildNodeString(node: Node,
                              stringBuilder: StringBuilder): Unit = {
    stringBuilder.append('S')
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append(node.name)
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append(node.content)
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append('*')
    node.options.foreach((option: (String, (Char, String))) => {
      stringBuilder.append(DEFAULT_DELIMITER)
      stringBuilder.append(option._1)
      stringBuilder.append(OPTION_DELIMITER)
      stringBuilder.append(option._2._1)
      stringBuilder.append(OPTION_DELIMITER)
      stringBuilder.append(option._2._2)
    })
    stringBuilder.append('\n')
  }

  /**
    * Build a link from the given edge.
    *
    * @param edge          The edge to turn into a gfa link
    * @param nodeMap       A map that maps node ids to nodes
    * @param stringBuilder The builder to add this edge to
    */
  private def buildEdgeString(edge: (Int, Int),
                              nodeMap: Map[Int, Node],
                              stringBuilder: StringBuilder): Unit = {
    stringBuilder.append('L')
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append(nodeMap(edge._1).name)
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append('+')
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append(nodeMap(edge._2).name)
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append('+')
    stringBuilder.append(DEFAULT_DELIMITER)
    stringBuilder.append("0M\n")
  }
}

