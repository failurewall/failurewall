package failurewall.circuitbreaker

/**
 * Whether a circuit breaker should increment the error counter or not.
 */
sealed abstract class CircuitBreakerFeedback

/**
 * A circuit breaker should not increment its own error counter.
 *
 * - the call is successful
 * - the operation is failed due to a business logic error such as a validation error or a 404 error
 */
case object Available extends CircuitBreakerFeedback

/**
 * A circuit breaker should increment its own error counter.
 *
 * - the remote service or resource crashes
 * - timeout happens
 */
case object Unavailable extends CircuitBreakerFeedback
