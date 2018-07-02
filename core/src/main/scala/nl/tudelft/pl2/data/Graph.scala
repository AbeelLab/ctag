package nl.tudelft.pl2.data

/**
  * Namespace wrapping auxiliary type definitions for
  * in-memory representation of graphs.
  */
object Graph {

  /**
    * A type representing a list/map of options
    * that can be (optionally) provided to each
    * line of a GFA1.0 file.
    */
  type Options = Map[String, (Char, String)]

  type Coordinates = Map[Integer, Long]
}
