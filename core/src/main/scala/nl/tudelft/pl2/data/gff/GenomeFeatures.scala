package nl.tudelft.pl2.data.gff

/**
  * A data class representing a combination of sequence
  * ID, source of analysis and type of feature found.
  *
  * @param id     The sequence ID as a String.
  * @param source The source as a String.
  * @param ty     The type as a String.
  */
case class Landmark(id: String,
                    source: String,
                    ty: String)

/**
  * A data class representing a single instance of a certain
  * feature in a genome. The type of feature this is and its
  * source should be defined in an accompanying [[Landmark]].
  *
  * @param start      The index at which this feature starts.
  * @param end        The index at which this feature ends.
  * @param score      The score of the feature represented as a
  *                   Double.
  * @param strand     The strand of the feature.
  * @param phase      The phase of the feature (0, 1, 2), -1 if
  *                   none is provided.
  * @param attributes The additional attributes provided for
  *                   the feature.
  */
case class Trait(start: Int,
                 end: Int,
                 score: String,
                 strand: String,
                 phase: String,
                 attributes: Map[String, String])
