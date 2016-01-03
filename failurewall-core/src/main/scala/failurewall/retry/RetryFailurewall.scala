package failurewall.retry

import failurewall.Failurewall
import failurewall.util.FailurewallHelper
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A [[Failurewall]] implementing the retry pattern.
 * Calls wrapped in [[RetryFailurewall]] can be retried if it fails.
 *
 * [[RetryFailurewall]] is recommended for resources which may become temporarily unavailable.
 *
 * @param maxTrialTimes max trial times
 * @param feedback feedback logic to test whether this failurewall should retry calling or not
 * @param executor ExecutionContext
 */
final class RetryFailurewall[A](maxTrialTimes: Int,
                                feedback: Try[A] => RetryFeedback,
                                implicit private[this] val executor: ExecutionContext)
  extends Failurewall[A, A] {

  require(maxTrialTimes > 0, "`maxTrialTimes` should be greater than 0.")

  /**
   * Wraps a call which needs retrials if it fails.
   * @param body call that needs retrials if it fails
   * @return the last result of `body`
   */
  override def call(body: => Future[A]): Future[A] = {
    def retry(i: Int): Future[A] = {
      FailurewallHelper.mapToTry(FailurewallHelper.callSafely(body)).flatMap {
        case result if feedback(result) == ShouldNotRetry => Future.fromTry(result)
        case result if i == maxTrialTimes => Future.fromTry(result)
        case _ => retry(i + 1)
      }
    }
    retry(1)
  }
}

object RetryFailurewall {
  /**
   * Creates [[RetryFailurewall]] with the given max trial times.
   * The created [[RetryFailurewall]] retries if the result is a failed [[Future]].
   * @param maxTrialTimes max trial times
   * @param executor ExecutionContext for [[RetryFailurewall]]
   */
  def apply[A](maxTrialTimes: Int, executor: ExecutionContext): RetryFailurewall[A] = {
    withFeedback(maxTrialTimes, executor) {
      case Success(_) => ShouldNotRetry
      case Failure(_) => ShouldRetry
    }
  }

  /**
   * Creates [[RetryFailurewall]] with the given max trial times and feedback logic.
   * @param maxTrialTimes max trial times
   * @param executor ExecutionContext for [[RetryFailurewall]]
   * @param feedback feedback logic to test whether this failurewall should retry calling or not
   */
  def withFeedback[A](maxTrialTimes: Int, executor: ExecutionContext)
                     (feedback: Try[A] => RetryFeedback): RetryFailurewall[A] = {
    new RetryFailurewall[A](maxTrialTimes, feedback, executor)
  }
}
