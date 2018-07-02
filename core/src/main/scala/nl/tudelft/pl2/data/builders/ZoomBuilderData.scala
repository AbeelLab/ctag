package nl.tudelft.pl2.data.builders

import nl.tudelft.pl2.data.indexing.{Index, Indexer}
import nl.tudelft.pl2.data.storage.readers.CtagReader
import nl.tudelft.pl2.data.storage.writers.CtagWriter

import scala.collection.mutable

trait ZoomBuilderData {
  val indexer: Indexer
  val prevLvlIndex: Index
  val prevLvlReader: CtagReader
  val currLvlWriter: CtagWriter

  val nodeDataByID: mutable.SortedMap[Int, BuilderNode]

  val nodeDataByLayer: mutable.HashMap[Int, mutable.Set[Int]]
    with mutable.MultiMap[Int, Int]

  val chunksRetrieved: Array[Boolean]


}
