package nl.tudelft.pl2.data

import java.io.File
import java.nio.file.{Files, Path}
import java.util.Observer

import javafx.beans.property.{DoubleProperty, SimpleDoubleProperty}
import nl.tudelft.pl2.data.AssertionHelper.assertAndThrow
import nl.tudelft.pl2.data.gff.{Landmark, Trait, TraitMap}
import nl.tudelft.pl2.representation.graph.LoadingState
import nl.tudelft.pl2.representation.ui.UIHelper
import org.apache.logging.log4j.{Logger, LogManager}

/**
  * An exception used to clarify something went wrong
  * during parsing. Most often this includes some syntactical
  * error in the file that is being parsed. This is often
  * a clear sign of corruption.
  *
  * @param msg   The message explaining what went wrong and
  *              where it went wrong.
  * @param split The split [[String]] by which the exception
  *              was thrown.
  */
case class Gff3ParseException(msg: String,
                              split: Array[String])
    extends RuntimeException(s"$msg\n For split: ${split.mkString(", ")}")

/**
  * A parser for GFF3.0 files. GFF3.0 files are parsed assuming
  * they are in the format as specified in the following page:
  * https://github.com/The-Sequence-Ontology/Specifications/blob/master/gff3.md
  *
  * The parser generates a [[TraitMap]] object from the parsed
  * GFF file. The columns of the GFF file are separated in two
  * categories: landmarks and traits. Each row is represented by
  * a combination of the two. The landmarks contain information
  * that is used often to represent a certain type of feature of
  * the genome. The traits contain information on a certain instance
  * of that feature in the genome.
  */
object Gff3Parser extends {
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("Gff3Parser")

  /**
    * The property defining how far into the GFF loading process
    * the parser currently is.
    */
  var loadingProperty: DoubleProperty = new SimpleDoubleProperty(0.0)

  /**
    * The attributes mapping from tag to value.
    */
  private type Attributes = Map[String, String]

  /**
    * The delimiters used by the GFF3.0 specification.
    */
  private val MAIN_DELIMITER = '\t'
  private val ATTRIBUTE_DELIMITER = ';'
  private val TAG_VALUE_DELIMITER = '='

  /**
    * The number of columns in the GFF file.
    */
  private val N_COLUMNS = 9

  /**
    * Parses a single GFF3.0 file from the provided path and
    * returns the parsed genome traits as a [[TraitMap]].
    *
    * @param file The file to parse as a GFF3.0 file.
    * @return The [[TraitMap]] parsed from the GFF3.0 file.
    */
  def parse(file: Path): TraitMap = {
    val reader = Files.newBufferedReader(file)
    val traitMap: TraitMap = new TraitMap()

    reader.lines().forEach(line => {
      val split = line.split(MAIN_DELIMITER)
      split.head match {
        case "#" => // Do nothing for comments.
        case _ => traitMap.addOrElseAppend(parseGffLine(split))
      }
    })
    traitMap.computeColors()
    traitMap
  }

  /**
    * Parses a single GFF-file line. This requires the line split
    * by its main delimiter ([[MAIN_DELIMITER]]). It is checked
    * whether the given line-split adheres to the used GFF3.0
    * standard and thereafter a [[Landmark]],[[Trait]]-pair is
    * returned to be added to the [[TraitMap]].
    *
    * @param split The split that should be parsed to obtain
    *              [[Landmark]] and [[Trait]].
    */
  private def parseGffLine(split: Array[String]): (Landmark, Trait) = {
    assertAndThrow(split.length >= N_COLUMNS, Gff3ParseException(
      s"Expected at least $N_COLUMNS columns in a GFF3 file.\n"
          + s"\tbut there were ${split.length}.", split))

    //scalastyle:off magic.number
    val landmark = Landmark(split(0), split(1), split(2))
    val traitIn = Trait(split(3).toInt, split(4).toInt, split(5),
      split(6), split(7), parseAttributes(split(8)))
    //scalastyle:on magic.number

    (landmark, traitIn)
  }

  def parseAsync(file: Path, traitMap: TraitMap, observer: Observer): Unit = {
    Scheduler.schedule(() => try {
      parse(file, traitMap, observer)
    } catch {
      case e: Throwable => LOGGER.error("Exception: ", e)
    })
  }

  /**
    * Parses a single GFF3.0 file from the provided path and
    * returns the parsed genome traits as a [[TraitMap]].
    */
  def parse(path: Path, traitMap: TraitMap, observer: Observer): Unit = {
    val reader = Files.newBufferedReader(path)

    val file = new File(path.toUri)
    val size = file.length()
    var bytesRead = 0
    val milestone = if (size / Gfa1Parser.MILESTONES > 0) size / Gfa1Parser.MILESTONES else 1
    var passedMilestones: Long = 0

    reader.lines().forEach(line => {
      bytesRead += line.length

      val passed = bytesRead / milestone
      if (passed > passedMilestones) {
        passedMilestones = passed
        //scalastyle:off null
        observer.update(null, LoadingState.MILESTONE)
        //scalastyle:on null
      }

      val split = line.split(MAIN_DELIMITER)
      split.head match {
        case "#" => // Do nothing for comments.
        case _ => traitMap.addOrElseAppend(parseGffLine(split))
      }
    })
    traitMap.computeColors()
    UIHelper.getGraph.setTraitMap(traitMap)
    //scalastyle:off null
    observer.update(null, LoadingState.FULLY_LOADED_GFF)
    //scalastyle:on null
  }

  /**
    * Parses a list of attributes from the original String that
    * represents it. The String is split on [[ATTRIBUTE_DELIMITER]]
    * and each attribute is then parsed individually to do so.
    *
    * @param attrsString The String that should represent a list
    *                    of attributes.
    * @return The mapping of tags to values as parsed from the
    *         given attribute-string.
    */
  private def parseAttributes(attrsString: String): Attributes =
    attrsString.split(ATTRIBUTE_DELIMITER).map { attr =>
      parseAttribute(attr.split(TAG_VALUE_DELIMITER))
    }.toMap

  /**
    * Parses a single attribute from the attribute split
    * provided. This checks whether the given split is an
    * array of exactly two Strings: tag and value, and
    * returns this split as tag,value-pair.
    *
    * @param attrSplit The attribute as a split of a String,
    *                  split by [[TAG_VALUE_DELIMITER]].
    * @return The attribute represented as a pair of tag and
    *         value.
    */
  private def parseAttribute(attrSplit: Array[String]): (String, String) = {
    assertAndThrow(attrSplit.length == 2, Gff3ParseException(
      "Expected a single key and single value.\n" +
          s"\tbut there were ${attrSplit.length} elements after split", attrSplit))

    (attrSplit(0), attrSplit(1))
  }
}
