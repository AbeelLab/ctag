package nl.tudelft.pl2.data.storage.writers

import java.io.{BufferedWriter, File, FileWriter}

import java.nio.file.Path
import java.util

import nl.tudelft.pl2.representation.ui.bookmark.Bookmark

/**
  * The bookmark writer used to write bookmark.
  * @param filePath The path to which the bookmarks are written.
  */
class BookmarkWriter(filePath: Path) extends AutoCloseable {

  /**
    * The file to which bookmarks are written.
    */
  val file: File = new File(filePath.toString)

  /**
    * The writer which is used to write the bookmark
    */
  val writer: BufferedWriter = new BufferedWriter(new FileWriter(file, true))

  /**
    * Clears the bookmark file and replaces it with new bookmarks.
    *
    * @param bookmarks The bookmarks which should be stored in the file.
    */
  def writeBookmarks(bookmarks: util.ArrayList[Bookmark]): Unit = {
    val newWriter: BufferedWriter = new BufferedWriter(new FileWriter(file,
      false))
    newWriter.write("")
    newWriter.close()
    bookmarks.forEach(bookmark => {
      writeBookmark(bookmark)
    })
    writer.flush()
  }

  /**
    * Writes a single bookmark to the file and flushes the writer.
    * @param bookmark The bookmark to be written.
    */
  def writeSingleBookmark(bookmark: Bookmark): Unit = {
    writeBookmark(bookmark)
    writer.flush()
  }

  /**
    * Writes a bookmark to the file.
    * @param bookmark The bookmark to be written.
    */
  private def writeBookmark(bookmark: Bookmark): Unit = writer.write(bookmark
      .toString + "\n")

  /**
    * Closes the writer.
    */
  override def close(): Unit = {
    writer.flush()
    writer.close()
  }
}
