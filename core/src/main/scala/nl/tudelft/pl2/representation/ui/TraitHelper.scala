package nl.tudelft.pl2.representation.ui

import java.util
import java.util.{Observable, Observer}

import nl.tudelft.pl2.data.gff.Trait
import nl.tudelft.pl2.representation.ui.canvasSearch.SearchTree
import nl.tudelft.pl2.representation.ui.graph.DrawableTrait
import org.apache.logging.log4j
import org.apache.logging.log4j.LogManager

import scala.collection.mutable

/**
  * The singleton which stores the information regarding the location and
  * selected traits.
  */
object TraitHelper extends Observable {

  private val LOGGER: log4j.Logger = LogManager.getLogger("TraitHelper")

  val observers: mutable.Set[Observer] = mutable.Set()

  /**
    * The search-tree used to store the drawable traits.
    */
  private val searchTree: SearchTree[DrawableTrait] = new SearchTree[DrawableTrait]

  /**
    * Map that maps Traits to the DrawableTraits representing them.
    */
  private val drawableTraitMap: mutable.Map[Trait, DrawableTrait] = mutable.Map()

  /**
    * The selected trait by the user.
    */
  private var selectedTrait: DrawableTrait = _

  private var filteredTraits: util.HashSet[Trait] = _

  /**
    * @return Gets the current search tree which is in use.
    */
  def getSearchTree: SearchTree[DrawableTrait] = searchTree

  /**
    * Add a drawable trait to the RTree.
    *
    * @param drawableTrait The trait to add
    * @param startX        The start x coordinate
    * @param startY        The start y coordinate
    * @param endX          The end x coordinate
    * @param endY          The end y coordinate
    */
  def add(drawableTrait: DrawableTrait,
          startX: Integer, startY: Integer,
          endX: Integer, endY: Integer): Unit = {
    drawableTraitMap.put(drawableTrait.getTrait, drawableTrait)
    searchTree.add(drawableTrait, startX, startY, endX, endY)
  }

  /**
    * @return Gets the current drawable trait the user selected.
    */
  def getSelectedTrait: DrawableTrait = selectedTrait

  /**
    * Select the drawable trait linked to the given trait.
    *
    * @param featureTrait The trait to select
    * @return Whether the trait was selected or not
    */
  def selectTrait(featureTrait: Trait): Boolean = {
    if (drawableTraitMap.contains(featureTrait)) {
      selectedTrait = drawableTraitMap(featureTrait)
      notifyObservers()
      true
    } else {
      false
    }
  }


  /**
    * Resets the search-tree to an empty search-tree.
    */
  def clearTree(): Unit = {
    drawableTraitMap.clear()
    searchTree.clear()
  }

  /**
    * Looksup the drawable trait the user clicked on. This will cause the
    * [[selectedTrait]] to be set to this instance of [[DrawableTrait]].
    *
    * @param x The x coordinate the user clicked on.
    * @param y The y coordinate the user clicked on.
    */
  def search(x: Int, y: Int): Unit = {
    LOGGER.debug("Looking for traits!")
    searchTree.search(x, y, (trt) => {
      LOGGER.debug("Found trait!")
      selectedTrait = trt.asInstanceOf[DrawableTrait]

      notifyObservers()
    }, _ => {
      LOGGER.debug("Did not find any trait!")
    })
  }

  override def notifyObservers(): Unit = {
    observers.foreach(obs => obs.update(this, selectedTrait))
  }

  override def addObserver(o: Observer): Unit = {
    observers.add(o)
  }

  def setFilteredTraits(set: util.HashSet[Trait]): Unit = {
    filteredTraits = set
    notifyObservers()
  }

  def getFilteredTraits: util.HashSet[Trait] =
    filteredTraits


}
