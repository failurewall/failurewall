package failurewall.semaphore

import failurewall.util.FailurewallHelper
import failurewall.{Failurewall, FailurewallException}
import java.util.concurrent.Semaphore
import scala.concurrent.{ExecutionContext, Future}

/**
 * A semaphore with [[Failurewall]]'s interface implemented by [[java.util.concurrent.Semaphore]].
 * The number of simultaneous invocation wrapped in [[StdSemaphoreFailurewall]] are restricted.
 *
 * @param semaphore Semaphore of Java's standard library
 * @param executor ExecutionContext
 */
final class StdSemaphoreFailurewall[A](semaphore: Semaphore,
                                       implicit private[this] val executor: ExecutionContext)
  extends Failurewall[A, A] {

  /**
   * Wraps a call in this semaphore.
   * If this execution can acquire permits, the given body is called.
   * @param body call needing a restriction for concurrency
   * @return the result of `body` if permits are available
   *         a failed future with [[FailurewallException]] if a call is rejected
   */
  override def call(body: => Future[A]): Future[A] = {
    semaphore.tryAcquire() match {
      case true =>
          val result = FailurewallHelper.call(body)
          result.onComplete { case _ => semaphore.release() }
          result
      case false => Future.failed(new FailurewallException("Failed acquiring a permit."))
    }
  }
}

object StdSemaphoreFailurewall {
  /**
   * Creates [[StdSemaphoreFailurewall]] with the given [[Semaphore]].
   * @param semaphore semaphore for [[StdSemaphoreFailurewall]]
   * @param executor ExecutionContext for [[StdSemaphoreFailurewall]]
   */
  def apply[A](semaphore: Semaphore, executor: ExecutionContext): StdSemaphoreFailurewall[A] = {
    new StdSemaphoreFailurewall[A](semaphore, executor)
  }

  /**
   * Creates [[StdSemaphoreFailurewall]] with the given number of permits.
   * @param permits the number of permits for [[Semaphore]]
   * @param executor ExecutionContext for [[StdSemaphoreFailurewall]]
   */
  def apply[A](permits: Int, executor: ExecutionContext): StdSemaphoreFailurewall[A] = {
    apply(new Semaphore(permits), executor)
  }
}
