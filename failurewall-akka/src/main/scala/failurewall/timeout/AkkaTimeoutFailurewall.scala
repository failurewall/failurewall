package failurewall.timeout

import akka.actor.Scheduler
import failurewall.util.FailurewallHelper
import failurewall.{Failurewall, FailurewallException}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure

/**
 * A [[Failurewall]] to give up waiting for a call to be completed if that takes a long time.
 *
 * @param duration duration for timeouts
 * @param scheduler scheduler
 * @param onTimeout a call to be executed on timeout
 * @param executor ExecutionContext
 * @note The accuracy of timeout duration depends on Akka's [[Scheduler]].
 * @note AkkaTimeoutFailurewall almost ensures that calls will be completed within the time limit.
 *       However, that does not mean that calls are stopped or shut down.
 *       For instance, if [[AkkaTimeoutFailurewall]] wraps a http call and times out,
 *       the returned [[Future]] is completed but the http call is not cancelled and
 *       it is possible that the TCP connection leaks.
 *       Define `onTimeout` callback if necessary.
 */
final class AkkaTimeoutFailurewall[A](duration: FiniteDuration,
                                      scheduler: Scheduler,
                                      onTimeout: => Unit,
                                      implicit private[this] val executor: ExecutionContext)
  extends Failurewall[A, A] {

  /**
   * Invokes a call and returns error if it times out.
   * @param body call to need timeout
   * @return the result of `body` with this [[Failurewall]] or
   *         a failed future with [[FailurewallException]] if the call times out
   */
  override def call(body: => Future[A]): Future[A] = {
    val error = new FailurewallException(s"It took $duration and timed out.")
    val promise = Promise[A]()
    val cancellable = scheduler.scheduleOnce(duration) { promise.failure(error) }
    val result = Future.firstCompletedOf(Seq(FailurewallHelper.callSafely(body), promise.future))
    result.onComplete {
      case Failure(e) if e eq error => onTimeout
      case _ => cancellable.cancel()
    }
    result
  }
}

object AkkaTimeoutFailurewall {
  /**
   * Creates [[AkkaTimeoutFailurewall]].
   * @param duration duration for timeouts
   * @param scheduler scheduler
   * @param executor ExecutionContext for [[AkkaTimeoutFailurewall]]
   * @param onTimeout a call to be executed on timeout
   * @note AkkaTimeoutFailurewall almost ensures that calls will be completed within the time limit.
   *       However, that does not mean that calls are stopped or shut down.
   *       For instance, if [[AkkaTimeoutFailurewall]] wraps a http call and times out,
   *       the [[Future]] is completed but the http call is not cancelled and
   *       it is possible that the TCP connection leaks.
   *       Define `onTimeout` callback if necessary.
   */
  def apply[A](duration: FiniteDuration, scheduler: Scheduler, executor: ExecutionContext)
              (onTimeout: => Unit): AkkaTimeoutFailurewall[A] = {
    new AkkaTimeoutFailurewall[A](duration, scheduler, onTimeout, executor)
  }
}
