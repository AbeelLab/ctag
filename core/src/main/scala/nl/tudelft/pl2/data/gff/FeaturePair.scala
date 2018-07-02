package nl.tudelft.pl2.data.gff


import javafx.beans.property.SimpleBooleanProperty
import nl.tudelft.pl2.representation.external.IntegerInterval

/**
  * A data class representing a feature in a genome.
  * The feature is stored in the trait while the
  * landmark contains the identifying information.
  *
  * @param landmark    The landmark that contains the
  *                    id, source and type of the trait
  * @param feaureTrait A trait representing a feature in the feature pair.
  */
case class FeaturePair(landmark: Landmark,
                       feaureTrait: Trait,
                       interval: IntegerInterval) {

  /**
    * Boolean property is set by the tableview checkboxes.
    * If checked, the boolean property is set to true.
    */
  private var selected: SimpleBooleanProperty =
    new SimpleBooleanProperty(false)


  /**
    * The id of the landmark
    *
    * @return The id string of the landmark
    */
  def id(): String = landmark.id

  /**
    * The source of the landmark.
    *
    * @return The source string of the landmark
    */
  def source(): String = landmark.source

  /**
    * The type of the landmark
    *
    * @return The string containing the type
    */
  def ty(): String = landmark.ty

  /**
    * The index at which this feature starts.
    *
    * @return The start index of the feature
    */
  def start(): Integer = new Integer(feaureTrait.start)

  /**
    * The index at which this feature ends.
    *
    * @return The end index of the feature
    */
  def end(): Integer = new Integer(feaureTrait.end)

  /**
    * The score of the feature represented as a Double.
    *
    * @return
    */
  def score(): String = feaureTrait.score

  /**
    * The strand of the feature.
    *
    * @return The strand of the feature.
    */
  def strand(): String = feaureTrait.strand

  /**
    * The phase of the feature (0, 1, 2), -1 if none is provided.
    *
    * @return The phase of the feature
    */
  def phase(): String = feaureTrait.phase

  /**
    * The additional attributes provided for the feature.
    *
    * @return The attributes in the trait
    */
  def attributes(): Map[String, String] = feaureTrait.attributes

  def getString: String =
    id() + source() + ty() + start() + end() + score() + strand() + phase() + attrString()

  def attrString(): String = attributes().map(t => t._1 + "" + t._2).mkString("")

  /**
    * SelectedProperty getter.
    *
    * @return SimpleBooleanProperty
    */
  def selectedProperty: SimpleBooleanProperty = selected

}
