package failurewall.retry

import org.apache.pekko.actor.Scheduler
import org.apache.pekko.pattern.after
import failurewall.Failurewall
import failurewall.util.FailurewallHelper
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A [[Failurewall]] implementing the retry pattern.
 * Calls wrapped in [[PekkoRetryFailurewall]] can be retried if it fails.
 * [[PekkoRetryFailurewall]] can delay retrials by Pekko's [[Scheduler]].
 *
 * [[PekkoRetryFailurewall]] is recommended for resources which may become temporarily unavailable.
 *
 * @param maxTrialTimes max trial times
 * @param strategy backoff strategy
 * @param scheduler Pekko's [[Scheduler]] to delay retrials
 * @param feedback feedback logic to test whether this failurewall should retry calling or not
 * @param executor ExecutionContext
 */
final class PekkoRetryFailurewall[A](maxTrialTimes: Int,
                                    strategy: BackoffStrategy,
                                    scheduler: Scheduler,
                                    feedback: Try[A] => RetryFeedback,
                                    implicit private val executor: ExecutionContext)
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
        case _ => after(strategy.nextDelay(i), scheduler)(retry(i + 1))
      }
    }
    retry(1)
  }
}

object PekkoRetryFailurewall {
  /**
   * Creates [[PekkoRetryFailurewall]].
   * The created [[PekkoRetryFailurewall]] retries if the result is a failed [[Future]].
   * @param maxTrialTimes max trial times
   * @param strategy backoff strategy
   * @param scheduler Pekko's [[Scheduler]] to delay retrials
   * @param executor ExecutionContext
   */
  def apply[A](maxTrialTimes: Int,
               strategy: BackoffStrategy,
               scheduler: Scheduler,
               executor: ExecutionContext): PekkoRetryFailurewall[A] = {
    withFeedback(maxTrialTimes, strategy, scheduler, executor) {
      case Success(_) => ShouldNotRetry
      case Failure(_) => ShouldRetry
    }
  }

  /**
   * Creates [[PekkoRetryFailurewall]] with the given feedback logic.
   * @param maxTrialTimes max trial times
   * @param strategy backoff strategy
   * @param scheduler Pekko's [[Scheduler]] to delay retrials
   * @param executor ExecutionContext
   * @param feedback feedback logic to test whether this failurewall should retry calling or not
   */
  def withFeedback[A](maxTrialTimes: Int,
                      strategy: BackoffStrategy,
                      scheduler: Scheduler,
                      executor: ExecutionContext)
                     (feedback: Try[A] => RetryFeedback): PekkoRetryFailurewall[A] = {
    new PekkoRetryFailurewall[A](maxTrialTimes, strategy, scheduler, feedback, executor)
  }
}
