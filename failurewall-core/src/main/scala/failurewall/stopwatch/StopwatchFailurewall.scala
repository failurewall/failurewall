package failurewall.stopwatch

import failurewall.Failurewall
import failurewall.util.FailurewallHelper
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * A [[Failurewall]] to measure execution time.
 *
 * @param executor ExecutionContext
 */
final class StopwatchFailurewall[A](implicit private[this] val executor: ExecutionContext)
  extends Failurewall[A, (Try[A], FiniteDuration)] {
  /**
   * Measures the execution time of the given `body`.
   * `call` returns the result of `body` as `Try[A]` with the execution time.
   * When `body` succeeds, the `Try[A]` is `Success[A]`.
   * Otherwise, the `Try[A]` is `Failure[A]`
   * @param body call which execution time is be measured
   * @return the result of `body` and the execution time
   */
  override def call(body: => Future[A]): Future[(Try[A], FiniteDuration)] = {
    val start = System.nanoTime()
    FailurewallHelper
      .mapToTry(FailurewallHelper.callSafely(body))
      .map { _ -> (System.nanoTime() - start).nanos }
  }
}

object StopwatchFailurewall {
  /**
   * Creates a new [[StopwatchFailurewall]].
   * @param executor ExecutionContext for [[StopwatchFailurewall]]
   */
  def apply[A](executor: ExecutionContext): StopwatchFailurewall[A] = {
    new StopwatchFailurewall[A]()(executor)
  }
}
