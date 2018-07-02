package nl.tudelft.pl2.data.caches

import java.nio.file.{Path, Paths}
import java.util.{Observable, Observer}

import nl.tudelft.pl2.data.loaders.MasterCacheLoader
import nl.tudelft.pl2.representation.external.{Bubble, Edge, Indel, Node}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.mutable.ListBuffer

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class MasterCacheTest extends FunSuite with BeforeAndAfter {

  val TB10ML_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("TB10_medium_to_large.gfa").toURI)

  val TB10S_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("TB10_small.gfa").toURI)

  val TEST1_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("test1.gfa").toURI)

  val TEST2_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("test2.gfa").toURI)

  val TEST3_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("test3.gfa").toURI)

  val TEST5_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("test5.gfa").toURI)

  private val TB10ML_NUM_NODES = 256

  private var cache: MasterCache = _

  test("Retrieve a node by its ID") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TB10ML_PATH, observer)
      cache.setZoomLevel(0)
      val node = cache.retrieveNodeByID(1)
      node should be {
        new Node(1, "44", 1, "A",
          mutable.ListBuffer(Edge(1, 3)),
          mutable.ListBuffer(), Map("START" -> ('Z', "20217")),
          Map())
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TB10ML_PATH)
    }
  }


  test("Retrieve max node id") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TB10ML_PATH, observer)
      val nodeID = cache.retrieveMaxNodeID
      nodeID should be {
        TB10ML_NUM_NODES
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TB10ML_PATH)
    }
  }

  test("Update max node") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TEST1_PATH, observer)
      val newMax = 10
      cache.retrieveMaxNodeID should be {
        2
      }
      cache.updateMaxNodeID(newMax)
      cache.retrieveMaxNodeID should be {
        newMax
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST1_PATH)
    }
  }

  test("Retrieve headermap") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TEST5_PATH, observer)
      cache.retrieveHeaderMap should be {
        mutable.HashMap[String, String]("VN" -> "2.0",
          "ORI" -> "TKK-01-0015.fasta").asJava
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST5_PATH)
    }
  }

  test("Retrieve chunks by layer id") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TEST1_PATH, observer)
      val chunks = cache.retrieveChunksByLayer(2)
      chunks.head.layers() should be {
        List(0, 1, 2)
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST1_PATH)
    }
  }

  test("Retrieve bubble") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TEST2_PATH, observer)
      cache.setZoomLevel(1)
      val bub = cache.retrieveNodeByID(0)
      bub should be {
        Bubble(
          0, "name", 0, "content", 'A', 'T',
          ListBuffer[Edge](),
          Map("START" -> ('Z', "0"), "START" -> ('Z', "1")),
          3)
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST2_PATH)
    }
  }

  test("Retrieve bubble TB10S") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TB10S_PATH, observer)
      cache.setZoomLevel(1)
      val bub = cache.retrieveNodeByID(1)
      val end = 4
      bub should be {
        Bubble(1, "2", 1, "A", 'T', 'C',
          ListBuffer(Edge(0, 1)),
          Map("START" -> ('Z', "1553")),
          end)
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TB10S_PATH)
    }
  }

  test("Retrieve indel") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TEST3_PATH, observer)
      cache.setZoomLevel(1)
      val indel = cache.retrieveNodeByID(0)
      indel should be {
        Indel(0, "name",
          0, "content", "A",
          ListBuffer[Edge](),
          Map("START" -> ('Z', "0"), "START" -> ('Z', "1")),
          2)
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST3_PATH)
    }
  }

  test("Set zoom level") {
    try {
      val observer = new Observer {
        override def update(o: Observable, arg: scala.Any): Unit = Unit
      }
      cache = MasterCacheLoader.loadGraph(TEST1_PATH, observer)
      cache.setZoomLevel(-1) should be {
        0
      }
      cache.setZoomLevel(3) should be {
        2

      }
      cache.setZoomLevel(0) should be {
        0
      }
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST1_PATH)
    }
  }
}
