package failurewall.circuitbreaker

import akka.actor.Scheduler
import akka.pattern.{CircuitBreaker, CircuitBreakerOpenException}
import failurewall.util.FailurewallHelper
import failurewall.{Failurewall, FailurewallException}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * A circuit breaker with [[Failurewall]]'s interface implemented by Akka's [[CircuitBreaker]].
 * Calls wrapped in [[AkkaCircuitBreakerFailurewall]] are protected by circuit breaker mechanisms.
 * A circuit breaker prevents a failure form cascading any other failures.
 *
 * A circuit breaker is a state machine that has a following state.
 *
 * - CLOSED
 * - OPEN
 * - HALF-OPEN
 *
 * The `CLOSED` state is the ordinary state.
 * In this state, calls are executed and failure statistics are recorded.
 * When failure count exceeds the threshold, the circuit breaker switches into the `OPEN` state.
 *
 * The `OPEN` state is the state under problems.
 * In this state, `call` never executes the `body`
 * and returns an error immediately, so-called `fail-fast`.
 * The `OPEN` state prevents applications from accessing resources under problems.
 * After the configured timeout, the circuit breaker switches into the `HALF-OPEN` state.
 *
 * The `HALF-OPEN` state is the challenge phase.
 * In this state, the first `call` executes the `body` in order to test that the problem is fixed.
 * The other calls never execute bodies and return errors immediately.
 * If the first call succeeds, the circuit breaker switches to `CLOSED` state.
 * If the first call fails, the circuit breaker switches to `OPEN` state.
 *
 * @param circuitBreaker Akka's CircuitBreaker
 * @param feedback feedback logic to test the result
 * @param executor ExecutionContext
 */
final class AkkaCircuitBreakerFailurewall[A](circuitBreaker: CircuitBreaker,
                                             feedback: Try[A] => Boolean,
                                             implicit private[this] val executor: ExecutionContext)
  extends Failurewall[A, A] {

  /**
   * Wraps a call in this circuit breaker.
   * @param body call that needs protected
   * @return the result of `body` with this [[Failurewall]]
   *         a failed future with [[FailurewallException]] if a call is rejected
   */
  override def call(body: => Future[A]): Future[A] = {
    def recoverToTry[T](future: Future[T]): Future[Try[T]] = {
      future.map[Try[T]](Success.apply).recover { case e => Failure(e) }
    }

    val promise = Promise[A]()
    circuitBreaker.withCircuitBreaker {
      recoverToTry(promise.completeWith(FailurewallHelper.callSafely(body)).future).filter(feedback)
    }.recoverWith {
      case e: scala.concurrent.TimeoutException =>
        Future.failed(new FailurewallException("Timed out on the circuit breaker.", e))
      case e: CircuitBreakerOpenException =>
        Future.failed(new FailurewallException("The circuit breaker is on OPEN or HALF-OPEN state.", e))
      case _: NoSuchElementException => recoverToTry(promise.future)
    }.flatMap(Future.fromTry)
  }
}

object AkkaCircuitBreakerFailurewall {
  /**
   * Creates [[AkkaCircuitBreakerFailurewall]] with the given [[CircuitBreaker]].
   * The created [[AkkaCircuitBreakerFailurewall]] considers a body as successful if it is a successful future.
   * @param circuitBreaker circuit breaker for [[AkkaCircuitBreakerFailurewall]]
   * @param executor ExecutionContext for [[AkkaCircuitBreakerFailurewall]]
   */
  def apply[A](circuitBreaker: CircuitBreaker,
               executor: ExecutionContext): AkkaCircuitBreakerFailurewall[A] = {
    withFeedback[A](circuitBreaker, executor)(_.isSuccess)
  }

  /**
   * Creates [[AkkaCircuitBreakerFailurewall]] with the given parameters.
   * The created [[AkkaCircuitBreakerFailurewall]] considers a body as successful if it is a successful future.
   * @param scheduler scheduler used in the circuit breaker
   * @param maxFailures allowable failure times
   * @param callTimeout timeout for invoking a body
   * @param resetTimeout timeout for a attempt to return to the CLOSED state.
   * @param executor ExecutionContext for [[AkkaCircuitBreakerFailurewall]]
   */
  def apply[A](scheduler: Scheduler,
               maxFailures: Int,
               callTimeout: FiniteDuration,
               resetTimeout: FiniteDuration,
               executor: ExecutionContext): AkkaCircuitBreakerFailurewall[A] = {
    withFeedback[A](scheduler, maxFailures, callTimeout, resetTimeout, executor)(_.isSuccess)
  }

  /**
   * Creates [[AkkaCircuitBreakerFailurewall]] with the given [[CircuitBreaker]].
   * `feedback` should return false if the circuit breaker should regard the result as a failure
   * and increment its failure counters, e.g. connection errors.
   * On the other hand, `feedback` should return true if errors about business logic occurs
   * since such errors never recover even if waiting a long time.
   * @param circuitBreaker circuit breaker for [[AkkaCircuitBreakerFailurewall]]
   * @param executor ExecutionContext for [[AkkaCircuitBreakerFailurewall]]
   * @param feedback the logic to test whether a body is successful or not
   */
  def withFeedback[A](circuitBreaker: CircuitBreaker, executor: ExecutionContext)
                     (feedback: Try[A] => Boolean): AkkaCircuitBreakerFailurewall[A] = {
    new AkkaCircuitBreakerFailurewall[A](circuitBreaker, feedback, executor)
  }

  /**
   * Creates [[AkkaCircuitBreakerFailurewall]] with the given parameters.
   * `feedback` should return false if the circuit breaker should regard the result as false
   * and increment its failure counters, e.g. connection errors.
   * On the other hand, `feedback` should return true if errors about business logic occurs
   * since such errors never recover even if waiting a long time.
   * @param scheduler scheduler used in the circuit breaker
   * @param maxFailures allowable failure times
   * @param callTimeout timeout for invoking a body
   * @param resetTimeout timeout for a attempt to return to the CLOSED state.
   * @param executor ExecutionContext for [[AkkaCircuitBreakerFailurewall]]
   * @param feedback the logic to test whether a body is successful or not
   */
  def withFeedback[A](scheduler: Scheduler,
                      maxFailures: Int,
                      callTimeout: FiniteDuration,
                      resetTimeout: FiniteDuration,
                      executor: ExecutionContext)
                     (feedback: Try[A] => Boolean): AkkaCircuitBreakerFailurewall[A] = {
    val circuitBreaker = CircuitBreaker(scheduler, maxFailures, callTimeout, resetTimeout)
    withFeedback[A](circuitBreaker, executor)(feedback)
  }
}
