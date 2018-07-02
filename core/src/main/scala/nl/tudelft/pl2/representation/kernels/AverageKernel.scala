package nl.tudelft.pl2.representation.kernels

/**
  * Simple [[DiscreteKernel]] for applying an averaging
  * filter. The average is calculated by taking one over
  * the total kernel width as the weight for each offset.
  *
  * @param kernelRadius The amount of indices the kernel is
  *                     applied to the left and to the right
  *                     of the application index.
  */
class AverageKernel(val kernelRadius: Int)
  extends DiscreteKernel(kernelRadius) {

  override def weightAt(offset: Int): Double =
    1 / kernelWidth.toDouble
}
