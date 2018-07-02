package nl.tudelft.pl2.data

import java.io.File
import java.util.logging.Logger

import javafx.application.Platform
import nl.tudelft.pl2.representation.graph.GraphHandle
import nl.tudelft.pl2.representation.ui.{ControllerManager, DefaultThreadCompleter}
import nl.tudelft.pl2.representation.ui.graph.CorruptedFileController
import org.apache.logging.log4j.LogManager

/**
  * Class for update completion
  *
  * @param graphFile The file to load
  */
class GraphUpdateCompleter(graphFile: File)
  extends DefaultThreadCompleter[GraphHandle] {

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("GraphUpdateCompleter")

  override def preemptedWithException(e: Throwable): Unit = {
    LOGGER.error("Exception caught when loading graph" +
      s"from file: $graphFile.", e)

    Platform.runLater(() =>
      ControllerManager.get(classOf[CorruptedFileController])
        .popup(graphFile))
  }
}
