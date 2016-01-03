package failurewall.retry

/**
 * Whether the call should be retried or not.
 */
sealed abstract class RetryFeedback

/**
 * The call should be retried.
 * This should be used when the error may be transient.
 *
 * - a session is disconnected
 * - a service is temporary unavailable
 */
case object ShouldRetry extends RetryFeedback

/**
 * The call should not be retried.
 * This should be used when the error is not transient.
 *
 * - business logic error
 * - the resource that user requests is not found
 * - a service is under maintenance
 */
case object ShouldNotRetry extends RetryFeedback
