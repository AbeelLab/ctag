package nl.tudelft.pl2.representation.ui.graph

import java.util
import java.util.Collections

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.canvas.{Canvas, GraphicsContext}
import nl.tudelft.pl2.representation.graph.GraphHandle

/**
  * Data class for the data used and created by [[NodeDrawer]].
  *
  * @param graphicsContext The tool used to draw the nodes on
  *                        the canvas.
  * @param coordinates     The [[GraphCoordinateSystem]] used
  *                        to simulate a grid to draw on.
  */
class NodeDrawerData(val graphicsContext: GraphicsContext,
                     val coordinates: GraphCoordinateSystem) {

  /**
    * The graph handle currently used for drawing the graph.
    */
  var graph: GraphHandle = _

  /**
    * All the nodes which are currently in cache.
    */
  val nodesByLayerMap: util.Map[Integer, util.HashMap[Integer, DrawableAbstractNode]] =
    new util.HashMap()

  /**
    * This maps the name of a node to an instance
    * of that node so that they can be lookup
    * using their name.
    */
  val nodesByNameMap = Collections
    .synchronizedMap(new util.HashMap[Integer, DrawableAbstractNode])

  /**
    * SimpleDoubleProperty of the number of shownLayers.
    */
  val shownLayers: SimpleDoubleProperty =
    new SimpleDoubleProperty(NodeDrawer.SHOW_DEFAULT)

  /**
    * The set of layers currently loaded in this {@link NodeDrawer}.
    */
  val layersLoaded = new util.TreeSet[Integer]

  /**
    * The trait map used by the drawer.
    */
  var drawableTraitTreeMap: util.TreeMap[Integer, DrawableTrait] =
    new util.TreeMap()

  def canvas: Canvas = graphicsContext.getCanvas

}
