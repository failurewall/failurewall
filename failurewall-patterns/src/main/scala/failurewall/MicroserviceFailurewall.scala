package failurewall

import akka.actor.Scheduler
import failurewall.circuitbreaker.AkkaCircuitBreakerFailurewall
import failurewall.retry.RetriableFailurewall
import failurewall.semaphore.StdSemaphoreFailurewall
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * [[MicroserviceFailurewall]] implements patterns for microservices.
 * This [[Failurewall]] consists of the following Failurewalls.
 *
 * - circuit breaker
 * - semaphore
 * - retry pattern
 *
 * Circuit breakers prevent an application from continuing unavailable resources.
 * Semaphores keep resources usage constant.
 * The retry pattern recovers invocation from temporary failures transparently.
 */
object MicroserviceFailurewall {

  /**
   * A config for a circuit breaker.
   * @param scheduler scheduler used in the circuit breaker
   * @param maxFailures allowable failure times
   * @param callTimeout timeout for invoking a body
   * @param resetTimeout timeout for a attempt to return to the CLOSED state.
   * @param feedback the logic to test whether a body is successful or not
   */
  final case class CircuitBreakerConfig[A](scheduler: Scheduler,
                                           maxFailures: Int,
                                           callTimeout: FiniteDuration,
                                           resetTimeout: FiniteDuration,
                                           feedback: Try[A] => Boolean)

  /**
   * A config for semaphore.
   * @param permits the number of permits for Semaphore
   */
  final case class SemaphoreConfig(permits: Int)

  /**
   * A config about retrials.
   * @param maxTrialTimes max trial times
   * @param feedback the logic to test whether a body is successful or not
   */
  final case class RetryConfig[A](maxTrialTimes: Int, feedback: Try[A] => Boolean)

  /**
   * Creates a [[Failurewall]] with a circuit breaker, a semaphore and the retry pattern.
   * The created [[Failurewall]] considers a called body as successful if it is a successful future.
   * @param circuitBreakerConfig config for a circuit breaker
   * @param semaphoreConfig config for a semaphore
   * @param retryConfig config about retrials.
   * @param executor ExecutionContext for [[Failurewall]]
   */
  def apply[A](circuitBreakerConfig: CircuitBreakerConfig[A],
               semaphoreConfig: SemaphoreConfig,
               retryConfig: RetryConfig[A],
               executor: ExecutionContext): Failurewall[A, A] = {
    val circuitBreaker = AkkaCircuitBreakerFailurewall.withFeedback[A](
      circuitBreakerConfig.scheduler,
      circuitBreakerConfig.maxFailures,
      circuitBreakerConfig.callTimeout,
      circuitBreakerConfig.resetTimeout,
      executor
    )(circuitBreakerConfig.feedback)
    val semaphore = StdSemaphoreFailurewall[A](semaphoreConfig.permits, executor)
    val retry = RetriableFailurewall.withFeedback[A](
      retryConfig.maxTrialTimes,
      executor
    )(retryConfig.feedback)
    circuitBreaker compose semaphore compose retry
  }
}
