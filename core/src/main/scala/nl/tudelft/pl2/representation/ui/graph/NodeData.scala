package nl.tudelft.pl2.representation.ui.graph

import nl.tudelft.pl2.representation.external.Node

/**
  * This class class contains the data used by the [[DrawableNode]]
  * class in order to be able to draw a node on the gui.
  *
  * The data class is created because otherwise the [[DrawableNode]]
  * class would have too many arguments and therefore
  * violate the checkstyle rules.
  *
  * @param chunk  The chunk which this node will be representing.
  * @param layer  The starting x coordinate of the node.
  * @param row    The staring y coordinate of the node.
  * @param nodes  A map of nodes which maps the ID of a
  *               [[Node]] to its [[DrawableNode]] representation.
  */
case class NodeData(var chunk: Node,
                    var layer: Int,
                    var row: Int,
                    coordinates: GraphCoordinateSystem)
                   (val nodes: java.util.Map[Integer, DrawableAbstractNode])

