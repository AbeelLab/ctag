package nl.tudelft.pl2.representation.ui.bookmark

import java.util

import nl.tudelft.pl2.representation.exceptions.CTagException


/**
  * The exception thrown when the bookmark is invalid.
  *
  * @param reason The reason why it is invalid.
  * @param loc    The location where the invalidness was found.
  */
case class InvalidBookmarkException(reason: String, loc: String)
  extends CTagException(reason, loc)

/**
  * A class representing a bookmark.
  *
  * @param highlightNodes A list of nodes which are highlighted.
  * @param graphName      The name of the graph the bookmark
  *                       belongs to.
  * @param description    The description of the bookmark.
  */
class Bookmark(val highlightNodes: util.Set[Int],
               val zoomLevel: Double,
               val layer: Double,
               val row: Double,
               val graphName: String,
               val description: String) {


  override def toString: String = {
    "BK:" + graphName + ";Z:" + zoomLevel + ";T:" + layer + ";R:" + row +
      ";N:" + highlightNodes.toString + ";DESC:" + description + ";"
  }


}
