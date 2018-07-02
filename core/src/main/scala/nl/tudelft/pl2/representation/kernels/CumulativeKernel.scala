package nl.tudelft.pl2.representation.kernels

/**
  * Simple [[DiscreteKernel]] for applying a cumulative
  * filter. The cumulative is calculated by taking one
  * as weight for each offset.
  *
  * @param kernelRadius The amount of indices the kernel is
  *                     applied to the left and to the right
  *                     of the application index.
  */
class CumulativeKernel(val kernelRadius: Int)
  extends DiscreteKernel(kernelRadius) {

  override def weightAt(offset: Int): Double = 1.0
}
