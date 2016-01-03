package failurewall.retry

import akka.actor.Scheduler
import akka.pattern.after
import failurewall.Failurewall
import failurewall.util.FailurewallHelper
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A [[Failurewall]] implementing the retry pattern.
 * Calls wrapped in [[AkkaRetryFailurewall]] can be retried if it fails.
 * [[AkkaRetryFailurewall]] can delay retrials by Akka's [[Scheduler]].
 *
 * [[AkkaRetryFailurewall]] is recommended for resources which may become temporarily unavailable.
 *
 * @param maxTrialTimes max trial times
 * @param strategy backoff strategy
 * @param scheduler Akka's [[Scheduler]] to delay retrials
 * @param feedback feedback logic to test whether this failurewall should retry calling or not
 * @param executor ExecutionContext
 */
final class AkkaRetryFailurewall[A](maxTrialTimes: Int,
                                    strategy: BackoffStrategy,
                                    scheduler: Scheduler,
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
        case _ => after(strategy.nextDelay(i), scheduler)(retry(i + 1))
      }
    }
    retry(1)
  }
}

object AkkaRetryFailurewall {
  /**
   * Creates [[AkkaRetryFailurewall]].
   * The created [[AkkaRetryFailurewall]] retries if the result is a failed [[Future]].
   * @param maxTrialTimes max trial times
   * @param strategy backoff strategy
   * @param scheduler Akka's [[Scheduler]] to delay retrials
   * @param executor ExecutionContext
   */
  def apply[A](maxTrialTimes: Int,
               strategy: BackoffStrategy,
               scheduler: Scheduler,
               executor: ExecutionContext): AkkaRetryFailurewall[A] = {
    withFeedback(maxTrialTimes, strategy, scheduler, executor) {
      case Success(_) => ShouldNotRetry
      case Failure(_) => ShouldRetry
    }
  }

  /**
   * Creates [[AkkaRetryFailurewall]] with the given feedback logic.
   * @param maxTrialTimes max trial times
   * @param strategy backoff strategy
   * @param scheduler Akka's [[Scheduler]] to delay retrials
   * @param executor ExecutionContext
   * @param feedback feedback logic to test whether this failurewall should retry calling or not
   */
  def withFeedback[A](maxTrialTimes: Int,
                      strategy: BackoffStrategy,
                      scheduler: Scheduler,
                      executor: ExecutionContext)
                     (feedback: Try[A] => RetryFeedback): AkkaRetryFailurewall[A] = {
    new AkkaRetryFailurewall[A](maxTrialTimes, strategy, scheduler, feedback, executor)
  }
}
