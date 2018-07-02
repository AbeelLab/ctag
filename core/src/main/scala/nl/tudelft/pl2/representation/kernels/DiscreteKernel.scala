package nl.tudelft.pl2.representation.kernels

//scalastyle:off underscore.import
import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions._
//scalastyle:on underscore.import

import java.util

/**
  * A class used to transform lists of integers into
  * potentially smaller lists of doubles through kernel
  * application.
  *
  * The [[DiscreteKernel]] is a kernel applied over indices
  * in a discrete manner. Even when the position of application
  * is somewhere between indices, the closest index is taken to
  * be the application index.
  *
  * This particular implementation of the discrete kernel uses
  * a cropping technique for edge handling. More on this here:
  * https://en.wikipedia.org/wiki/Kernel_(image_processing).
  *
  * @param kernelRadius The amount of indices the kernel is
  *                     applied to the left and to the right
  *                     of the application index.
  */
abstract class DiscreteKernel(kernelRadius: Int) {

  /**
    * The list of weights over the given kernel radius
    * as calculated by [[weightAt()]].
    */
  private val weights =
    for (i <- -kernelRadius to kernelRadius) yield {
      (i, weightAt(i))
    }

  /**
    * Calculates the weight of a value at the given offset
    * away from the center index and returns it as a Double.
    *
    * @param offset The offset away from the center index.
    * @return [[Double]] representing the weight at center+offset.
    */
  def weightAt(offset: Int): Double

  /**
    * Applies the kernel for a given index in the given list
    * and returns the resulting value as a Double.
    *
    * @param list  The list over which to apply the kernel.
    * @param index The center index to apply the kernel at.
    * @return The value coming out of the kernel application.
    */
  def applyForListAt(list: util.List[Integer],
                     index: Integer): Double =
    weights.map { case (offset, weight) =>
      list(index + offset).doubleValue() * weight
    }.sum

  /**
    * Applies the kernel for a list of integers for each of
    * the indices in it, using cropping for edges.
    *
    * @param list The list of integers to apply the kernel over.
    * @return The result of applying the kernel over the given list
    *         of integers as a list of doubles.
    */
  def apply(list: util.List[Integer]): util.List[Double] =
    (for (i <- kernelRadius to list.size() - kernelRadius) yield {
      applyForListAt(list, i)
    }).toList.asJava

  /**
    * Applies the kernel for a list of integers for a number
    * of the indices in it, using cropping for edges. The number
    * of applications is given and the resulting list will have
    * this many entries. The result will be the chosen center
    * indices mapped to the values generated by them.
    *
    * @param list       The list to apply the kernel over.
    * @param targetSize The size preferred for the output list.
    * @return The mapping of application centers and the value
    *         resulting from kernel application.
    */
  def apply(list: util.List[Integer],
            targetSize: Integer): util.Map[Integer, Double] = {
    val firstKernel = kernelRadius
    val spaceBetweenKernels = (list.size() - kernelWidth).toDouble /
      (targetSize - 1).toDouble

    (for (i <- 0 until targetSize) yield {
      val kernelIndex = (firstKernel + spaceBetweenKernels * i).toInt
      (kernelIndex.asInstanceOf[Integer], applyForListAt(list, kernelIndex))
    }).toMap.asJava
  }

  /**
    * @return The width of the kernel calculated by its radius.
    */
  def kernelWidth: Int = kernelRadius * 2 + 1

}
