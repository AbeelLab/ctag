package nl.tudelft.pl2.data

import java.util.concurrent.{Callable, Executors, ExecutorService, Future}

/**
  * Singleton object representing a scheduler.
  */
object Scheduler {
  /**
    * The thread pool used by the scheduler.
    */
  val pool: ExecutorService = Executors.newCachedThreadPool()

  /**
    * Schedule a runnable and return a future that can be used
    * to determine when the runnable is done.
    *
    * @param runnable The runnable to schedule
    * @return The future of the runnable
    */
  def schedule(runnable: Runnable): Future[_] =
    pool.submit(runnable)

  /**
    * Schedule a lambda and return a future that can be used
    * to determine when the runnable is done.
    *
    * @param lambda The lambda to schedule as a runnable.
    * @return The future of the runnable.
    */
  def schedule(lambda: () => Unit): Future[_] =
    pool.submit(new Runnable {
      override def run(): Unit = lambda()
    })

  /**
    * Schedule a callable and return a future that can be used
    * to determine when the callable is done.
    *
    * @param callable The callable to schedule
    * @return The future of the callable that will contain the return value
    */
  def schedule[T](callable: Callable[T]): Future[T] =
    pool.submit(callable)

  /**
    * Stop all threads and close the scheduler
    */
  def shutdown(): Unit =
    pool.shutdown()


}
