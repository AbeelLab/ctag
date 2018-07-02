package nl.tudelft.pl2.representation.ui

import java.util

import javafx.fxml.FXMLLoader
import org.apache.logging.log4j.{Logger, LogManager}


/**
  * Class that keeps a mapping of [[Controller]] types
  * to the (only) instance of each of these [[Controller]]s.
  *
  * This mapping is on-to-one, meaning there can only be one
  * instance of a single [[Controller]] type.
  *
  * @author Chris Lemaire
  */
object ControllerManager {
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("ControllerManager")

  /**
    * The mapping of controller types to their respective
    * instances. This acts as the map of singletons for
    * each of the controller types.
    */
  private val controllers = new util.HashMap[Class[_ <: Controller], Controller]

  /**
    * Adds a [[Controller]] to the map of controller
    * singletons mapping their types to their instances
    * that is kept in this class.
    *
    * @param controller The { @link Controller} instance
    *                   to add to the mapping.
    */
  def add(controller: Controller): Unit = {
    val clazz = controller.getClass
    if (!controllers.containsKey(clazz)) {
      controllers.put(clazz, controller)
      LOGGER.info("Successfully registered controller of type {}.", clazz)
    }
    else {
      LOGGER.error("Tried re-registering controller of type {}.", clazz)
    }
  }

  /**
    * Pre-loads a given FXML file, adding any potential
    * controllers used in this FXML file to the ControllerManager
    * indirectly.
    *
    * @param fxmlResource The resource path to the FXML file to load.
    */
  def preLoad(fxmlResource: String): Unit =
    new FXMLLoader().load(Thread.currentThread
      .getContextClassLoader
      .getResourceAsStream(fxmlResource))

  /**
    * Gets the controller of the given class-type from
    * the internal singleton mapping and returns it.
    *
    * @param clazz The class of the type of { @link Controller}
    *              to get.
    * @tparam T The type of { @link Controller} to get.
    * @return The { @link Controller} instance of type T.
    */
  def get[T <: Controller](clazz: Class[T]): T =
    controllers.get(clazz).asInstanceOf[T]

}
