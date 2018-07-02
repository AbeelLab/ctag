package nl.tudelft.pl2.data.gff

import java.util

import javafx.scene.paint.Color
import nl.tudelft.pl2.data.IntervalTreeMap
import nl.tudelft.pl2.representation.external.IntegerInterval
import nl.tudelft.pl2.representation.ui.ColorHelper

import scala.collection.mutable
import scala.collection.JavaConverters.seqAsJavaListConverter

/**
  * Class representing the [[Trait]]s by [[Landmark]] as
  * loaded by the [[nl.tudelft.pl2.data.Gff3Parser]].
  *
  * In addition to storing mappings from [[Landmark]]s
  * to lists of [[Trait]]s, this class also keeps track of
  * what sequences, sources and types were seen in the feature
  * file.
  */
class TraitMap
  extends mutable.HashMap[String, IntervalTreeMap[java.lang.Long, (Landmark, Trait)]] {

  /**
    * The set of sources.
    */
  private val sources: mutable.Set[String] = mutable.Set()

  /**
    * The set of genome feature types.
    */
  private val types: mutable.Set[String] = mutable.Set()

  private val colors: util.HashMap[String, Color] = new util.HashMap[String,
    Color]()

  def getColors: util.HashMap[String, Color] = colors

  /**
    * Adds a landmark-trait combination to the [[TraitMap]].
    * When this [[TraitMap]] already contains the given
    * [[Landmark]] as key, the given [[Trait]] is added to
    * that [[Landmark]]'s accompanying value, otherwise, a
    * new value is created for the [[Landmark]].
    *
    * @param kv The key-value pair of [[Landmark]] and [[Trait]]
    *           to be added or appended to this [[TraitMap]].
    */
  def addOrElseAppend(kv: (Landmark, Trait)): Unit = {
    addLandmark(kv._1)
    if (!contains(kv._1.id)) {
      this (kv._1.id) = new IntervalTreeMap[java.lang.Long, (Landmark, Trait)]()
      if (kv._1.id.contains('.')) {
        this (kv._1.id.substring(0, kv._1.id.lastIndexOf('.'))) =
          new IntervalTreeMap[java.lang.Long, (Landmark, Trait)]()
      }
    }
    this (kv._1.id).put(IntegerInterval(kv._2.start, kv._2.end), kv)
  }

  /**
    * Add sequenceId, source and type of the given [[Landmark]]
    * to this [[TraitMap]]'s sets of these if they do not exist
    * there already.
    *
    * @param landmark [[Landmark]] of which the information
    *                 is to be added.
    */
  private def addLandmark(landmark: Landmark): Unit = {
    sources += landmark.source
    types += landmark.ty
  }

  override def clear(): Unit = {
    super.clear()
    sources.clear()
    types.clear()
  }

  def computeColors(): Unit = {
    types.forall(str => {
      val color = ColorHelper.generateColorFromString(str)
      ColorHelper.colorsUsed.add(color)
      colors.put(str, color)
      true
    })
  }

  /**
    * Get the list of feature pairs in the traitMap.
    *
    * @return A list containing feature pairs
    */
  def retrieveFeatureList(): util.List[FeaturePair] =
    values.flatMap(_.values.flatten).map((tuple) =>
      FeaturePair(tuple._1, tuple._2, IntegerInterval(
        tuple._2.start, tuple._2.end))).toList.asJava

}
