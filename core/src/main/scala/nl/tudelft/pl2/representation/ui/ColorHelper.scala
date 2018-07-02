package nl.tudelft.pl2.representation.ui

import javafx.scene.paint.Color

import scala.collection.mutable

object ColorHelper {

  val colorsUsed: mutable.Set[Color] = mutable.Set()

  def generateColorFromString(str: String): Color = {
    val hash = (str.hashCode * 125).toString
    val length: Int = hash.length / 3
    val rInt = hash.splitAt(length)._1.toInt.abs
    val bInt = hash.splitAt(2 * length)._2.toInt.abs
    val gInt = hash.splitAt(length)._2.splitAt(length)._1.toInt.abs
    Color.rgb(rInt % 256, bInt % 256, gInt % 256)
  }
}
