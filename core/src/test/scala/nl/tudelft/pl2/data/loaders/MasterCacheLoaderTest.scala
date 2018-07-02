package nl.tudelft.pl2.data.loaders

import java.nio.file.{Path, Paths}
import java.util.{Observable, Observer}

import nl.tudelft.pl2.data.caches.MasterCache
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class MasterCacheLoaderTest extends FunSuite with BeforeAndAfter {
  private val observer = new Observer {
    override def update(o: Observable, arg: scala.Any): Unit = Unit
  }

  val ONLY_HEADERS_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("only_headers.gfa").toURI)

  val TB10S_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("TB10_small.gfa").toURI)


  val TB10M_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("TB10_medium.gfa").toURI)

  val TB10ML_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("TB10_medium_to_large.gfa").toURI)

  val TEST1_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("test1.gfa").toURI)

  val TEST3_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("test3.gfa").toURI)

  val TEST4_PATH: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("test4.gfa").toURI)


  var cache: MasterCache = _


  test("Load small mastercache") {
    try {
      cache = MasterCacheLoader.loadGraph(TB10S_PATH, observer)
      cache shouldBe a[MasterCache]
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TB10S_PATH)
    }
  }

  test("Load medium mastercache") {
    try {
      cache = MasterCacheLoader.loadGraph(TB10M_PATH, observer)
      cache shouldBe a[MasterCache]
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TB10M_PATH)
    }
  }

  test("Load large mastercache") {
    try {
      cache = MasterCacheLoader.loadGraph(TB10ML_PATH, observer)
      cache shouldBe a[MasterCache]
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TB10ML_PATH)
    }
  }

  test("Load only headers mastercache") {
    an[Exception] should be thrownBy MasterCacheLoader.loadGraph(ONLY_HEADERS_PATH, observer)
    MasterCacheLoader.clearFiles(ONLY_HEADERS_PATH)
  }


  test("Load test1 mastercache") {
    try {
      cache = MasterCacheLoader.loadGraph(TEST1_PATH, observer)
      cache shouldBe a[MasterCache]
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST1_PATH)
    }
  }

  test("Load test3 mastercache") {
    try {
      cache = MasterCacheLoader.loadGraph(TEST3_PATH, observer)
      cache shouldBe a[MasterCache]
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST3_PATH)
    }
  }

  test("Load test4 mastercache") {
    try {
      cache = MasterCacheLoader.loadGraph(TEST4_PATH, observer)
      cache shouldBe a[MasterCache]
    } finally {
      MasterCacheLoader.unload(cache)
      MasterCacheLoader.clearFiles(TEST4_PATH)
    }
  }
}
