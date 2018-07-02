package nl.tudelft.pl2.data

import java.io.BufferedReader
import java.util.Observer
import java.util.logging.Logger

import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.data.builders.ZeroZoomBuilder
import nl.tudelft.pl2.data.AssertionHelper.assertAndThrow
import nl.tudelft.pl2.representation.graph.LoadingState
import org.apache.logging.log4j.LogManager

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
case class Gfa1ParseException(msg: String,
                              split: Array[String])
  extends RuntimeException(s"$msg\n For split: ${split.mkString(", ")}")

/**
  * A parser for reading/parsing GFA1.0 files.
  * To parse, the parser requires some input and an
  * output manager. This output managing is handled
  * through a Cache.
  *
  * In functioning, the parsing only splits lines and
  * checks the syntactical validity of that line. Each
  * line is identified to be either a segment, link,
  * header or comment and appropriate actions are
  * taken to pass that type's arguments to the cache.
  *
  * In this implementation, containments and paths are
  * ignored as these are not present in the population
  * graphs of interest.
  *
  * @author Chris Lemaire
  */
object Gfa1Parser {
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("Gfa1Parser")

  private final val TYPE_COL = 0

  private final val HEADER_OPTIONS_COL = 1

  private final val SEG_NAME_COL = 1
  private final val SEG_CONTENT_COL = 2
  private final val SEG_OPTIONS_COL = 4

  private final val LINK_FROM_COL = 1
  private final val LINK_FROM_REV_COL = 2
  private final val LINK_TO_COL = 3
  private final val LINK_TO_REV_COL = 4
  private final val LINK_OPTIONS_COL = 6

  /**
    * The amount of milestones while loading.
    */
  final val MILESTONES = 1000

  /**
    * Default delimiter used to separate columns in a
    * GFA1 file.
    */
  private final val DELIMITER = '\t'

  /**
    * Default delimiter used to separate tag, type and
    * value in an option.
    */
  private final val OPTION_DELIMITER = ':'

  /**
    * Parses a GFA1 file through a given buffered reader.
    * The GFA1 file is parsed for header options, segments,
    * links. The results are stored by the caching manager
    * passed through.
    *
    * @param reader  BufferedReader from which the GFA content
    *                is read.
    * @param builder The [[ZeroZoomBuilder]] used to store the read Graph
    * @return Graph object representing parsed content.
    */
  def parse(reader: BufferedReader,
            builder: ZeroZoomBuilder,
            observer: Observer,
            size: Long): Unit = {
    var lineSplit: Array[String] = new Array[String](0)

    LOGGER.debug("Parsing file with {} bytes", size)
    var bytesRead = 0
    val milestone = if (size / MILESTONES > 0) size / MILESTONES else 1
    var passedMilestones: Long = 0
    LOGGER.debug("A milestone is {} bytes", milestone)

    reader.lines().forEach(line => {
      if (!line.isEmpty) {
        bytesRead += line.length

        val passed = bytesRead / milestone
        if (passed > passedMilestones) {
          passedMilestones = passed
          //scalastyle:off null
          observer.update(null, LoadingState.MILESTONE)
          //scalastyle:on null
        }

        lineSplit = line.split(DELIMITER)
        lineSplit(TYPE_COL) match {
          case "H" => builder.registerHeader(
            parseOptionals(lineSplit, HEADER_OPTIONS_COL))
          case "S" => parseSegment(builder, lineSplit)
          case "L" => parseLink(builder, lineSplit)
          case "#" => // A comment, don't do anything
          case _ => throw Gfa1ParseException(
            s"Unrecognized line type '${lineSplit(TYPE_COL)}'\n" +
              s"for line: $line", lineSplit)
        }
      }
    })
    //scalastyle:off null
    observer.update(null, LoadingState.FULLY_PARSED)
    //scalastyle:on null
    LOGGER.debug("Parser parsed {} bytes", bytesRead)
  }

  /**
    * Parses a single option and returns a tuple
    * representing the tag, type and value of that
    * option.
    *
    * @param option String representing a single option.
    * @return Tuple representing an option object.
    */
  private def parseOption(option: String): (String, (Char, String)) = {
    val optionSplit = option.split(OPTION_DELIMITER)
    (optionSplit(0), (optionSplit(1)(0), optionSplit(2)))
  }

  /**
    * Parses the options provided from the end of a
    * line. Given is a line-split and the index from
    * which options can be read.
    *
    * @param split Line split from which options will
    *              have to be parsed.
    * @param from  Index from which on options should be
    *              parsed.
    * @return The map of options parsed.
    */
  private def parseOptionals(split: Array[String],
                             from: Int): Options = {
    split.drop(from).map(parseOption).toMap
  }

  /**
    * Parses a single Segment object from a line-split.
    *
    * @param builder The [[ZeroZoomBuilder]] used to store the
    *                graph.
    * @param split   The line split of the line including
    *                the Segment object to be parsed.
    * @return Segment object representing given line-split.
    */
  private def parseSegment(builder: ZeroZoomBuilder,
                           split: Array[String]): Unit = {
    assertAndThrow(split.length >= SEG_OPTIONS_COL, Gfa1ParseException(
      s"Expected at least $SEG_OPTIONS_COL arguments to segment line,\n" +
        s"\tbut there were only ${split.length}.", split))

    builder.registerNode(name = split(SEG_NAME_COL),
      content = split(SEG_CONTENT_COL),
      options = parseOptionals(split, SEG_OPTIONS_COL))
  }

  /**
    * Parses a single Link object from a line-split.
    *
    * @param split The line split of the line including
    *              the Link object to be parsed.
    * @return Link object representing given line-split.
    */
  private def parseLink(builder: ZeroZoomBuilder,
                        split: Array[String]): Unit = {
    assertAndThrow(split.length >= LINK_OPTIONS_COL, Gfa1ParseException(
      s"Expected at least $LINK_OPTIONS_COL arguments to link line,\n" +
        s"\tbut there were only ${split.length}.", split))

    assertAndThrow(split(LINK_FROM_REV_COL).length == 1 &&
      List("-", "+").contains(split(LINK_FROM_REV_COL)), Gfa1ParseException(
      "Expected single character for polarity of 'from' segment," +
        s"but was: ${split(LINK_FROM_REV_COL)}", split))
    assertAndThrow(split(LINK_TO_REV_COL).length == 1 &&
      List("-", "+").contains(split(LINK_TO_REV_COL)), Gfa1ParseException(
      "Expected single character for polarity of 'to' segment in join," +
        s"but was: ${split(LINK_TO_REV_COL)}", split))

    builder.registerEdge(from = split(LINK_FROM_COL),
      reversedFrom = split(LINK_FROM_REV_COL)(0) == '-',
      to = split(LINK_TO_COL),
      reversedTo = split(LINK_TO_REV_COL)(0) == '-',
      options = parseOptionals(split, LINK_OPTIONS_COL))
  }
}
