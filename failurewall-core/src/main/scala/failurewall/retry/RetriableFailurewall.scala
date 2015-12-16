package failurewall.retry

import failurewall.Failurewall
import failurewall.util.FailurewallHelper
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A [[Failurewall]] implementing the retry pattern.
 * Calls wrapped in [[RetriableFailurewall]] can be retried if it fails.
 *
 * [[RetriableFailurewall]] is recommended to access resources which may be temporarily unavailable.
 *
 * @param maxTrialTimes max trial times
 * @param feedback feedback logic to test whether the result is successful or not
 */
final class RetriableFailurewall[A](maxTrialTimes: Int,
                                    feedback: Try[A] => Boolean,
                                    implicit private[this] val executor: ExecutionContext)
  extends Failurewall[A, A] {

  require(maxTrialTimes > 0, "`maxTrialTimes` should be greater than 0.")

  /**
   * Wraps a call in this retriable [[Failurewall]].
   * @param body call that needs retrials if it fails
   * @return the result of `body` with this [[Failurewall]]
   */
  override def call(body: => Future[A]): Future[A] = {
    def recoverToTry[T](future: Future[T]): Future[Try[T]] = {
      future.map[Try[T]](Success.apply).recover { case e => Failure(e) }
    }
    def retry(i: Int): Future[A] = {
      recoverToTry(FailurewallHelper.call(body)).flatMap {
        case result if feedback(result) => Future.fromTry(result)
        case result if i == maxTrialTimes => Future.fromTry(result)
        case _ => retry(i + 1)
      }
    }
    retry(1)
  }
}

object RetriableFailurewall {
  /**
   * Creates [[RetriableFailurewall]] with the given times and feedback logic.
   * The created [[RetriableFailurewall]] retries if the result is a failure [[Future]].
   * @param maxTrialTimes max trial times
   * @param executor ExecutionContext for [[RetriableFailurewall]]
   */
  def apply[A](maxTrialTimes: Int, executor: ExecutionContext): RetriableFailurewall[A] = {
    withFeedback(maxTrialTimes, executor)(_.isSuccess)
  }

  /**
   * Creates [[RetriableFailurewall]] with the given times and feedback logic.
   * `feedback` should return false if errors are transient,
   * e.g. connection errors and temporary unavailable errors.
   * On the other hand, `feedback` should return true if errors are not transient,
   * e.g. business logic errors and maintenance errors.
   * @param maxTrialTimes max trial times
   * @param feedback feedback logic to test whether the result is successful or not
   * @param executor ExecutionContext for [[RetriableFailurewall]]
   */
  def withFeedback[A](maxTrialTimes: Int, executor: ExecutionContext)
                     (feedback: Try[A] => Boolean): RetriableFailurewall[A] = {
    new RetriableFailurewall[A](maxTrialTimes, feedback, executor)
  }
}
