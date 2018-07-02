package nl.tudelft.pl2.representation.ui.bookmark

import java.util
import java.nio.file.Path
import java.util.{Observable, Observer}

import nl.tudelft.pl2.data.storage.writers.BookmarkWriter

/**
  * This class is responsible for all the bookmarks in the program.
  * It is therefore capable of creating new bookmarks and adding
  * the newly created bookmarks to its list of bookmarks.
  */
object BookmarkManager extends Observable {


  /**
    * The location of the graph name.
    */
  private val GRAPH_INDEX:Int = 0

  /**
    * The location of the zoom level.
    */
  private val ZOOM_INDEX: Int = 1

  /**
    * The location of the translation index.
    */
  private val TRANSLATION_INDEX: Int = 2

  /**
    * The location of the translation index.
    */
  private val ROW_INDEX: Int = 3

  /**
    * The location of the description.
    */
  private val NODE_INDEX: Int = 4


  /**
    * The location of the description.
    */
  private val DESCRIPTION_INDEX: Int = 5

  /**
    * The bookmark writer used to write bookmarks.
    */
  private var bookmarkWriter: BookmarkWriter = _

  /**
    * The list of bookmarks currently loaded into the application.
    */
  private var bookmarks: util.ArrayList[Bookmark] = new util
  .ArrayList[Bookmark]()

  /**
    * A list of observers which are observing
    * this object.
    */
  private var observers: List[Observer] = Nil

  /**
    * Creates a new bookmark from the information which is needed
    * to create a bookmark.
    *
    * @param nodes     A Treeset of nodes over which the bookmark spans.
    * @param zoomLevel The level of zoom in the ui.
    * @param layer     The translation of the graph in the ui.
    * @param graphName The name of the graph this bookmark is used for.
    * @param desc      The description belonging to the bookmark.
    * @return Returns a new bookmark object after adding the bookmark
    *         to the list of bookmarks.
    */
  def buildBookmark(nodes: util.Set[Int],
                    zoomLevel: Double,
                    layer: Double,
                    row: Double,
                    graphName: String,
                    desc: String, write: Boolean): Bookmark = {
    val newBookmark: Bookmark = new Bookmark(nodes, zoomLevel, layer, row, graphName, desc)
    bookmarks.add(newBookmark)
    if (write) bookmarkWriter.writeSingleBookmark(newBookmark)
    notifyObservers()
    newBookmark
  }

  /**
    * Builds a bookmark from the given input string.
    * Throws an exception when an invalid string is
    * provided.
    *
    * @param input The string with the bookmark info.
    * @return The newly built Bookmark.
    */
  def buildBookmark(input: String, write: Boolean): Bookmark = {
    val info: Array[String] = input.split(";")

    if (info.length != 6) {
      throw InvalidBookmarkException("The bookmark did not contain " +
          "all the data needed to create a bookmark",
        "During parsing of the bookmark we did not find everything needed")
    }

    val graphName = info(GRAPH_INDEX).split(":")(1)
    val description = info(DESCRIPTION_INDEX).split(":")(1)
    var zoomLevel: Double = 0
    var layer: Double = 0
    var row: Double = 0
    try {
      zoomLevel = info(ZOOM_INDEX).split(":")(1).toDouble
      layer = info(TRANSLATION_INDEX).split(":")(1).toDouble
      row = info(ROW_INDEX).split(":")(1).toDouble
    } catch {
      case _: Exception => throw InvalidBookmarkException("The book mark did not contain " +
        "A correct number for either the zoom level or the translation", "During the parsing " +
          "of the numbers we found a character which was not a number")
    }

    val nodes: util.TreeSet[Int] = convertToTreeSet(info(NODE_INDEX).split(":")(1))

    buildBookmark(nodes, zoomLevel, layer, row, graphName, description, write)
  }

  /**
    * This method converts a string into a treeset.
    *
    * @param input The input string for the method with the elements.
    * @return A treeset filled with numbers.
    */
  private def convertToTreeSet(input: String): util.TreeSet[Int] = {
    val fixedInput = input.substring(1, input.length - 1)
    val items: Array[String] = fixedInput.split(", ")

    val set: util.TreeSet[Int] = new util.TreeSet[Int]()

    items.filterNot(_.isEmpty).foreach(f => {
      try {
        set.add(f.toInt)
      } catch {
        case _: Exception => throw InvalidBookmarkException("The bookmark did" +
            "not contain a number where a number was required. " + f + "is not a number.",
          "During the parsing of the numbers we found a character which was not a " +
              "number")
      }
    })
    set
  }

  /**
    * This creates a new bookmark writer to be used by the manager.
    * @param path The path to which the files should be written.
    */
  def createBookmarkWriter(path: Path): Unit = {
    this.bookmarkWriter = new BookmarkWriter(path)
  }

  /**
    * Allow the UI elements to register as an observer
    * of this object so that when this object is updated
    * they can be notified.
    *
    * @param observer The observer which is observing
    *                 this object.
    */
  def registerObserver(observer: Observer): Unit = {
    observers = observer :: observers
  }

  override def notifyObservers(): Unit = {
    observers.foreach(observer => {
      observer.update(this, bookmarks)
    })
  }

  /**
    * Gets the bookmarks from the manager
    * @return
    */
  def getBookmarks: util.ArrayList[Bookmark] = bookmarks

  /**
    * Removes a bookmark from the list and writes the new list to the
    * bookmark file
    * @param bookmark
    */
  def removeBookmark(bookmark: Bookmark): Unit = {
    this.bookmarks.remove(bookmark)
    bookmarkWriter.writeBookmarks(this.bookmarks)
  }

  /**
    * Updates the list of bookmarks to a new list read from the file.
    * @param newBookmarks The new list of bookmarks to be used.
    */
  def setBookmarks(newBookmarks: util.ArrayList[Bookmark]): Unit = {
    this.bookmarks = newBookmarks
    notifyObservers()
  }


}
