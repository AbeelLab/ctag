package nl.tudelft.pl2.representation.ui

import java.util.concurrent.Callable

abstract class ThreadCompleter[T] {

  protected var result: Option[T] = None

  def wrap(f: Runnable): Runnable = () =>
    try {
      f.run()
      completedSuccessfully()
    } catch {
      case e: Throwable => preemptedWithException(e)
    }

  def wrap(f: Callable[T]): Callable[T] = () =>
    try {
      result = Some(f.call())
      completedSuccessfully(result.get)
      result.get
    } catch {
      case e: Throwable =>
        preemptedWithException(e)
        throw e
    }

  def completedSuccessfully(): Unit

  def completedSuccessfully(result: T): Unit

  def preemptedWithException(e: Throwable): Unit

}

class DefaultThreadCompleter[T] extends ThreadCompleter[T] {
  override def completedSuccessfully(): Unit = {}

  override def completedSuccessfully(result: T): Unit = {}
  override def preemptedWithException(e: Throwable): Unit = {}
}
