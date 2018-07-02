package nl.tudelft.pl2.data

object AssertionHelper {

  /**
    * Asserts the given condition and, when evaluated to
    * false, it throws the given exception.
    *
    * @param cond      The condition that should be true
    *                  if and only if the assertion succeeds.
    * @param exception The exception to be thrown when the
    *                  assertion fails.
    */
  def assertAndThrow(cond: Boolean,
                     exception: => Exception): Unit =
    if (!cond) throw exception

}
