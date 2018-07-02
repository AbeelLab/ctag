package nl.tudelft.pl2.representation

/**
  * A data class representing the view on
  * a Graph.
  *
  * @param layer The graph layer (x-coordinate)
  *              of view.
  * @param zoom  The level of zoom represented
  *              as an int.
  */
case class GraphPosition(layer: Int,
                         zoom: Int)
