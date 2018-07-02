package nl.tudelft.pl2.representation.kernels

/**
  * Simple [[DiscreteKernel]] for applying a normal
  * filter. The curve is created by taking the inverse of 2^^
  * of the absolute value of the offset.
  *
  * @param kernelRadius The amount of indices the kernel is
  *                     applied to the left and to the right
  *                     of the application index.
  */
class NormalKernel(val kernelRadius: Int)
    extends DiscreteKernel(kernelRadius) {

  override def weightAt(offset: Int): Double = {
    1 / Math.pow(2, Math.abs(offset))
  }
}
