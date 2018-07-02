package nl.tudelft.pl2.representation.ui

import java.io.File
import java.util.{Observable, Observer}
import java.util.logging.Logger
import java.util.prefs.Preferences

import javafx.stage.Stage
import javax.swing.filechooser.FileSystemView
import nl.tudelft.pl2.data.GraphUpdateCompleter
import nl.tudelft.pl2.representation.exceptions.NodeNotFoundException
import nl.tudelft.pl2.representation.external.Node
import nl.tudelft.pl2.representation.graph.GraphHandle
import nl.tudelft.pl2.representation.graph.loaders.ChunkedGraphLoader
import nl.tudelft.pl2.representation.ui.graph.{GraphController, NodeDrawer}
import org.apache.logging.log4j.LogManager

/**
  * This is a singleton object which will
  * help the UI with storing important information such as the graph
  * and the GFF file information as well later on.
  *
  * @author Cedric Willekens
  */
object UIHelper extends Observable {
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("UIHelper")

  /**
    * The primary stage of the application.
    */
  private var primaryStage: Stage = _

  /**
    * The preferences.
    */
  private val prefs: Preferences =
    Preferences.userRoot().node("ctag")

  /**
    * A list of observers which are observing
    * this object.
    */
  private var observers: List[Observer] = Nil

  /**
    * The default directory which should be opened
    * when opening a new gfa file.
    *
    * DEFAULT: The default directory is the users
    * document folder.
    */
  private val lastDir: String = prefs.get("last_output_dir",
    FileSystemView.getFileSystemView.getDefaultDirectory.getPath)

  /**
    * A graph representation of the graph view.
    */
  private var graph: GraphHandle = _

  /**
    * A graph loader instance.
    */
  private var graphLoader: ChunkedGraphLoader = _

  /**
    * This causes the graph to be updated to a new graph.
    *
    * @param graphFile The gfa file which contains a graph
    *                  representation.
    */
  def updateGraph(graphFile: File): Unit = {
    primaryStage.setTitle("C-TAG - " + graphFile.getName)

    graphLoader = new ChunkedGraphLoader()

    if (graph != null) {
      graph.unload()
    }

    graph = graphLoader.load(graphFile.toPath,
      new GraphUpdateCompleter(graphFile))

    LastOpenedHelper.addRecentGfaFile(graphFile.getAbsolutePath)
    prefs.put("last_output_dir", graphFile.getParent)

    notifyObservers()
  }

  /**
    * A method to get the graph from the UIHelper.
    *
    * @return The current instance of graph.
    */
  def getGraph: GraphHandle = graph

  /**
    * Get the graphLoader used to load the graph currently in the gui.
    *
    * @return The loader of the current graph
    */
  def getGraphLoader: ChunkedGraphLoader = graphLoader

  /**
    * A method to get the nodeDrawer from the UIHelper.
    *
    * @return The node drawer instance.
    */
  def drawer: NodeDrawer = ControllerManager
    .get(classOf[GraphController]).getDrawer


  /**
    * A method to get the dir which should be used
    * when opening the file chooser.
    *
    * @return An instance of [[File]] which represents
    *         the directory of this file.
    */
  def getOpeningDir: File = new File(lastDir)

  /**
    * This method moves the current graph
    * to a specific layer in that graph and notifies
    * the observers of the change.
    *
    * @param layer The layer we want to move to.
    */
  def goToLayer(layer: Double): Unit = {
    graph.moveToLayer(layer.toInt)
    drawer.setCentre(graph
      .getSegmentsFromLayer(layer.toInt)
      .iterator().next())

    notifyObservers()
  }

  /**
    * Moves the center of the graph to a specific node.
    *
    * @param id The id of the node to which should be moved.
    */
  def goToSegmentById(id: Int): Unit = {
    try {
      val layer: Int = this.graph.getSegmentLayer(id)
      goToLayer(layer)
    } catch {
      case _ : NodeNotFoundException =>
        LOGGER.debug("Node could not be found")
      case any : Any => throw any
    }
  }

  override def addObserver(o: Observer): Unit =
    observers = o :: observers

  override def notifyObservers(arg: scala.Any): Unit = {
    observers.foreach(observer => {
      observer.update(this, arg)
    })
  }

  override def notifyObservers(): Unit = {
    observers.foreach(observer => {
      observer.update(this, graph)
    })
  }

  /**
    * This method gets the observer of a
    * specific type.
    *
    * @param Class The class of which this observer
    *              must be.
    * @tparam T The type of the class.
    * @return An observer of instance T.
    */
  def getObserver[T](Class: Class[T]): T = {
    val newList: List[Observer] = observers.filter(obs => obs.getClass.equals(Class))
    newList.head.asInstanceOf[T]
  }

  /**
    * Add colors to the given nde.
    *
    * @param node The node to add tc color to
    * @return Whether a color was added
    */
  def addColorToNode(node: Node): Boolean =
    if (drawer != null) {
      drawer.getUpdater.addColorToNode(node)
    } else {
      false
    }

  /**
    * Remove colors from the given nde.
    *
    * @param node The node to remove tc color from
    * @return Whether a color was removed
    */
  def removeColorFromNode(node: Node): Boolean =
    try {
      drawer.getUpdater.removeColorFromNode(node)
    } catch {
      case _: Throwable => false
    }

  /**
    * Register the primary stage used to display the application to the
    * UIhelper.
    *
    * @param stage The stage used to display the application.
    */
  def registerPrimaryStage(stage: Stage): Unit = this.primaryStage = stage

}
