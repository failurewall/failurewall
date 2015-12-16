package failurewall

/**
 * An exception for [[Failurewall]]'s error.
 */
class FailurewallException(message: String = null,
                           cause: Throwable = null) extends RuntimeException(message, cause)
