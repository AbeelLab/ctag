package nl.tudelft.pl2.data.storage.readers

import java.io.{BufferedReader, File, FileReader}
import java.nio.file.Path

import nl.tudelft.pl2.representation.ui.bookmark.{Bookmark, BookmarkManager}
import java.util

/**
  * The reader used in order to read bookmarks.
  */
object BookmarkReader {

  /**
    * Reads all the bookmarks which are in the file defiend by the parameter.
    * @param filePath The filepath from which the method should read.
    * @return The list of bookmarks which was read from the file.
    */
  def readBookmarks(filePath: Path): util.ArrayList[Bookmark] = {
    val bookmarks = new util.ArrayList[Bookmark]()
    val file = new File(filePath.toString)
    if (!file.exists()) {
      file.createNewFile()
    }
    val reader = new BufferedReader(new FileReader(file))

    reader.lines().forEach(line => bookmarks
        .add(BookmarkManager.buildBookmark(line.replace("\n", ""), false)))
    bookmarks
  }


}
