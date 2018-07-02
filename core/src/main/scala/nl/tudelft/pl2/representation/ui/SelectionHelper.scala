package nl.tudelft.pl2.representation.ui

import java.util

import nl.tudelft.pl2.representation.NodeFinder
import nl.tudelft.pl2.representation.external.Node
import nl.tudelft.pl2.representation.ui.UIHelper.notifyObservers

import scala.collection.{immutable, mutable}
import scala.collection.JavaConverters.{asScalaSetConverter, setAsJavaSetConverter}

object SelectionHelper {

  /**
    * The nodes that are currently selected.
    */
  private var selectedNodes: mutable.Set[Int] = new mutable.TreeSet[Int]

  /**
    * Update the list with selected nodes.
    *
    * @param id The id of the node for which the
    *           selected state should be changed.
    */
  def toggleSelected(id: Int): Unit = {
    if (selectedNodes.contains(id)) {
      selectedNodes.remove(id)
    } else {
      selectedNodes.add(id)
    }
    UIHelper.notifyObservers()
  }

  /**
    * Adds one specific node to the list
    * of selected nodes.
    *
    * @param id The id of the node to be
    *           added.
    */
  def addSelectedNode(id: Int): Unit = {
    selectedNodes.add(id)
    notifyObservers()
  }

  def addSelectedNodes(nodes: util.Set[Int]): Unit = {
    selectedNodes = selectedNodes ++ nodes.asScala
    notifyObservers()
  }


  /**
    * This method removes a node from the
    * selected nodes list.
    *
    * @param nodes The node to be removed.
    */
  def removeSelectedNodes(nodes: util.Set[Node]): Unit = {
    nodes.forEach(el => selectedNodes.remove(el.id))
    notifyObservers()
  }

  /**
    * A method to get a list of selected nodes.
    *
    * @return An immutable set of id's of the
    *         nodes which are selected.
    */
  def getSelectedNodes: util.Set[Int] =
    (new immutable.TreeSet[Int] ++ this.selectedNodes).asJava

  /**
    * This method clears the selected nodes
    * and notifies the observers that it did.
    */
  def clearSelectedNodes(): Unit = {
    this.selectedNodes = new mutable.TreeSet[Int]()
    notifyObservers()
  }

}
