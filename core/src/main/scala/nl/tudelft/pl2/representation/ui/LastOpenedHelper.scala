package nl.tudelft.pl2.representation.ui

import java.util.prefs.Preferences
import java.util.{Observable, Observer}

import nl.tudelft.pl2.representation.ui.menu.MenuBarController
import org.apache.logging.log4j.{LogManager, Logger}

/**
  * The object which stores all the
  * last opened files.
  */
object LastOpenedHelper extends Observable {

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("LastOpenedHelper")

  /**
    * The prefix for recent GFA file preferences.
    */
  private val RECENT_GFA_KEY = "recent_gfa_file_"

  /**
    * The prefix for recent GFF file preferences.
    */
  private val RECENT_GFF_KEY = "recent_gff_file_"
  /**
    * The preference store for this class.
    */
  private val prefs: Preferences =
    Preferences.userRoot().node("ctag")
  /**
    * A list of observers which are observing
    * this object.
    */
  private var observers: List[Observer] = Nil
  /**
    * The list of GFA files recently opened in order
    * of most recently opened.
    */
  private var recentGfaFiles: List[String] =
    readPreferences(RECENT_GFA_KEY)

  /**
    * The list of GFF files recently opened in order
    * of most recently opened.
    */
  private var recentGffFiles: List[String] =
    readPreferences(RECENT_GFF_KEY)

  /**
    * This method adds a file as the
    * most recent file.
    *
    * @param file The path to the file
    *             which needs to be stored.
    */
  def addRecentGfaFile(file: String): Unit = {
    recentGfaFiles = file :: recentGfaFiles
      .filterNot(_ == file)
      .take(MenuBarController.NUMBER_OF_RECENT_FILES - 1)

    writeAllPreferences()
    notifyObservers()
  }

  /**
    * This method adds a file as the
    * most recent file.
    *
    * @param file The path to the file
    *             which needs to be stored.
    */
  def addRecentGffFile(file: String): Unit = {
    recentGffFiles = file :: recentGffFiles
      .filterNot(_ == file)
      .take(MenuBarController.NUMBER_OF_RECENT_FILES - 1)

    writeAllPreferences()
    notifyObservers()
  }

  /**
    * Zips the recent GFA files with an index.
    *
    * @return A List of (index, GFA-file) tuples
    */
  def zippedGfaFiles: List[(Integer, String)] =
    recentGfaFiles.zipWithIndex.map(_.swap)
      .map(kv => kv._1.asInstanceOf[Integer] -> kv._2)

  /**
    * Zips the recent GFF files with an index.
    *
    * @return A List of (index, GFF-file) tuples
    */
  def zippedGffFiles: List[(Integer, String)] =
    recentGffFiles.zipWithIndex.map(_.swap)
      .map(kv => kv._1.asInstanceOf[Integer] -> kv._2)

  /**
    * This method gets the map of most recent files.
    *
    * @return The map with the most recent files.
    */
  def getMostRecentFiles: List[String] = recentGfaFiles

  /**
    * This method returns the most recent used file.
    *
    * @return The most recent filed opened.
    */
  def getMostRecentFile: String = recentGfaFiles.head

  override def addObserver(o: Observer): Unit = {
    observers = o :: observers
    o.update(this, this)
  }

  override def notifyObservers(arg: scala.Any): Unit = {
    super.notifyObservers()
    observers.foreach(observer => {
      observer.update(this, arg)
    })
  }

  override def notifyObservers(): Unit =
    observers.foreach(observer => {
      observer.update(this, this)
    })

  /**
    * Clears the preferences for this last opened helper.
    */
  private def clear(): Unit = {
    recentGfaFiles.indices.foreach(i =>
      prefs.remove(RECENT_GFA_KEY + i))
  }

  /**
    * Writes preferences with the given list of recent files
    * and the given keyPrefix before each key.
    *
    * @param files     The recent files.
    * @param keyPrefix The key prefix for each preference.
    */
  private def writePreferences(files: List[String],
                               keyPrefix: String): Unit =
    files.zipWithIndex.foreach { case (pref, i) =>
      LOGGER.info("Writing recent file: {} to {}", pref, i)
      prefs.put(keyPrefix + i, pref)
    }

  /**
    * This method writes all the files
    * to permanent storage using the
    * preferences api.
    */
  private def writeAllPreferences(): Unit = {
    clear()
    writePreferences(recentGfaFiles, RECENT_GFA_KEY)
    writePreferences(recentGffFiles, RECENT_GFF_KEY)
  }

  /**
    * This method reads the most recent files from
    * the permanent storage.
    *
    * @return The map which contains the most recent
    *         files accessed.
    */
  private def readPreferences(keyPrefix: String): List[String] =
    Stream.from(0, 1).map(i => {
      val keyName = keyPrefix + i
      val fileName = prefs.get(keyName, "")
      LOGGER.info("Reading most recent: {} for position {}", fileName, i)
      (i, fileName)
    }).takeWhile(_._2.nonEmpty).map(_._2).toList.distinct

}
